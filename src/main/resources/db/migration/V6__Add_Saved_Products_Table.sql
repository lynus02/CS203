CREATE TABLE saved_products (
                                id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,

    -- fk → users.user_id (varchar)
                                user_id VARCHAR(255) NOT NULL,

    -- fk → product.product_id
                                product_id BIGINT NOT NULL,

    -- user-chosen name for saved config
                                config_name VARCHAR(255) NOT NULL,

    -- from frontend
                                product_value DOUBLE NOT NULL,

    -- fk → country.country_id
                                origin_country_id BIGINT NOT NULL,
                                destination_country_id BIGINT NOT NULL,

                                import_date DATETIME NOT NULL,
                                saved_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                PRIMARY KEY (id),

    -- indexes
                                KEY idx_saved_user (user_id),
                                KEY idx_saved_product (product_id),
                                KEY idx_saved_origin_country (origin_country_id),
                                KEY idx_saved_destination_country (destination_country_id),

    -- foreign keys
                                CONSTRAINT fk_saved_user
                                    FOREIGN KEY (user_id)
                                        REFERENCES users(user_id)
                                        ON DELETE CASCADE,

                                CONSTRAINT fk_saved_product
                                    FOREIGN KEY (product_id)
                                        REFERENCES product(product_id),

                                CONSTRAINT fk_saved_origin_country
                                    FOREIGN KEY (origin_country_id)
                                        REFERENCES country(country_id),

                                CONSTRAINT fk_saved_destination_country
                                    FOREIGN KEY (destination_country_id)
                                        REFERENCES country(country_id)
);
