package com.vahan.oneops.machine;

import com.vahan.oneops.common.BadRequestException;
import com.vahan.oneops.common.NotFoundException;
import com.vahan.oneops.customer.Customer;
import com.vahan.oneops.customer.CustomerRepository;
import com.vahan.oneops.machine.MachineHourDtos.EntryDto;
import com.vahan.oneops.machine.MachineHourDtos.EntryInput;
import com.vahan.oneops.machine.MachineHourDtos.ListDto;
import com.vahan.oneops.machine.MachineHourDtos.SettingsDto;
import com.vahan.oneops.machine.MachineHourDtos.SettingsInput;
import com.vahan.oneops.machine.MachineHourDtos.Totals;
import com.vahan.oneops.project.ChildPart;
import com.vahan.oneops.project.ChildPartRepository;
import com.vahan.oneops.project.Project;
import com.vahan.oneops.project.ProjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Machine hours, the way the "VMC Hr Monitering" sheet works:
 * billable hours = (design + setting + run minutes) / 60, cost = hours x rate, tax = cost x tax rate.
 * Idle and power-cut minutes are recorded but not billed.
 */
@Service
public class MachineHourService {

    static final String RATE_KEY = "machine_hour_rate";
    static final String TAX_KEY = "machine_hour_tax_percent";
    private static final BigDecimal DEFAULT_RATE = new BigDecimal("450");
    private static final BigDecimal DEFAULT_TAX = new BigDecimal("18");

    private final MachineHourRepository entries;
    private final AppSettingRepository settings;
    private final ProjectRepository projects;
    private final ChildPartRepository parts;
    private final CustomerRepository customers;

    public MachineHourService(MachineHourRepository entries, AppSettingRepository settings,
                              ProjectRepository projects, ChildPartRepository parts, CustomerRepository customers) {
        this.entries = entries;
        this.settings = settings;
        this.projects = projects;
        this.parts = parts;
        this.customers = customers;
    }

    // ---------- settings ----------

    @Transactional(readOnly = true)
    public SettingsDto getSettings() {
        return new SettingsDto(read(RATE_KEY, DEFAULT_RATE), read(TAX_KEY, DEFAULT_TAX));
    }

    @Transactional
    public SettingsDto updateSettings(SettingsInput in) {
        write(RATE_KEY, in.ratePerHour());
        write(TAX_KEY, in.taxPercent());
        return getSettings();
    }

    private BigDecimal read(String key, BigDecimal fallback) {
        return settings.findById(key).map(s -> new BigDecimal(s.getValue())).orElse(fallback);
    }

    private void write(String key, BigDecimal value) {
        String text = value.stripTrailingZeros().toPlainString();
        settings.findById(key).ifPresentOrElse(s -> s.setValue(text), () -> settings.save(new AppSetting(key, text)));
    }

    // ---------- calculation ----------

    /** Billable hours, to 2 decimals (display only; the amount is worked out from the minutes). */
    public static BigDecimal billableHours(int design, int setting, int run) {
        return BigDecimal.valueOf(design + setting + run).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal amount(int design, int setting, int run, BigDecimal ratePerHour) {
        return ratePerHour.multiply(BigDecimal.valueOf(design + setting + run))
                .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }

    public static BigDecimal taxOn(BigDecimal amount, BigDecimal taxPercent) {
        return amount.multiply(taxPercent).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    // ---------- entries ----------

    /** month = "2026-11" (optional), projectId optional. Newest first. */
    @Transactional(readOnly = true)
    public ListDto list(String month, Long projectId) {
        YearMonth ym = null;
        if (month != null && !month.isBlank()) {
            try {
                ym = YearMonth.parse(month.trim());
            } catch (RuntimeException e) {
                throw new BadRequestException("Month must look like 2026-11");
            }
        }
        final YearMonth wanted = ym;
        List<MachineHourEntry> rows = (projectId == null
                ? entries.findAllByOrderByEntryDateDescIdDesc()
                : entries.findByProjectIdOrderByEntryDateDescIdDesc(projectId)).stream()
                .filter(e -> wanted == null || YearMonth.from(e.getEntryDate()).equals(wanted))
                .toList();
        List<EntryDto> dtos = toDtos(rows);
        BigDecimal hours = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO;
        for (MachineHourEntry e : rows) {
            hours = hours.add(e.getBillableHours());
            amount = amount.add(e.getAmount());
            tax = tax.add(e.getTaxAmount());
        }
        return new ListDto(dtos, new Totals(hours, amount, tax, amount.add(tax)));
    }

    @Transactional
    public EntryDto add(EntryInput in) {
        MachineHourEntry e = new MachineHourEntry(in.projectId());
        fill(e, in, null);
        return toDtos(List.of(entries.save(e))).get(0);
    }

    @Transactional
    public EntryDto update(Long id, EntryInput in) {
        MachineHourEntry e = entries.findById(id).orElseThrow(() -> new NotFoundException("Entry not found"));
        fill(e, in, e);
        return toDtos(List.of(e)).get(0);
    }

    @Transactional
    public void delete(Long id) {
        MachineHourEntry e = entries.findById(id).orElseThrow(() -> new NotFoundException("Entry not found"));
        entries.delete(e);
    }

    private void fill(MachineHourEntry e, EntryInput in, MachineHourEntry existing) {
        projects.findById(in.projectId()).orElseThrow(() -> new BadRequestException("Project not found"));
        if (in.childPartId() != null) {
            ChildPart part = parts.findById(in.childPartId())
                    .orElseThrow(() -> new BadRequestException("Part not found"));
            if (!part.getProject().getId().equals(in.projectId())) {
                throw new BadRequestException("That part does not belong to the chosen project");
            }
        }
        int design = zero(in.designMinutes());
        int setting = zero(in.settingMinutes());
        int run = zero(in.runMinutes());
        BigDecimal rate;
        BigDecimal tax;
        if (existing == null) {
            rate = in.ratePerHour() != null ? in.ratePerHour() : read(RATE_KEY, DEFAULT_RATE);
            tax = read(TAX_KEY, DEFAULT_TAX);
        } else {
            rate = in.ratePerHour() != null ? in.ratePerHour() : existing.getRatePerHour();
            tax = existing.getTaxPercent();
        }
        BigDecimal amount = amount(design, setting, run, rate);
        e.set(in.entryDate(), in.projectId(), in.childPartId(), in.quantity(), in.hardened(),
                design, setting, run, zero(in.idleMinutes()), zero(in.powerCutMinutes()),
                rate.setScale(2, RoundingMode.HALF_UP), tax.setScale(2, RoundingMode.HALF_UP),
                billableHours(design, setting, run), amount, taxOn(amount, tax),
                blankToNull(in.remarks()));
    }

    private List<EntryDto> toDtos(List<MachineHourEntry> rows) {
        Set<Long> projectIds = new HashSet<>();
        Set<Long> partIds = new HashSet<>();
        for (MachineHourEntry e : rows) {
            projectIds.add(e.getProjectId());
            if (e.getChildPartId() != null) {
                partIds.add(e.getChildPartId());
            }
        }
        Map<Long, Project> projectById = projects.findAllById(projectIds).stream()
                .collect(Collectors.toMap(Project::getId, Function.identity()));
        Map<Long, ChildPart> partById = parts.findAllById(partIds).stream()
                .collect(Collectors.toMap(ChildPart::getId, Function.identity()));
        Set<Long> customerIds = projectById.values().stream().map(Project::getCustomerId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> customerNames = customers.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getId, Customer::getName));
        return rows.stream().map(e -> {
            Project p = projectById.get(e.getProjectId());
            ChildPart part = e.getChildPartId() == null ? null : partById.get(e.getChildPartId());
            return new EntryDto(e.getId(), e.getEntryDate(),
                    e.getProjectId(), p == null ? null : p.getDrawingNo(), p == null ? null : p.getName(),
                    p == null || p.getCustomerId() == null ? null : customerNames.get(p.getCustomerId()),
                    e.getChildPartId(), part == null ? null : part.getDrawingNo(), part == null ? null : part.getName(),
                    e.getQuantity(), e.isHardened(),
                    e.getDesignMinutes(), e.getSettingMinutes(), e.getRunMinutes(),
                    e.getIdleMinutes(), e.getPowerCutMinutes(),
                    e.getBillableHours(), e.getRatePerHour(), e.getTaxPercent(),
                    e.getAmount(), e.getTaxAmount(), e.getAmount().add(e.getTaxAmount()), e.getRemarks());
        }).toList();
    }

    private static int zero(Integer v) {
        return v == null ? 0 : v;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
