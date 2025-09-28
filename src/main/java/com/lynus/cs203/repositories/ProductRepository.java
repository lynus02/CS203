package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByProductCode(Integer productCode);

    @Query("""
        SELECT p FROM Product p
        WHERE LOWER(p.productDescription) LIKE LOWER(CONCAT('%', :q, '%'))
           OR CAST(p.productCode AS string) LIKE CONCAT('%', :q, '%')
    """)
    List<Product> findByDescriptionOrCode(@Param("q") String q);

}
