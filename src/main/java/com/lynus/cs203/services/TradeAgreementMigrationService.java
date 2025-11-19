package com.lynus.cs203.services;

import com.lynus.cs203.entities.AgreementCountry;
import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.MigrationStatus;
import com.lynus.cs203.entities.TradeAgreement;
import com.lynus.cs203.repositories.AgreementCountryRepository;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.MigrationStatusRepository;
import com.lynus.cs203.repositories.TradeAgreementRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeAgreementMigrationService {
    private final TradeAgreementRepository tradeAgreementRepository;
    private final AgreementCountryRepository agreementCountryRepository;
    private final CountryRepository countryRepository;
    private final MigrationStatusRepository migrationStatusRepository;

    private static final int BATCH_SIZE = 100;  // Increased for RDS
    private static final int MAX_RETRY_ATTEMPTS = 3;

    public void migrateTradeAgreements() {
        log.info("Starting trade agreement migration process");

        // check if migration has already been completed
        Optional<MigrationStatus> status = migrationStatusRepository.findById("csv_tradeAgreement_migration");
        if (status.isPresent() && status.get().isCompleted()) {
            log.info("Trade agreement migration already completed at {}, skipping", status.get().getCompletedAt());
            return;
        }

        // Pre-load existing data with pagination
        Map<String, Country> existingCountriesByName = loadExistingCountriesByName();
        Map<String, TradeAgreement> existingAgreements = loadExistingAgreements();

        // Process CSV in batches
        List<String[]> csvBatch = new ArrayList<>();
        int lineCount = 0;
        int totalProcessed = 0;
        long startTime = System.currentTimeMillis();

        try {
            ClassPathResource resource = new ClassPathResource("data/Cleaned_Trade_Agreements1.csv");
            log.debug("Loading trade agreement CSV file: {}", resource.getPath());

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
                    if (columns.length >= 5) {
                        csvBatch.add(columns);
                    } else {
                        log.warn("Line {}: Invalid format, skipping", lineCount);
                    }

                    // Process batch when it reaches batch size
                    if (csvBatch.size() >= BATCH_SIZE) {
                        totalProcessed += processBatchWithRetry(csvBatch, existingCountriesByName, existingAgreements);
                        csvBatch.clear();

                        if (lineCount % 50 == 0) {
                            log.info("Processed {} lines, total records saved: {}", lineCount, totalProcessed);
                        }
                    }
                }

                // Process remaining records
                if (!csvBatch.isEmpty()) {
                    totalProcessed += processBatchWithRetry(csvBatch, existingCountriesByName, existingAgreements);
                }

                long totalTime = System.currentTimeMillis() - startTime;
                log.info("Trade agreement migration completed - Total lines: {}, Records saved: {}, Time: {}ms",
                        lineCount - 1, totalProcessed, totalTime);

                // Mark migration as complete
                markMigrationComplete();

            }
        } catch (IOException e) {
            log.error("Failed to read trade agreement CSV file", e);
            throw new RuntimeException("Trade agreement migration failed", e);
        } catch (Exception e) {
            log.error("Unexpected error during trade agreement migration", e);
            throw new RuntimeException("Trade agreement migration failed", e);
        }
    }

    public int processBatchWithRetry(List<String[]> csvBatch, Map<String, Country> existingCountriesByName, Map<String, TradeAgreement> existingAgreements) {
        int attempt = 0;
        DataAccessException lastException = null;

        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                attempt++;
                log.debug("Processing trade agreement batch attempt {}/{}", attempt, MAX_RETRY_ATTEMPTS);
                return processBatch(csvBatch, existingCountriesByName, existingAgreements);
            } catch (DataAccessException e) {
                lastException = e;
                log.warn("Trade agreement batch processing attempt {} failed: {}", attempt, e.getMessage());

                if (attempt < MAX_RETRY_ATTEMPTS) {
                    // Exponential backoff
                    long backoffTime = (long) (2000 * Math.pow(2, attempt - 1));
                    log.debug("Waiting {}ms before retry...", backoffTime);
                    try {
                        Thread.sleep(backoffTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Trade agreement migration interrupted during retry", ie);
                    }
                }
            }
        }

        log.error("All {} retry attempts failed for trade agreement batch processing", MAX_RETRY_ATTEMPTS);
        throw new RuntimeException("Failed to process trade agreement batch after " + MAX_RETRY_ATTEMPTS + " attempts", lastException);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 300)
    public int processBatch(List<String[]> csvBatch, Map<String, Country> existingCountriesByName, Map<String, TradeAgreement> existingAgreements) {
        List<TradeAgreement> newAgreements = new ArrayList<>();
        List<AgreementCountry> newAgreementCountries = new ArrayList<>();
        int skippedCountries = 0;
        int errorCount = 0;

        for (String[] columns : csvBatch) {
            try {
                String agreementName = removeQuotes(columns[0].trim());
                String agreementType = removeQuotes(columns[1].trim());
                String effectiveDateStr = removeQuotes(columns[2].trim());
                String expirationDateStr = removeQuotes(columns[3].trim());
                String signatories = removeQuotes(columns[4].trim());
                System.out.println("Raw effective_date string: " + effectiveDateStr);
                System.out.println("Raw expiration_date string: " + expirationDateStr);

                // Get or create trade agreement
                TradeAgreement agreement = existingAgreements.get(agreementName);
                if (agreement == null) {
                    agreement = new TradeAgreement();
                    agreement.setAgreementName(agreementName);
                    agreement.setAgreementType(agreementType);
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
                    agreement.setEffectiveDate(LocalDate.parse(effectiveDateStr, formatter));
                    agreement.setExpirationDate(LocalDate.parse(expirationDateStr, formatter));
                    existingAgreements.put(agreementName, agreement);
                    newAgreements.add(agreement);
                }

                // Process signatories (country names)
                String[] countryNames = signatories.split(";");

                for (String countryName : countryNames) {
                    String trimmedCountryName = countryName.trim();

                    // Find country by name
                    Country country = existingCountriesByName.get(trimmedCountryName);
                    if (country == null) {
                        log.warn("Country not found: '{}' in agreement: {}", trimmedCountryName, agreementName);
                        skippedCountries++;
                        continue;
                    }

                    // Create agreement-country relationship
                    AgreementCountry agreementCountry = new AgreementCountry();
                    agreementCountry.setAgreement(agreement);
                    agreementCountry.setCountry(country);
                    newAgreementCountries.add(agreementCountry);
                }

            } catch (Exception e) {
                log.warn("Error processing trade agreement CSV row: {}", e.getMessage());
                errorCount++;
            }
        }

        // Save all entities in order
        int saved = 0;

        if (!newAgreements.isEmpty()) {
            tradeAgreementRepository.saveAll(newAgreements);
            saved += newAgreements.size();
            log.debug("Saved {} new trade agreements", newAgreements.size());
        }

        if (!newAgreementCountries.isEmpty()) {
            // Remove duplicates before saving
            Set<String> existingRelationships = new HashSet<>();
            List<AgreementCountry> uniqueRelationships = new ArrayList<>();

            for (AgreementCountry ac : newAgreementCountries) {
                String key = ac.getAgreement().getAgreementName() + ":" + ac.getCountry().getCountryName();
                if (!existingRelationships.contains(key)) {
                    existingRelationships.add(key);
                    uniqueRelationships.add(ac);
                }
            }

            agreementCountryRepository.saveAll(uniqueRelationships);
            saved += uniqueRelationships.size();
            log.debug("Saved {} new agreement-country relationships", uniqueRelationships.size());
        }

        if (skippedCountries > 0) {
            log.debug("Skipped {} countries not found in database", skippedCountries);
        }
        if (errorCount > 0) {
            log.debug("Encountered {} errors in batch", errorCount);
        }

        log.debug("Trade agreement batch processed - Saved: {}, Errors: {}", saved, errorCount);
        return saved;
    }

    private Map<String, Country> loadExistingCountriesByName() {
        log.info("Loading existing countries by name...");
        Map<String, Country> countries = new HashMap<>();
        countryRepository.findAll().forEach(c -> countries.put(c.getCountryName(), c));
        log.info("Loaded {} countries by name", countries.size());
        return countries;
    }

    private Map<String, TradeAgreement> loadExistingAgreements() {
        log.info("Loading existing trade agreements...");
        Map<String, TradeAgreement> agreements = new HashMap<>();
        tradeAgreementRepository.findAll().forEach(a -> agreements.put(a.getAgreementName(), a));
        log.info("Loaded {} trade agreements", agreements.size());
        return agreements;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 60)
    public void markMigrationComplete() {
        MigrationStatus migrationStatus = migrationStatusRepository.findById("csv_tradeAgreement_migration")
                .orElse(new MigrationStatus());
        migrationStatus.setMigrationName("csv_tradeAgreement_migration");
        migrationStatus.setCompleted(true);
        migrationStatus.setCompletedAt(LocalDateTime.now());
        migrationStatusRepository.save(migrationStatus);
        log.info("Trade agreement migration status updated to completed");
    }

    private String[] parseCSVLine(String line) {
        // Simple CSV parser that handles quoted strings containing commas
        return line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
    }

    private String removeQuotes(String input) {
        if (input.startsWith("\"") && input.endsWith("\"")) {
            return input.substring(1, input.length() - 1);
        }
        return input;
    }
}