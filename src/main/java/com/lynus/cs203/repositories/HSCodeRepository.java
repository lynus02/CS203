package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.HSCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HSCodeRepository extends JpaRepository<HSCode, Long> {
    List<HSCode> findByDescriptionContainingIgnoreCaseOrHsCodeContainingIgnoreCase(String description, String hsCode);
    Optional<HSCode> findByDescription(String description);
    Optional<HSCode> findByHsCode(String hsCode);
}