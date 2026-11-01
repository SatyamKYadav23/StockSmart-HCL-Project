package com.hcl.stocksmart.repository;

import com.hcl.stocksmart.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LocationRepository extends JpaRepository<Location, Long> {

    boolean existsByName(String name);
}