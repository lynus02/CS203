package com.lynus.cs203.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "saved_products",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"user_id", "product_id"})},
        indexes = {
                @Index(name = "idx_user_id", columnList = "user_id"),
                @Index(name = "idx_saved_at", columnList = "saved_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedProductConfig {

    @Id
    private String id; // matches VARCHAR(255) primary key in your table

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "product_id", nullable = false)
    private String productId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "hs_code", nullable = false)
    private String hsCode;

    @Column(nullable = false)
    private String category;

    @Column(name = "base_tariff_rate", nullable = false)
    private double baseTariffRate;

    @Column(name = "product_value", nullable = false)
    private double productValue;

    @Column(name = "origin_country", nullable = false)
    private String originCountry;

    @Column(name = "destination_country", nullable = false)
    private String destinationCountry;

    @Column(name = "import_date", nullable = false)
    private LocalDateTime importDate;

    @Column(name = "saved_at")
    private LocalDateTime savedAt;
}
