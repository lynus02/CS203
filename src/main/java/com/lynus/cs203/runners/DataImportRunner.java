package com.lynus.cs203.runners;

import com.lynus.cs203.exceptions.InvalidPasswordException;
import com.lynus.cs203.services.DataMigrationService;
import com.lynus.cs203.services.TradeAgreementMigrationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataImportRunner implements CommandLineRunner {
    private final DataMigrationService dataMigrationService;
    private final TradeAgreementMigrationService tradeAgreementMigrationService;

    public DataImportRunner(DataMigrationService dataMigrationService,
                            TradeAgreementMigrationService tradeAgreementMigrationService) {
        this.dataMigrationService = dataMigrationService;
        this.tradeAgreementMigrationService = tradeAgreementMigrationService;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Starting CSV Data Migration Process ===");

        boolean dataSuccess = false;
        boolean tradeAgreementSuccess = false;

        try {
            // Run data migration first
            System.out.println("Step 1: Migrating tariff data...");
            dataMigrationService.migrateData();
            dataSuccess = true;

            // Run trade agreement migration
            System.out.println("Step 2: Migrating trade agreement data...");
            tradeAgreementMigrationService.migrateTradeAgreements();
            tradeAgreementSuccess = true;

            System.out.println("=== CSV Data Migration Process Completed Successfully ===");
        } catch (Exception e) {
            System.err.println("=== MIGRATION FAILED ===");
            System.err.println("Data Migration success: " + dataSuccess);
            System.err.println("Trade Agreement Migration success: " + tradeAgreementSuccess);
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();

            throw new RuntimeException("CSV Migration Failed. Application startup aborted.", e);
        }
    }

}
