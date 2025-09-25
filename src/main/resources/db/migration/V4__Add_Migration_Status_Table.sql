CREATE TABLE migration_status (
                  migration_name VARCHAR(255) PRIMARY KEY,
                  completed BOOLEAN NOT NULL DEFAULT FALSE,
                  completed_at TIMESTAMP
);