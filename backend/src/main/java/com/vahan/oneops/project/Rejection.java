package com.vahan.oneops.project;

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
 * Pieces spoiled by a supplier on one outsourced stage, and the amount debited to them.
 * Plain ids for the stage and supplier, like the other modules; supplierId is the supplier at the time.
 */
@Entity
@Table(name = "rejection")
public class Rejection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "part_stage_id", nullable = false)
    private Long partStageId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "rejected_qty", nullable = false)
    private int rejectedQty;

    @Column(name = "part_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal partCost;

    @Column(name = "machining_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal machiningCost;

    @Column(name = "debit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal debitAmount;

    /** True when the admin typed the debit instead of using the calculated one. */
    @Column(name = "debit_manual", nullable = false)
    private boolean debitManual;

    @Column(name = "bill_no", length = 40)
    private String billNo;

    @Column(name = "rejected_on")
    private LocalDate rejectedOn;

    @Column(length = 500)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Rejection() {
    }

    public Rejection(Long partStageId, Long supplierId) {
        this.partStageId = partStageId;
        this.supplierId = supplierId;
    }

    public void set(int rejectedQty, BigDecimal partCost, BigDecimal machiningCost, BigDecimal debitAmount,
                    boolean debitManual, String billNo, LocalDate rejectedOn, String remarks) {
        this.rejectedQty = rejectedQty;
        this.partCost = partCost;
        this.machiningCost = machiningCost;
        this.debitAmount = debitAmount;
        this.debitManual = debitManual;
        this.billNo = billNo;
        this.rejectedOn = rejectedOn;
        this.remarks = remarks;
    }

    public Long getId() { return id; }
    public Long getPartStageId() { return partStageId; }
    public Long getSupplierId() { return supplierId; }
    public int getRejectedQty() { return rejectedQty; }
    public BigDecimal getPartCost() { return partCost; }
    public BigDecimal getMachiningCost() { return machiningCost; }
    public BigDecimal getDebitAmount() { return debitAmount; }
    public boolean isDebitManual() { return debitManual; }
    public String getBillNo() { return billNo; }
    public LocalDate getRejectedOn() { return rejectedOn; }
    public String getRemarks() { return remarks; }
    public Instant getCreatedAt() { return createdAt; }
}
