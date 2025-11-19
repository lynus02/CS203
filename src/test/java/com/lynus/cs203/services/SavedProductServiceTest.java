package com.lynus.cs203.services;

import com.lynus.cs203.dtos.request.SavedProductRequest;
import com.lynus.cs203.dtos.response.SavedProductResponse;
import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.SavedProductConfig;
import com.lynus.cs203.entities.User;
import com.lynus.cs203.exceptions.UserNotFoundException;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.ProductRepository;
import com.lynus.cs203.repositories.SavedProductConfigRepository;
import com.lynus.cs203.repositories.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Saved Product Service Unit Test")
public class SavedProductServiceTest {

    @Mock
    private SavedProductConfigRepository savedRepo;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CountryRepository countryRepository;

    @InjectMocks
    private SavedProductService savedProductService;

    @Test
    @DisplayName("Should save configuration when request is valid")
    void saveForUser_WhenValidRequest_ShouldSaveSuccessfully() {
        // Arrange
        String userId = "user123";

        User user = new User();
        user.setUserId(userId);

        Product product = new Product();
        product.setProductId(10L);
        product.setProductDescription("Rice");
        product.setProductCode(1003);
        product.setFoodCategory("Grains");

        Country origin = new Country();
        origin.setCountryId(1L);
        origin.setCountryName("Singapore");

        Country dest = new Country();
        dest.setCountryId(2L);
        dest.setCountryName("United States");

        LocalDateTime importDate = LocalDateTime.of(2025, 1, 1, 0, 0);

        SavedProductRequest request = SavedProductRequest.builder()
                .productId(10L)
                .configName("My Config")
                .productValue(500.0)
                .originCountryId(1L)
                .destinationCountryId(2L)
                .importDate(importDate)
                .build();

        SavedProductConfig saved = SavedProductConfig.builder()
                .id(99L)
                .user(user)
                .product(product)
                .configName("My Config")
                .productValue(500.0)
                .originCountry(origin)
                .destinationCountry(dest)
                .importDate(importDate)
                .savedAt(LocalDateTime.now())
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(countryRepository.findById(1L)).thenReturn(Optional.of(origin));
        when(countryRepository.findById(2L)).thenReturn(Optional.of(dest));
        when(savedRepo.existsByUserAndConfigName(user, "My Config")).thenReturn(false);
        when(savedRepo.existsByUserAndProductAndOriginCountryAndDestinationCountry(user, product, origin, dest))
                .thenReturn(false);
        when(savedRepo.save(any(SavedProductConfig.class))).thenReturn(saved);

        // Act
        SavedProductResponse response = savedProductService.saveForUser(userId, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getConfigName()).isEqualTo("My Config");
        assertThat(response.getImportDate()).isEqualTo(importDate.toString());
        assertThat(response.getProduct().getId()).isEqualTo(10L);
        assertThat(response.getOriginCountry()).isEqualTo("Singapore");
        assertThat(response.getDestinationCountry()).isEqualTo("United States");

        // Verify save was called with proper object
        ArgumentCaptor<SavedProductConfig> captor = ArgumentCaptor.forClass(SavedProductConfig.class);
        verify(savedRepo).save(captor.capture());

        SavedProductConfig captured = captor.getValue();
        assertThat(captured.getConfigName()).isEqualTo("My Config");
        assertThat(captured.getProductValue()).isEqualTo(500.0);
        assertThat(captured.getImportDate()).isEqualTo(importDate);
    }

    @Test
    @DisplayName("Should throw when user not found during save")
    void saveForUser_WhenUserNotFound_ShouldThrow() {
        when(userRepository.findById("abc")).thenReturn(Optional.empty());

        SavedProductRequest request = SavedProductRequest.builder()
                .productId(10L)
                .configName("Config1")
                .originCountryId(1L)
                .destinationCountryId(2L)
                .importDate(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> savedProductService.saveForUser("abc", request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessageContaining("User not found");

        verify(savedRepo, never()).save(any());
    }

    @Test
    @DisplayName("Should throw when product does not exist")
    void saveForUser_WhenProductMissing_ShouldThrow() {
        User user = new User();
        user.setUserId("u1");

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        SavedProductRequest request = SavedProductRequest.builder()
                .productId(99L)
                .configName("Config1")
                .originCountryId(1L)
                .destinationCountryId(2L)
                .importDate(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> savedProductService.saveForUser("u1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    @DisplayName("Should throw when origin country not found")
    void saveForUser_WhenOriginMissing_ShouldThrow() {
        User user = new User();
        user.setUserId("u1");

        Product product = new Product();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
        when(countryRepository.findById(1L)).thenReturn(Optional.empty());

        SavedProductRequest request = SavedProductRequest.builder()
                .productId(10L)
                .originCountryId(1L)
                .destinationCountryId(2L)
                .configName("Config1")
                .importDate(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> savedProductService.saveForUser("u1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Origin country not found");
    }

    @Test
    @DisplayName("Should throw when destination country not found")
    void saveForUser_WhenDestinationMissing_ShouldThrow() {
        User user = new User();
        user.setUserId("u1");

        Product product = new Product();
        Country origin = new Country();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
        when(countryRepository.findById(1L)).thenReturn(Optional.of(origin));
        when(countryRepository.findById(2L)).thenReturn(Optional.empty());

        SavedProductRequest request = SavedProductRequest.builder()
                .productId(10L)
                .originCountryId(1L)
                .destinationCountryId(2L)
                .configName("Config1")
                .importDate(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> savedProductService.saveForUser("u1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Destination country not found");
    }

    @Test
    @DisplayName("Should throw when config name is null or blank")
    void saveForUser_WhenConfigNameEmpty_ShouldThrow() {
        User user = new User();
        Product product = new Product();
        Country origin = new Country();
        Country dest = new Country();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
        when(countryRepository.findById(anyLong()))
                .thenReturn(Optional.of(origin))
                .thenReturn(Optional.of(dest));

        SavedProductRequest request = SavedProductRequest.builder()
                .productId(10L)
                .configName(" ")
                .originCountryId(1L)
                .destinationCountryId(2L)
                .importDate(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> savedProductService.saveForUser("u1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Config name cannot be empty");
    }

    @Test
    @DisplayName("Should throw when import date is missing")
    void saveForUser_WhenImportDateMissing_ShouldThrow() {
        User user = new User();
        Product product = new Product();
        Country origin = new Country();
        Country dest = new Country();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
        when(countryRepository.findById(anyLong()))
                .thenReturn(Optional.of(origin))
                .thenReturn(Optional.of(dest));

        SavedProductRequest request = SavedProductRequest.builder()
                .productId(10L)
                .configName("Config1")
                .originCountryId(1L)
                .destinationCountryId(2L)
                .importDate(null)
                .build();

        assertThatThrownBy(() -> savedProductService.saveForUser("u1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Import date is required");
    }

    @Test
    @DisplayName("Should throw when config name already exists for user")
    void saveForUser_WhenConfigNameExists_ShouldThrow() {
        User user = new User();
        Product product = new Product();
        Country origin = new Country();
        Country dest = new Country();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
        when(countryRepository.findById(anyLong()))
                .thenReturn(Optional.of(origin))
                .thenReturn(Optional.of(dest));

        when(savedRepo.existsByUserAndConfigName(user, "Conf1")).thenReturn(true);

        SavedProductRequest request = SavedProductRequest.builder()
                .productId(10L)
                .configName("Conf1")
                .originCountryId(1L)
                .destinationCountryId(2L)
                .importDate(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> savedProductService.saveForUser("u1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    @DisplayName("Should throw when duplicate combination (product & countries) exists")
    void saveForUser_WhenDuplicateCombinationExists_ShouldThrow() {
        User user = new User();
        Product product = new Product();
        Country origin = new Country();
        Country dest = new Country();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
        when(countryRepository.findById(anyLong()))
                .thenReturn(Optional.of(origin))
                .thenReturn(Optional.of(dest));

        when(savedRepo.existsByUserAndConfigName(user, "Config")).thenReturn(false);
        when(savedRepo.existsByUserAndProductAndOriginCountryAndDestinationCountry(user, product, origin, dest))
                .thenReturn(true);

        SavedProductRequest request = SavedProductRequest.builder()
                .productId(10L)
                .configName("Config")
                .originCountryId(1L)
                .destinationCountryId(2L)
                .importDate(LocalDateTime.now())
                .build();

        assertThatThrownBy(() -> savedProductService.saveForUser("u1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already saved this configuration");
    }

    @Test
    @DisplayName("Should throw duplicate exception when race condition occurs")
    void saveForUser_WhenRaceCondition_ShouldThrow() {
        User user = new User();
        Product product = new Product();
        Country origin = new Country();
        Country dest = new Country();

        SavedProductRequest request = SavedProductRequest.builder()
                .productId(1L)
                .configName("C1")
                .originCountryId(1L)
                .destinationCountryId(2L)
                .importDate(LocalDateTime.now())
                .build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(productRepository.findById(anyLong())).thenReturn(Optional.of(product));
        when(countryRepository.findById(anyLong()))
                .thenReturn(Optional.of(origin))
                .thenReturn(Optional.of(dest));

        when(savedRepo.existsByUserAndConfigName(any(), anyString())).thenReturn(false);
        when(savedRepo.existsByUserAndProductAndOriginCountryAndDestinationCountry(any(), any(), any(), any()))
                .thenReturn(false);

        when(savedRepo.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> savedProductService.saveForUser("u1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be saved again");
    }

    @Test
    @DisplayName("Should return saved configs for user")
    void getForUser_WhenUserExists_ShouldReturnList() {
        User user = new User();
        user.setUserId("u1");

        SavedProductConfig config1 = SavedProductConfig.builder()
                .id(1L)
                .user(user)
                .product(new Product())
                .originCountry(new Country())
                .destinationCountry(new Country())
                .importDate(LocalDateTime.now())
                .savedAt(LocalDateTime.now())
                .configName("C1")
                .build();

        when(userRepository.findById("u1")).thenReturn(Optional.of(user));
        when(savedRepo.findByUser(user)).thenReturn(List.of(config1));

        List<SavedProductResponse> results = savedProductService.getForUser("u1");

        assertThat(results).hasSize(1);
        verify(savedRepo).findByUser(user);
    }

    @Test
    @DisplayName("Should throw when user not found during get")
    void getForUser_WhenUserNotFound_ShouldThrow() {
        when(userRepository.findById("u2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savedProductService.getForUser("u2"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    @DisplayName("Should delete config when user owns it")
    void delete_WhenUserOwnsConfig_ShouldDelete() {
        User user = new User();
        user.setUserId("u1");

        SavedProductConfig config = SavedProductConfig.builder()
                .id(5L)
                .user(user)
                .build();

        when(savedRepo.findById(5L)).thenReturn(Optional.of(config));

        savedProductService.delete("u1", 5L);

        verify(savedRepo).delete(config);
    }

    @Test
    @DisplayName("Should throw when deleting config not owned by user")
    void delete_WhenNotOwner_ShouldThrow() {
        User owner = new User();
        owner.setUserId("owner");

        SavedProductConfig config = SavedProductConfig.builder()
                .id(5L)
                .user(owner)
                .build();

        when(savedRepo.findById(5L)).thenReturn(Optional.of(config));

        assertThatThrownBy(() -> savedProductService.delete("someoneElse", 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not authorized");
    }

    @Test
    @DisplayName("Should throw when deleting config that does not exist")
    void delete_WhenConfigMissing_ShouldThrow() {
        when(savedRepo.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savedProductService.delete("u1", 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not found");
    }
}
