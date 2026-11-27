package com.vahan.oneops.project;

import jakarta.persistence.Column;
import com.vahan.oneops.material.RmShape;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "child_part")
public class ChildPart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "drawing_no", nullable = false, unique = true, length = 60)
    private String drawingNo;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private int quantity;

    // ---- raw material (all empty until the admin sets it) ----

    /** Plain id of the material. */
    @Column(name = "material_id")
    private Long materialId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rm_shape", length = 10)
    private RmShape rmShape;

    /** Sizes in mm; meaning depends on the shape (see V6 migration). */
    @Column(name = "rm_dim_a", precision = 10, scale = 2)
    private BigDecimal rmDimA;
    @Column(name = "rm_dim_b", precision = 10, scale = 2)
    private BigDecimal rmDimB;
    @Column(name = "rm_dim_c", precision = 10, scale = 2)
    private BigDecimal rmDimC;

    /** Weight per piece, only for shape MANUAL. */
    @Column(name = "manual_weight_kg", precision = 10, scale = 3)
    private BigDecimal manualWeightKg;

    /** Weighed weight of the stock for the whole part quantity. */
    @Column(name = "actual_weight_kg", precision = 10, scale = 3)
    private BigDecimal actualWeightKg;

    /** Price per kg when the raw material was saved. */
    @Column(name = "rm_rate_per_kg", precision = 10, scale = 2)
    private BigDecimal rmRatePerKg;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ChildPart() {
    }

    public ChildPart(Project project, String drawingNo, String name, int quantity) {
        this.project = project;
        this.drawingNo = drawingNo;
        this.name = name;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public Project getProject() { return project; }
    public String getDrawingNo() { return drawingNo; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public Long getMaterialId() { return materialId; }
    public RmShape getRmShape() { return rmShape; }
    public BigDecimal getRmDimA() { return rmDimA; }
    public BigDecimal getRmDimB() { return rmDimB; }
    public BigDecimal getRmDimC() { return rmDimC; }
    public BigDecimal getManualWeightKg() { return manualWeightKg; }
    public BigDecimal getActualWeightKg() { return actualWeightKg; }
    public BigDecimal getRmRatePerKg() { return rmRatePerKg; }

    /** Sets the whole raw material at once. Everything null clears it. */
    public void setRawMaterial(Long materialId, RmShape shape, BigDecimal a, BigDecimal b, BigDecimal c,
                               BigDecimal manualWeightKg, BigDecimal actualWeightKg, BigDecimal ratePerKg) {
        this.materialId = materialId;
        this.rmShape = shape;
        this.rmDimA = a;
        this.rmDimB = b;
        this.rmDimC = c;
        this.manualWeightKg = manualWeightKg;
        this.actualWeightKg = actualWeightKg;
        this.rmRatePerKg = ratePerKg;
    }
}
