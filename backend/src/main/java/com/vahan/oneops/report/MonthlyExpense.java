package com.vahan.oneops.report;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** The consumables and salary typed in for one month (key like 2026-11). */
@Entity
@Table(name = "monthly_expense")
public class MonthlyExpense {

    @Id
    @Column(name = "month_key", length = 7)
    private String monthKey;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal consumables = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salary = BigDecimal.ZERO;

    @Column(length = 500)
    private String notes;

    protected MonthlyExpense() {
    }

    public MonthlyExpense(String monthKey) {
        this.monthKey = monthKey;
    }

    public void set(BigDecimal consumables, BigDecimal salary, String notes) {
        this.consumables = consumables;
        this.salary = salary;
        this.notes = notes;
    }

    public String getMonthKey() { return monthKey; }
    public BigDecimal getConsumables() { return consumables; }
    public BigDecimal getSalary() { return salary; }
    public String getNotes() { return notes; }
}
