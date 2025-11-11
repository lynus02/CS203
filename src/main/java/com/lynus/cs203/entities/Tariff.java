package com.lynus.cs203.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@RequiredArgsConstructor
@Getter
@Setter
@Entity
@Table (name = "tariff")
public class Tariff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Long tradeId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

    @Column(name = "tariff_rate")
    private double tariffRate; // original "value" column

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate = LocalDate.of(2020, 1, 1);
}
