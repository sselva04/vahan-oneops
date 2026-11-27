package com.vahan.oneops.report;

import com.vahan.oneops.common.BadRequestException;
import com.vahan.oneops.machine.MachineHourDtos.EntryDto;
import com.vahan.oneops.machine.MachineHourEntry;
import com.vahan.oneops.machine.MachineHourRepository;
import com.vahan.oneops.machine.MachineHourService;
import com.vahan.oneops.project.PartStage;
import com.vahan.oneops.project.PartStageRepository;
import com.vahan.oneops.project.Rejection;
import com.vahan.oneops.project.RejectionRepository;
import com.vahan.oneops.purchase.RmPurchase;
import com.vahan.oneops.purchase.RmPurchaseRepository;
import com.vahan.oneops.report.ReportDtos.ExpenseInput;
import com.vahan.oneops.report.ReportDtos.MonthDto;
import com.vahan.oneops.tool.ToolItem;
import com.vahan.oneops.tool.ToolItemRepository;
import com.vahan.oneops.tool.ToolResharpen;
import com.vahan.oneops.tool.ToolResharpenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Monthly figures put together from the other modules. Nothing is stored here except the typed expenses.
 * Where a figure belongs to a month: machine hours by entry date, outsourced work by the date it was sent,
 * rejections by their date, tools by purchase / resharpening date, raw material by in-date.
 */
@Service
public class ReportService {

    private final MachineHourRepository machineHours;
    private final MachineHourService machineHourService;
    private final PartStageRepository stages;
    private final RejectionRepository rejections;
    private final ToolItemRepository tools;
    private final ToolResharpenRepository resharpens;
    private final RmPurchaseRepository rmPurchases;
    private final MonthlyExpenseRepository expenses;

    public ReportService(MachineHourRepository machineHours, MachineHourService machineHourService,
                         PartStageRepository stages, RejectionRepository rejections, ToolItemRepository tools,
                         ToolResharpenRepository resharpens, RmPurchaseRepository rmPurchases,
                         MonthlyExpenseRepository expenses) {
        this.machineHours = machineHours;
        this.machineHourService = machineHourService;
        this.stages = stages;
        this.rejections = rejections;
        this.tools = tools;
        this.resharpens = resharpens;
        this.rmPurchases = rmPurchases;
        this.expenses = expenses;
    }

    public static YearMonth parse(String month) {
        try {
            return YearMonth.parse(month.trim());
        } catch (RuntimeException e) {
            throw new BadRequestException("Month must look like 2026-11");
        }
    }

    /** Several consecutive months starting at `from` (count 1 to 24). */
    @Transactional(readOnly = true)
    public List<MonthDto> months(YearMonth from, int count) {
        if (count < 1 || count > 24) {
            throw new BadRequestException("Ask for 1 to 24 months");
        }
        Map<YearMonth, Acc> acc = new LinkedHashMap<>();
        for (int i = 0; i < count; i++) {
            acc.put(from.plusMonths(i), new Acc());
        }
        for (MachineHourEntry e : machineHours.findAll()) {
            Acc a = acc.get(YearMonth.from(e.getEntryDate()));
            if (a != null) {
                a.entries++;
                a.hours = a.hours.add(e.getBillableHours());
                a.turnover = a.turnover.add(e.getAmount());
                a.tax = a.tax.add(e.getTaxAmount());
            }
        }
        for (PartStage s : stages.findAll()) {
            if (s.getSupplierId() != null && s.getSentOn() != null && s.getJobQty() != null && s.getRatePerUnit() != null) {
                Acc a = acc.get(YearMonth.from(s.getSentOn()));
                if (a != null) {
                    a.outsourced = a.outsourced.add(s.getRatePerUnit().multiply(BigDecimal.valueOf(s.getJobQty())));
                }
            }
        }
        for (Rejection r : rejections.findAll()) {
            Acc a = r.getRejectedOn() == null ? null : acc.get(YearMonth.from(r.getRejectedOn()));
            if (a != null) {
                a.debit = a.debit.add(r.getDebitAmount());
            }
        }
        for (ToolItem t : tools.findAll()) {
            Acc a = t.getPurchasedOn() == null ? null : acc.get(YearMonth.from(t.getPurchasedOn()));
            if (a != null) {
                a.tools = a.tools.add(t.getCost());
            }
        }
        for (ToolResharpen r : resharpens.findAll()) {
            Acc a = r.getResharpenedOn() == null ? null : acc.get(YearMonth.from(r.getResharpenedOn()));
            if (a != null) {
                a.tools = a.tools.add(r.getCost());
            }
        }
        for (RmPurchase p : rmPurchases.findAll()) {
            Acc a = acc.get(YearMonth.from(p.getInDate()));
            if (a != null) {
                a.rm = a.rm.add(p.getTotalCost());
            }
        }
        List<MonthDto> out = new ArrayList<>();
        for (Map.Entry<YearMonth, Acc> en : acc.entrySet()) {
            String key = en.getKey().toString();
            Acc a = en.getValue();
            MonthlyExpense x = expenses.findById(key).orElse(null);
            BigDecimal consumables = x == null ? BigDecimal.ZERO : x.getConsumables();
            BigDecimal salary = x == null ? BigDecimal.ZERO : x.getSalary();
            BigDecimal totalExpense = consumables.add(salary);
            out.add(new MonthDto(key, a.entries, a.hours, a.turnover, a.tax, consumables, salary, totalExpense,
                    a.turnover.subtract(totalExpense), x == null ? null : x.getNotes(),
                    a.outsourced, a.debit, a.tools, a.rm));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public MonthDto month(YearMonth month) {
        return months(month, 1).get(0);
    }

    @Transactional
    public MonthDto saveExpenses(YearMonth month, ExpenseInput in) {
        String key = month.toString();
        MonthlyExpense x = expenses.findById(key).orElseGet(() -> new MonthlyExpense(key));
        x.set(in.consumables(), in.salary(), in.notes() == null || in.notes().isBlank() ? null : in.notes().trim());
        expenses.save(x);
        return month(month);
    }

    /** A CSV Excel can open: the month's figures, then every machine-hour entry of the month. */
    @Transactional(readOnly = true)
    public String csv(YearMonth month) {
        MonthDto m = month(month);
        StringBuilder sb = new StringBuilder();
        line(sb, "Month", m.month());
        line(sb, "Machine hours (billable)", m.machineHours());
        line(sb, "Turnover (machine hours, before tax)", m.turnover());
        line(sb, "Tax on machine hours", m.tax());
        line(sb, "Consumables", m.consumables());
        line(sb, "Salary", m.salary());
        line(sb, "Total expense", m.totalExpense());
        line(sb, "Profit (turnover - expense)", m.profit());
        line(sb, "Outsourced work sent this month", m.outsourcedCost());
        line(sb, "Supplier debits (rejections)", m.rejectionDebit());
        line(sb, "Tool spend (purchase + resharpening)", m.toolSpend());
        line(sb, "Raw material purchased", m.rmPurchases());
        line(sb, "Notes", m.notes());
        sb.append('\n');
        line(sb, "Date", "Customer", "Project", "Part", "Qty", "Design min", "Setting min", "Run min", "Idle min",
                "Hours", "Rate", "Amount", "Tax", "Total", "Remarks");
        List<EntryDto> entries = machineHourService.list(month.toString(), null).entries();
        for (EntryDto e : entries) {
            line(sb, e.entryDate(), e.customerName(), e.projectDrawingNo(), e.partDrawingNo(), e.quantity(),
                    e.designMinutes(), e.settingMinutes(), e.runMinutes(), e.idleMinutes(), e.billableHours(),
                    e.ratePerHour(), e.amount(), e.taxAmount(), e.total(), e.remarks());
        }
        return sb.toString();
    }

    private static void line(StringBuilder sb, Object... cells) {
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            Object c = cells[i];
            String text = c == null ? "" : c instanceof BigDecimal b ? b.toPlainString()
                    : c instanceof LocalDate d ? d.toString() : c.toString();
            // stop a cell starting with = + - @ from being read as a formula when opened in Excel
            if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0 && !(c instanceof Number)) {
                text = "'" + text;
            }
            if (text.contains(",") || text.contains("\"") || text.contains("\n")) {
                text = "\"" + text.replace("\"", "\"\"") + "\"";
            }
            sb.append(text);
        }
        sb.append('\n');
    }

    private static final class Acc {
        int entries;
        BigDecimal hours = BigDecimal.ZERO;
        BigDecimal turnover = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        BigDecimal outsourced = BigDecimal.ZERO;
        BigDecimal debit = BigDecimal.ZERO;
        BigDecimal tools = BigDecimal.ZERO;
        BigDecimal rm = BigDecimal.ZERO;
    }
}
