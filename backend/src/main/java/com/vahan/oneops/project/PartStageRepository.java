package com.vahan.oneops.project;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PartStageRepository extends JpaRepository<PartStage, Long> {

    @Query("select s from PartStage s join fetch s.stageMaster "
            + "where s.childPart.id in :partIds order by s.childPart.id, s.sequenceNo")
    List<PartStage> findForParts(@Param("partIds") Collection<Long> partIds);

    @Query("select s from PartStage s join fetch s.childPart c where c.project.id in :projectIds")
    List<PartStage> findForProjects(@Param("projectIds") Collection<Long> projectIds);

    @Query("select s from PartStage s join fetch s.childPart c join fetch c.project join fetch s.stageMaster "
            + "where s.assigneeId = :userId")
    List<PartStage> findAssignedTo(@Param("userId") Long userId);

    /** Jobs that went out and came back, for the average turnaround of each supplier. */
    @Query("select s from PartStage s where s.supplierId in :supplierIds "
            + "and s.sentOn is not null and s.receivedOn is not null")
    List<PartStage> findReceivedForSuppliers(@Param("supplierIds") Collection<Long> supplierIds);

    @Query("select coalesce(max(s.sequenceNo), 0) from PartStage s where s.childPart.id = :partId")
    int maxSequence(@Param("partId") Long partId);
}
