// java
package com.lynus.cs203.services;

import com.lynus.cs203.dtos.request.TariffCalculationRequest;
import com.lynus.cs203.dtos.response.TariffCalculationResponse;
import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.ProductRepository;
import com.lynus.cs203.repositories.TariffRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffCalculationServiceTest {

    @Mock
    private TariffRepository tariffRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private TariffCalculationService service;

    @Test
    @DisplayName("calculateTariff - happy path returns correct amount")
    void calculateTariff_HappyPath_ReturnsCorrectAmount() {
        // Arrange
        Product p = new Product();
        p.setProductCode(123);

        Country c = new Country();
        c.setCountryCode("US");

        Tariff t = new Tariff();
        t.setTariffRate(10.0);

        TariffCalculationRequest req = TariffCalculationRequest.builder()
                .productCode(123)
                .countryCode("US")
                .customsValue(200.0)
                .build();

        when(productRepository.findByProductCode(123)).thenReturn(Optional.of(p));
        when(countryRepository.findByCountryCode("US")).thenReturn(Optional.of(c));
        when(tariffRepository.findByProductAndCountry(p, c)).thenReturn(Optional.of(t));

        // Act
        TariffCalculationResponse resp = service.calculateTariff(req);

        // Assert
        assertEquals(123, resp.getProductCode());
        assertEquals("US", resp.getCountryCode());
        assertEquals(200.0, resp.getCustomsValue());
        assertEquals(20.0, resp.getTariffAmount(), 1e-6);

        verify(productRepository).findByProductCode(123);
        verify(countryRepository).findByCountryCode("US");
        verify(tariffRepository).findByProductAndCountry(p, c);
    }

    @Test
    @DisplayName("calculateTariff - missing product throws IllegalArgumentException")
    void calculateTariff_ProductNotFound_Throws() {
        // Arrange
        when(productRepository.findByProductCode(999)).thenReturn(Optional.empty());

        TariffCalculationRequest req = TariffCalculationRequest.builder()
                .productCode(999)
                .countryCode("US")
                .customsValue(100.0)
                .build();

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.calculateTariff(req));

        // Assert
        assertTrue(ex.getMessage().contains("Invalid product code"));
        verify(productRepository).findByProductCode(999);
        verifyNoMoreInteractions(countryRepository, tariffRepository);
    }

    @Test
    @DisplayName("calculateTariff - missing country throws IllegalArgumentException")
    void calculateTariff_CountryNotFound_Throws() {
        // Arrange
        Product p = new Product();
        p.setProductCode(1);
        when(productRepository.findByProductCode(1)).thenReturn(Optional.of(p));
        when(countryRepository.findByCountryCode("XX")).thenReturn(Optional.empty());

        TariffCalculationRequest req = TariffCalculationRequest.builder()
                .productCode(1)
                .countryCode("XX")
                .customsValue(50.0)
                .build();

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.calculateTariff(req));

        // Assert
        assertTrue(ex.getMessage().contains("Invalid country code"));
        verify(productRepository).findByProductCode(1);
        verify(countryRepository).findByCountryCode("XX");
        verifyNoMoreInteractions(tariffRepository);
    }

    @Test
    @DisplayName("calculateTariff - missing tariff throws IllegalArgumentException")
    void calculateTariff_TariffNotFound_Throws() {
        // Arrange
        Product p = new Product();
        p.setProductCode(2);

        Country c = new Country();
        c.setCountryCode("FR");

        when(productRepository.findByProductCode(2)).thenReturn(Optional.of(p));
        when(countryRepository.findByCountryCode("FR")).thenReturn(Optional.of(c));
        when(tariffRepository.findByProductAndCountry(p, c)).thenReturn(Optional.empty());

        TariffCalculationRequest req = TariffCalculationRequest.builder()
                .productCode(2)
                .countryCode("FR")
                .customsValue(1000.0)
                .build();

        // Act
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.calculateTariff(req));

        // Assert
        assertTrue(ex.getMessage().contains("No tariff found"));
        verify(productRepository).findByProductCode(2);
        verify(countryRepository).findByCountryCode("FR");
        verify(tariffRepository).findByProductAndCountry(p, c);
    }
}
