package com.vahan.oneops.tool;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ToolResharpenRepository extends JpaRepository<ToolResharpen, Long> {
    List<ToolResharpen> findByToolItemIdInOrderByIdAsc(Collection<Long> toolIds);

    boolean existsByToolItemId(Long toolItemId);
}
