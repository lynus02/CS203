package com.lynus.cs203.runners;

import com.lynus.cs203.services.DataMigrationService;
import com.lynus.cs203.services.TradeAgreementMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataImportRunner implements CommandLineRunner {
    private final DataMigrationService dataMigrationService;
    private final TradeAgreementMigrationService tradeAgreementMigrationService;

    @Override
    public void run(String... args) throws Exception {
        log.info("=== Starting CSV Data Migration Process ===");

        boolean dataSuccess = false;
        boolean tradeAgreementSuccess = false;

        try {
            // Run data migration first
            log.info("Step 1: Migrating tariff data...");
            dataMigrationService.migrateData();
            dataSuccess = true;
            log.info("Tariff data migration completed successfully");

            // Run trade agreement migration
            log.info("Step 2: Migrating trade agreement data...");
            tradeAgreementMigrationService.migrateTradeAgreements();
            tradeAgreementSuccess = true;
            log.info("Trade agreement migration completed successfully");

            log.info("=== CSV Data Migration Process Completed Successfully ===");

        } catch (Exception e) {
            log.error("=== DATA MIGRATION FAILED ===");
            log.error("Data Migration success: {}", dataSuccess);
            log.error("Trade Agreement Migration success: {}", tradeAgreementSuccess);
            log.error("Migration error: {}", e.getMessage(), e);

            throw new RuntimeException("CSV Migration Failed. Application startup aborted.", e);
        }
    }

}
