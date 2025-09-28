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

    public TariffCalculationService(TariffRepository tariffRepository,
                                    ProductRepository productRepository,
                                    CountryRepository countryRepository) {
        this.tariffRepository = tariffRepository;
        this.productRepository = productRepository;
        this.countryRepository = countryRepository;
    }

    public double getTariffRate(Integer productCode, String countryParam) {
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product code: " + productCode));

        Country country = countryRepository.findByCountryCode(countryParam)
                .orElseGet(() -> countryRepository.findByCountryName(countryParam)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid country: " + countryParam)));

        Tariff tariff = tariffRepository.findByProductAndCountry(product, country)
                .orElseThrow(() -> new IllegalArgumentException("No tariff found for this product-country combination"));

        return tariff.getTariffRate();
    }

    public double calculateTariff(Integer productCode, String countryParam, double customsValue) {
        // Find product
        Product product = productRepository.findByProductCode(productCode)
                .orElseThrow(() -> new IllegalArgumentException("Invalid product code: " + productCode));

        // Find country
        Country country = countryRepository.findByCountryCode(countryParam)
                .orElseGet(() -> countryRepository.findByCountryName(countryParam)
                        .orElseThrow(() -> new IllegalArgumentException("Invalid country: " + countryParam)));


        // Find tariff
        Tariff tariff = tariffRepository.findByProductAndCountry(product, country)
                .orElseThrow(() -> new IllegalArgumentException("No tariff found for this product-country combination"));

        // Calculate tariff amount
        return tariff.getTariffRate() * customsValue / 100.0;
    }
}

