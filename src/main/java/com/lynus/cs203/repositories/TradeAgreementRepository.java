package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.AgreementCountry;
import com.lynus.cs203.entities.TradeAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradeAgreementRepository extends JpaRepository<TradeAgreement, Long>{
    Optional<TradeAgreement> findByAgreementName(String agreementName);
    Optional<TradeAgreement> findByAgreementId(Long agreementId);

    @Query("SELECT t FROM TradeAgreement t WHERE t.agreementId IN :ids")
    List<TradeAgreement> findAllByIds(@Param("ids") List<Long> ids);
}