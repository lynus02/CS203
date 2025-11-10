CREATE TABLE trade_agreement (
    agreement_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agreement_name VARCHAR(500) NOT NULL,
    agreement_type VARCHAR(100),
    effective_date DATE,
    expiration_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    );

CREATE TABLE agreement_country (
    agreement_country_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agreement_id BIGINT,
    country_id BIGINT,
    CONSTRAINT fk_agreement_country_agreement FOREIGN KEY (agreement_id) REFERENCES trade_agreement(agreement_id),
    CONSTRAINT fk_agreement_country_country FOREIGN KEY (country_id) REFERENCES country(country_id)
);
