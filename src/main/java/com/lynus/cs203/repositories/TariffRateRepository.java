package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.TariffRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface TariffRateRepository extends JpaRepository<TariffRate, Integer> {
    Page<TariffRate> findByHsDescriptionContainingIgnoreCaseOrProductCode6ContainingIgnoreCase(String hsDescription, String productCode6, Pageable pageable);
    Page<TariffRate> findByHsDescriptionContainingIgnoreCaseAndProductCode6ContainingIgnoreCaseAndReporterNameContainingIgnoreCase(
            String hsDescription, String productCode6, String reporter_name, Pageable pageable);
    Page<TariffRate> findByReporterNameContainingIgnoreCase(String country, Pageable pageable);

    Page<TariffRate> findByReporterNameContainingIgnoreCaseAndHsDescriptionContainingIgnoreCase(
            String country, String query, Pageable pageable
    );

    Page<TariffRate> findByReporterNameContainingIgnoreCaseAndProductCode6ContainingIgnoreCase(
            String country, String code, Pageable pageable
    );


}