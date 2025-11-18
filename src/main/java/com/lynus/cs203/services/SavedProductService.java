package com.lynus.cs203.services;

import com.lynus.cs203.dtos.request.SavedProductRequest;
import com.lynus.cs203.dtos.response.ProductDto;
import com.lynus.cs203.dtos.response.SavedProductResponse;
import com.lynus.cs203.entities.*;
import com.lynus.cs203.exceptions.UserNotFoundException;
import com.lynus.cs203.repositories.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SavedProductService {

    private final SavedProductConfigRepository savedRepo;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final CountryRepository countryRepository;

    /** Create saved config */
    public SavedProductResponse saveForUser(String userId, SavedProductRequest request) {
        log.info("Saving product config for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Cannot save config. User not found: " + userId));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + request.getProductId()));

        Country origin = countryRepository.findById(request.getOriginCountryId())
                .orElseThrow(() -> new IllegalArgumentException("Origin country not found"));

        Country destination = countryRepository.findById(request.getDestinationCountryId())
                .orElseThrow(() -> new IllegalArgumentException("Destination country not found"));

        SavedProductConfig config = SavedProductConfig.builder()
                .user(user)
                .product(product)
                .configName(request.getConfigName())
                .productValue(request.getProductValue())
                .originCountry(origin)
                .destinationCountry(destination)
                .importDate(request.getImportDate())
                .savedAt(LocalDateTime.now())
                .build();

        SavedProductConfig saved = savedRepo.save(config);

        return toResponse(saved);
    }

    /** Get all saved configs for user */
    public List<SavedProductResponse> getForUser(String userId) {
        log.info("Loading saved products for user {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found: " + userId));

        return savedRepo.findByUser(user)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Delete one saved config by ID */
    public void delete(String userId, Long configId) {
        log.info("Deleting saved product {} for user {}", configId, userId);

        SavedProductConfig config = savedRepo.findById(configId)
                .orElseThrow(() -> new IllegalArgumentException("Saved config not found"));

        if (!config.getUser().getUserId().equals(userId)) {
            throw new IllegalStateException("Not authorized to delete this config");
        }

        savedRepo.delete(config);
    }

    private SavedProductResponse toResponse(SavedProductConfig config) {
        Product product = config.getProduct();

        return SavedProductResponse.builder()
                .id(config.getId())
                .configName(config.getConfigName())
                .productValue(config.getProductValue())
                .originCountry(config.getOriginCountry().getCountryName())
                .destinationCountry(config.getDestinationCountry().getCountryName())
                .importDate(config.getImportDate().toString())
                .savedAt(config.getSavedAt().toString())
                .product(
                        ProductDto.builder()
                                .id(product.getProductId())
                                .hsCode(String.valueOf(product.getProductCode()))
                                .category(product.getFoodCategory())
                                .build()
                )
                .build();
    }
}
