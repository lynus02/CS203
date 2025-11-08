package com.lynus.cs203.services;

import com.lynus.cs203.dtos.request.TariffCalculationRequest;
import com.lynus.cs203.dtos.response.TariffCalculationResponse;
import com.lynus.cs203.entities.*;
import com.lynus.cs203.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class TariffCalculationService {

    private final TariffRepository tariffRepository;
    private final ProductRepository productRepository;
    private final CountryRepository countryRepository;
    private final AgreementCountryRepository agreementCountryRepository;
    private final TradeAgreementRepository tradeAgreementRepository;

    // define sensitivity tiers
    private static final String HIGH_SENSTIVITY = "HIGH";
    private static final String MEDIUM_SENSITIVITY = "MEDIUM";
    private static final String LOW_SENSITIVITY = "LOW";

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

        // Find export country (origin: where goods come from
        log.debug("Looking up export country with code: {}", request.getExportCountryCode());
        Country exportCountry = countryRepository.findByCountryCode(request.getExportCountryCode())
                .orElseThrow(() -> {
                    log.warn("Country not found with code: {}", request.getExportCountryCode());
                    return new IllegalArgumentException(
                            "Invalid export country code: " + request.getExportCountryCode());
                });

        log.debug("Found export country with code: {})", exportCountry.getCountryCode());

        // Find import country (destination: where goods go to)
        log.debug("Looking up export country with code: {}", request.getDesCountryCode());
        Country desCountry = countryRepository.findByCountryCode(request.getDesCountryCode())
                .orElseThrow(() -> {
                    log.warn("Country not found with code: {}", request.getExportCountryCode());
                    return new IllegalArgumentException(
                            "Invalid export country code: " + request.getExportCountryCode());
                });
        log.debug("Found destination country with code: {})", desCountry.getCountryCode());

        // Find tariff
        log.debug("Looking up tariff for Product: {} and Country: {}",
                product.getProductCode(), desCountry.getCountryCode());
        Tariff tariff = tariffRepository.findByProductAndCountry(product, desCountry)
                .orElseThrow(() -> {
                    log.warn("No tariff found for Product Code: {} and Country Code: {}",
                            product.getProductCode(), desCountry.getCountryCode());
                    return new IllegalArgumentException(
                            "No tariff found for this product-country combination");
                });

        log.debug("Found tariff rate: {}% for Product: {} and Country: {}",
                tariff.getTariffRate(), product.getProductCode(), desCountry.getCountryCode());

        // Calculate sensitivity tier from hscode
        String sensitivityTier = calculateSensitivityTier(Integer.toString(product.getProductCode()).substring(0, 2));
        log.debug("Calculated sensitivity tier: {} for HS Code: {}", sensitivityTier, product.getProductCode());

        // Check for trade agreements and calculate preferential rate
        List<Long> tradeAgreementIds = agreementCountryRepository.findAgreementsBetweenCountries(exportCountry.getCountryName(), desCountry.getCountryName());

        log.debug("Found {} trade agreements between Export Country: {} and Destination Country: {}",
                tradeAgreementIds.size(), exportCountry.getCountryName(), desCountry.getCountryName());

        // If a few agreements exist, find lowest discount multiplier (best discount
        double baseRate = tariff.getTariffRate();
        double bestFinalRate = baseRate;
        String bestAgreementType = "MFN"; // set default to Most Favored Nation

        if (!tradeAgreementIds.isEmpty()) {
            for (Long agreementId : tradeAgreementIds) {
                TradeAgreement agreement = tradeAgreementRepository.findByAgreementId(agreementId)
                        .orElse(null);

                if (agreement != null) {
                    // split agreement types by "&"
                    String[] types = agreement.getAgreementType().split("&");
                    for (String agreementType : types) {
                        double discountMultiplier = getDiscountMultiplier(sensitivityTier, agreementType.trim());

                        double discountedRate = baseRate * discountMultiplier;

                        // Pick the lowest tariff rate
                        if (discountedRate < bestFinalRate) {
                            bestFinalRate = discountedRate;
                            bestAgreementType = agreementType.trim();
                        }
                    }
                }
            }
        }
        // Calculate tariff amount
        double tariffAmount = (bestFinalRate / 100.0) * request.getCustomsValue();

        log.info("Tariff calculation completed. Tariff Amount: {}", tariffAmount);
        return TariffCalculationResponse.builder()
                .productCode(product.getProductCode())
                .exportCountryCode(exportCountry.getCountryCode())
                .desCountryCode(desCountry.getCountryCode())
                .customsValue(request.getCustomsValue())
                .tariffAmount(tariffAmount)
                .agreementType(bestAgreementType)
                .build();
    }

    public String calculateSensitivityTier(String hsCode) {
        if (hsCode == null || hsCode.length() < 2) {
            throw new IllegalArgumentException("Invalid HS Code: " + hsCode);
        }

        try {
            int chapter = Integer.parseInt(hsCode.substring(0, 2));
            if (chapter == 10 || chapter == 41) {
                return HIGH_SENSTIVITY;
            } else if (chapter >= 20 && chapter <= 21 || chapter == 30 || chapter == 40) {
                return MEDIUM_SENSITIVITY;
            } else if ((chapter >= 50 && chapter <= 71) || (chapter == 80)) {
                return LOW_SENSITIVITY;
            } else {
                return MEDIUM_SENSITIVITY;
            }
        } catch (NumberFormatException e) {
            return MEDIUM_SENSITIVITY;
        }
    }

    private double getDiscountMultiplier(String sensitivityTier, String agreementType) {
        switch (sensitivityTier.toUpperCase()) {
            case "HIGH":
                switch (agreementType.toUpperCase()) {
                    case "PSA": return 0.90; // 10% discount - small reduction for sensitive goods
                    case "FTA": return 0.70; // 30% discount - moderate reduction
                    case "CU":  return 0.50; // 50% discount - significant reduction
                    case "EIA": return 0.30; // 70% discount - deep reduction
                    default:    return 1.00; // No discount
                }

            case "MEDIUM":
                switch (agreementType.toUpperCase()) {
                    case "PSA": return 0.85; // 15% discount
                    case "FTA": return 0.50; // 50% discount
                    case "CU":  return 0.25; // 75% discount
                    case "EIA": return 0.10; // 90% discount
                    default:    return 1.00;
                }

            case "LOW":
                switch (agreementType.toUpperCase()) {
                    case "PSA": return 0.80; // 20% discount
                    case "FTA": return 0.30; // 70% discount
                    case "CU":  return 0.10; // 90% discount
                    case "EIA": return 0.00; // 100% discount - duty free
                    default:    return 1.00;
                }

            default:
                return 1.00; // Default to no discount
        }

    }
}

