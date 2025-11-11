package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.SavedProductConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedProductConfigRepository extends JpaRepository<SavedProductConfig, String> {
    List<SavedProductConfig> findByUserId(String userId);

    // Find a saved product by user and product
    Optional<SavedProductConfig> findByUserIdAndProductId(String userId, String productId);

    void deleteByUserIdAndProductId(String userId, String productId);
}
