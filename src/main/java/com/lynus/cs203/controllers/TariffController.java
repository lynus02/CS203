package com.lynus.cs203.controllers;

import com.lynus.cs203.dtos.request.TariffCalculationRequest;
import com.lynus.cs203.dtos.response.ErrorResponse;
import com.lynus.cs203.dtos.response.TariffCalculationResponse;
import com.lynus.cs203.services.TariffCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "Tariff Calculation", description = "Tariff calculation operations and rate lookup operations")
@Slf4j
@CrossOrigin
@RequiredArgsConstructor
@RequestMapping("/tariffs")
@RestController
public class TariffController {

    private final TariffCalculationService tariffCalculationService;

    @Operation(
            summary = "Calculate tariff amount",
            description = "Calculate tariff amount for given product, country, and customs value"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Tariff calculation successful",
                    content = @Content(schema = @Schema(implementation = TariffCalculationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Product, country, or tariff not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @PostMapping("/calculate")
    public ResponseEntity<TariffCalculationResponse> calculate(
            @Valid @RequestBody TariffCalculationRequest request
    ) {
        log.info("Received tariff calculation request - Product: {}, Country: {}, Value: {}",
                request.getProductCode(), request.getCountryCode(), request.getCustomsValue());

        TariffCalculationResponse response = tariffCalculationService.calculateTariff(request);

        log.info("Tariff calculation successful - Amount: {}", response.getTariffAmount());
        return ResponseEntity.ok(response);

    }



}