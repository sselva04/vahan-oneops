package com.vahan.oneops.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StageMasterRepository extends JpaRepository<StageMaster, Long> {
    List<StageMaster> findByActiveTrueOrderByIdAsc();

    boolean existsByNameIgnoreCase(String name);

    Optional<StageMaster> findByNameIgnoreCase(String name);
}
