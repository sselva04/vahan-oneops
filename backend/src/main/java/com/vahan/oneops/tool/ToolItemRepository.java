package com.vahan.oneops.tool;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToolItemRepository extends JpaRepository<ToolItem, Long> {
    List<ToolItem> findAllByOrderByIdDesc();
}
