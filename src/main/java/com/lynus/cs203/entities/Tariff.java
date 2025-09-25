package com.lynus.cs203.entities;

import jakarta.persistence.*;

@Entity
@Table (name = "tariff")
public class Tariff {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tradeId;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne
    @JoinColumn(name = "country_id")
    private Country country;

    private double tariffRate; // original "value" column

    // contructor
    public Tariff() {

    }

    public Tariff(Long id, Product product, Country country, double tariffRate) {
        this.tradeId = id;
        this.product = product;
        this.country = country;
        this.tariffRate = tariffRate;
    }
    // getters and setters
    public Long getId() {
        return tradeId;
    }

    public void setId(Long id) {
        this.tradeId = id;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Country getCountry() {
        return country;
    }

    public void setCountry(Country country) {
        this.country = country;
    }

    public double getTariffRate() {
        return tariffRate;
    }

    public void setTariffRate(double tariffRate) {
        this.tariffRate = tariffRate;
    }
}
