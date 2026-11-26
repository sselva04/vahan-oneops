package com.vahan.oneops.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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
}
