package com.lynus.cs203.services;

import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.MigrationStatus;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.MigrationStatusRepository;
import com.lynus.cs203.repositories.ProductRepository;
import com.lynus.cs203.repositories.TariffRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataMigrationService {

    private final CountryRepository countryRepository;
    private final ProductRepository productRepository;
    private final TariffRepository tariffRepository;
    private final MigrationStatusRepository migrationStatusRepository;

    private static final int BATCH_SIZE = 200;
    private static final int PROGRESS_LOG_INTERVAL = 100;
    private static final int MAX_RETRY_ATTEMPTS = 3;

    public void migrateData(){
        log.info("Starting CSV data migration process");

        // Check if migration has already been completed
        Optional<MigrationStatus> status = migrationStatusRepository.findById("csv_data_migration");
        if (status.isPresent() && status.get().isCompleted()) {
            log.info("CSV migration already completed at {}, skipping", status.get().getCompletedAt());
            return;
        }

        // Pre-load existing data to avoid repeated queries
        Map<String, Country> existingCountries = loadExistingCountries();
        Map<Integer, Product> existingProducts = loadExistingProducts();

        // Process CSV in batches
        List<String[]> csvBatch = new ArrayList<>();
        int lineCount = 0;
        int totalProcessed = 0;
        long startTime = System.currentTimeMillis();

        try {
            ClassPathResource resource = new ClassPathResource("data/tariff_data.csv");
            log.info("Loading CSV file: {}", resource.getPath());

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

                    // Parse CSV line
                    String[] columns = parseCSVLine(line);
                    if (columns.length >= 8) {
                        csvBatch.add(columns);
                    } else {
                        log.warn("Line {}: Invalid format, skipping", lineCount);
                    }

                    // Process batch when it reaches batch size
                    if (csvBatch.size() >= BATCH_SIZE) {
                        totalProcessed += processBatchWithRetry(csvBatch, existingCountries, existingProducts);
                        csvBatch.clear();

                        // Progress logging
                        if (lineCount % PROGRESS_LOG_INTERVAL == 0) {
                            logProgress(lineCount, totalProcessed, startTime);
                        }
                    }
                }

                // Process remaining records
                if (!csvBatch.isEmpty()) {
                    totalProcessed += processBatchWithRetry(csvBatch, existingCountries, existingProducts);
                }

                long totalTime = System.currentTimeMillis() - startTime;
                log.info("CSV migration completed - Total lines: {}, Records saved: {}, Time: {}ms, Rate: {:.2f} records/sec",
                        lineCount - 1, totalProcessed, totalTime, (totalProcessed * 1000.0) / totalTime);

                // Mark migration as complete
                markMigrationComplete();

            }
        } catch (IOException e) {
            log.error("Failed to read CSV file", e);
            throw new RuntimeException("CSV migration failed", e);
        } catch (Exception e) {
            log.error("Unexpected error during migration", e);
            throw new RuntimeException("CSV migration failed", e);
        }
    }

    public int processBatchWithRetry(List<String[]> csvBatch, Map<String, Country> existingCountries, Map<Integer, Product> existingProducts) {
        int attempt = 0;
        DataAccessException lastException = null;

        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                attempt++;
                log.debug("Processing batch attempt {}/{}", attempt, MAX_RETRY_ATTEMPTS);
                return processBatch(csvBatch, existingCountries, existingProducts);

            } catch (DataAccessException e) {
                lastException = e;
                log.warn("Batch processing attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    // Exponential backoff
                    long backoffTime = (long) (2000 * Math.pow(2, attempt - 1));
                    log.debug("Waiting {}ms before retry...", backoffTime);
                    try {
                        Thread.sleep(backoffTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Migration interrupted during retry", ie);
                    }
                }
            }
        }
        // If we get here, all retries failed
        log.error("All {} retry attempts failed for batch processing", MAX_RETRY_ATTEMPTS);
        throw new RuntimeException("Failed to process batch after " + MAX_RETRY_ATTEMPTS + " attempts", lastException);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 300)
    public int processBatch(List<String[]> csvBatch, Map<String, Country> existingCountries, Map<Integer, Product> existingProducts) {
        log.debug("Processing batch of {} records", csvBatch.size());

        List<Country> newCountries = new ArrayList<>();
        List<Product> newProducts = new ArrayList<>();
        List<Tariff> newTariffs = new ArrayList<>();
        int errorCount = 0;

        for (String[] columns : csvBatch) {
            try {
                String countryCode = columns[1].trim();
                String countryName = removeQuotes(columns[2].trim());
                Integer productCode = Integer.parseInt(columns[3].trim());
                String hsDescription = removeQuotes(columns[4].trim());
                String hsUom = columns[5].trim();
                String foodCategory = removeQuotes(columns[6].trim());
                Double tariffRate = Double.parseDouble(columns[7].trim());

                // Get or create country
                Country country = existingCountries.get(countryCode);
                if (country == null) {
                    country = new Country();
                    country.setCountryCode(countryCode);
                    country.setCountryName(countryName);
                    existingCountries.put(countryCode, country);
                    newCountries.add(country);
                }

                // Get or create product
                Product product = existingProducts.get(productCode);
                if (product == null) {
                    product = new Product();
                    product.setProductCode(productCode);
                    product.setProductDescription(hsDescription);
                    product.setUomCode(hsUom);
                    product.setFoodCategory(foodCategory);
                    existingProducts.put(productCode, product);
                    newProducts.add(product);
                }

                // Create tariff
                Tariff tariff = new Tariff();
                tariff.setProduct(product);
                tariff.setCountry(country);
                tariff.setTariffRate(tariffRate);
                newTariffs.add(tariff);

            } catch (Exception e) {
                log.warn("Error processing CSV row: {}. Data: {}", e.getMessage(), Arrays.toString(columns));
                errorCount++;
            }
        }

        // Save all entities in order
        int saved = 0;
        if (!newCountries.isEmpty()) {
            countryRepository.saveAll(newCountries);
            saved += newCountries.size();
            log.debug("Saved {} new countries", newCountries.size());
        }

        if (!newProducts.isEmpty()) {
            productRepository.saveAll(newProducts);
            saved += newProducts.size();
            log.debug("Saved {} new products", newProducts.size());
        }

        if (!newTariffs.isEmpty()) {
            tariffRepository.saveAll(newTariffs);
            saved += newTariffs.size();
            log.debug("Saved {} new tariffs", newTariffs.size());
        }
        log.debug("Batch processed - Saved: {}, Errors: {}", saved, errorCount);
        return saved;
    }

    private Map<String, Country> loadExistingCountries() {
        log.info("Loading existing countries...");
        Map<String, Country> countries = new HashMap<>();
        countryRepository.findAll().forEach(c -> countries.put(c.getCountryCode(), c));
        log.info("Loaded {} countries total", countries.size());
        return countries;
    }

    private Map<Integer, Product> loadExistingProducts() {
        log.info("Loading existing products...");
        Map<Integer, Product> products = new HashMap<>();
        productRepository.findAll().forEach(p -> products.put(p.getProductCode(), p));
        log.info("Loaded {} products total", products.size());
        return products;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 60)
    public void markMigrationComplete() {
        MigrationStatus migrationStatus = migrationStatusRepository.findById("csv_data_migration")
                .orElse(new MigrationStatus());
        migrationStatus.setMigrationName("csv_data_migration");
        migrationStatus.setCompleted(true);
        migrationStatus.setCompletedAt(LocalDateTime.now());
        migrationStatusRepository.save(migrationStatus);
        log.info("Migration status updated to completed");
    }

    private void logProgress(int lineCount, int totalProcessed, long startTime) {
        long elapsed = System.currentTimeMillis() - startTime;
        double recordsPerSecond = totalProcessed / (elapsed / 1000.0);
        double linesPerSecond = lineCount / (elapsed / 1000.0);

        log.info("Progress: {} lines read, {} records saved, {:.2f} records/sec, {:.2f} lines/sec",
                lineCount, totalProcessed, recordsPerSecond, linesPerSecond);
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