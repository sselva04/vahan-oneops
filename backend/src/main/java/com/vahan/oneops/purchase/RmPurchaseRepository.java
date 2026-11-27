package com.vahan.oneops.purchase;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RmPurchaseRepository extends JpaRepository<RmPurchase, Long> {
    List<RmPurchase> findAllByOrderByInDateDescIdDesc();
}
