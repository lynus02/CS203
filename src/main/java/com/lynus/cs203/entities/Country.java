package com.lynus.cs203.entities;

import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "country")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String countryCode; // reporter_code

    @Column(name = "country_name")
    private String countryName; // reporter_name

    @OneToMany(mappedBy = "country")
    private List<Tariff> tariffs;

    // Contructor
    public Country() {
    }

    public Country(String countryCode, String name) {
        this.countryCode = countryCode;
        this.countryName = name;
    }

    // getters and setters


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getName() {
        return countryName;
    }

    public void setName(String name) {
        this.countryName = name;
    }

    public List<Tariff> getTariffs() {
        return tariffs;
    }

    public void setTariffs(List<Tariff> tariffs) {
        this.tariffs = tariffs;
    }
}
