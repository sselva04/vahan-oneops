package com.vahan.oneops.supplier;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SupplierProcessRepository extends JpaRepository<SupplierProcess, Long> {

    @Query("select p from SupplierProcess p join fetch p.stageMaster "
            + "where p.supplier.id in :supplierIds order by p.id")
    List<SupplierProcess> findForSuppliers(@Param("supplierIds") Collection<Long> supplierIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SupplierProcess p where p.supplier.id = :supplierId")
    void deleteForSupplier(@Param("supplierId") Long supplierId);
}
