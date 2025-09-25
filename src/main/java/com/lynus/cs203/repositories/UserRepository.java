package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.User;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = "profile")
    List<User> findAll(Sort sort);

    @EntityGraph(attributePaths = "profile")
    Optional<User> findById(String id);

    @EntityGraph(attributePaths = "profile")
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.userProfile WHERE u.email = :email")
    Optional<User> findByEmailWithProfile(@Param("email") String email);

}
