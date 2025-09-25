package com.lynus.cs203.services;

import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.ProductRepository;
import com.lynus.cs203.repositories.TariffRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DataMigrationService {

    private final CountryRepository countryRepository;
    private final ProductRepository productRepository;
    private final TariffRepository tariffRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataMigrationService(CountryRepository countryRepository,
                                ProductRepository productRepository,
                                TariffRepository tariffRepository,
                                JdbcTemplate jdbcTemplate) {
        this.countryRepository = countryRepository;
        this.productRepository = productRepository;
        this.tariffRepository = tariffRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public void migrateDate(){
        // Read from your sql table

    }
}
