package com.vahan.oneops.purchase;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** One raw material purchase for a part. Weight, rate and total are stored as bought. */
@Entity
@Table(name = "rm_purchase")
public class RmPurchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "child_part_id", nullable = false)
    private Long childPartId;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "weight_kg", nullable = false, precision = 12, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "rate_per_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerKg;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "in_date", nullable = false)
    private LocalDate inDate;

    @Column(name = "out_date")
    private LocalDate outDate;

    @Column(length = 500)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected RmPurchase() {
    }

    public RmPurchase(Long childPartId, Long supplierId) {
        this.childPartId = childPartId;
        this.supplierId = supplierId;
    }

    public void set(Long childPartId, Long supplierId, BigDecimal weightKg, BigDecimal ratePerKg,
                    BigDecimal totalCost, LocalDate inDate, LocalDate outDate, String remarks) {
        this.childPartId = childPartId;
        this.supplierId = supplierId;
        this.weightKg = weightKg;
        this.ratePerKg = ratePerKg;
        this.totalCost = totalCost;
        this.inDate = inDate;
        this.outDate = outDate;
        this.remarks = remarks;
    }

    public Long getId() { return id; }
    public Long getChildPartId() { return childPartId; }
    public Long getSupplierId() { return supplierId; }
    public BigDecimal getWeightKg() { return weightKg; }
    public BigDecimal getRatePerKg() { return ratePerKg; }
    public BigDecimal getTotalCost() { return totalCost; }
    public LocalDate getInDate() { return inDate; }
    public LocalDate getOutDate() { return outDate; }
    public String getRemarks() { return remarks; }
}
