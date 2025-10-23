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
@Table(name= "trade_agreement")
@Data
public class TradeAgreement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "agreement_id")
    private Long agreementId;

    @Column(name = "agreement_name", unique = true)
    private String agreementName; // e.g., "NAFTA", "EU", etc.

    @Column(name = "agreement_type")
    private String agreementType; // e.g., "FTA", "EIA", etc.

    @Column(name = "status")
    private String status = "In force"; // default as In force

}
