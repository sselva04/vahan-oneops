package com.vahan.oneops.project;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "project")
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "drawing_no", nullable = false, unique = true, length = 60)
    private String drawingNo;

    @Column(nullable = false, length = 200)
    private String name;

    /** Plain user id: the project module does not depend on the user table. */
    @Column(name = "created_by", nullable = false, updatable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Project() {
    }

    public Project(String drawingNo, String name, Long createdBy) {
        this.drawingNo = drawingNo;
        this.name = name;
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public String getDrawingNo() { return drawingNo; }
    public String getName() { return name; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
