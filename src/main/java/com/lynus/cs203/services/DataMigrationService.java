package com.lynus.cs203.services;

import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.ProductRepository;
import com.lynus.cs203.repositories.TariffRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.List;

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

    public void migrateData(){
        // Read from your sql table
        String sql = "SELECT * FROM tariff_data";

        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);

        for (Map<String,Object> row : rows) {
            String countryCode = (String) row.get("reporter_code");
            String countryName = (String) row.get("reporter_name");
            Integer productCode = ((Number) row.get("product_code_6")).intValue();
            String hsDescription = (String) row.get("hs_description");
            String hsUom = (String) row.get("hs_uom");
            String hsCategory = (String) row.get("hs_category");
            String foodCategory = (String) row.get("food_category");
            Double tariffRate = ((Number) row.get("value")).doubleValue();

            // Create country
            Country country = countryRepository.findByCountryCode(countryCode)
                    .orElseGet(() -> countryRepository.save(new Country(countryCode, countryName)));

            // Create product
            Product product = productRepository.findByProductCode(productCode)
                    .orElseGet(() -> productRepository.save(new Product(productCode, hsDescription, hsUom, foodCategory)));

            // Create tariff
            Tariff tariff = new Tariff(product, country, tariffRate);
            tariffRepository.save(tariff);
        }

        System.out.println("Data migration completed.");
    }
}
