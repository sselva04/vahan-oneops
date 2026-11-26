package com.vahan.oneops.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findAllByOrderByCreatedAtDesc();

    boolean existsByDrawingNo(String drawingNo);
}
