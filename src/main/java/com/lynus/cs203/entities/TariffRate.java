package com.lynus.cs203.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tariff_data")
public class TariffRate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int trade_id;

    private String reporter_code;

    @Column(name = "reporter_name")
    private String reporterName;

    @Column(name = "product_code_6")
    private String productCode6;

    @Column(name = "hs_description")
    private String hsDescription;
    private String hs_uom;
    private String food_category;
    private int value;
}