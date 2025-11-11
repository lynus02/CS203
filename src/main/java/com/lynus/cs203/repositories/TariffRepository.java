package com.lynus.cs203.repositories;

import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface TariffRepository extends JpaRepository<Tariff, Long> {
    Optional<Tariff> findByProductAndCountry(Product product, Country country);

    Page<Tariff> findByCountry_CountryName(String countryName, Pageable pageable);

    @Query("""
        SELECT t FROM Tariff t
        WHERE t.country.countryName = :countryName
          AND t.product.productCode LIKE CONCAT('%', :productCode, '%')
    """)
    Page<Tariff> findByCountry_CountryNameAndProduct_ProductCodeContaining(
            @Param("countryName") String countryName,
            @Param("productCode") String productCode,
            Pageable pageable
    );

    Page<Tariff> findByCountry_CountryNameAndProduct_ProductDescriptionContainingIgnoreCase(
            String countryName, String productDescription, Pageable pageable);

    Page<Tariff> findByProduct_ProductDescriptionContainingIgnoreCaseOrProduct_ProductCode(
            String productDescription, String productCode, Pageable pageable);

    @Query("""
        SELECT t FROM Tariff t
        WHERE LOWER(t.product.productDescription) LIKE LOWER(CONCAT('%', :productDescription, '%'))
           OR t.product.productCode LIKE CONCAT('%', :productCode, '%')
    """)
    Page<Tariff> findByProductDescriptionOrProductCodeContaining(
            @Param("productDescription") String productDescription,
            @Param("productCode") String productCode,
            Pageable pageable
    );

    @Query("""
        SELECT t FROM Tariff t
        WHERE LOWER(t.product.productDescription) LIKE LOWER(CONCAT('%', :keyword, '%'))
    """)
    Page<Tariff> findSimilarProducts(@Param("keyword") String keyword, Pageable pageable);

//    @Query("""
//        SELECT t FROM Tariff t
//        WHERE t.product = :product
//          AND t.country = :country
//          AND t.effectiveDate <= :date
//          AND (t.expiryDate IS NULL OR t.expiryDate >= :date)
//        ORDER BY t.effectiveDate DESC
//    """)
//    Optional<Tariff> findApplicableTariff(
//            @Param("product") Product product,
//            @Param("country") Country country,
//            @Param("date") java.time.LocalDate date
//    );
}