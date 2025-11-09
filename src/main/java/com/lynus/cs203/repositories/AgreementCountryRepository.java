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
    @Query("SELECT ac.country FROM AgreementCountry ac WHERE ac.agreement.agreementId = :agreementId")
    List<Country> findCountriesByAgreementId(@Param("agreementId") Long agreementId);

    // Find all agreements that include a specific country
    @Query("SELECT ac.agreement.agreementId FROM AgreementCountry ac WHERE ac.country.countryName = :countryName")
    List<Long> findAgreementsByCountryName(@Param("countryName") String countryName);

    Optional<AgreementCountry> findByAgreementAndCountry(TradeAgreement agreement, Country country);

    // Find agreements between two countries (where both are signatories)
    @Query("""
    SELECT ac.agreement.agreementId
    FROM AgreementCountry ac
    WHERE LOWER(ac.country.countryName) IN (LOWER(:countryA), LOWER(:countryB))
    GROUP BY ac.agreement.agreementId
    HAVING COUNT(DISTINCT LOWER(ac.country.countryName)) = 2
    """)
    List<Long> findAgreementsBetweenCountries(
            @Param("countryA") String countryA,
            @Param("countryB") String countryB
    );



    // Additional useful queries
    @Query("SELECT COUNT(ac) FROM AgreementCountry ac WHERE ac.agreement.agreementId = :agreementId")
    int countCountriesInAgreement(@Param("agreementId") Long agreementId);

    @Query("SELECT ac FROM AgreementCountry ac WHERE ac.country.countryId = :countryId")
    List<AgreementCountry> findByCountryId(@Param("countryId") Long countryId);
}