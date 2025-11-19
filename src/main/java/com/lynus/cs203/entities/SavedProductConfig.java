package com.lynus.cs203.entities;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "saved_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedProductConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK → users.user_id */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
    private User user;

    /** FK → product.product_id */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "product_id", nullable = false)
    private Product product;

    @Column(name = "config_name", nullable = false)
    private String configName;

    @Column(name = "product_value", nullable = false)
    private double productValue;

    /** FK → country.country_id */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "origin_country_id", referencedColumnName = "country_id", nullable = false)
    private Country originCountry;

    /** FK → country.country_id */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "destination_country_id", referencedColumnName = "country_id", nullable = false)
    private Country destinationCountry;

    @Column(name = "import_date", nullable = false)
    private LocalDateTime importDate;

    @Column(name = "saved_at", nullable = false)
    private LocalDateTime savedAt = LocalDateTime.now();
}
