package com.lynus.cs203.runners;

import com.lynus.cs203.services.DataMigrationService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataImportRunner implements CommandLineRunner {
    private final DataMigrationService dataMigrationService;

    public DataImportRunner(DataMigrationService dataMigrationService) {
        this.dataMigrationService = dataMigrationService;
    }

    @Override
    public void run(String... args) throws Exception {
        dataMigrationService.migrateData();
    }

}
