package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.TariffCalculationRequest;
import com.lynus.cs203.dtos.response.TariffCalculationResponse;
import com.lynus.cs203.dtos.response.TariffDto;
import com.lynus.cs203.services.TariffCalculationService;
import com.lynus.cs203.services.TariffSuggestionService;
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
class TariffControllerTest {

    @Mock
    private TariffCalculationService calculationService;

    @Mock
    private TariffSuggestionService suggestionService;

    @InjectMocks
    private TariffController tariffController;

    @Test
    void calculateEndpoint_returnsResponse() {
        // Arrange
        TariffCalculationRequest req = TariffCalculationRequest.builder()
                .productCode(1)
                .exportCountryCode("US")
                .desCountryCode("CN")
                .customsValue(100.0)
                .build();

        TariffCalculationResponse expected = TariffCalculationResponse.builder()
                .productCode(1)
                .exportCountryCode("US")
                .desCountryCode("CN")
                .customsValue(100.0)
                .tariffAmount(5.0)
                .build();

        when(calculationService.calculateTariff(req)).thenReturn(expected);

        // Act
        ResponseEntity<TariffCalculationResponse> response = tariffController.calculate(req);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(expected);
        verify(calculationService).calculateTariff(req);
        verifyNoMoreInteractions(calculationService, suggestionService);
    }

    @Test
    void suggestionsEndpoint_returnsPagedResults() {
        // Arrange
        String q = "q";
        String country = "US";
        int page = 0;
        int size = 10;

        TariffDto dto = new TariffDto();
        // set minimal fields used in assertions (keeps test resilient to DTO internals)
        try {
            dto.getClass().getField("productCode6").set(dto, "1");
        } catch (Exception ignored) {
            // field may be private or different; alternative assertions below rely on size only
        }

        Page<TariffDto> pageResp = new PageImpl<>(List.of(dto), PageRequest.of(page, size), 1);
        when(suggestionService.suggestProducts(q, country, page, size)).thenReturn(pageResp);

        // Act
        Page<TariffDto> result = tariffController.suggestProducts(q, country, page, size);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(suggestionService).suggestProducts(q, country, page, size);
        verifyNoMoreInteractions(suggestionService, calculationService);
    }

    @Test
    void getTariffRatesBySize_returnsList() {
        // Arrange
        int size = 5;
        String country = "United States";

        TariffDto dto = new TariffDto();
        List<TariffDto> list = List.of(dto);
        when(suggestionService.getTariffRatesBySize(size, country)).thenReturn(list);

        // Act
        List<TariffDto> result = tariffController.getTariffRatesBySize(size, country);

        // Assert
        assertThat(result).isEqualTo(list);
        verify(suggestionService).getTariffRatesBySize(size, country);
        verifyNoMoreInteractions(suggestionService, calculationService);
    }
}
