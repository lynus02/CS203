package com.lynus.cs203.integration.services;

import com.lynus.cs203.entities.Country;
import com.lynus.cs203.entities.Product;
import com.lynus.cs203.entities.Tariff;
import com.lynus.cs203.repositories.CountryRepository;
import com.lynus.cs203.repositories.ProductRepository;
import com.lynus.cs203.repositories.TariffRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
properties = {"spring.flyway.enabled=false"})
@AutoConfigureMockMvc
//@TestPropertySource(properties = {
//        "spring.datasource.url=jdbc:mysql://localhost:3306/tariff",
//        "spring.datasource.username=root",
//        "spring.datasource.password=password123"
//})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class TariffControllerIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CountryRepository countryRepository;

    @Autowired
    private TariffRepository tariffRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        tariffRepository.deleteAll();
        productRepository.deleteAll();
        countryRepository.deleteAll();
    }

//    @Test
//    void calculateEndpoint_givenValidRequest_whenPost_thenReturnsTariffAmount() {
//        // Arrange
//        Country country = new Country();
//        country.setCountryCode("C840");
//        country.setCountryName("United States");
//        country = countryRepository.save(country);
//
//        Product product = new Product();
//        product.setProductCode(5005);
//        product.setProductDescription("Test Item");
//        product = productRepository.save(product);
//
//        Tariff tariff = new Tariff();
//        tariff.setCountry(country);
//        tariff.setProduct(product);
//        tariff.setTariffRate(2.5); // 2.5%
//        tariffRepository.save(tariff);
//
//        Map<String, Object> req = Map.of(
//                "productCode", 5005,
//                "countryCode", "US",
//                "customsValue", 200.0
//        );
//
//        // Act / Assert (BDD)
//        given()
//                .contentType("application/json")
//                .body(req)
//                .when()
//                .post("/tariffs/calculate")
//                .then()
//                .statusCode(200)
//                .body("productCode", equalTo(5005))
//                .body("countryCode", equalTo("US"))
//                .body("tariffAmount", equalTo(5.0f)); // 2.5% of 200 = 5.0
//    }

    @Test
    void suggestEndpoint_givenProductsAndCountry_whenGet_thenReturnsSuggestions() {
        // Arrange
        Country country = new Country();
        country.setCountryCode("US");
        country.setCountryName("United States");
        country = countryRepository.save(country);

        Product p1 = new Product();
        p1.setProductCode(1001);
        p1.setProductDescription("Electric Motor");
        productRepository.save(p1);

        Product p2 = new Product();
        p2.setProductCode(2002);
        p2.setProductDescription("Motor Oil");
        productRepository.save(p2);

        Tariff t1 = new Tariff();
        t1.setCountry(country);
        t1.setProduct(p1);
        t1.setTariffRate(5.0);
        tariffRepository.save(t1);

        Tariff t2 = new Tariff();
        t2.setCountry(country);
        t2.setProduct(p2);
        t2.setTariffRate(10.0);
        tariffRepository.save(t2);

        // Act / Assert (BDD)
        given()
                .queryParam("q", "motor")
                .queryParam("country", "United States")
                .queryParam("page", 0)
                .queryParam("size", 10)
                .when()
                .get("/tariffs/suggest")
                .then()
                .statusCode(200)
                .body("content.size()", greaterThanOrEqualTo(2))
                // reporterName matches the requested country
                .body("content.reporterName", hasItems("United States"))
                // ensure descriptions are present in the returned DTOs
                .body("content.hsDescription", hasItems(notNullValue(), notNullValue()));
    }
}
