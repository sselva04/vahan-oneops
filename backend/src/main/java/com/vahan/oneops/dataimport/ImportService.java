package com.vahan.oneops.dataimport;

import com.vahan.oneops.common.BadRequestException;
import com.vahan.oneops.common.ConflictException;
import com.vahan.oneops.dataimport.ImportDtos.BatchInfo;
import com.vahan.oneops.dataimport.ImportDtos.ExpenseItem;
import com.vahan.oneops.dataimport.ImportDtos.HourItem;
import com.vahan.oneops.dataimport.ImportDtos.PartItem;
import com.vahan.oneops.dataimport.ImportDtos.ProjectItem;
import com.vahan.oneops.dataimport.ImportDtos.PurchaseItem;
import com.vahan.oneops.dataimport.ImportDtos.RejectionItem;
import com.vahan.oneops.dataimport.ImportDtos.ResharpenItem;
import com.vahan.oneops.dataimport.ImportDtos.SectionResult;
import com.vahan.oneops.dataimport.ImportDtos.StageItem;
import com.vahan.oneops.dataimport.ImportDtos.ToolImportItem;
import com.vahan.oneops.machine.MachineHourService;
import com.vahan.oneops.project.RejectionService;
import com.vahan.oneops.purchase.RmPurchaseService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Dev-only data import (switched on with vahan.import.enabled=true). It loads rows with plain SQL, tags every row it
 * creates with the batch name, and can remove a whole batch again. Rows are matched by natural keys, and anything that
 * already exists is left alone and counted as skipped.
 */
@Service
@ConditionalOnProperty(name = "vahan.import.enabled", havingValue = "true")
public class ImportService {

    private static final Pattern BATCH = Pattern.compile("^[A-Za-z0-9_-]{1,50}$");
    private static final int MAX_PROBLEMS = 25;
    /** Tables that carry the import_batch column. */
    private static final List<String> TABLES = List.of("supplier", "project", "child_part", "part_stage", "rejection",
            "machine_hour_entry", "tool_item", "tool_resharpen", "rm_purchase", "monthly_expense");

    private final NamedParameterJdbcTemplate jdbc;
    private final MachineHourService machineHours;

    public ImportService(NamedParameterJdbcTemplate jdbc, MachineHourService machineHours) {
        this.jdbc = jdbc;
        this.machineHours = machineHours;
    }

    // ---------- batches ----------

    @Transactional(readOnly = true)
    public List<BatchInfo> batches() {
        Map<String, Map<String, Integer>> byBatch = new TreeMap<>();
        for (String table : TABLES) {
            jdbc.query("select import_batch b, count(*) c from " + table + " where import_batch is not null group by import_batch",
                    new MapSqlParameterSource(), rs -> {
                        byBatch.computeIfAbsent(rs.getString("b"), k -> new LinkedHashMap<>()).put(table, rs.getInt("c"));
                    });
        }
        return byBatch.entrySet().stream().map(e -> new BatchInfo(e.getKey(), e.getValue())).toList();
    }

    /** A batch name can only be started once; remove the old one first to load again. */
    @Transactional(readOnly = true)
    public void begin(String batch) {
        checkBatch(batch);
        for (BatchInfo b : batches()) {
            if (b.batch().equals(batch)) {
                throw new ConflictException("Batch '" + batch + "' already has data. Remove it first, or use another name.");
            }
        }
    }

    /** Removes what the batch created. A row somebody has since attached other data to is kept and counted. */
    @Transactional
    public Map<String, Integer> remove(String batch) {
        checkBatch(batch);
        MapSqlParameterSource p = new MapSqlParameterSource("b", batch);
        Map<String, Integer> removed = new LinkedHashMap<>();
        removed.put("rejection", jdbc.update("delete from rejection where import_batch = :b", p));
        removed.put("rm_purchase", jdbc.update("delete from rm_purchase where import_batch = :b", p));
        removed.put("machine_hour_entry", jdbc.update("delete from machine_hour_entry where import_batch = :b", p));
        removed.put("tool_resharpen", jdbc.update("delete from tool_resharpen where import_batch = :b", p));
        removed.put("tool_item", jdbc.update("delete from tool_item where import_batch = :b", p));
        removed.put("part_stage", jdbc.update("delete from part_stage where import_batch = :b "
                + "and not exists (select 1 from rejection r where r.part_stage_id = part_stage.id)", p));
        removed.put("child_part", jdbc.update("delete from child_part where import_batch = :b "
                + "and not exists (select 1 from part_stage s where s.child_part_id = child_part.id) "
                + "and not exists (select 1 from machine_hour_entry m where m.child_part_id = child_part.id) "
                + "and not exists (select 1 from rm_purchase r where r.child_part_id = child_part.id) "
                + "and not exists (select 1 from tool_item t where t.broken_part_id = child_part.id)", p));
        removed.put("project", jdbc.update("delete from project where import_batch = :b "
                + "and not exists (select 1 from child_part c where c.project_id = project.id) "
                + "and not exists (select 1 from machine_hour_entry m where m.project_id = project.id) "
                + "and not exists (select 1 from tool_item t where t.broken_project_id = project.id)", p));
        removed.put("monthly_expense", jdbc.update("delete from monthly_expense where import_batch = :b", p));
        removed.put("supplier", jdbc.update("delete from supplier where import_batch = :b "
                + "and not exists (select 1 from part_stage s where s.supplier_id = supplier.id) "
                + "and not exists (select 1 from rm_purchase r where r.supplier_id = supplier.id) "
                + "and not exists (select 1 from rejection j where j.supplier_id = supplier.id) "
                + "and not exists (select 1 from supplier_process sp where sp.supplier_id = supplier.id)", p));
        return removed;
    }

    // ---------- sections ----------

    @Transactional
    public SectionResult customers(List<String> names) {
        Problems pr = new Problems();
        Map<String, Long> existing = nameMap("select id, name from customer");
        int created = 0;
        for (String raw : names) {
            String name = tidy(raw);
            if (name == null || name.length() > 120) {
                pr.add("Customer name missing or too long: " + raw);
                continue;
            }
            if (existing.containsKey(name.toLowerCase())) {
                pr.skipped++;
                continue;
            }
            jdbc.update("insert into customer (name, active, created_at) values (:n, true, :t)",
                    new MapSqlParameterSource("n", name).addValue("t", now()));
            existing.put(name.toLowerCase(), 0L);
            created++;
        }
        return pr.result(created);
    }

    @Transactional
    public SectionResult suppliers(String batch, List<String> names) {
        checkBatch(batch);
        Problems pr = new Problems();
        Map<String, Long> existing = supplierKeys();
        int created = 0;
        for (String raw : names) {
            String name = tidy(raw);
            if (name == null || name.length() > 200) {
                pr.add("Supplier name missing or too long: " + raw);
                continue;
            }
            if (existing.containsKey(key(name))) {
                pr.skipped++;
                continue;
            }
            jdbc.update("insert into supplier (name, active, created_at, import_batch) values (:n, true, :t, :b)",
                    new MapSqlParameterSource("n", name).addValue("t", now()).addValue("b", batch));
            existing.put(key(name), 0L);
            created++;
        }
        return pr.result(created);
    }

    @Transactional
    public SectionResult projects(String batch, Long adminId, List<ProjectItem> items) {
        checkBatch(batch);
        Problems pr = new Problems();
        Map<String, Long> customers = nameMap("select id, name from customer");
        int created = 0;
        for (ProjectItem it : items) {
            String no = tidy(it.drawingNo());
            String name = tidy(it.name());
            if (no == null || no.length() > 60 || name == null) {
                pr.add("Project with missing or too long drawing number/name: " + it.drawingNo());
                continue;
            }
            if (exists("select count(*) from project where drawing_no = :k", no)) {
                pr.skipped++;
                continue;
            }
            Long customerId = it.customerName() == null ? null : customers.get(it.customerName().trim().toLowerCase());
            jdbc.update("insert into project (drawing_no, name, created_by, customer_id, created_at, import_batch) "
                            + "values (:d, :n, :u, :c, :t, :b)",
                    new MapSqlParameterSource("d", no).addValue("n", name.length() > 200 ? name.substring(0, 200) : name)
                            .addValue("u", adminId).addValue("c", customerId).addValue("t", now()).addValue("b", batch));
            created++;
        }
        return pr.result(created);
    }

    @Transactional
    public SectionResult parts(String batch, List<PartItem> items) {
        checkBatch(batch);
        Problems pr = new Problems();
        Map<String, Long> projects = new HashMap<>();
        int created = 0;
        for (PartItem it : items) {
            String no = tidy(it.drawingNo());
            if (no == null || no.length() > 60 || it.projectDrawingNo() == null || it.quantity() == null || it.quantity() < 1) {
                pr.add("Part with missing or invalid data: " + it.drawingNo());
                continue;
            }
            if (exists("select count(*) from child_part where drawing_no = :k", no)) {
                pr.skipped++;
                continue;
            }
            Long projectId = projects.computeIfAbsent(it.projectDrawingNo(), k -> id("select id from project where drawing_no = :k", k));
            if (projectId == null) {
                pr.add("Part " + no + ": project " + it.projectDrawingNo() + " not found");
                continue;
            }
            String name = tidy(it.name());
            jdbc.update("insert into child_part (project_id, drawing_no, name, quantity, created_at, import_batch) "
                            + "values (:p, :d, :n, :q, :t, :b)",
                    new MapSqlParameterSource("p", projectId).addValue("d", no)
                            .addValue("n", name == null ? no : name.length() > 200 ? name.substring(0, 200) : name)
                            .addValue("q", it.quantity()).addValue("t", now()).addValue("b", batch));
            created++;
        }
        return pr.result(created);
    }

    @Transactional
    public SectionResult stages(String batch, Long adminId, List<StageItem> items) {
        checkBatch(batch);
        Problems pr = new Problems();
        Map<String, Long> parts = new HashMap<>();
        Map<String, Long> masters = nameMap("select id, name from stage_master");
        Map<String, Long> suppliers = supplierKeys();
        int created = 0;
        for (StageItem it : items) {
            String stage = tidy(it.stageName());
            if (stage == null || it.sequenceNo() == null || it.partDrawingNo() == null) {
                pr.add("Stage with missing data on part " + it.partDrawingNo());
                continue;
            }
            Long partId = parts.computeIfAbsent(it.partDrawingNo(), k -> id("select id from child_part where drawing_no = :k", k));
            if (partId == null) {
                pr.add("Stage " + stage + ": part " + it.partDrawingNo() + " not found");
                continue;
            }
            Long supplierId = it.supplierName() == null ? null : suppliers.get(key(it.supplierName()));
            if (it.supplierName() != null && supplierId == null) {
                pr.add("Stage " + stage + " on " + it.partDrawingNo() + ": supplier '" + it.supplierName() + "' not found");
                continue;
            }
            if (it.jobQty() != null && it.jobQty() < 1 || it.ratePerUnit() != null && it.ratePerUnit().signum() < 0) {
                pr.add("Stage " + stage + " on " + it.partDrawingNo() + ": invalid quantity or rate");
                continue;
            }
            if (exists2("select count(*) from part_stage where child_part_id = :a and sequence_no = :b", partId, it.sequenceNo())) {
                pr.skipped++;
                continue;
            }
            Long masterId = masters.get(stage.toLowerCase());
            if (masterId == null) {
                jdbc.update("insert into stage_master (name, active) values (:n, true)", new MapSqlParameterSource("n", stage));
                masterId = id("select id from stage_master where name = :k", stage);
                masters.put(stage.toLowerCase(), masterId);
            }
            boolean done = "DONE".equalsIgnoreCase(it.status());
            LocalDate when = it.receivedOn() != null ? it.receivedOn() : it.sentOn();
            OffsetDateTime completedAt = done ? (when == null ? now() : when.atTime(LocalTime.NOON).atOffset(ZoneOffset.UTC)) : null;
            String remarks = it.remarks() == null || it.remarks().isBlank() ? null
                    : it.remarks().length() > 500 ? it.remarks().substring(0, 500) : it.remarks();
            jdbc.update("insert into part_stage (child_part_id, stage_master_id, sequence_no, status, created_at, supplier_id, "
                            + "job_qty, rate_per_unit, pre_inspection_done, final_inspection_done, remarks, sent_on, received_on, "
                            + "started_at, completed_at, completed_by, import_batch) "
                            + "values (:part, :master, :seq, :status, :t, :sup, :qty, :rate, :pre, :fin, :rem, :sent, :recv, "
                            + ":started, :completed, :by, :b)",
                    new MapSqlParameterSource("part", partId).addValue("master", masterId).addValue("seq", it.sequenceNo())
                            .addValue("status", done ? "DONE" : "NOT_STARTED").addValue("t", now()).addValue("sup", supplierId)
                            .addValue("qty", it.jobQty()).addValue("rate", it.ratePerUnit()).addValue("pre", it.preInspectionDone())
                            .addValue("fin", it.finalInspectionDone()).addValue("rem", remarks)
                            .addValue("sent", it.sentOn()).addValue("recv", it.receivedOn())
                            .addValue("started", completedAt).addValue("completed", completedAt)
                            .addValue("by", done ? adminId : null).addValue("b", batch));
            created++;
        }
        return pr.result(created);
    }

    @Transactional
    public SectionResult machineHours(String batch, List<HourItem> items) {
        checkBatch(batch);
        Problems pr = new Problems();
        BigDecimal tax = machineHours.getSettings().taxPercent();
        int created = 0;
        for (HourItem it : items) {
            if (it.entryDate() == null) {
                pr.add("Machine-hour row without a date (" + label(it.partDrawingNo(), it.projectDrawingNo()) + ")");
                continue;
            }
            Long partId = null;
            Long projectId = null;
            if (it.partDrawingNo() != null) {
                List<Map<String, Object>> rows = jdbc.queryForList("select id, project_id from child_part where drawing_no = :k",
                        new MapSqlParameterSource("k", it.partDrawingNo()));
                if (!rows.isEmpty()) {
                    partId = ((Number) rows.get(0).get("id")).longValue();
                    projectId = ((Number) rows.get(0).get("project_id")).longValue();
                }
            } else if (it.projectDrawingNo() != null) {
                projectId = id("select id from project where drawing_no = :k", it.projectDrawingNo());
            }
            if (projectId == null) {
                pr.add("Machine-hour row " + it.entryDate() + ": " + label(it.partDrawingNo(), it.projectDrawingNo()) + " not found");
                continue;
            }
            int design = nz(it.designMinutes()), setting = nz(it.settingMinutes()), run = nz(it.runMinutes());
            BigDecimal rate = it.ratePerHour() == null ? machineHours.getSettings().ratePerHour() : it.ratePerHour();
            BigDecimal amount = MachineHourService.amount(design, setting, run, rate);
            jdbc.update("insert into machine_hour_entry (entry_date, project_id, child_part_id, quantity, hardened, design_minutes, "
                            + "setting_minutes, run_minutes, idle_minutes, power_cut_minutes, rate_per_hour, tax_percent, billable_hours, "
                            + "amount, tax_amount, remarks, created_at, import_batch) values (:d, :proj, :part, :q, :h, :de, :se, :ru, :id, "
                            + ":pc, :rate, :tax, :hrs, :amt, :tamt, :rem, :t, :b)",
                    new MapSqlParameterSource("d", it.entryDate()).addValue("proj", projectId).addValue("part", partId)
                            .addValue("q", it.quantity() == null || it.quantity() < 1 ? 1 : it.quantity()).addValue("h", it.hardened())
                            .addValue("de", design).addValue("se", setting).addValue("ru", run).addValue("id", nz(it.idleMinutes()))
                            .addValue("pc", nz(it.powerCutMinutes())).addValue("rate", rate.setScale(2, RoundingMode.HALF_UP))
                            .addValue("tax", tax.setScale(2, RoundingMode.HALF_UP))
                            .addValue("hrs", MachineHourService.billableHours(design, setting, run)).addValue("amt", amount)
                            .addValue("tamt", MachineHourService.taxOn(amount, tax)).addValue("rem", trunc(it.remarks(), 500))
                            .addValue("t", now()).addValue("b", batch));
            created++;
        }
        return pr.result(created);
    }

    @Transactional
    public SectionResult tools(String batch, List<ToolImportItem> items) {
        checkBatch(batch);
        Problems pr = new Problems();
        int created = 0;
        for (ToolImportItem it : items) {
            String type = tidy(it.toolType());
            String desc = tidy(it.description());
            if (type == null || desc == null) {
                pr.add("Tool without type or description");
                continue;
            }
            if (it.brokenOn() != null && it.scrappedOn() != null) {
                pr.add("Tool " + desc + ": both broken and scrapped");
                continue;
            }
            jdbc.update("insert into tool_item (tool_type, description, item_code, tool_size, cost, purchased_on, issued_on, broken_on, "
                            + "scrapped_on, remarks, created_at, import_batch) values (:ty, :de, :ic, :sz, :co, :pu, :is, :br, :sc, :re, :t, :b)",
                    new MapSqlParameterSource("ty", trunc(type, 60)).addValue("de", trunc(desc, 200)).addValue("ic", trunc(tidy(it.itemCode()), 60))
                            .addValue("sz", trunc(tidy(it.toolSize()), 60))
                            .addValue("co", it.cost() == null || it.cost().signum() < 0 ? BigDecimal.ZERO : it.cost())
                            .addValue("pu", it.purchasedOn()).addValue("is", it.issuedOn()).addValue("br", it.brokenOn())
                            .addValue("sc", it.scrappedOn()).addValue("re", trunc(it.remarks(), 500)).addValue("t", now()).addValue("b", batch));
            if (it.resharpens() != null && !it.resharpens().isEmpty()) {
                Long toolId = jdbc.queryForObject("select max(id) from tool_item where import_batch = :b", new MapSqlParameterSource("b", batch), Long.class);
                for (ResharpenItem r : it.resharpens()) {
                    jdbc.update("insert into tool_resharpen (tool_item_id, resharpened_on, cost, created_at, import_batch) values (:i, :d, :c, :t, :b)",
                            new MapSqlParameterSource("i", toolId).addValue("d", r.resharpenedOn())
                                    .addValue("c", r.cost() == null || r.cost().signum() < 0 ? BigDecimal.ZERO : r.cost())
                                    .addValue("t", now()).addValue("b", batch));
                }
            }
            created++;
        }
        return pr.result(created);
    }

    @Transactional
    public SectionResult rmPurchases(String batch, List<PurchaseItem> items) {
        checkBatch(batch);
        Problems pr = new Problems();
        Map<String, Long> suppliers = supplierKeys();
        int created = 0;
        for (PurchaseItem it : items) {
            Long partId = it.partDrawingNo() == null ? null : id("select id from child_part where drawing_no = :k", it.partDrawingNo());
            Long supplierId = it.supplierName() == null ? null : suppliers.get(key(it.supplierName()));
            if (partId == null || supplierId == null || it.weightKg() == null || it.weightKg().signum() <= 0
                    || it.ratePerKg() == null || it.ratePerKg().signum() < 0 || it.inDate() == null) {
                pr.add("Purchase for " + it.partDrawingNo() + ": part or supplier not found, or weight/rate/date invalid");
                continue;
            }
            jdbc.update("insert into rm_purchase (child_part_id, supplier_id, weight_kg, rate_per_kg, total_cost, in_date, out_date, remarks, "
                            + "created_at, import_batch) values (:p, :s, :w, :r, :tot, :in, :out, :rem, :t, :b)",
                    new MapSqlParameterSource("p", partId).addValue("s", supplierId).addValue("w", it.weightKg().setScale(3, RoundingMode.HALF_UP))
                            .addValue("r", it.ratePerKg().setScale(2, RoundingMode.HALF_UP))
                            .addValue("tot", RmPurchaseService.total(it.weightKg(), it.ratePerKg())).addValue("in", it.inDate())
                            .addValue("out", it.outDate()).addValue("rem", trunc(it.remarks(), 500)).addValue("t", now()).addValue("b", batch));
            created++;
        }
        return pr.result(created);
    }

    @Transactional
    public SectionResult rejections(String batch, List<RejectionItem> items) {
        checkBatch(batch);
        Problems pr = new Problems();
        Map<String, Long> suppliers = supplierKeys();
        int created = 0;
        for (RejectionItem it : items) {
            Long supplierId = it.supplierName() == null ? null : suppliers.get(key(it.supplierName()));
            if (supplierId == null || it.rejectedQty() == null || it.rejectedQty() < 1 || it.partCost() == null
                    || it.partCost().signum() < 0 || it.stageName() == null || it.partDrawingNo() == null) {
                pr.add("Rejection on " + it.partDrawingNo() + ": supplier not found or data invalid");
                continue;
            }
            MapSqlParameterSource p = new MapSqlParameterSource("d", it.partDrawingNo()).addValue("s", it.stageName())
                    .addValue("sup", supplierId);
            String sql = "select ps.id from part_stage ps join child_part cp on cp.id = ps.child_part_id "
                    + "join stage_master sm on sm.id = ps.stage_master_id where cp.drawing_no = :d and lower(sm.name) = lower(:s) "
                    + "and ps.supplier_id = :sup and ";
            if (it.sentOn() == null) {
                sql += "ps.sent_on is null";
            } else {
                sql += "ps.sent_on = :sent";
                p.addValue("sent", it.sentOn());
            }
            List<Long> stageIds = jdbc.queryForList(sql, p, Long.class);
            if (stageIds.isEmpty()) {
                pr.add("Rejection on " + it.partDrawingNo() + " (" + it.stageName() + "): stage not found");
                continue;
            }
            BigDecimal machining = it.machiningCost() == null ? BigDecimal.ZERO : it.machiningCost();
            BigDecimal calc = RejectionService.calculatedDebit(it.rejectedQty(), it.partCost(), machining);
            BigDecimal debit = it.debitAmount() == null || it.debitAmount().signum() < 0 ? calc
                    : it.debitAmount().setScale(2, RoundingMode.HALF_UP);
            jdbc.update("insert into rejection (part_stage_id, supplier_id, rejected_qty, part_cost, machining_cost, debit_amount, debit_manual, "
                            + "bill_no, rejected_on, remarks, created_at, import_batch) values (:ps, :sup, :q, :pc, :mc, :da, :dm, :bn, :ro, :rem, :t, :b)",
                    new MapSqlParameterSource("ps", stageIds.get(0)).addValue("sup", supplierId).addValue("q", it.rejectedQty())
                            .addValue("pc", it.partCost().setScale(2, RoundingMode.HALF_UP)).addValue("mc", machining.setScale(2, RoundingMode.HALF_UP))
                            .addValue("da", debit).addValue("dm", debit.subtract(calc).abs().compareTo(new BigDecimal("0.01")) > 0)
                            .addValue("bn", trunc(it.billNo(), 40)).addValue("ro", it.rejectedOn()).addValue("rem", trunc(it.remarks(), 500))
                            .addValue("t", now()).addValue("b", batch));
            created++;
        }
        return pr.result(created);
    }

    @Transactional
    public SectionResult expenses(String batch, List<ExpenseItem> items) {
        checkBatch(batch);
        Problems pr = new Problems();
        int created = 0;
        for (ExpenseItem it : items) {
            if (it.month() == null || !it.month().matches("^\\d{4}-\\d{2}$")) {
                pr.add("Expense with a bad month: " + it.month());
                continue;
            }
            if (exists("select count(*) from monthly_expense where month_key = :k", it.month())) {
                pr.skipped++;
                continue;
            }
            jdbc.update("insert into monthly_expense (month_key, consumables, salary, notes, import_batch) values (:m, :c, :s, :n, :b)",
                    new MapSqlParameterSource("m", it.month())
                            .addValue("c", it.consumables() == null || it.consumables().signum() < 0 ? BigDecimal.ZERO : it.consumables())
                            .addValue("s", it.salary() == null || it.salary().signum() < 0 ? BigDecimal.ZERO : it.salary())
                            .addValue("n", trunc(it.notes(), 500)).addValue("b", batch));
            created++;
        }
        return pr.result(created);
    }

    // ---------- helpers ----------

    private static void checkBatch(String batch) {
        if (batch == null || !BATCH.matcher(batch).matches()) {
            throw new BadRequestException("Batch name: 1 to 50 letters, digits, - or _");
        }
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private static String tidy(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    private static String trunc(String s, int max) {
        String t = tidy(s);
        return t == null ? null : t.length() > max ? t.substring(0, max) : t;
    }

    private static int nz(Integer v) {
        return v == null || v < 0 ? 0 : v;
    }

    private static String label(String part, String project) {
        return part != null ? "part " + part : "project " + project;
    }

    /** Names compared without punctuation or spacing, so "V.N.R INDUSTRIES" and "VNR Industries" are the same. */
    private static String key(String name) {
        return name == null ? "" : name.toUpperCase().replaceAll("[^A-Z0-9]", "");
    }

    private Map<String, Long> supplierKeys() {
        Map<String, Long> map = new HashMap<>();
        jdbc.query("select id, name from supplier", new MapSqlParameterSource(), rs -> {
            map.putIfAbsent(key(rs.getString("name")), rs.getLong("id"));
        });
        return map;
    }

    private Map<String, Long> nameMap(String sql) {
        Map<String, Long> map = new HashMap<>();
        jdbc.query(sql, new MapSqlParameterSource(), rs -> {
            map.put(rs.getString("name").toLowerCase(), rs.getLong("id"));
        });
        return map;
    }

    private Long id(String sql, String k) {
        List<Long> ids = jdbc.queryForList(sql, new MapSqlParameterSource("k", k), Long.class);
        return ids.isEmpty() ? null : ids.get(0);
    }

    private boolean exists(String sql, String k) {
        Long n = jdbc.queryForObject(sql, new MapSqlParameterSource("k", k), Long.class);
        return n != null && n > 0;
    }

    private boolean exists2(String sql, Long a, Integer b) {
        Long n = jdbc.queryForObject(sql, new MapSqlParameterSource("a", a).addValue("b", b), Long.class);
        return n != null && n > 0;
    }

    private static final class Problems {
        int skipped;
        final List<String> list = new ArrayList<>();
        int more;

        void add(String message) {
            if (list.size() < MAX_PROBLEMS) {
                list.add(message);
            } else {
                more++;
            }
        }

        SectionResult result(int created) {
            List<String> shown = new ArrayList<>(list);
            if (more > 0) {
                shown.add("... and " + more + " more");
            }
            return new SectionResult(created, skipped + list.size() + more, shown);
        }
    }
}
