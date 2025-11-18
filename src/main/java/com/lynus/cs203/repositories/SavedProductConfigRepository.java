package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.SavedProductConfig;
import com.lynus.cs203.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedProductConfigRepository extends JpaRepository<SavedProductConfig, Long> {
    List<SavedProductConfig> findByUser(User user);

    Optional<SavedProductConfig> findByUserAndProduct(User user, Product product);

    void deleteByUserAndProduct(User user, Product product);
}
