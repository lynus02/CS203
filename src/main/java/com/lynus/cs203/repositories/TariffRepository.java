package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.Tariff;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TariffRepository extends JpaRepository<Tariff, Long> {

}