package com.vahan.oneops.dataimport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Shapes of the import file, section by section. Everything is matched by natural keys (drawing numbers, names). */
public final class ImportDtos {

    private ImportDtos() {
    }

    public record ProjectItem(String drawingNo, String name, String customerName) {
    }

    public record PartItem(String projectDrawingNo, String drawingNo, String name, Integer quantity) {
    }

    public record StageItem(String partDrawingNo, String stageName, Integer sequenceNo, String supplierName,
                            Integer jobQty, BigDecimal ratePerUnit, boolean preInspectionDone,
                            boolean finalInspectionDone, String remarks, LocalDate sentOn, LocalDate receivedOn,
                            String status) {
    }

    /** Either partDrawingNo (the project is the part's project) or projectDrawingNo. */
    public record HourItem(LocalDate entryDate, String partDrawingNo, String projectDrawingNo, Integer quantity,
                           boolean hardened, Integer designMinutes, Integer settingMinutes, Integer runMinutes,
                           Integer idleMinutes, Integer powerCutMinutes, BigDecimal ratePerHour, String remarks) {
    }

    public record ResharpenItem(LocalDate resharpenedOn, BigDecimal cost) {
    }

    public record ToolImportItem(String toolType, String description, String itemCode, String toolSize,
                                 BigDecimal cost, LocalDate purchasedOn, LocalDate issuedOn, LocalDate brokenOn,
                                 LocalDate scrappedOn, String remarks, List<ResharpenItem> resharpens) {
    }

    public record PurchaseItem(String partDrawingNo, String supplierName, BigDecimal weightKg, BigDecimal ratePerKg,
                               LocalDate inDate, LocalDate outDate, String remarks) {
    }

    /** The stage is found by part, stage name, supplier and sent date. */
    public record RejectionItem(String partDrawingNo, String stageName, String supplierName, LocalDate sentOn,
                                Integer rejectedQty, BigDecimal partCost, BigDecimal machiningCost,
                                BigDecimal debitAmount, String billNo, LocalDate rejectedOn, String remarks) {
    }

    public record ExpenseItem(String month, BigDecimal consumables, BigDecimal salary, String notes) {
    }

    /** problems: the first few rows that could not be loaded, with the reason. */
    public record SectionResult(int created, int skipped, List<String> problems) {
    }

    public record BatchInfo(String batch, Map<String, Integer> rows) {
    }

    public record Status(boolean enabled, List<BatchInfo> batches) {
    }
}
