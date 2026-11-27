package com.vahan.oneops.machine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One VMC job. Plain ids for the project and part, like the other modules.
 * The rate, tax rate and the calculated amounts are stored on the row, so they never change afterwards
 * when the default rate is edited.
 */
@Entity
@Table(name = "machine_hour_entry")
public class MachineHourEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entry_date", nullable = false)
    private LocalDate entryDate;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "child_part_id")
    private Long childPartId;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private boolean hardened;

    @Column(name = "design_minutes", nullable = false)
    private int designMinutes;

    @Column(name = "setting_minutes", nullable = false)
    private int settingMinutes;

    @Column(name = "run_minutes", nullable = false)
    private int runMinutes;

    @Column(name = "idle_minutes", nullable = false)
    private int idleMinutes;

    @Column(name = "power_cut_minutes", nullable = false)
    private int powerCutMinutes;

    @Column(name = "rate_per_hour", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerHour;

    @Column(name = "tax_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxPercent;

    @Column(name = "billable_hours", nullable = false, precision = 10, scale = 2)
    private BigDecimal billableHours;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @Column(length = 500)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected MachineHourEntry() {
    }

    public MachineHourEntry(Long projectId) {
        this.projectId = projectId;
    }

    public void set(LocalDate entryDate, Long projectId, Long childPartId, int quantity, boolean hardened,
                    int designMinutes, int settingMinutes, int runMinutes, int idleMinutes, int powerCutMinutes,
                    BigDecimal ratePerHour, BigDecimal taxPercent, BigDecimal billableHours,
                    BigDecimal amount, BigDecimal taxAmount, String remarks) {
        this.entryDate = entryDate;
        this.projectId = projectId;
        this.childPartId = childPartId;
        this.quantity = quantity;
        this.hardened = hardened;
        this.designMinutes = designMinutes;
        this.settingMinutes = settingMinutes;
        this.runMinutes = runMinutes;
        this.idleMinutes = idleMinutes;
        this.powerCutMinutes = powerCutMinutes;
        this.ratePerHour = ratePerHour;
        this.taxPercent = taxPercent;
        this.billableHours = billableHours;
        this.amount = amount;
        this.taxAmount = taxAmount;
        this.remarks = remarks;
    }

    public Long getId() { return id; }
    public LocalDate getEntryDate() { return entryDate; }
    public Long getProjectId() { return projectId; }
    public Long getChildPartId() { return childPartId; }
    public int getQuantity() { return quantity; }
    public boolean isHardened() { return hardened; }
    public int getDesignMinutes() { return designMinutes; }
    public int getSettingMinutes() { return settingMinutes; }
    public int getRunMinutes() { return runMinutes; }
    public int getIdleMinutes() { return idleMinutes; }
    public int getPowerCutMinutes() { return powerCutMinutes; }
    public BigDecimal getRatePerHour() { return ratePerHour; }
    public BigDecimal getTaxPercent() { return taxPercent; }
    public BigDecimal getBillableHours() { return billableHours; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public String getRemarks() { return remarks; }
}
