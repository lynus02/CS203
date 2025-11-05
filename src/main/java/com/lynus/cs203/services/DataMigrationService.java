package com.lynus.cs203.services;

import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.MigrationStatus;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.MigrationStatusRepository;
import com.lynus.cs203.repositories.ProductRepository;
import com.lynus.cs203.repositories.TariffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataMigrationService {

    private final CountryRepository countryRepository;
    private final ProductRepository productRepository;
    private final TariffRepository tariffRepository;
    private final MigrationStatusRepository migrationStatusRepository;

    public void migrateData(){
        log.info("Starting CSV data migration process");

        // Check if migration has already been completed
        Optional<MigrationStatus> status = migrationStatusRepository.findById("csv_data_migration");
        if (status.isPresent() && status.get().isCompleted()) {
            log.info("CSV migration already completed at {}, skipping", status.get().getCompletedAt());
            return;
        }

        // Use AtomicInteger for counters in lambda expressions
        AtomicInteger countryCount = new AtomicInteger(0);
        AtomicInteger productCount = new AtomicInteger(0);
        AtomicInteger tariffCount = new AtomicInteger(0);
        int lineCount = 0;

        try {
            ClassPathResource resource = new ClassPathResource("data/tariff_data.csv");
            log.debug("Loading CSV file: {}", resource.getPath());

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    lineCount++;

                    if (isFirstLine) {
                        log.debug("Skipping CSV header line");
                        isFirstLine = false;    // Skip header line
                        continue;
                    }

                    if (lineCount % 1000 == 0) {
                        log.debug("Processing line {}...", lineCount);
                    }

                    String[] columns = parseCSVLine(line);

                    // Validate column count
                    if (columns.length < 8) {
                        log.warn("Skipping invalid line {}: expected 8 columns, got {}", lineCount, columns.length);
                        continue;
                    }

                    // Skip trade_id (index 0), parse from index 1 onwards
                    String countryCode = columns[1].trim();
                    String countryName = removeQuotes(columns[2].trim());
                    Integer productCode = Integer.parseInt(columns[3].trim());
                    String hsDescription = removeQuotes(columns[4].trim());
                    String hsUom = columns[5].trim();
                    String foodCategory = removeQuotes(columns[6].trim());
                    Double tariffRate = Double.parseDouble(columns[7].trim());

                    // Create or get country
                    Country country = countryRepository.findByCountryCode(countryCode)
                            .orElseGet(() -> {
                                Country newCountry = new Country();
                                newCountry.setCountryCode(countryCode);
                                newCountry.setCountryName(countryName);
                                Country saved = countryRepository.save(newCountry);
                                countryCount.incrementAndGet();
                                log.trace("Created new country: {} - {}", countryCode, countryName);
                                return saved;
                            });

                    // Create or get product
                    Product product = productRepository.findByProductCode(productCode)
                            .orElseGet(() -> {
                                Product newProduct = new Product();
                                newProduct.setProductCode(productCode);
                                newProduct.setProductDescription(hsDescription);
                                newProduct.setUomCode(hsUom);
                                newProduct.setFoodCategory(foodCategory);
                                Product saved = productRepository.save(newProduct);
                                productCount.incrementAndGet();
                                log.trace("Created new product: {} - {}", productCode, hsDescription);
                                return saved;
                            });

                    // Create tariff
                    Tariff tariff = new Tariff();
                    tariff.setProduct(product);
                    tariff.setCountry(country);
                    tariff.setTariffRate(tariffRate);
                    tariffRepository.save(tariff);
                    tariffCount.incrementAndGet();
                }
            }
            log.info("CSV data migration completed successfully - Countries: {}, Products: {}, Tariffs: {}, Total lines: {}",
                    countryCount.get(), productCount.get(), tariffCount.get(), lineCount - 1); // Subtract header line

        } catch (IOException e) {
            log.error("Error reading tariff CSV file", e);
            throw new RuntimeException("Failed to read tariff_data.csv", e);
        } catch (NumberFormatException e) {
            log.error("Error parsing number from tariff CSV at line {}", lineCount, e);
            throw new RuntimeException("Invalid number format in tariff_data.csv at line " + lineCount, e);
        } catch (Exception e) {
            log.error("Unexpected error during tariff data migration at line {}", lineCount, e);
            throw new RuntimeException("Tariff data migration failed at line " + lineCount, e);
        }

        // Mark migration as complete
        MigrationStatus migrationStatus = new MigrationStatus();
        migrationStatus.setMigrationName("csv_data_migration");
        migrationStatus.setCompleted(true);
        migrationStatus.setCompletedAt(LocalDateTime.now());
        migrationStatusRepository.save(migrationStatus);

        log.info("Migration status updated to completed");
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