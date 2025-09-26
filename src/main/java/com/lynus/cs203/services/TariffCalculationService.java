package com.lynus.cs203.services;

import com.lynus.cs203.dtos.request.TariffCalculationRequest;
import com.lynus.cs203.dtos.response.TariffCalculationResponse;
import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.ProductRepository;
import com.lynus.cs203.repositories.TariffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class TariffCalculationService {
    private final TariffRepository tariffRepository;
    private final ProductRepository productRepository;
    private final CountryRepository countryRepository;

    public TariffCalculationResponse calculateTariff(TariffCalculationRequest request) {
        log.info("Calculating tariff");

        // Find product
        log.debug("Looking up product with code: {}", request.getProductCode());
        Product product = productRepository.findByProductCode(request.getProductCode())
                .orElseThrow(() -> {
                    log.warn("Product not found with code: {}", request.getProductCode());
                    return new IllegalArgumentException(
                            "Invalid product code: " + request.getProductCode());
                });

        log.debug("Found product with code: {})", product.getProductCode());

        // Find country
        log.debug("Looking up country with code: {}", request.getCountryCode());
        Country country = countryRepository.findByCountryCode(request.getCountryCode())
                .orElseThrow(() -> {
                    log.warn("Country not found with code: {}", request.getCountryCode());
                    return new IllegalArgumentException(
                            "Invalid country code: " + request.getCountryCode());
                });

        log.debug("Found country with code: {})", country.getCountryCode());

        // Find tariff
        log.debug("Looking up tariff for Product: {} and Country: {}",
                product.getProductCode(), country.getCountryCode());
        Tariff tariff = tariffRepository.findByProductAndCountry(product, country)
                .orElseThrow(() -> {
                    log.warn("No tariff found for Product Code: {} and Country Code: {}",
                            product.getProductCode(), country.getCountryCode());
                    return new IllegalArgumentException(
                            "No tariff found for this product-country combination");
                });

        log.debug("Found tariff rate: {}% for Product: {} and Country: {}",
                tariff.getTariffRate(), product.getProductCode(), country.getCountryCode());

        // Calculate tariff amount
        double tariffAmount = tariff.getTariffRate() * request.getCustomsValue() / 100.0;

        log.info("Tariff calculation completed. Tariff Amount: {}", tariffAmount);
        return TariffCalculationResponse.builder()
                .productCode(product.getProductCode())
                .countryCode(country.getCountryCode())
                .customsValue(request.getCustomsValue())
                .tariffAmount(tariffAmount)
                .build();
    }
}

