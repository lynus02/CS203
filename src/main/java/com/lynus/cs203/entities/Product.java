package com.lynus.cs203.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@RequiredArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_id")
    private Long productId;

    @Column(name = "product_code", unique = true)
    private String productCode; // hsCode

    @Column(name = "hs_description")
    private String productDescription; //hs_description

    @Column(name = "hs_uom")
    private String uomCode;

    @Column(name = "food_category")
    private String foodCategory;

}
