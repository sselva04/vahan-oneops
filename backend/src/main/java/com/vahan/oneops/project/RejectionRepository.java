package com.vahan.oneops.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface RejectionRepository extends JpaRepository<Rejection, Long> {

    List<Rejection> findByPartStageIdInOrderByIdAsc(Collection<Long> stageIds);

    List<Rejection> findAllByOrderByIdDesc();

    List<Rejection> findBySupplierIdOrderByIdDesc(Long supplierId);

    boolean existsByPartStageId(Long partStageId);

    @Query("select coalesce(sum(r.rejectedQty), 0) from Rejection r where r.partStageId = :stageId")
    long sumQuantityForStage(@Param("stageId") Long stageId);

    /** One row per supplier: [supplierId, total debit, total rejected pieces]. */
    @Query("select r.supplierId, sum(r.debitAmount), sum(r.rejectedQty) from Rejection r group by r.supplierId")
    List<Object[]> totalsBySupplier();
}
