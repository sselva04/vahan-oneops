package com.vahan.oneops.project;

import jakarta.persistence.Column;
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
import java.time.LocalDate;

/**
 * One stage of one child part, in a given order, owned by one person.
 * Every child part has its own list, so two parts of the same project can follow different routes.
 */
@Entity
@Table(name = "part_stage")
public class PartStage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "child_part_id", nullable = false)
    private ChildPart childPart;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stage_master_id", nullable = false)
    private StageMaster stageMaster;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    /** Plain user id of the owner (null = not yet allocated). */
    @Column(name = "assignee_id")
    private Long assigneeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StageStatus status = StageStatus.NOT_STARTED;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    /** Plain user id of whoever marked it done. */
    @Column(name = "completed_by")
    private Long completedBy;

    // ---- only for a stage that is given to a supplier (supplierId is empty for in-house work) ----

    /** Plain id of the supplier doing this stage. */
    @Column(name = "supplier_id")
    private Long supplierId;

    /** How many pieces were sent; may be fewer than the part quantity. */
    @Column(name = "job_qty")
    private Integer jobQty;

    @Column(name = "rate_per_unit", precision = 12, scale = 2)
    private BigDecimal ratePerUnit;

    @Column(name = "pre_inspection_done", nullable = false)
    private boolean preInspectionDone;

    @Column(name = "final_inspection_done", nullable = false)
    private boolean finalInspectionDone;

    @Column(length = 500)
    private String remarks;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected PartStage() {
    }

    public PartStage(ChildPart childPart, StageMaster stageMaster, int sequenceNo, Long assigneeId, LocalDate dueDate) {
        this.childPart = childPart;
        this.stageMaster = stageMaster;
        this.sequenceNo = sequenceNo;
        this.assigneeId = assigneeId;
        this.dueDate = dueDate;
    }

    public Long getId() { return id; }
    public ChildPart getChildPart() { return childPart; }
    public StageMaster getStageMaster() { return stageMaster; }
    public int getSequenceNo() { return sequenceNo; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public StageStatus getStatus() { return status; }
    public void setStatus(StageStatus status) { this.status = status; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Long getCompletedBy() { return completedBy; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public Integer getJobQty() { return jobQty; }
    public void setJobQty(Integer jobQty) { this.jobQty = jobQty; }
    public BigDecimal getRatePerUnit() { return ratePerUnit; }
    public void setRatePerUnit(BigDecimal ratePerUnit) { this.ratePerUnit = ratePerUnit; }
    public boolean isPreInspectionDone() { return preInspectionDone; }
    public void setPreInspectionDone(boolean preInspectionDone) { this.preInspectionDone = preInspectionDone; }
    public boolean isFinalInspectionDone() { return finalInspectionDone; }
    public void setFinalInspectionDone(boolean finalInspectionDone) { this.finalInspectionDone = finalInspectionDone; }
    public String getRemarks() { return remarks; }
    public void setRemarks(String remarks) { this.remarks = remarks; }

    public void start() {
        this.status = StageStatus.IN_PROGRESS;
        this.startedAt = Instant.now();
    }

    public void complete(Long userId) {
        this.status = StageStatus.DONE;
        this.completedAt = Instant.now();
        this.completedBy = userId;
    }
}
