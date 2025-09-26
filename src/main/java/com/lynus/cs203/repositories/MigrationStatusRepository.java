package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.MigrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MigrationStatusRepository extends JpaRepository<MigrationStatus, String> {
}
