package com.lynus.cs203.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
@Entity
@Table(name= "agreement_country")
@Data
public class AgreementCountry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_country_id")
    private Long agreementCountryId;

    @ManyToOne
    @JoinColumn(name = "agreement_id")
    private TradeAgreement agreement;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

}
