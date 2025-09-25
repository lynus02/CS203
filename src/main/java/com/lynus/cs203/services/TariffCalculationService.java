package com.lynus.cs203.services;

import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.ProductRepository;
import com.lynus.cs203.repositories.TariffRepository;
import org.springframework.stereotype.Service;

@Service
public class TariffCalculationService {
    private final TariffRepository tariffRepository;
    private final ProductRepository productRepository;
    private final CountryRepository countryRepository;

    public TariffCalculationService(TariffRepository tariffRepository, ProductRepository productRepository, CountryRepository countryRepository) {
        this.tariffRepository = tariffRepository;
        this.productRepository = productRepository;
        this.countryRepository = countryRepository;
    }

    // tariff calculations
    public double calculateTariff(Integer productCode, String countryCode, double customsValue) {
        // get product
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product code: " + productCode));

        // Get country
        Country country = countryRepository.findByCountryCode(countryCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid country code: " + countryCode));

        // get tariff rate
        Tariff tariff = tariffRepository.findByProductAndCountry(product, country)
                .orElseThrow(() -> new IllegalArgumentException("No tariff found for product code " + productCode + " and country code " + countryCode));

        // calculate tariff
        return (tariff.getTariffRate() / 100) * customsValue;
    }

}
