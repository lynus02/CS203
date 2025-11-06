package com.lynus.cs203.runners;

import com.lynus.cs203.services.DataMigrationService;
import com.lynus.cs203.services.TradeAgreementMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataImportRunner implements CommandLineRunner {
    private final DataMigrationService dataMigrationService;
    private final TradeAgreementMigrationService tradeAgreementMigrationService;
    private final Environment environment;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // Don't use transaction for the runner itself
    public void run(String... args) throws Exception {
        log.info("=== Starting CSV Data Migration Process ===");
        // Check if migration should run
        if (shouldSkipMigration()) {
            log.info("Migration skipped based on configuration");
            return;
        }

        log.info("Active profiles: {}", Arrays.toString(environment.getActiveProfiles()));
        log.info("This may take several minutes for large datasets...");

        // Run migrations synchronously during startup
        runMigrations();
    }

    public void runMigrations() {
        boolean dataSuccess = false;
        boolean tradeAgreementSuccess = false;
        long totalStartTime = System.currentTimeMillis();

        try {
            // Run data migration first
            log.info("Step 1: Migrating tariff data...");
            long startTime = System.currentTimeMillis();
            dataMigrationService.migrateData();
            dataSuccess = true;
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("Tariff data migration completed successfully in {}ms", elapsed);

            // Run trade agreement migration
            log.info("Step 2: Migrating trade agreement data...");
            startTime = System.currentTimeMillis();
            tradeAgreementMigrationService.migrateTradeAgreements();
            tradeAgreementSuccess = true;
            elapsed = System.currentTimeMillis() - startTime;
            log.info("Trade agreement migration completed successfully in {}ms", elapsed);

            long totalTime = System.currentTimeMillis() - totalStartTime;
            log.info("=== CSV Data Migration Process Completed Successfully in {}ms ===", totalTime);

        } catch (Exception e) {
            log.error("=== DATA MIGRATION FAILED ===");
            log.error("Data Migration success: {}", dataSuccess);
            log.error("Trade Agreement Migration success: {}", tradeAgreementSuccess);
            log.error("Migration error: {}", e.getMessage(), e);

            log.warn("Application will continue running despite migration failure");        }
    }

    private boolean shouldSkipMigration() {
        // Allow skipping migration via environment variable for AWS environments
        String skipMigration = environment.getProperty("app.migration.skip", "false");
        if ("true".equalsIgnoreCase(skipMigration)) {
            log.info("Migration skipped via app.migration.skip property");
            return true;
        }

        // Skip if running tests
        if (Arrays.stream(environment.getActiveProfiles()).anyMatch("test"::equals)) {
            log.info("Migration skipped for test profile");
            return true;
        }

        return false;
    }
}
