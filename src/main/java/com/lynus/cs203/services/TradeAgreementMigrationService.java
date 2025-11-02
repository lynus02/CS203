package com.lynus.cs203.services;

import com.lynus.cs203.entities.AgreementCountry;
import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.MigrationStatus;
import com.lynus.cs203.entities.TradeAgreement;
import com.lynus.cs203.repositories.AgreementCountryRepository;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.MigrationStatusRepository;
import com.lynus.cs203.repositories.TradeAgreementRepository;
import lombok.AllArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@AllArgsConstructor
public class TradeAgreementMigrationService {
    private TradeAgreementRepository tradeAgreementRepository;
    private AgreementCountryRepository agreementCountryRepository;
    private CountryRepository countryRepository;
    private final MigrationStatusRepository migrationStatusRepository;

    public void migrateTradeAgreements() {
        // check if migration has already been completed
        Optional<MigrationStatus> status = migrationStatusRepository.findById("csv_tradeAgreement_migration");
        if (status.isPresent() && status.get().isCompleted()) {
            System.out.println("Trade Agreement migration already completed, skipping.");
            return;
        }

        System.out.println("Starting trade agreement migration from CSV...");

        try {
            ClassPathResource resource = new ClassPathResource("data/trade_agreement.csv");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
                String line;
                boolean isFirstLine = true;

                while ((line = reader.readLine()) != null) {
                    if (isFirstLine) {
                        isFirstLine = false; // skip header line
                        continue;
                    }

                    String[] columns = parseCSVLine(line);

                    // Parse columns from CSV
                    String agreementName = removeQuotes(columns[0].trim());
                    String agreementType = removeQuotes(columns[1].trim());
                    String signatories = removeQuotes(columns[2].trim());

                    TradeAgreement agreement = tradeAgreementRepository.findByAgreementName(agreementName)
                            .orElseGet(() -> {
                                TradeAgreement newAgreement = new TradeAgreement();
                                newAgreement.setAgreementName(agreementName);
                                newAgreement.setAgreementType(agreementType);
                                return tradeAgreementRepository.save(newAgreement);
                            });

                    // Process signatories (country codes)
                    String[] countryNames = signatories.split(";");
                    for (String countryName : countryNames) {
                        String trimmedCountryName = countryName.trim();

                        // Fetch the country entity
                        Optional<Country> countryOpt = countryRepository.findByCountryName(trimmedCountryName);

                        if (countryOpt.isEmpty()) {
                            // Try to find by country code as a fallback
                            countryOpt = countryRepository.findByCountryCode(trimmedCountryName);
                        }

                        if (countryOpt.isEmpty()) {
                            System.err.println("Country not found by name or code: " + trimmedCountryName);
                            continue;
                        }
                        Country country = countryOpt.get();

                        // check if this country relationship already exists
                        boolean relationshipExists = agreementCountryRepository
                                .findByAgreementAndCountry(agreement, country)
                                .isPresent();

                        if (!relationshipExists) {
                            AgreementCountry agreementCountry = new AgreementCountry();
                            agreementCountry.setAgreement(agreement);
                            agreementCountry.setCountry(country);

                            // save
                            agreementCountryRepository.save(agreementCountry);

                        }
                    }
                }
                System.out.println("Trade agreement migration from CSV completed.");
            }
        }catch (IOException e) {
            System.err.println("Error reading trade agreement CSV file: " + e.getMessage());
            throw new RuntimeException("Failed to read trade_agreement.csv", e);
        } catch (NumberFormatException e) {
            System.err.println("Error parsing number from trade agreement CSV: " + e.getMessage());
            throw new RuntimeException("Invalid number format in trade_agreement.csv", e);
        } catch (Exception e) {
            System.err.println("Unexpected error during trade agreement migration: " + e.getMessage());
            throw new RuntimeException("Trade agreement migration failed", e);
        }

        // Mark migration as complete
        MigrationStatus migrationStatus = new MigrationStatus();
        migrationStatus.setMigrationName("csv_tradeAgreement_migration");
        migrationStatus.setCompleted(true);
        migrationStatus.setCompletedAt(LocalDateTime.now());
        migrationStatusRepository.save(migrationStatus);
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
