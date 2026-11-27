package com.vahan.oneops.report;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public final class ReportDtos {

    private ReportDtos() {
    }

    public record ExpenseInput(
            @NotNull @DecimalMin("0") BigDecimal consumables,
            @NotNull @DecimalMin("0") BigDecimal salary,
            @Size(max = 500) String notes) {
    }

    /**
     * One month, as in the "VMC Turnover" sheet: turnover = machine-hour amounts (before tax),
     * expense = consumables + salary (typed in), profit = turnover - expense.
     * The last four figures are for information and are not part of the profit.
     */
    public record MonthDto(
            String month,
            int machineEntries, BigDecimal machineHours,
            BigDecimal turnover, BigDecimal tax,
            BigDecimal consumables, BigDecimal salary, BigDecimal totalExpense, BigDecimal profit,
            String notes,
            BigDecimal outsourcedCost, BigDecimal rejectionDebit, BigDecimal toolSpend, BigDecimal rmPurchases) {
    }
}
