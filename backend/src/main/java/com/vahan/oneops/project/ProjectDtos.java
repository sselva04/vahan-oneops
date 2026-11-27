package com.vahan.oneops.project;

import jakarta.validation.Valid;
import com.vahan.oneops.material.RmShape;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Request and response shapes of the project module, grouped in one file. */
public final class ProjectDtos {

    private ProjectDtos() {
    }

    // ---------- requests ----------

    public record CreateProject(
            @NotBlank @Size(max = 60) String drawingNo,
            @NotBlank @Size(max = 200) String name) {
    }

    public record AddPart(
            @NotBlank @Size(max = 60) String drawingNo,
            @NotBlank @Size(max = 200) String name,
            @Min(value = 1, message = "Quantity must be at least 1") int quantity) {
    }

    /** Give either stageMasterId (a stage from the list) or customName (a stage typed in), not both. */
    public record StageInput(Long stageMasterId, @Size(max = 80) String customName,
                             Long assigneeId, LocalDate dueDate) {
    }

    /** The stages are appended to the part in the order given. */
    public record AddStages(@NotEmpty @Valid List<StageInput> stages) {
    }

    /** Replaces both values: assigneeId = null means "unassigned", dueDate = null means "no due date". */
    public record UpdateStage(Long assigneeId, LocalDate dueDate) {
    }

    public record CreateStageMaster(@NotBlank @Size(max = 80) String name) {
    }

    /**
     * Replaces the supplier details of one stage. supplierId = null means the stage is done in-house
     * (then quantity and rate must be empty). Quantity and rate are per the supplier's sheet: pieces sent, cost per piece.
     */
    public record OutsourceInput(
            Long supplierId,
            @Min(value = 1, message = "Quantity must be at least 1") Integer jobQty,
            @DecimalMin(value = "0", message = "Rate cannot be negative")
            @Digits(integer = 10, fraction = 2, message = "Rate can have at most 2 decimals") BigDecimal ratePerUnit,
            boolean preInspectionDone,
            boolean finalInspectionDone,
            @Size(max = 500) String remarks) {
    }

    // ---------- responses ----------

    public record StageMasterDto(Long id, String name) {
        static StageMasterDto from(StageMaster m) {
            return new StageMasterDto(m.getId(), m.getName());
        }
    }

    public record StageDto(Long id, Long stageMasterId, String stageName, int sequenceNo,
                           Long assigneeId, String assigneeName, StageStatus status, LocalDate dueDate,
                           Instant startedAt, Instant completedAt,
                           Long supplierId, String supplierName, Integer jobQty, BigDecimal ratePerUnit,
                           BigDecimal totalCost, boolean preInspectionDone, boolean finalInspectionDone,
                           String remarks) {
        static StageDto from(PartStage s, Map<Long, String> names, Map<Long, String> supplierNames) {
            Long assignee = s.getAssigneeId();
            Long supplier = s.getSupplierId();
            BigDecimal total = s.getJobQty() != null && s.getRatePerUnit() != null
                    ? s.getRatePerUnit().multiply(BigDecimal.valueOf(s.getJobQty().longValue())) : null;
            return new StageDto(s.getId(), s.getStageMaster().getId(), s.getStageMaster().getName(),
                    s.getSequenceNo(), assignee, assignee == null ? null : names.get(assignee),
                    s.getStatus(), s.getDueDate(), s.getStartedAt(), s.getCompletedAt(),
                    supplier, supplier == null ? null : supplierNames.get(supplier), s.getJobQty(),
                    s.getRatePerUnit(), total, s.isPreInspectionDone(), s.isFinalInspectionDone(), s.getRemarks());
        }
    }

    /**
     * Sets or clears the raw material of a part. materialId = null clears it (all other fields must then be empty).
     * Sizes are in mm: BLOCK a=length b=width c=thickness, ROUND a=diameter b=length,
     * TUBE a=outer diameter b=inner diameter c=length, MANUAL manualWeightKg = weight of one piece.
     * actualWeightKg is the weighed stock for the whole part quantity and may be added later.
     */
    public record RawMaterialInput(
            Long materialId,
            RmShape shape,
            @DecimalMin(value = "0.01", message = "Size must be above 0") @Digits(integer = 8, fraction = 2) BigDecimal dimA,
            @DecimalMin(value = "0.01", message = "Size must be above 0") @Digits(integer = 8, fraction = 2) BigDecimal dimB,
            @DecimalMin(value = "0.01", message = "Size must be above 0") @Digits(integer = 8, fraction = 2) BigDecimal dimC,
            @DecimalMin(value = "0.001", message = "Weight must be above 0") @Digits(integer = 7, fraction = 3) BigDecimal manualWeightKg,
            @DecimalMin(value = "0.001", message = "Weight must be above 0") @Digits(integer = 7, fraction = 3) BigDecimal actualWeightKg) {
    }

    /** What the raw material of a part comes to. Cost uses the actual weight once it is entered, else the theoretical. */
    public record RawMaterialDto(Long materialId, String materialName, RmShape shape,
                                 BigDecimal dimA, BigDecimal dimB, BigDecimal dimC, BigDecimal manualWeightKg,
                                 BigDecimal theoreticalPerPieceKg, BigDecimal theoreticalTotalKg,
                                 BigDecimal actualWeightKg, BigDecimal differenceKg, BigDecimal differencePercent,
                                 BigDecimal ratePerKg, BigDecimal cost, boolean costFromActual) {
    }

    public record PartDto(Long id, String drawingNo, String name, int quantity, List<StageDto> stages,
                          RawMaterialDto rawMaterial) {
    }

    public record ProjectDetailDto(Long id, String drawingNo, String name, Instant createdAt, List<PartDto> parts,
                                   BigDecimal rawMaterialCost) {
    }

    public record ProjectSummaryDto(Long id, String drawingNo, String name, Instant createdAt,
                                    int partCount, int stageCount, int stagesDone, int stagesUnassigned) {
    }

    /** One row of "My work": a stage allocated to a person, with enough context to show it on its own. */
    public record WorkItemDto(Long stageId, Long projectId, String projectDrawingNo, String projectName,
                              Long partId, String partDrawingNo, String partName, int quantity,
                              String stageName, int sequenceNo, StageStatus status, LocalDate dueDate,
                              Instant startedAt, Instant completedAt, List<String> waitingFor,
                              String supplierName) {
    }
}
