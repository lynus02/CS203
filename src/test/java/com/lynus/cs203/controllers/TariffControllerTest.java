package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.TariffCalculationRequest;
import com.lynus.cs203.dtos.response.TariffCalculationResponse;
import com.lynus.cs203.dtos.response.TariffDto;
import com.lynus.cs203.services.TariffCalculationService;
import com.lynus.cs203.services.TariffSuggestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tariff Controller Test")
class TariffControllerTest {

    @Mock
    private TariffCalculationService tariffCalculationService;

    @Mock
    private TariffSuggestionService tariffSuggestionService;

    @InjectMocks
    private TariffController tariffController;

    private TariffCalculationRequest validRequest;
    private TariffCalculationResponse mockResponse;
    private List<TariffDto> mockTariffList;
    private Page<TariffDto> mockTariffPage;

    @BeforeEach
    void setUp() {
        // Setup test data
        validRequest = TariffCalculationRequest.builder()
                .productCode(1)
                .exportCountryCode("US")
                .desCountryCode("CA")
                .customsValue(1000.0)
                .build();

        mockResponse = TariffCalculationResponse.builder()
                .productCode(1)
                .exportCountryCode("US")
                .desCountryCode("CA")
                .customsValue(1000.0)
                .tariffAmount(50.0)
                .agreementType("FTA")
                .build();

        mockTariffList = List.of(
                TariffDto.builder()
                        .trade_id(12345L)
                        .hsDescription("Live animals; animal products")
                        .productCode6("100190")
                        .food_category("Grains")
                        .value(1500.75)
                        .reporterName("United States")
                        .build(),
                TariffDto.builder()
                        .trade_id(67890L)
                        .hsDescription("Vegetable products")
                        .productCode6("080110")
                        .food_category("Fruits")
                        .value(2300.50)
                        .reporterName("Canada")
                        .build()
        );

        mockTariffPage = new PageImpl<>(
                mockTariffList,
                PageRequest.of(0, 20), mockTariffList.size());
    }

    @Test
    @DisplayName("Should calculate tariff and return response")
    void calculateTariff_ShouldReturnSuccessfulResponse_WhenRequestIsValid() {
        // Arrange
        when(tariffCalculationService.calculateTariff(validRequest))
                .thenReturn(mockResponse);

        // Act
        ResponseEntity<TariffCalculationResponse> response = tariffController.calculate(validRequest);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockResponse);
        assertThat(response.getBody().getTariffAmount()).isEqualTo(50.0);

        // Verify
        verify(tariffCalculationService).calculateTariff(validRequest);
    }

    @Test
    @DisplayName("Should calculate tariff with correct parameters")
    void calculateTariff_ShouldCallServiceWithCorrectParameters() {
        // Arrange
        when(tariffCalculationService.calculateTariff(validRequest))
                .thenReturn(mockResponse);

        // Act
        tariffController.calculate(validRequest);

        // Assert
        verify(tariffCalculationService).calculateTariff(validRequest);
    }


    @Test
    @DisplayName("Should return list of tariffs")
    void getTariffRatesBySize_ShouldReturnListOfTariffs() {
        // Arrange
        int size = 10;
        String country = "US";
        when(tariffSuggestionService.getTariffRatesBySize(size, country))
                .thenReturn(mockTariffList);

        // Act
        ResponseEntity<List<TariffDto>> response = tariffController.getTariffRatesBySize(size, country);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockTariffList);

        // Verify
        verify(tariffSuggestionService).getTariffRatesBySize(size, country);
    }

    @Test
    @DisplayName("Should handle null country parameter")
    void getTariffRatesBySize_ShouldHandleNullCountry() {
        // Arrange
        int size = 10;
        when(tariffSuggestionService.getTariffRatesBySize(size, null))
                .thenReturn(mockTariffList);

        // Act
        ResponseEntity<List<TariffDto>> response = tariffController.getTariffRatesBySize(size, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockTariffList);
        verify(tariffSuggestionService).getTariffRatesBySize(size, null);
    }

    @Test
    @DisplayName("Should handle empty country parameter")
    void getTariffRatesBySize_ShouldHandleEmptyCountry() {
        // Arrange
        int size = 10;
        String country = "";
        when(tariffSuggestionService.getTariffRatesBySize(size, ""))
                .thenReturn(mockTariffList);

        // Act
        ResponseEntity<List<TariffDto>> response = tariffController.getTariffRatesBySize(size, country);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockTariffList);
        verify(tariffSuggestionService).getTariffRatesBySize(size, country);
    }

    @Test
    @DisplayName("Should return paged tariff suggestions")
    void suggestProducts_ShouldReturnPagedTariffSuggestions() {
        // Arrange
        when(tariffSuggestionService.suggestProducts("test", "US", 0, 20))
                .thenReturn(mockTariffPage);

        // Act
        ResponseEntity<Page<TariffDto>> response =
                tariffController.suggestProducts("test", "US", 0, 20);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockTariffPage);
        verify(tariffSuggestionService).suggestProducts("test", "US", 0, 20);
    }

    @Test
    @DisplayName("Should use default pagination values")
    void suggestProducts_ShouldUseDefaultPaginationValues() {
        // Arrange
        when(tariffSuggestionService.suggestProducts("test", null, 0, 20))
                .thenReturn(mockTariffPage);

        // Act
        ResponseEntity<Page<TariffDto>> response =
                tariffController.suggestProducts("test", null, 0, 20);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockTariffPage);
        verify(tariffSuggestionService).suggestProducts("test", null, 0, 20);
    }

    @Test
    @DisplayName("Should handle empty query parameter")
    void suggestProducts_ShouldHandleEmptyQueryParameter() {
        // Arrange
        when(tariffSuggestionService.suggestProducts("", null, 0, 20))
                .thenReturn(mockTariffPage);

        // Act
        ResponseEntity<Page<TariffDto>> response =
                tariffController.suggestProducts("", null, 0, 20);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockTariffPage);
        verify(tariffSuggestionService).suggestProducts("", null, 0, 20);
    }

    @Test
    @DisplayName("Should handle custom country parameter in suggestions")
    void suggestProducts_ShouldHandleCustomCountryParameter() {
        // Arrange
        when(tariffSuggestionService.suggestProducts("test", "CA", 2, 50))
                .thenReturn(mockTariffPage);

        // Act
        ResponseEntity<Page<TariffDto>> response =
                tariffController.suggestProducts("test", "CA", 2, 50);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(mockTariffPage);
        verify(tariffSuggestionService).suggestProducts("test", "CA", 2, 50);
    }
}
