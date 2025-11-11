// java
package com.lynus.cs203.services;

import com.lynus.cs203.dtos.response.CountryDto;
import com.lynus.cs203.dtos.response.TariffDto;
import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.TariffRepository;
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
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tariff Suggestion Service Test")
class TariffSuggestionServiceTest {

    @Mock
    private TariffRepository tariffRepository;

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private TariffSuggestionService tariffSuggestionService;

    private Tariff testTariff1;
    private Tariff testTariff2;
    private Country testCountry1;
    private Country testCountry2;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testCountry1 = new Country();
        testCountry1.setCountryCode("US");
        testCountry1.setCountryName("United States");

        testCountry2 = new Country();
        testCountry2.setCountryCode("CA");
        testCountry2.setCountryName("Canada");

        testProduct = new Product();
        testProduct.setProductCode("1001");
        testProduct.setProductDescription("Test Product Description");

        testTariff1 = new Tariff();
        testTariff1.setCountry(testCountry1);
        testTariff1.setProduct(testProduct);
        testTariff1.setTariffRate(5.0);

        testTariff2 = new Tariff();
        testTariff2.setCountry(testCountry1);
        testTariff2.setProduct(testProduct);
        testTariff2.setTariffRate(10.0);
    }

    @Test
    @DisplayName("Should get tariff rates by size and country")
    void getTariffRatesBySize_WithValidCountry_ShouldReturnTariff() {
        // Arrange
        List<Tariff> tariffList = List.of(testTariff1, testTariff2);
        Page<Tariff> tariffPage = new PageImpl<>(tariffList,
                PageRequest.of(0, 10),
                tariffList.size());

        when(tariffRepository.findByCountry_CountryName(eq("United States"), any(Pageable.class)))
                .thenReturn(tariffPage);

        List<TariffDto> result = tariffSuggestionService.getTariffRatesBySize(10, "United States");

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isNotNull();
        assertThat(result.get(1)).isNotNull();

        // Verify
        verify(tariffRepository)
                .findByCountry_CountryName(eq("United States"), any(Pageable.class));
    }

    @Test
    @DisplayName("Should suggest products with numeric query and country")
    void suggestProducts_WithNumericQueryAndCountry_ShouldReturnTariffDtos() {
        // Arrange
        String query = "honey";
        String country = "United States";
        int page = 0;
        int size = 10;

        Page<Tariff> tariffPage = new PageImpl<>(List.of(testTariff1));
        when(tariffRepository.findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(
                eq(country), eq(query), any(Pageable.class)))
                .thenReturn(tariffPage);

        // Act
        Page<TariffDto> result = tariffSuggestionService.suggestProducts(query, country, page, size);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        // Verify
        verify(tariffRepository)
                .findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(
                        eq(country), eq(query), any(Pageable.class));
        verifyNoMoreInteractions(tariffRepository);
    }

    @Test
    @DisplayName("Should suggest products with text query and country")
    void suggestProducts_WithTextQueryAndCountry_ShouldReturnTariffDtos() {
        // Arrange
        String query = "honey";
        String country = "United States";
        int page = 0;
        int size = 10;

        Page<Tariff> tariffPage = new PageImpl<>(List.of(testTariff1));
        // service will call the country+description method when country is provided and query is text
        when(tariffRepository.findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(
                eq(country), eq(query), any(Pageable.class)))
                .thenReturn(tariffPage);

        // Act
        Page<TariffDto> result = tariffSuggestionService.suggestProducts(query, country, page, size);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        // Verify
        verify(tariffRepository)
                .findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(
                        eq(country), eq(query), any(Pageable.class));
        verifyNoMoreInteractions(tariffRepository);
    }

    @Test
    @DisplayName("Should suggest product with text query and no country")
    void suggestProducts_WithTextQueryAndNoCountry_ShouldReturnTariffDtos() {
        // Arrange
        String query = "1001";
        int page = 0;
        int size = 10;

        Page<Tariff> tariffPage = new PageImpl<>(List.of(testTariff1));
        when(tariffRepository.findByProductDescriptionOrProductCodeContaining(
                eq(query), eq(query), any(Pageable.class)))
                .thenReturn(tariffPage);

        // Act
        Page<TariffDto> result = tariffSuggestionService.suggestProducts(query, null, page, size);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);

        // Verify
        verify(tariffRepository)
                .findByProductDescriptionOrProductCodeContaining(
                        eq(query), eq(query), any(Pageable.class));
        verifyNoMoreInteractions(tariffRepository);
    }

    @Test
    @DisplayName("Should handle empty country as null country")
    void suggestProducts_WithEmptyCountry_ShouldHandleAsNull() {
        String query = "test";
        String country = "";
        int page = 0;
        int size = 10;

        Page<Tariff> tariffPage = new PageImpl<>(List.of());
        when(tariffRepository.findByProductDescriptionOrProductCodeContaining(
                eq(query), eq(query), any(Pageable.class)))
                .thenReturn(tariffPage);

        // Act
        Page<TariffDto> result = tariffSuggestionService.suggestProducts(query, country, page, size);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();

        // Verify
        verify(tariffRepository)
                .findByProductDescriptionOrProductCodeContaining(
                        eq(query), eq(query), any(Pageable.class));
        verifyNoMoreInteractions(tariffRepository);
    }

    @Test
    @DisplayName("Should return empty list when no tariffs found for country")
    void getTariffRatesBySize_NoTariffsFound_ShouldReturnEmptyList() {
        // Arrange
        String country = "nonexistent country";
        int size = 10;
        Page<Tariff> tariffPage = new PageImpl<>(List.of());

        when(tariffRepository.findByCountry_CountryName(eq(country), any(Pageable.class)))
                .thenReturn(tariffPage);

        // Act
        List<TariffDto> result = tariffSuggestionService.getTariffRatesBySize(size, country);

        // Assert
        assertThat(result).isEmpty();

        // Verify
        verify(tariffRepository)
                .findByCountry_CountryName(eq(country), any(Pageable.class));
    }

    @Test
    @DisplayName("Should handle large numeric code")
    void suggestProducts_WithLargeNumericCode_ShouldHandleCorrectly() {
        // Arrange
        String query = "123456789";
        String country = "United States";
        int page = 0;
        int size = 10;

        Page<Tariff> tariffPage = new PageImpl<>(List.of());
        when(tariffRepository.findByCountry_CountryNameAndProduct_ProductCodeContaining(
                eq(country), eq(query), any(Pageable.class)))
                .thenReturn(tariffPage);

        // Act
        Page<TariffDto> result = tariffSuggestionService.suggestProducts(query, country, page, size);

        // Assert
        assertThat(result).isNotNull();

        // Verify
        verify(tariffRepository)
                .findByCountry_CountryNameAndProduct_ProductCodeContaining(
                        eq(country), eq(query), any(Pageable.class));
        verifyNoMoreInteractions(tariffRepository);
    }

    @Test
    @DisplayName("Should handle pagination correctly")
    void suggestProducts_WithPagination_ShouldReturnCorrectPage() {
        // Arrange
        String query = "test";
        String country = "Canada";
        int page = 2;
        int size = 15;

        Page<Tariff> tariffPage = new PageImpl<>(List.of());
        when(tariffRepository.findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(
                eq(country), eq(query), any(Pageable.class)))
                .thenReturn(tariffPage);

        // Act
        Page<TariffDto> result = tariffSuggestionService.suggestProducts(query, country, page, size);

        // Assert
        assertThat(result).isNotNull();

        // Verify
        verify(tariffRepository)
                .findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(
                        eq(country), eq(query), any(Pageable.class));
    }

    @Test
    @DisplayName("Should return all countries as ContryDto list")
    void getAllCountries_ShouldReturnCountriesAsDtoList(){
        // Arrange
        List<Country> mockCountries = List.of(
                testCountry1, testCountry2
        );

        when(countryRepository.findAll()).thenReturn(mockCountries);

        // Act
        List<CountryDto> result = tariffSuggestionService.getAllCountries();

        // Assert
        assertThat(result).hasSize(2);
        verify(countryRepository).findAll();
    }
}
