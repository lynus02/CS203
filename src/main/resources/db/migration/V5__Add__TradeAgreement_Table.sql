CREATE TABLE trade_agreement (
    agreement_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agreement_name VARCHAR(500) NOT NULL,
    agreement_type VARCHAR(100),
    status VARCHAR(50) DEFAULT 'In Force'
    );

CREATE TABLE agreement_country (
    agreement_country_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agreement_id BIGINT,
    country_name CHAR(255),
    CONSTRAINT fk_agreement_country_agreement FOREIGN KEY (agreement_id) REFERENCES trade_agreement(agreement_id)
    );
