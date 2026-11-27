package com.vahan.oneops.machine;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class MachineHourDtos {

    private MachineHourDtos() {
    }

    /** The default hourly rate and tax rate used for new entries. */
    public record SettingsInput(
            @NotNull @DecimalMin("0") @DecimalMax("100000") BigDecimal ratePerHour,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal taxPercent) {
    }

    public record SettingsDto(BigDecimal ratePerHour, BigDecimal taxPercent) {
    }

    /**
     * Minutes left empty count as 0. ratePerHour empty = the default rate (when adding) or the rate the
     * entry already has (when editing).
     */
    public record EntryInput(
            @NotNull LocalDate entryDate,
            @NotNull Long projectId,
            Long childPartId,
            @NotNull @Min(1) Integer quantity,
            boolean hardened,
            @Min(0) Integer designMinutes,
            @Min(0) Integer settingMinutes,
            @Min(0) Integer runMinutes,
            @Min(0) Integer idleMinutes,
            @Min(0) Integer powerCutMinutes,
            @DecimalMin("0") BigDecimal ratePerHour,
            @Size(max = 500) String remarks) {
    }

    public record EntryDto(
            Long id, LocalDate entryDate,
            Long projectId, String projectDrawingNo, String projectName, String customerName,
            Long childPartId, String partDrawingNo, String partName,
            int quantity, boolean hardened,
            int designMinutes, int settingMinutes, int runMinutes, int idleMinutes, int powerCutMinutes,
            BigDecimal billableHours, BigDecimal ratePerHour, BigDecimal taxPercent,
            BigDecimal amount, BigDecimal taxAmount, BigDecimal total,
            String remarks) {
    }

    public record Totals(BigDecimal billableHours, BigDecimal amount, BigDecimal taxAmount, BigDecimal total) {
    }

    public record ListDto(List<EntryDto> entries, Totals totals) {
    }
}
