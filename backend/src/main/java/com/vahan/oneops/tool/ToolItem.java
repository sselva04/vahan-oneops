package com.vahan.oneops.tool;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** One physical tool (one row of the "Tool Stock Details" sheet). */
@Entity
@Table(name = "tool_item")
public class ToolItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tool_type", nullable = false, length = 60)
    private String toolType;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(name = "item_code", length = 60)
    private String itemCode;

    @Column(name = "tool_size", length = 60)
    private String toolSize;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    @Column(name = "purchased_on")
    private LocalDate purchasedOn;

    @Column(name = "issued_on")
    private LocalDate issuedOn;

    @Column(name = "broken_on")
    private LocalDate brokenOn;

    @Column(name = "scrapped_on")
    private LocalDate scrappedOn;

    @Column(name = "broken_reason", length = 300)
    private String brokenReason;

    @Column(name = "broken_project_id")
    private Long brokenProjectId;

    @Column(name = "broken_part_id")
    private Long brokenPartId;

    @Column(length = 500)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ToolItem() {
    }

    public ToolItem(String toolType, String description) {
        this.toolType = toolType;
        this.description = description;
    }

    public void set(String toolType, String description, String itemCode, String toolSize, BigDecimal cost,
                    LocalDate purchasedOn, LocalDate issuedOn, LocalDate brokenOn, LocalDate scrappedOn,
                    String brokenReason, Long brokenProjectId, Long brokenPartId, String remarks) {
        this.toolType = toolType;
        this.description = description;
        this.itemCode = itemCode;
        this.toolSize = toolSize;
        this.cost = cost;
        this.purchasedOn = purchasedOn;
        this.issuedOn = issuedOn;
        this.brokenOn = brokenOn;
        this.scrappedOn = scrappedOn;
        this.brokenReason = brokenReason;
        this.brokenProjectId = brokenProjectId;
        this.brokenPartId = brokenPartId;
        this.remarks = remarks;
    }

    public Long getId() { return id; }
    public String getToolType() { return toolType; }
    public String getDescription() { return description; }
    public String getItemCode() { return itemCode; }
    public String getToolSize() { return toolSize; }
    public BigDecimal getCost() { return cost; }
    public LocalDate getPurchasedOn() { return purchasedOn; }
    public LocalDate getIssuedOn() { return issuedOn; }
    public LocalDate getBrokenOn() { return brokenOn; }
    public LocalDate getScrappedOn() { return scrappedOn; }
    public String getBrokenReason() { return brokenReason; }
    public Long getBrokenProjectId() { return brokenProjectId; }
    public Long getBrokenPartId() { return brokenPartId; }
    public String getRemarks() { return remarks; }
}
