package com.lynus.cs203.integration.services;

import com.lynus.cs203.Cs203Application;
import com.lynus.cs203.entities.*;
import com.lynus.cs203.repositories.*;
import com.lynus.cs203.runners.DataImportRunner;
import com.lynus.cs203.services.DataMigrationService;
import com.lynus.cs203.services.TradeAgreementMigrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Data Migration Service Integration Tests")
public class DataMigrationIntegrationTest {

    @Autowired
    private DataImportRunner dataImportRunner;

    @Autowired
    private DataMigrationService dataMigrationService;

    @Autowired
    private TradeAgreementMigrationService tradeAgreementMigrationService;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TariffRepository tariffRepository;

    @Autowired
    private TradeAgreementRepository tradeAgreementRepository;

    @Autowired
    private AgreementCountryRepository agreementCountryRepository;

    @Autowired
    private MigrationStatusRepository migrationStatusRepository;

    @BeforeEach
    void setUp() {
        // Clear all data before each test
        migrationStatusRepository.deleteAll();
        tariffRepository.deleteAll();
        agreementCountryRepository.deleteAll();
        tradeAgreementRepository.deleteAll();
        countryRepository.deleteAll();
        productRepository.deleteAll();

        createTestTariffCsv();
        createTestTradeAgreementCsv();
    }

    private void createTestTariffCsv() {
        String csvContent = """
        trade_id,country_code,country_name,product_code,hs_description,hs_uom,food_category,tariff_rate
        1,US,United States,1001,"Wheat Flour","KG","Grains",2.5
        2,CA,Canada,1002,"Corn Meal","KG","Grains",3.0
        3,US,United States,1003,"Rice","KG","Grains",1.8
        4,MX,Mexico,1001,"Wheat Flour","KG","Grains",4.2
        5,SG,Singapore,1004,"Sugar","KG","Sweeteners",5.0
        6,MY,Malaysia,1005,"Palm Oil","L","Oils",2.8
        7,TH,Thailand,1006,"Coconut Oil","L","Oils",3.2
        """;

        File testFile = new File("src/test/resources/data/tariff_data.csv");
        testFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write(csvContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createTestTradeAgreementCsv() {
        String csvContent = """
            agreement_name,agreement_type,effective_date,expiration_date,signatories
            "USMCA","Free Trade Agreement","1/1/2020","1/1/2030","United States;Canada;Mexico"
            "ASEAN FTA","Regional Agreement","1/1/2010","1/1/2030","Singapore;Malaysia;Thailand"
            """;

        File testFile = new File("src/test/resources/data/Cleaned_Trade_Agreements1.csv");
        testFile.getParentFile().mkdirs();
        try (FileWriter writer = new FileWriter(testFile)) {
            writer.write(csvContent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    @DisplayName("Should successfully complete data migration from CSV files")
    void testDataMigrationService_ShouldMigrateTariffData() {
        dataMigrationService.migrateData();

        // Verify countries
        assertThat(countryRepository.count()).isGreaterThanOrEqualTo(6);
        assertThat(countryRepository.findByCountryCode("US")).isPresent();
        assertThat(countryRepository.findByCountryCode("CA")).isPresent();
        assertThat(countryRepository.findByCountryCode("MX")).isPresent();
        assertThat(countryRepository.findByCountryCode("SG")).isPresent();
        assertThat(countryRepository.findByCountryCode("MY")).isPresent();
        assertThat(countryRepository.findByCountryCode("TH")).isPresent();

        // Verify products
        assertThat(productRepository.findByProductCode(1001)).isPresent();
        assertThat(productRepository.findByProductCode(1002)).isPresent();
        assertThat(productRepository.findByProductCode(1003)).isPresent();

        // Verify tariffs
        assertThat(tariffRepository.count()).isEqualTo(7);

        // Verify migration status
        Optional<MigrationStatus> statusOpt = migrationStatusRepository.findById("csv_data_migration");
        assertThat(statusOpt).isPresent();
        assertThat(statusOpt.get().isCompleted());
        assertThat(statusOpt.get().getCompleted()).isNotNull();
    }

    @Test
    @DisplayName("Should skip data migration if already completed")
    void testDataMigrationService_ShouldSkipIfAlreadyMigrated() {
        // Mark migration as completed
        MigrationStatus status = new MigrationStatus();
        status.setMigrationName("csv_data_migration");
        status.setCompleted(true);
        status.setCompletedAt(LocalDateTime.now());
        migrationStatusRepository.save(status);

        long initialCountryCount = countryRepository.count();
        long initialProductCount = productRepository.count();
        long initialTariffCount = tariffRepository.count();

        // Run migration
        dataMigrationService.migrateData();

        // Verify no new data was added
        assertThat(countryRepository.count()).isEqualTo(initialCountryCount);
        assertThat(productRepository.count()).isEqualTo(initialProductCount);
        assertThat(tariffRepository.count()).isEqualTo(initialTariffCount);

        // Verify migration status remains unchanged
        Optional<MigrationStatus> statusOpt = migrationStatusRepository.findById("csv_data_migration");
        assertThat(statusOpt).isPresent();
    }

    @Test
    @DisplayName("Should successfully complete tariff agreement migration from CSV files")
    void testDataMigrationService_ShouldMigrateTariffAgreementData() {
        // First migrate tariff data
        dataMigrationService.migrateData();

        // Then migrate trade agreement data
        tradeAgreementMigrationService.migrateTradeAgreements();

        // Verify trade agreements
        List<TradeAgreement> tradeAgreementList = tradeAgreementRepository.findAll();
        assertThat(tradeAgreementList.size()).isEqualTo(2);

        Optional<TradeAgreement> usmcaAgreement = tradeAgreementRepository.findByAgreementName("USMCA");
        Optional<TradeAgreement> aseanFtaAgreement = tradeAgreementRepository.findByAgreementName("ASEAN FTA");

        assertThat(usmcaAgreement).isPresent();
        assertThat(aseanFtaAgreement).isPresent();
        assertThat(usmcaAgreement.get().getAgreementType()).isEqualTo("Free Trade Agreement");
        assertThat(aseanFtaAgreement.get().getAgreementType()).isEqualTo("Regional Agreement");

        // Verify agreement countries
        List<AgreementCountry> agreementCountryList = agreementCountryRepository.findAll();
        assertThat(agreementCountryList).isNotEmpty();

        // Verify migration status
        Optional<MigrationStatus> statusOpt = migrationStatusRepository.findById("csv_tradeAgreement_migration");
        assertThat(statusOpt).isPresent();
        assertThat(statusOpt.get().isCompleted()).isTrue();
        assertThat(statusOpt.get().getCompleted()).isNotNull();
    }

    @Test
    @DisplayName("Should skip tariff agreement migration if already completed")
    void testTradeAgreementMigrationService_ShouldSkipIfAlreadyMigrated() {
        // Mark trade agreement migration as completed
        MigrationStatus status = new MigrationStatus();
        status.setMigrationName("csv_tradeAgreement_migration");
        status.setCompleted(true);
        status.setCompletedAt(LocalDateTime.now());
        migrationStatusRepository.save(status);

        long initialTradeAgreementCount = tradeAgreementRepository.count();
        long initialAgreementCountryCount = agreementCountryRepository.count();

        // Run migration
        tradeAgreementMigrationService.migrateTradeAgreements();

        // Verify no new data was added
        assertThat(tradeAgreementRepository.count()).isEqualTo(initialTradeAgreementCount);
        assertThat(agreementCountryRepository.count()).isEqualTo(initialAgreementCountryCount);

        // Verify migration status remains unchanged
        Optional<MigrationStatus> statusOpt = migrationStatusRepository.findById("csv_tradeAgreement_migration");
        assertThat(statusOpt).isPresent();
    }

    @Test
    @DisplayName("Should run full data import process without errors")
    void testDataImportRunner_ShouldRunFullImportProcess() throws Exception {
        // Run the data import runner
        dataImportRunner.runMigrations();

        // Verify countries migrated
        assertThat(countryRepository.count()).isGreaterThanOrEqualTo(6);

        // Verify products migrated
        assertThat(productRepository.count()).isGreaterThanOrEqualTo(3);

        // Verify tariffs migrated
        assertThat(tariffRepository.count()).isEqualTo(7);

        // Verify trade agreements migrated
        Optional<TradeAgreement> usmcaOpt = tradeAgreementRepository.findByAgreementName("USMCA");
        Optional<TradeAgreement> aseanOpt = tradeAgreementRepository.findByAgreementName("ASEAN FTA");

        assertThat(usmcaOpt).isPresent();
        assertThat(aseanOpt).isPresent();

        // Verify agreement-country relationships exist
        long agreementCountryCount = agreementCountryRepository.count();
        assertThat(agreementCountryCount).isGreaterThanOrEqualTo(6); // 3 for USMCA + 3 for ASEAN

        // Verify tariff migration status
        Optional<MigrationStatus> dataMigrationStatus = migrationStatusRepository.findById("csv_data_migration");
        assertThat(dataMigrationStatus).isPresent();
        assertThat(dataMigrationStatus.get().isCompleted()).isTrue();

        // Verify trade agreement migration status - this is line 239 that's failing
        Optional<MigrationStatus> tradeAgreementStatusOpt = migrationStatusRepository.findById("csv_tradeAgreement_migration");
        assertThat(tradeAgreementStatusOpt)
                .as("Trade agreement migration status should exist")
                .isPresent();
        assertThat(tradeAgreementStatusOpt.get().isCompleted())
                .as("Trade agreement migration should be completed")
                .isTrue();
    }
}
