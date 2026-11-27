package com.vahan.oneops.purchase;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class RmPurchaseDtos {

    private RmPurchaseDtos() {
    }

    public record PurchaseInput(
            @NotNull Long partId,
            @NotNull Long supplierId,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal weightKg,
            @NotNull @DecimalMin("0") BigDecimal ratePerKg,
            @NotNull LocalDate inDate,
            LocalDate outDate,
            @Size(max = 500) String remarks) {
    }

    public record PurchaseDto(
            Long id, Long partId, String partDrawingNo, String partName,
            Long projectId, String projectDrawingNo, String customerName, String materialName,
            Long supplierId, String supplierName,
            BigDecimal weightKg, BigDecimal ratePerKg, BigDecimal totalCost,
            LocalDate inDate, LocalDate outDate, String remarks) {
    }

    public record Totals(BigDecimal weightKg, BigDecimal totalCost) {
    }

    public record ListDto(List<PurchaseDto> purchases, Totals totals) {
    }
}
