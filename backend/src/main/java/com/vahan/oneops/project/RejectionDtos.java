package com.vahan.oneops.project;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Request and response shapes for supplier rejections. */
public final class RejectionDtos {

    private RejectionDtos() {
    }

    /**
     * debitAmount empty means "calculate it": rejected pieces x part cost - machining cost (never below 0).
     * Giving an amount overrides the calculation.
     */
    public record RejectionInput(
            @NotNull @Min(value = 1, message = "Rejected quantity must be at least 1") Integer rejectedQty,
            @NotNull @DecimalMin(value = "0", message = "Part cost cannot be negative")
            @Digits(integer = 10, fraction = 2, message = "At most 2 decimals") BigDecimal partCost,
            @DecimalMin(value = "0", message = "Machining cost cannot be negative")
            @Digits(integer = 10, fraction = 2, message = "At most 2 decimals") BigDecimal machiningCost,
            @DecimalMin(value = "0", message = "Debit cannot be negative")
            @Digits(integer = 10, fraction = 2, message = "At most 2 decimals") BigDecimal debitAmount,
            @Size(max = 40) String billNo,
            LocalDate rejectedOn,
            @Size(max = 500) String remarks) {
    }

    public record RejectionDto(Long id, int rejectedQty, BigDecimal partCost, BigDecimal machiningCost,
                               BigDecimal debitAmount, boolean debitManual, BigDecimal calculatedDebit,
                               String billNo, LocalDate rejectedOn, String remarks) {
        static RejectionDto from(Rejection r) {
            return new RejectionDto(r.getId(), r.getRejectedQty(), r.getPartCost(), r.getMachiningCost(),
                    r.getDebitAmount(), r.isDebitManual(),
                    RejectionService.calculatedDebit(r.getRejectedQty(), r.getPartCost(), r.getMachiningCost()),
                    r.getBillNo(), r.getRejectedOn(), r.getRemarks());
        }
    }

    /** One line of the Rejections page. */
    public record RejectionRowDto(Long id, Long supplierId, String supplierName, Long projectId,
                                  String projectDrawingNo, String partDrawingNo, String partName, String stageName,
                                  int rejectedQty, BigDecimal partCost, BigDecimal machiningCost,
                                  BigDecimal debitAmount, boolean debitManual, String billNo,
                                  LocalDate rejectedOn, String remarks) {
    }
}
