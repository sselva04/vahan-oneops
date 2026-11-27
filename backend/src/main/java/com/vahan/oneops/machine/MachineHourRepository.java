package com.vahan.oneops.machine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MachineHourRepository extends JpaRepository<MachineHourEntry, Long> {

    List<MachineHourEntry> findAllByOrderByEntryDateDescIdDesc();

    List<MachineHourEntry> findByProjectIdOrderByEntryDateDescIdDesc(Long projectId);
}
