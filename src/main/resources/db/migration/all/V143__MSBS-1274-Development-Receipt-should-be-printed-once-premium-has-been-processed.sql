CREATE TABLE print_job (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    creation_timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    printer_id VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    completed BOOLEAN NOT NULL DEFAULT FALSE,
    completed_timestamp TIMESTAMP
);
