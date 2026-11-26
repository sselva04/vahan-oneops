package com.vahan.oneops.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ChildPartRepository extends JpaRepository<ChildPart, Long> {
    List<ChildPart> findByProjectIdInOrderByDrawingNoAsc(Collection<Long> projectIds);

    boolean existsByDrawingNo(String drawingNo);
}
