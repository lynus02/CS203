CREATE TABLE users (
                        user_id VARCHAR(255) PRIMARY KEY DEFAULT (UUID()),
                        email VARCHAR(255) UNIQUE NOT NULL,
                        password_hash VARCHAR(60) NOT NULL,
                        is_active TINYINT(1) NOT NULL DEFAULT 1,
                        created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_profiles (
                               user_id VARCHAR(255) PRIMARY KEY,
                               first_name VARCHAR(100) NOT NULL,
                               last_name VARCHAR(100) NOT NULL,
                               avatar_url TEXT NULL,
                               FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

CREATE TABLE roles (
                       role_id TINYINT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
                       name VARCHAR(50) UNIQUE NOT NULL,
                       description TEXT NULL
);

CREATE TABLE user_roles (
                           user_id VARCHAR(255) NOT NULL,
                           role_id TINYINT UNSIGNED NOT NULL,
                           assigned_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           PRIMARY KEY (user_id, role_id),
                           FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
                           FOREIGN KEY (role_id) REFERENCES roles(role_id) ON DELETE CASCADE
);

INSERT INTO roles (name, description) VALUES
('USER', 'Regular user with limited access'),
('ADMIN', 'Administrator with full access');

