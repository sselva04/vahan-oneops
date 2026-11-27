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

/** One resharpening of a tool, with what it cost. */
@Entity
@Table(name = "tool_resharpen")
public class ToolResharpen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tool_item_id", nullable = false)
    private Long toolItemId;

    @Column(name = "resharpened_on")
    private LocalDate resharpenedOn;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ToolResharpen() {
    }

    public ToolResharpen(Long toolItemId, LocalDate resharpenedOn, BigDecimal cost) {
        this.toolItemId = toolItemId;
        this.resharpenedOn = resharpenedOn;
        this.cost = cost;
    }

    public Long getId() { return id; }
    public Long getToolItemId() { return toolItemId; }
    public LocalDate getResharpenedOn() { return resharpenedOn; }
    public BigDecimal getCost() { return cost; }
}
