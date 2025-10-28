package com.lynus.cs203.services;

import com.lynus.cs203.dtos.response.TariffDto;
import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.TariffRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TariffSuggestionServiceTest {

    @InjectMocks
    private TariffSuggestionService tariffSuggestionService;

    @Mock
    private TariffRepository tariffRepository;

    @Test
    @DisplayName("getTariffRatesBySize: should return DTO list and call repository")
    void getTariffRatesBySize_ShouldReturnListOfDtos() {
        // Arrange
        String country = "United States";
        int size = 2;

        Product p1 = new Product();
        p1.setProductCode(1001);
        Product p2 = new Product();
        p2.setProductCode(2002);

        Country c = new Country();
        c.setCountryName(country);

        Tariff t1 = new Tariff();
        t1.setProduct(p1);
        t1.setCountry(c);
        t1.setTariffRate(5.0);

        Tariff t2 = new Tariff();
        t2.setProduct(p2);
        t2.setCountry(c);
        t2.setTariffRate(10.0);

        Page<Tariff> page = new PageImpl<>(List.of(t1, t2));
        when(tariffRepository.findByCountry_CountryName(eq(country), any(Pageable.class))).thenReturn(page);

        // Act
        List<TariffDto> result = tariffSuggestionService.getTariffRatesBySize(size, country);

        // Assert
        assertThat(result).hasSize(2);
        verify(tariffRepository).findByCountry_CountryName(eq(country), any(Pageable.class));
    }

    @Test
    @DisplayName("suggestProducts: numeric query with country should call findByCountryAndProductCodeContaining")
    void suggestProducts_NumericQueryWithCountry_ShouldCallFindByCountryAndProductCodeContaining() {
        // Arrange
        String q = "12345";
        String country = "United States";
        int page = 0;
        int size = 10;

        Product p = new Product();
        p.setProductCode(12345);

        Country c = new Country();
        c.setCountryName(country);

        Tariff t = new Tariff();
        t.setProduct(p);
        t.setCountry(c);
        t.setTariffRate(2.5);

        Page<Tariff> pageResp = new PageImpl<>(List.of(t));
        when(tariffRepository.findByCountryAndProductCodeContaining(eq(country), eq(12345), any(Pageable.class)))
                .thenReturn(pageResp);

        // Act
        Page<TariffDto> result = tariffSuggestionService.suggestProducts(q, country, page, size);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(tariffRepository).findByCountryAndProductCodeContaining(eq(country), eq(12345), any(Pageable.class));
    }

    @Test
    @DisplayName("suggestProducts: non-numeric query with country should call description search")
    void suggestProducts_NonNumericQueryWithCountry_ShouldCallFindByCountryAndDescription() {
        // Arrange
        String q = "motor";
        String country = "United States";
        int page = 0;
        int size = 5;

        Product p = new Product();
        p.setProductCode(5005);
        p.setProductDescription("Electric Motor");

        Country c = new Country();
        c.setCountryName(country);

        Tariff t = new Tariff();
        t.setProduct(p);
        t.setCountry(c);
        t.setTariffRate(3.0);

        Page<Tariff> pageResp = new PageImpl<>(List.of(t));
        when(tariffRepository.findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(eq(country), eq(q), any(Pageable.class)))
                .thenReturn(pageResp);

        // Act
        Page<TariffDto> result = tariffSuggestionService.suggestProducts(q, country, page, size);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(tariffRepository).findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(eq(country), eq(q), any(Pageable.class));
    }

    @Test
    @DisplayName("suggestProducts: no country should call description-or-code search")
    void suggestProducts_NoCountry_ShouldCallFindByProductDescriptionOrProductCodeContaining() {
        // Arrange
        String q = "motor";
        String country = null;
        int page = 0;
        int size = 10;

        Product p = new Product();
        p.setProductCode(7007);
        p.setProductDescription("Motor Oil");

        Country c = new Country();
        c.setCountryName("AnyCountry");

        Tariff t = new Tariff();
        t.setProduct(p);
        t.setCountry(c);
        t.setTariffRate(7.5);

        Page<Tariff> pageResp = new PageImpl<>(List.of(t));
        when(tariffRepository.findByProductDescriptionOrProductCodeContaining(eq(q.toLowerCase()), eq(-1), any(Pageable.class)))
                .thenReturn(pageResp);

        // Act
        Page<TariffDto> result = tariffSuggestionService.suggestProducts(q, country, page, size);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(tariffRepository).findByProductDescriptionOrProductCodeContaining(eq(q.toLowerCase()), eq(-1), any(Pageable.class));
    }

    @Test
    void suggestProducts_whenCountryProvided_callsCountryDescriptionQuery() {
        // Arrange
        String q = "motor";
        String country = "United States";
        int page = 0;
        int size = 10;
        Page emptyPage = new PageImpl<>(List.of());

        when(tariffRepository.findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(
                eq(country), eq(q), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        Page<?> result = tariffSuggestionService.suggestProducts(q, country, page, size);

        // Assert
        assertThat(result).isNotNull();
        verify(tariffRepository).findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(
                eq(country), eq(q), any(Pageable.class));
        verifyNoMoreInteractions(tariffRepository);
    }

    @Test
    void suggestProducts_whenNoCountry_nonNumericQuery_callsDescriptionOrCodeContainingWithMinusOne() {
        // Arrange
        String q = "honey";
        int page = 0;
        int size = 20;
        Page emptyPage = new PageImpl<>(List.of());

        when(tariffRepository.findByProductDescriptionOrProductCodeContaining(
                eq(q.toLowerCase()), eq(-1), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        Page<?> result = tariffSuggestionService.suggestProducts(q, null, page, size);

        // Assert
        assertThat(result).isNotNull();
        verify(tariffRepository).findByProductDescriptionOrProductCodeContaining(
                eq(q.toLowerCase()), eq(-1), any(Pageable.class));
        verifyNoMoreInteractions(tariffRepository);
    }

    @Test
    @DisplayName("suggestProducts: no country and numeric query should call description-or-code with numeric code")
    void suggestProducts_NoCountry_NumericQuery_ShouldCallFindByProductDescriptionOrProductCodeContainingWithNumeric() {
        // Arrange
        String q = "42";
        int page = 0;
        int size = 10;

        Product p = new Product();
        p.setProductCode(42);
        p.setProductDescription("Answer");

        Country c = new Country();
        c.setCountryName("Any");

        Tariff t = new Tariff();
        t.setProduct(p);
        t.setCountry(c);
        t.setTariffRate(1.0);

        Page<Tariff> pageResp = new PageImpl<>(List.of(t));
        when(tariffRepository.findByProductDescriptionOrProductCodeContaining(eq(q.toLowerCase()), eq(42), any(Pageable.class)))
                .thenReturn(pageResp);

        // Act
        Page<TariffDto> result = tariffSuggestionService.suggestProducts(q, null, page, size);

        // Assert
        assertThat(result.getContent()).hasSize(1);
        verify(tariffRepository).findByProductDescriptionOrProductCodeContaining(eq(q.toLowerCase()), eq(42), any(Pageable.class));
    }

    @Test
    @DisplayName("suggestProducts: empty country should behave like no country and call description-or-code search")
    void suggestProducts_EmptyCountry_ShouldCallFindByProductDescriptionOrProductCodeContaining() {
        // Arrange
        String q = "honey";
        String country = "";
        int page = 0;
        int size = 10;
        Page<Tariff> emptyPage = new PageImpl<>(List.of());

        when(tariffRepository.findByProductDescriptionOrProductCodeContaining(
                eq(q.toLowerCase()), eq(-1), any(Pageable.class)))
                .thenReturn(emptyPage);

        // Act
        Page<TariffDto> result = tariffSuggestionService.suggestProducts(q, country, page, size);

        // Assert
        assertThat(result).isNotNull();
        verify(tariffRepository).findByProductDescriptionOrProductCodeContaining(
                eq(q.toLowerCase()), eq(-1), any(Pageable.class));
        verifyNoMoreInteractions(tariffRepository);
    }

}
