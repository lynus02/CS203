package com.lynus.cs203.services;

import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.MigrationStatus;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.MigrationStatusRepository;
import com.lynus.cs203.repositories.ProductRepository;
import com.lynus.cs203.repositories.TariffRepository;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class DataMigrationService {

    private final CountryRepository countryRepository;
    private final ProductRepository productRepository;
    private final TariffRepository tariffRepository;
    private final MigrationStatusRepository migrationStatusRepository;

    public void migrateData(){
        // Check if migration has already been completed
        Optional<MigrationStatus> status = migrationStatusRepository.findById("csv_data_migration");
        if (status.isPresent() && status.get().isCompleted()) {
            System.out.println("CSV migration already completed, skipping.");
            return;
        }

        try {
            ClassPathResource resource = new ClassPathResource("data/tariff_data.csv");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false; // Skip header line
                        continue;
                    }

                    String[] columns = parseCSVLine(line);

                    // Skip trade_id (index 0), parse from index 1 onwards
                    String countryCode = columns[1].trim();
                    String countryName = removeQuotes(columns[2].trim());
                    Integer productCode = Integer.parseInt(columns[3].trim());
                    String hsDescription = removeQuotes(columns[4].trim());
                    String hsUom = columns[5].trim();
                    String foodCategory = removeQuotes(columns[6].trim());
                    Double tariffRate = Double.parseDouble(columns[7].trim());

                    // Create country
                    Country country = countryRepository.findByCountryCode(countryCode)
                            .orElseGet(() -> {
                                Country newCountry = new Country();
                                newCountry.setCountryCode(countryCode);
                                newCountry.setCountryName(countryName);
                                return countryRepository.save(newCountry);
                            });

                    // Create product
                    Product product = productRepository.findByProductCode(productCode)
                            .orElseGet(() -> {
                                Product newProduct = new Product();
                                newProduct.setProductCode(productCode);
                                newProduct.setProductDescription(hsDescription);
                                newProduct.setUomCode(hsUom);
                                newProduct.setFoodCategory(foodCategory);
                                return productRepository.save(newProduct);
                            });

                    // Create tariff
                    Tariff tariff = new Tariff();
                    tariff.setProduct(product);
                    tariff.setCountry(country);
                    tariff.setTariffRate(tariffRate);
                    tariffRepository.save(tariff);
                }
            }
            System.out.println("Data migration from CSV completed.");

        } catch (IOException e) {
            System.err.println("Error reading tariff CSV file: " + e.getMessage());
            throw new RuntimeException("Failed to read tariff_data.csv", e);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing number from tariff CSV: " + e.getMessage());
            throw new RuntimeException("Invalid number format in tariff_data.csv", e);
        } catch (Exception e) {
            System.err.println("Unexpected error during tariff data migration: " + e.getMessage());
            throw new RuntimeException("Tariff data migration failed", e);
        }

        // Mark migration as complete
        MigrationStatus migrationStatus = new MigrationStatus();
        migrationStatus.setMigrationName("csv_data_migration");
        migrationStatus.setCompleted(true);
        migrationStatus.setCompletedAt(LocalDateTime.now());
        migrationStatusRepository.save(migrationStatus);
    }

    private String[] parseCSVLine(String line) {
        // Simple CSV parser that handles quoted strings containing commas
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
    }

    private String removeQuotes(String str) {
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }
}