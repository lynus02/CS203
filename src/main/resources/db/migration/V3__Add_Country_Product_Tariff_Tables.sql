CREATE TABLE country (
                       country_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                       country_code VARCHAR(255) UNIQUE,
                       country_name VARCHAR(255)
);

CREATE TABLE product (
                         product_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                         product_code VARCHAR(255) UNIQUE,
                         hs_description VARCHAR(255),
                         hs_uom VARCHAR(255),
                         food_category VARCHAR(255)
);

CREATE TABLE tariff (
                        trade_id BIGINT PRIMARY KEY AUTO_INCREMENT,
                        product_id BIGINT,
                        country_id BIGINT,
                        tariff_rate DOUBLE,
                        effective_date DATE NOT NULL DEFAULT '2020-01-01',
                        expiry_date DATE DEFAULT NULL,
                        CONSTRAINT fk_tariff_product FOREIGN KEY (product_id) REFERENCES product(product_id),
                        CONSTRAINT fk_tariff_country FOREIGN KEY (country_id) REFERENCES country(country_id)
);