package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.AgreementCountry;
import com.lynus.cs203.entities.TradeAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TradeAgreementRepository extends JpaRepository<TradeAgreement, Long>{
    Optional<TradeAgreement> findByAgreementName(String agreementName);
    Optional<TradeAgreement> findByAgreementId(Long agreementId);

    @Query("SELECT t FROM TradeAgreement t WHERE t.agreementId IN :ids")
    List<TradeAgreement> findAllByIds(@Param("ids") List<Long> ids);

    // Add to TradeAgreementRepository.java
    @Query("SELECT DISTINCT ta.agreementId FROM TradeAgreement ta " +
           "JOIN AgreementCountry ac1 ON ta.agreementId = ac1.agreement.agreementId " +
           "JOIN AgreementCountry ac2 ON ta.agreementId = ac2.agreement.agreementId " +
           "WHERE ac1.country.countryName = :exportCountry " +
           "AND ac2.country.countryName = :destCountry " +
           "AND (ta.effectiveDate IS NULL OR ta.effectiveDate <= :date) " +
           "AND (ta.expirationDate IS NULL OR ta.expirationDate >= :date)")
    List<Long> findActiveAgreementsBetweenCountriesOnDate(
            @Param("exportCountry") String exportCountry,
            @Param("destCountry") String destCountry,
            @Param("date") LocalDate date);
}