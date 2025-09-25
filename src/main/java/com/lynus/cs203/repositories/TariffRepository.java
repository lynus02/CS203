package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {

    Optional<Tariff> findByProductAndCountry(Product product, Country country);
}