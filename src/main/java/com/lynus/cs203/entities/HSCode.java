package com.lynus.cs203.entities;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "hs_codes")
public class HSCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Getter
    @Column(name = "HS_Code")
    private String hsCode;

    @Getter
    @Column(name = "HS_Description")
    private String description;

    @Column(name = "HS_UOM")
    private String uomCode;

    @Column(name = "ReferenceId")
    private String referenceId;

    // getters and setters
}
