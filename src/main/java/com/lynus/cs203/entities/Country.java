package com.lynus.cs203.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.List;

@RequiredArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "country")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "country_id")
    private Long countryId;

    @Column(name = "country_code", unique = true)
    private String countryCode; // reporter_code

    @Column(name = "country_name")
    private String countryName; // reporter_name

    @OneToMany(mappedBy = "country")
    private List<Tariff> tariffs;

}
