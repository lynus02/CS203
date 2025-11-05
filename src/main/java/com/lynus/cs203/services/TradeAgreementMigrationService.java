package com.lynus.cs203.services;

import com.lynus.cs203.entities.AgreementCountry;
import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.MigrationStatus;
import com.lynus.cs203.entities.TradeAgreement;
import com.lynus.cs203.repositories.AgreementCountryRepository;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.MigrationStatusRepository;
import com.lynus.cs203.repositories.TradeAgreementRepository;
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
public class TradeAgreementMigrationService {
    private final TradeAgreementRepository tradeAgreementRepository;
    private final AgreementCountryRepository agreementCountryRepository;
    private final CountryRepository countryRepository;
    private final MigrationStatusRepository migrationStatusRepository;

    public void migrateTradeAgreements() {
        log.info("Starting trade agreement migration process");

        // check if migration has already been completed
        Optional<MigrationStatus> status = migrationStatusRepository.findById("csv_tradeAgreement_migration");
        if (status.isPresent() && status.get().isCompleted()) {
            log.info("Trade agreement migration already completed at {}, skipping", status.get().getCompletedAt());
            return;
        }

        AtomicInteger agreementCount = new AtomicInteger(0);
        AtomicInteger countryRelationCount = new AtomicInteger(0);
        AtomicInteger skippedCountryCount = new AtomicInteger(0);
        int lineCount = 0;

        try {
            ClassPathResource resource = new ClassPathResource("data/trade_agreement.csv");
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

                    if (lineCount % 100 == 0) {
                        log.debug("Processing trade agreement line {}...", lineCount);
                    }

                    String[] columns = parseCSVLine(line);

                    // Validate column count
                    if (columns.length < 3) {
                        log.warn("Skipping invalid line {}: expected 3 columns, got {}", lineCount, columns.length);
                        continue;
                    }

                    // Parse columns from CSV
                    String agreementName = removeQuotes(columns[0].trim());
                    String agreementType = removeQuotes(columns[1].trim());
                    String signatories = removeQuotes(columns[2].trim());

                    log.debug("Processing agreement: {} - {}", agreementName, agreementType);

                    // Create or get trade agreement
                    TradeAgreement agreement = tradeAgreementRepository.findByAgreementName(agreementName)
                            .orElseGet(() -> {
                                TradeAgreement newAgreement = new TradeAgreement();
                                newAgreement.setAgreementName(agreementName);
                                newAgreement.setAgreementType(agreementType);
                                TradeAgreement saved = tradeAgreementRepository.save(newAgreement);
                                agreementCount.incrementAndGet();
                                log.trace("Created new trade agreement: {} - {}", agreementName, agreementType);
                                return saved;
                            });

                    // Process signatories (country codes)
                    String[] countryNames = signatories.split(";");
                    log.debug("Processing {} signatories for agreement: {}", countryNames.length, agreementName);

                    for (String countryName : countryNames) {
                        String trimmedCountryName = countryName.trim();

                        // Fetch the country entity
                        Optional<Country> countryOpt = countryRepository.findByCountryName(trimmedCountryName);

                        if (countryOpt.isEmpty()) {
                            // Try to find by country code as a fallback
                            countryOpt = countryRepository.findByCountryCode(trimmedCountryName);
                        }

                        if (countryOpt.isEmpty()) {
                            log.warn("Country not found by name or code: '{}' in agreement: {}",
                                    trimmedCountryName, agreementName);
                            skippedCountryCount.incrementAndGet();
                            continue;
                        }

                        Country country = countryOpt.get();

                        // Check if this country relationship already exists
                        boolean relationshipExists = agreementCountryRepository
                                .findByAgreementAndCountry(agreement, country)
                                .isPresent();

                        if (!relationshipExists) {
                            AgreementCountry agreementCountry = new AgreementCountry();
                            agreementCountry.setAgreement(agreement);
                            agreementCountry.setCountry(country);
                            agreementCountryRepository.save(agreementCountry);
                            countryRelationCount.incrementAndGet();

                            log.trace("Created agreement-country relationship: {} - {}",
                                    agreementName, country.getCountryName());
                        } else {
                            log.trace("Agreement-country relationship already exists: {} - {}",
                                    agreementName, country.getCountryName());
                        }
                    }
                }
            }
            log.info("Trade agreement migration completed successfully - Agreements: {}, Country relations: {}, Skipped countries: {}, Total lines: {}",
                    agreementCount.get(), countryRelationCount.get(), skippedCountryCount.get(), lineCount - 1);

        } catch (IOException e) {
            log.error("Error reading trade agreement CSV file", e);
            throw new RuntimeException("Failed to read trade_agreement.csv", e);
        } catch (NumberFormatException e) {
            log.error("Error parsing number from trade agreement CSV at line {}", lineCount, e);
            throw new RuntimeException("Invalid number format in tariff_agreement.csv at line " + lineCount, e);
        } catch (Exception e) {
            log.error("Unexpected error during trade agreement migration at line {}", lineCount, e);
            throw new RuntimeException("Trade agreement migration failed at line " + lineCount, e);
        }

        // Mark migration as complete
        MigrationStatus migrationStatus = new MigrationStatus();
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
