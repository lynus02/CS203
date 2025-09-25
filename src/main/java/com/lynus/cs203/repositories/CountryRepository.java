package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.Country;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CountryRepository extends JpaRepository<Country, Long> {
    Optional<Country> findByCountryCode(Long countryCode);
    List<Country> findByNameContainingIgnoreCase(String name);

}