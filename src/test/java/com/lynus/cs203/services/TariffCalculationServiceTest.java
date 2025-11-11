// java
package com.lynus.cs203.services;

import com.lynus.cs203.dtos.request.TariffCalculationRequest;
import com.lynus.cs203.dtos.response.TariffCalculationResponse;
import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.entities.TradeAgreement;
import com.lynus.cs203.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tariff Calculation Service Test")
class TariffCalculationServiceTest {

    @Mock
    private TariffRepository tariffRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CountryRepository countryRepository;

    @Mock
    private TradeAgreementRepository tradeAgreementRepository;

    @Mock
    private AgreementCountryRepository agreementCountryRepository;

    @InjectMocks
    private TariffCalculationService tariffCalculationService;

    private Product testProduct;
    private Country exportCountry;
    private Country desCountry;
    private Tariff testTariff;
    private TariffCalculationRequest validRequest;
    private LocalDate dateToUse;

    @BeforeEach
    void setUp() {
        testProduct = new Product();
        testProduct.setProductCode("1001");

        exportCountry = new Country();
        exportCountry.setCountryCode("US");
        exportCountry.setCountryName("United States");

        desCountry = new Country();
        desCountry.setCountryCode("CN");
        desCountry.setCountryName("Canada");

        testTariff = new Tariff();
        testTariff.setTariffRate(5.0);

        validRequest = TariffCalculationRequest.builder()
                .productCode("1001")
                .exportCountryCode("US")
                .desCountryCode("CN")
                .customsValue(1000.0)
                .date(LocalDate.of(2025, 11, 11))
                .build();
    }

  @Test
    @DisplayName("Should calculate tariff with trade agreements")
    void calculateTariff_WithNoTradeAgreements_ShouldCalculateTariff() throws Exception {
        // Arrange
        when(productRepository.findByProductCode(validRequest.getProductCode()))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode(validRequest.getExportCountryCode()))
                .thenReturn(Optional.of(exportCountry));
        when(countryRepository.findByCountryCode(validRequest.getDesCountryCode()))
                .thenReturn(Optional.of(desCountry));
        when(tariffRepository.findByProductAndCountry(testProduct, desCountry))
                .thenReturn(Optional.of(testTariff));
        when(agreementCountryRepository
                .findAgreementsBetweenCountriesOnDate(exportCountry.getCountryName(), desCountry.getCountryName(), validRequest.getDate()))
                .thenReturn(Collections.emptyList());

        // Act
        TariffCalculationResponse response = tariffCalculationService.calculateTariff(validRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getProductCode()).isEqualTo("1001");
        assertThat(response.getExportCountryCode()).isEqualTo("US");
        assertThat(response.getDesCountryCode()).isEqualTo("CN");
        assertThat(response.getCustomsValue()).isEqualTo(1000.0);
        assertThat(response.getTariffAmount()).isEqualTo(50.0); // 5% of 1000.0
        assertThat(response.getAgreementType()).isEqualTo("MFN");

        // Verify interactions
        verify(productRepository).findByProductCode(validRequest.getProductCode());
        verify(countryRepository, times(2)).findByCountryCode(anyString());
        verify(tariffRepository).findByProductAndCountry(testProduct, desCountry);
        verify(agreementCountryRepository).findAgreementsBetweenCountriesOnDate(
                exportCountry.getCountryName(), desCountry.getCountryName(), validRequest.getDate());
    }

    @Test
    @DisplayName("Should calculate tariff with FTA agreement")
    void calculateTariff_WithFTAAgreement_ShouldCalculatePreferentialTariff() throws Exception {
        // Arrange
        testProduct.setProductCode("1001");       // HIGH sensitivity

        TradeAgreement tradeAgreement = new TradeAgreement();
        tradeAgreement.setAgreementId(1L);
        tradeAgreement.setAgreementType("FTA");

        when(productRepository.findByProductCode(validRequest.getProductCode()))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode(validRequest.getExportCountryCode()))
                .thenReturn(Optional.of(exportCountry));
        when(countryRepository.findByCountryCode(validRequest.getDesCountryCode()))
                .thenReturn(Optional.of(desCountry));
        when(tariffRepository.findByProductAndCountry(testProduct, desCountry))
                .thenReturn(Optional.of(testTariff));
        when(agreementCountryRepository
                .findAgreementsBetweenCountriesOnDate(exportCountry.getCountryName(), desCountry.getCountryName(), validRequest.getDate()))
                .thenReturn(List.of(1L));
        when(tradeAgreementRepository.findByAgreementId(1L))
                .thenReturn(Optional.of(tradeAgreement));

        // Act
        TariffCalculationResponse response = tariffCalculationService.calculateTariff(validRequest);

        // Assert
        assertThat(response.getTariffAmount()).isEqualTo(35.0);  // 3.5% of 1000.0
        assertThat(response.getAgreementType()).isEqualTo("FTA");
    }


    @Test
    @DisplayName("Should calculate tariff with multiple agreement types")
    void calculateTariff_WithMultipleAgreementTypes_ShouldUseBestRate() throws Exception {
        // Arrange
        testProduct.setProductCode("2001");       // MEDIUM sensitivity

        TradeAgreement multiAgreement = new TradeAgreement();
        multiAgreement.setAgreementId(1L);
        multiAgreement.setAgreementType("PSA&FTA&CU");

        when(productRepository.findByProductCode(validRequest.getProductCode()))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode(validRequest.getExportCountryCode()))
                .thenReturn(Optional.of(exportCountry));
        when(countryRepository.findByCountryCode(validRequest.getDesCountryCode()))
                .thenReturn(Optional.of(desCountry));
        when(tariffRepository.findByProductAndCountry(testProduct, desCountry))
                .thenReturn(Optional.of(testTariff));
        when(agreementCountryRepository
                .findAgreementsBetweenCountriesOnDate(exportCountry.getCountryName(), desCountry.getCountryName(), validRequest.getDate()))
                .thenReturn(List.of(1L));
        when(tradeAgreementRepository.findByAgreementId(1L))
                .thenReturn(Optional.of(multiAgreement));

        // Act
        TariffCalculationResponse response = tariffCalculationService.calculateTariff(validRequest);

        // Assert
        assertThat(response.getTariffAmount()).isEqualTo(12.5);  // (5 * 0.25) /100 * 1000 = 12.5
        assertThat(response.getAgreementType()).isEqualTo("CU");

    }

    @Test
    @DisplayName("Should calculate tariff with EIA agreement for low sensitivity product")
    void calculateTariff_WithEIAAgreementLowSensitivity_ShouldCalculatePreferentialTariff() throws Exception {
        // Arrange
        testProduct.setProductCode("5001");       // LOW sensitivity

        TradeAgreement tradeAgreement = new TradeAgreement();
        tradeAgreement.setAgreementId(1L);
        tradeAgreement.setAgreementType("EIA");

        when(productRepository.findByProductCode(validRequest.getProductCode()))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode(validRequest.getExportCountryCode()))
                .thenReturn(Optional.of(exportCountry));
        when(countryRepository.findByCountryCode(validRequest.getDesCountryCode()))
                .thenReturn(Optional.of(desCountry));
        when(tariffRepository.findByProductAndCountry(testProduct, desCountry))
                .thenReturn(Optional.of(testTariff));
        when(agreementCountryRepository
                .findAgreementsBetweenCountriesOnDate(exportCountry.getCountryName(), desCountry.getCountryName(), validRequest.getDate()))
                .thenReturn(List.of(1L));
        when(tradeAgreementRepository.findByAgreementId(1L))
                .thenReturn(Optional.of(tradeAgreement));

        // Act
        TariffCalculationResponse response = tariffCalculationService.calculateTariff(validRequest);

        // Assert
        assertThat(response.getTariffAmount()).isEqualTo(0.0);
        assertThat(response.getAgreementType()).isEqualTo("EIA");
    }

    @Test
    @DisplayName("Should throw exception for invalid product code")
    void calculateTariff_InvalidProductCode_ShouldThrowException() {
        // Arrange
        when(productRepository.findByProductCode("9999"))
                .thenReturn(Optional.empty());

        TariffCalculationRequest request = TariffCalculationRequest.builder()
                .productCode("9999")
                .exportCountryCode("US")
                .desCountryCode("CN")
                .customsValue(1000.0)
                .date(LocalDate.of(2025, 11, 11))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> tariffCalculationService.calculateTariff(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid product code");

        // Verify
        verify(productRepository).findByProductCode("9999");
        verifyNoInteractions(countryRepository, tariffRepository, agreementCountryRepository, tradeAgreementRepository);
    }

    @Test
    @DisplayName("Should throw exception when export country not found")
    void calculateTariff_ExportCountryNotFound_ShouldThrowException() {
        // Arrange
        when(productRepository.findByProductCode("1001"))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode("XX"))
                .thenReturn(Optional.empty());

        TariffCalculationRequest request = TariffCalculationRequest.builder()
                .productCode("1001")
                .exportCountryCode("XX")
                .desCountryCode("CN")
                .customsValue(1000.0)
                .date(LocalDate.of(2025, 11, 11))
                .build();

        // Act & Assert
        assertThatThrownBy(() -> tariffCalculationService.calculateTariff(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid export country code");

        // Verify
        verify(productRepository).findByProductCode("1001");
        verify(countryRepository).findByCountryCode("XX");
        verifyNoInteractions(tariffRepository, agreementCountryRepository, tradeAgreementRepository);
    }

    @Test
    @DisplayName("Should throw exception when destination country not found")
    void calculateTariff_DesCountryNotFound_ShouldThrowException() {
        // Arrange
        when(productRepository.findByProductCode("1001"))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode("US"))
                .thenReturn(Optional.of(exportCountry));
        when(countryRepository.findByCountryCode("YY"))
                .thenReturn(Optional.empty());

        TariffCalculationRequest request = TariffCalculationRequest.builder()
                .productCode("1001")
                .exportCountryCode("US")
                .desCountryCode("YY")
                .customsValue(1000.0)
                .build();

        // Act & Assert
        assertThatThrownBy(() -> tariffCalculationService.calculateTariff(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid destination country code");

        // Verify
        verify(productRepository).findByProductCode("1001");
        verify(countryRepository).findByCountryCode("US");
        verify(countryRepository).findByCountryCode("YY");
        verifyNoInteractions(tariffRepository, agreementCountryRepository, tradeAgreementRepository);
    }

    @Test
    @DisplayName("Should throw exception when tariff not found")
    void calculateTariff_TariffNotFound_ShouldThrowException() {
        // Arrange
        when(productRepository.findByProductCode("1001"))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode("US"))
                .thenReturn(Optional.of(exportCountry));
        when(countryRepository.findByCountryCode("CN"))
                .thenReturn(Optional.of(desCountry));
        when(tariffRepository.findByProductAndCountry(testProduct, desCountry))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> tariffCalculationService.calculateTariff(validRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No tariff found for this product-country combination");

        // Verify
        verify(tariffRepository).findByProductAndCountry(testProduct, desCountry);
        verifyNoInteractions(agreementCountryRepository, tradeAgreementRepository);
    }

    @Test
    @DisplayName("Should calculate sensitivity tiers correctly")
    void calculateSensitiveTier_ShouldReturnCorrectTiers() {
        // HIGH sensitivity
        assertThat(tariffCalculationService.calculateSensitivityTier("10")).isEqualTo("HIGH");
        assertThat(tariffCalculationService.calculateSensitivityTier("41")).isEqualTo("HIGH");

        // MEDIUM sensitivity
        assertThat(tariffCalculationService.calculateSensitivityTier("20")).isEqualTo("MEDIUM");
        assertThat(tariffCalculationService.calculateSensitivityTier("21")).isEqualTo("MEDIUM");
        assertThat(tariffCalculationService.calculateSensitivityTier("30")).isEqualTo("MEDIUM");
        assertThat(tariffCalculationService.calculateSensitivityTier("40")).isEqualTo("MEDIUM");

        // LOW sensitivity
        assertThat(tariffCalculationService.calculateSensitivityTier("50")).isEqualTo("LOW");
        assertThat(tariffCalculationService.calculateSensitivityTier("60")).isEqualTo("LOW");
        assertThat(tariffCalculationService.calculateSensitivityTier("71")).isEqualTo("LOW");
        assertThat(tariffCalculationService.calculateSensitivityTier("80")).isEqualTo("LOW");

        // Default MEDIUM sensitivity for others
        assertThat(tariffCalculationService.calculateSensitivityTier("01")).isEqualTo("MEDIUM");
        assertThat(tariffCalculationService.calculateSensitivityTier("99")).isEqualTo("MEDIUM");
    }

    @Test
    @DisplayName("Should throw exception for invalid HS code")
    void calculateSensitiveTier_InvalidHSCode_ShouldThrowException() {
        // Null HS code
        assertThatThrownBy(() -> tariffCalculationService.calculateSensitivityTier(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid HS Code");

        // Too short HS code
        assertThatThrownBy(() -> tariffCalculationService.calculateSensitivityTier("1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid HS Code");
    }

    @Test
    @DisplayName("Should handle null trade agreement")
    void calculateTariff_NullTradeAgreement_ShouldCalculateMFNTariff() {
        // Arrange
        when(productRepository.findByProductCode(validRequest.getProductCode()))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode(validRequest.getExportCountryCode()))
                .thenReturn(Optional.of(exportCountry));
        when(countryRepository.findByCountryCode(validRequest.getDesCountryCode()))
                .thenReturn(Optional.of(desCountry));
        when(tariffRepository.findByProductAndCountry(testProduct, desCountry))
                .thenReturn(Optional.of(testTariff));
        when(agreementCountryRepository
                .findAgreementsBetweenCountriesOnDate(exportCountry.getCountryName(), desCountry.getCountryName(), validRequest.getDate()))
                .thenReturn(List.of(1L));
        when(tradeAgreementRepository.findByAgreementId(1L))
                .thenReturn(Optional.empty());

        // Act
        TariffCalculationResponse response = tariffCalculationService.calculateTariff(validRequest);

        // Assert
        assertThat(response.getTariffAmount()).isEqualTo(50.0);
        assertThat(response.getAgreementType()).isEqualTo("MFN");
    }

    @Test
    @DisplayName("Should handle unknown trade agreement type")
    void calculateTariff_UnknownTradeAgreementType_ShouldCalculateMFNTariff() {
        // Arrange
        TradeAgreement tradeAgreement = new TradeAgreement();
        tradeAgreement.setAgreementId(1L);
        tradeAgreement.setAgreementType("UNKNOWN");

        when(productRepository.findByProductCode(validRequest.getProductCode()))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode(validRequest.getExportCountryCode()))
                .thenReturn(Optional.of(exportCountry));
        when(countryRepository.findByCountryCode(validRequest.getDesCountryCode()))
                .thenReturn(Optional.of(desCountry));
        when(tariffRepository.findByProductAndCountry(testProduct, desCountry))
                .thenReturn(Optional.of(testTariff));
        when(agreementCountryRepository
                .findAgreementsBetweenCountries(exportCountry.getCountryName(), desCountry.getCountryName()))
                .thenReturn(List.of(1L));
        when(tradeAgreementRepository.findByAgreementId(1L))
                .thenReturn(Optional.of(tradeAgreement));

        // Act
        TariffCalculationResponse response = tariffCalculationService.calculateTariff(validRequest);

        // Assert
        assertThat(response.getTariffAmount()).isEqualTo(50.0);
        assertThat(response.getAgreementType()).isEqualTo("MFN");
    }

    @Test
    @DisplayName("Should calculate tariff with PSA agreement for high sensitivity product")
    void calculateTariff_WithPSAAgreementHighSensitivity_ShouldCalculatePreferentialTariff() {
        // Arrange
        testProduct.setProductCode("1001");       // HIGH sensitivity

        TradeAgreement tradeAgreement = new TradeAgreement();
        tradeAgreement.setAgreementId(1L);
        tradeAgreement.setAgreementType("PSA");

        when(productRepository.findByProductCode(validRequest.getProductCode()))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode(validRequest.getExportCountryCode()))
                .thenReturn(Optional.of(exportCountry));
        when(countryRepository.findByCountryCode(validRequest.getDesCountryCode()))
                .thenReturn(Optional.of(desCountry));
        when(tariffRepository.findByProductAndCountry(testProduct, desCountry))
                .thenReturn(Optional.of(testTariff));
        when(agreementCountryRepository
                .findAgreementsBetweenCountries(exportCountry.getCountryName(), desCountry.getCountryName()))
                .thenReturn(List.of(1L));
        when(tradeAgreementRepository.findByAgreementId(1L))
                .thenReturn(Optional.of(tradeAgreement));

        // Act
        TariffCalculationResponse response = tariffCalculationService.calculateTariff(validRequest);

        // Assert
        assertThat(response.getTariffAmount()).isEqualTo(45.0);  // 0.9 * 5% of 1000.0
        assertThat(response.getAgreementType()).isEqualTo("PSA");
    }

    @Test
    @DisplayName("should calculate tariff with CU agreement for high sensitivity product")
    void calculateTariff_WithCUAgreementHighSensitivity_ShouldCalculatePreferentialTariff() {
        // Arrange
        testProduct.setProductCode("1001");       // HIGH sensitivity

        TradeAgreement tradeAgreement = new TradeAgreement();
        tradeAgreement.setAgreementId(1L);
        tradeAgreement.setAgreementType("CU");

        when(productRepository.findByProductCode(validRequest.getProductCode()))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode(validRequest.getExportCountryCode()))
                .thenReturn(Optional.of(exportCountry));
        when(countryRepository.findByCountryCode(validRequest.getDesCountryCode()))
                .thenReturn(Optional.of(desCountry));
        when(tariffRepository.findByProductAndCountry(testProduct, desCountry))
                .thenReturn(Optional.of(testTariff));
        when(agreementCountryRepository
                .findAgreementsBetweenCountries(exportCountry.getCountryName(), desCountry.getCountryName()))
                .thenReturn(List.of(1L));
        when(tradeAgreementRepository.findByAgreementId(1L))
                .thenReturn(Optional.of(tradeAgreement));

        // Act
        TariffCalculationResponse response = tariffCalculationService.calculateTariff(validRequest);

        // Assert
        assertThat(response.getTariffAmount()).isEqualTo(25.0);  // 0.5 * 5% of 1000.0
        assertThat(response.getAgreementType()).isEqualTo("CU");
    }

    @Test
    @DisplayName("should calculate tariff with EIA agreement for medium sensitivity product")
    void calculateTariff_WithEIAAgreementMediumSensitivity_ShouldCalculatePreferentialTariff() {
        // Arrange
        testProduct.setProductCode("2001");       // MEDIUM sensitivity

        TradeAgreement tradeAgreement = new TradeAgreement();
        tradeAgreement.setAgreementId(1L);
        tradeAgreement.setAgreementType("EIA");

        when(productRepository.findByProductCode(validRequest.getProductCode()))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode(validRequest.getExportCountryCode()))
                .thenReturn(Optional.of(exportCountry));
        when(countryRepository.findByCountryCode(validRequest.getDesCountryCode()))
                .thenReturn(Optional.of(desCountry));
        when(tariffRepository.findByProductAndCountry(testProduct, desCountry))
                .thenReturn(Optional.of(testTariff));
        when(agreementCountryRepository
                .findAgreementsBetweenCountries(exportCountry.getCountryName(), desCountry.getCountryName()))
                .thenReturn(List.of(1L));
        when(tradeAgreementRepository.findByAgreementId(1L))
                .thenReturn(Optional.of(tradeAgreement));

        // Act
        TariffCalculationResponse response = tariffCalculationService.calculateTariff(validRequest);

        // Assert
        assertThat(response.getTariffAmount()).isEqualTo(5.0);  // (5 * 0.10) /100 * 1000 = 10.0
        assertThat(response.getAgreementType()).isEqualTo("EIA");
    }

    @Test
    @DisplayName("Should calculate tariff with other agreement for medium sensitivity product")
    void calculateTariff_WithOtherAgreementMediumSensitivity_ShouldCalculateMFNTariff() {
        // Arrange
        testProduct.setProductCode("2001");       // MEDIUM sensitivity

        TradeAgreement tradeAgreement = new TradeAgreement();
        tradeAgreement.setAgreementId(1L);
        tradeAgreement.setAgreementType("OTHERS");

        when(productRepository.findByProductCode(validRequest.getProductCode()))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode(validRequest.getExportCountryCode()))
                .thenReturn(Optional.of(exportCountry));
        when(countryRepository.findByCountryCode(validRequest.getDesCountryCode()))
                .thenReturn(Optional.of(desCountry));
        when(tariffRepository.findByProductAndCountry(testProduct, desCountry))
                .thenReturn(Optional.of(testTariff));
        when(agreementCountryRepository
                .findAgreementsBetweenCountries(exportCountry.getCountryName(), desCountry.getCountryName()))
                .thenReturn(List.of(1L));
        when(tradeAgreementRepository.findByAgreementId(1L))
                .thenReturn(Optional.of(tradeAgreement));

        // Act
        TariffCalculationResponse response = tariffCalculationService.calculateTariff(validRequest);

        // Assert
        assertThat(response.getTariffAmount()).isEqualTo(50.0);  // Full MFN rate
        assertThat(response.getAgreementType()).isEqualTo("MFN");
    }

    @Test
    @DisplayName("Should handle default sensitivity tier in discount multiplier")
    void getDiscountMultiplier_DefaultSensitivityTier_ShouldReturnNoDiscount() {
        // This tests the default case in getDiscountMultiplier method
        // Arrange
        testProduct.setProductCode("9999");       // Will default to MEDIUM sensitivity

        TradeAgreement tradeAgreement = new TradeAgreement();
        tradeAgreement.setAgreementId(1L);
        tradeAgreement.setAgreementType("FTA");

        when(productRepository.findByProductCode(validRequest.getProductCode()))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode(validRequest.getExportCountryCode()))
                .thenReturn(Optional.of(exportCountry));
        when(countryRepository.findByCountryCode(validRequest.getDesCountryCode()))
                .thenReturn(Optional.of(desCountry));
        when(tariffRepository.findByProductAndCountry(testProduct, desCountry))
                .thenReturn(Optional.of(testTariff));
        when(agreementCountryRepository
                .findAgreementsBetweenCountries(exportCountry.getCountryName(), desCountry.getCountryName()))
                .thenReturn(List.of(1L));
        when(tradeAgreementRepository.findByAgreementId(1L))
                .thenReturn(Optional.of(tradeAgreement));

        // Act
        TariffCalculationResponse response = tariffCalculationService.calculateTariff(validRequest);

        // Assert - For default sensitivity (treated as MEDIUM), FTA should give 50% discount
        assertThat(response.getTariffAmount()).isEqualTo(25.0);  // (5 * 0.5) /100 * 1000 = 25.
        assertThat(response.getAgreementType()).isEqualTo("FTA");
    }

    @Test
    @DisplayName("Should handle tariff with FTA with LOW sensitivity product")
    void calculateTariff_WithFTAAgreementLowSensitivity_ShouldCalculatePreferentialTariff() {
        // Arrange
        testProduct.setProductCode("5001");       // LOW sensitivity

        TradeAgreement tradeAgreement = new TradeAgreement();
        tradeAgreement.setAgreementId(1L);
        tradeAgreement.setAgreementType("FTA");

        when(productRepository.findByProductCode(validRequest.getProductCode()))
                .thenReturn(Optional.of(testProduct));
        when(countryRepository.findByCountryCode(validRequest.getExportCountryCode()))
                .thenReturn(Optional.of(exportCountry));
        when(countryRepository.findByCountryCode(validRequest.getDesCountryCode()))
                .thenReturn(Optional.of(desCountry));
        when(tariffRepository.findByProductAndCountry(testProduct, desCountry))
                .thenReturn(Optional.of(testTariff));
        when(agreementCountryRepository
                .findAgreementsBetweenCountries(exportCountry.getCountryName(), desCountry.getCountryName()))
                .thenReturn(List.of(1L));
        when(tradeAgreementRepository.findByAgreementId(1L))
                .thenReturn(Optional.of(tradeAgreement));

        // Act
        TariffCalculationResponse response = tariffCalculationService.calculateTariff(validRequest);

        // Assert
        assertThat(response.getTariffAmount()).isEqualTo(15.0);  // 5%*0.3*1000.0
        assertThat(response.getAgreementType()).isEqualTo("FTA");
    }

}
