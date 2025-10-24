package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.AgreementCountry;
import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.TradeAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgreementCountryRepository extends JpaRepository<AgreementCountry, Long> {
    // Find all countries in an agreement
    @Query("Select ac.country from AgreementCountry ac where ac.agreement = :agreementId")
    List<String> findCountriesByAgreementId(@Param("agreementId") Long agreementId);

    // Find all agreements that include a specific country
    @Query("Select ac.agreement from AgreementCountry ac where ac.country = :countryName")
    List<Long> findAgreementsByCountryName(@Param("countryName") String countryName);

    Optional<AgreementCountry> findByAgreementAndCountry(TradeAgreement agreement, Country country);
}