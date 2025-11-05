package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.TariffCalculationRequest;
import com.lynus.cs203.dtos.response.TariffCalculationResponse;
import com.lynus.cs203.dtos.response.TariffDto;
import com.lynus.cs203.services.TariffCalculationService;
import com.lynus.cs203.services.TariffSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Tariff Calculation", description = "Tariff calculation operations and rate lookup operations")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/tariffs")
public class TariffController {

    private final TariffCalculationService tariffCalculationService;
    private final TariffSuggestionService tariffSuggestionService;

    @Operation(
            summary = "Calculate tariff amount",
            description = "Calculate tariff amount for given product, country, and customs value"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tariff calculation successful"),
            @ApiResponse(responseCode = "404", description = "Product, country, or tariff not found")
    })
    @PostMapping("/calculate")
    public ResponseEntity<TariffCalculationResponse> calculate(
            @Valid @RequestBody TariffCalculationRequest request
    ) {
        log.info("Calculating tariff - Product: {}, Export: {}, Destination: {}, Value: {}",
                request.getProductCode(), request.getExportCountryCode(),
                request.getDesCountryCode(), request.getCustomsValue());

        TariffCalculationResponse response = tariffCalculationService.calculateTariff(request);

        log.info("Tariff calculation completed - Amount: {}", response.getTariffAmount());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Get tariff rates by size",
            description = "Retrieve tariff rates filtered by product size and optional country"
    )
    @ApiResponse(responseCode = "200", description = "Tariff rates retrieved successfully")
    @GetMapping("/size={size}")
    public List<TariffDto> getTariffRatesBySize(
            @PathVariable int size,
            @RequestParam(required = false) String country
    ) {
        log.info("Retrieving tariff rates for size: {}, country: {}", size, country);

        return tariffSuggestionService.getTariffRatesBySize(size, country);
    }

    @Operation(
            summary = "Suggest products with tariffs",
            description = "Search and suggest products with tariff information using pagination"
    )
    @ApiResponse(responseCode = "200", description = "Product suggestions retrieved successfully")
    @GetMapping("/suggest")
    public Page<TariffDto> suggestProducts(
            @RequestParam("q") String query,
            @RequestParam(required = false) String country,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Suggesting products - Query: '{}', Country: {}, Page: {}, Size: {}", query, country, page, size);

        return tariffSuggestionService.suggestProducts(query, country, page, size);
    }
}