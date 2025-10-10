package com.egov2025.repository;

import com.egov2025.model.Cupon;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CuponRepository extends JpaRepository<Cupon, Long> {
    Optional<Cupon> findByCodIgnoreCase(String cod);
}
