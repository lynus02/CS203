package com.lynus.cs203.integration.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lynus.cs203.dtos.request.TariffCalculationRequest;
import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.ProductRepository;
import com.lynus.cs203.repositories.TariffRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Tariff Controller Integration Test")
class TariffControllerIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private TariffRepository tariffRepository;

    @BeforeEach
    void setUp() {
        tariffRepository.deleteAll();
    }

    @Test
    @DisplayName("Should calculate tariff for valid request")
    void calculateTariff_WithValidRequest_ShouldReturnTariffCalculation() {

    }

    @Test
    @DisplayName("Should suggest products based on query and country")
    void suggestProducts_WithQueryAndCountry_ShouldReturnSuggestions() throws Exception {
        // Arrange
        Country country = createUniqueCountry("TC001", "TestCountry1");
        Product product = createUniqueProduct(10001, "Test Product");
        createTariff(product, country, 5.0);

        // Act & Assert
        mockMvc.perform(get("/tariffs/suggest")
                        .param("q", "Test")
                        .param("country", "TestCountry1")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Should suggest products based on numeric query")
    void suggestProducts_WithNumericQuery_ShouldReturnSuggestions() throws Exception {
        // Arrange
        Country country = createUniqueCountry("TC002", "TestCountry2");
        Product product = createUniqueProduct(20002, "Another Product");
        createTariff(product, country, 7.5);

        // Act & Assert
        mockMvc.perform(get("/tariffs/suggest")
                        .param("q", "20002")
                        .param("country", "TestCountry2")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Should suggest products by size and country")
    void getTariffRatesBySize_WithSizeAndCountry_ShouldReturnRates() throws Exception {
        // Arrange
        Country country = createUniqueCountry("TC003", "TestCountry3");
        Product p1 = createUniqueProduct(30001, "Test Product");
        Product p2 = createUniqueProduct(30002, "Another Product");
        createTariff(p1, country, 5.0);
        createTariff(p2, country, 10.0);

        // Act & Assert
        mockMvc.perform(get("/tariffs/size=10")
                        .param("country", "TestCountry3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("Should return empty results for non-existent country")
    void getTariffRatesBySize_WithNonExistentCountry_ShouldReturnEmpty() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/tariffs/size=10")
                        .param("country", "NonExistentCountry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("Should return 400 for missing required fields")
    void calculateTariff_WithMissingFields_ShouldReturnBadRequest() throws Exception {
        TariffCalculationRequest request = TariffCalculationRequest.builder()
                .productCode(5005)
                .exportCountryCode("C840")
                .desCountryCode(null)
                .customsValue(200.0)
                .build();

        // Act & Assert
        mockMvc.perform(post("/tariffs/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should suggest products without country filter")
    void suggestProducts_WithoutCountry_ShouldReturnSuggestions() throws Exception {
        // Arrange
        Country country = createUniqueCountry("TC004", "TestCountry4");
        Product product = createUniqueProduct(40001, "Sample Product");
        createTariff(product, country, 3.5);

        // Act & Assert
        mockMvc.perform(get("/tariffs/suggest")
                        .param("q", "Sample")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", isA(List.class)));
    }

    // ========== HELPER METHODS ==========
    private Country createUniqueCountry(String countryCode, String countryName) {
        return countryRepository.findByCountryCode(countryCode)
                .orElseGet(() -> {
                    Country country = new Country();
                    country.setCountryCode(countryCode);
                    country.setCountryName(countryName);
                    return countryRepository.save(country);
                });
    }

    private Product createUniqueProduct(int productCode, String productDescription) {
        return productRepository.findByProductCode(productCode)
                .orElseGet(() -> {
                    Product product = new Product();
                    product.setProductCode(productCode);
                    product.setProductDescription(productDescription);
                    return productRepository.save(product);
                });
    }

    private Tariff createTariff(Product product, Country country, double tariffRate) {
        Tariff tariff = new Tariff();
        tariff.setProduct(product);
        tariff.setCountry(country);
        tariff.setTariffRate(tariffRate);
        return tariffRepository.save(tariff);
    }
}
