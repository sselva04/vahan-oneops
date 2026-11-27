package com.vahan.oneops.tool;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class ToolDtos {

    private ToolDtos() {
    }

    /**
     * count: how many identical tools to add (default 1); ignored when editing.
     * A tool is either broken or scrapped, not both. A broken project/part needs a broken date.
     */
    public record ToolInput(
            @NotBlank @Size(max = 60) String toolType,
            @NotBlank @Size(max = 200) String description,
            @Size(max = 60) String itemCode,
            @Size(max = 60) String toolSize,
            @DecimalMin("0") BigDecimal cost,
            LocalDate purchasedOn,
            LocalDate issuedOn,
            LocalDate brokenOn,
            LocalDate scrappedOn,
            @Size(max = 300) String brokenReason,
            Long brokenProjectId,
            Long brokenPartId,
            @Size(max = 500) String remarks,
            @Min(1) @Max(100) Integer count) {
    }

    public record ResharpenInput(LocalDate resharpenedOn, @DecimalMin("0") BigDecimal cost) {
    }

    public record ResharpenDto(Long id, LocalDate resharpenedOn, BigDecimal cost) {
    }

    /** status: IN_STOCK, ISSUED, BROKEN or SCRAPPED. */
    public record ToolDto(
            Long id, String toolType, String description, String itemCode, String toolSize, BigDecimal cost,
            LocalDate purchasedOn, LocalDate issuedOn, LocalDate brokenOn, LocalDate scrappedOn,
            String brokenReason, Long brokenProjectId, String brokenProjectDrawingNo,
            Long brokenPartId, String brokenPartDrawingNo,
            String remarks, String status,
            List<ResharpenDto> resharpens, int resharpenCount, BigDecimal resharpenCost, BigDecimal totalCost) {
    }
}
