package com.vahan.oneops.material;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends JpaRepository<Material, Long> {

    Optional<Material> findByNameIgnoreCase(String name);

    List<Material> findByActiveTrue();
}
