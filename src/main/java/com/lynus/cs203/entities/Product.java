package com.lynus.cs203.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "product")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "Product_Code")
    private String productCode; // hsCode

    @Getter
    @Column(name = "HS_Description")
    private String productDescription; //hs_description

    @Column(name = "HS_UOM")
    private String uomCode;

    @Column(name = "food_category")
    private String foodCategory;

    public Product() {
    }

    public Product(String productCode, String productDescription, String uomCode, String foodCategory) {
        this.productCode = productCode;
        this.productDescription = productDescription;
        this.uomCode = uomCode;
        this.foodCategory = foodCategory;
    }
    // getters and setters
}
