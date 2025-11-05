package com.lynus.cs203.controllers;

import com.lynus.cs203.entities.Product;
import com.lynus.cs203.repositories.ProductRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Products", description = "Product management operations")
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
public class ProductController {
    private final ProductRepository productRepository;

    @Operation(
            summary = "Get all products",
            description = "Retrieve a list of all available products"
    )
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    @GetMapping
    public List<Product> getAllProducts() {
        log.info("Retrieving all products");

        List<Product> products = productRepository.findAll();
        log.debug("Retrieved {} products", products.size());

        return products;
    }

    @Operation(
            summary = "Search products",
            description = "Search products by description or code"
    )
    @ApiResponse(responseCode = "200", description = "Products retrieved successfully")
    @GetMapping("/search")
    public List<Product> search(
            @Parameter(
                    description = "Search query for product description or code"
            )
            @RequestParam String q
    ) {
        log.info("Searching products with query: '{}'", q);

        List<Product> products = productRepository.findByDescriptionOrCode(q);
        log.debug("Found {} products for query: '{}'", products.size(), q);

        return products;
    }
}
