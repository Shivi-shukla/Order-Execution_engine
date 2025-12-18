CREATE DATABASE IF NOT EXISTS orderengine CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE orderengine;

CREATE TABLE IF NOT EXISTS orders (
    id VARCHAR(255) AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(36) UNIQUE NOT NULL,
    token_in VARCHAR(44) NOT NULL,
    token_out VARCHAR(44) NOT NULL,
    amount_in DECIMAL(38,8) NOT NULL,
    status VARCHAR(20) NOT NULL,
    tx_hash VARCHAR(100),
    error TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_orders_status (status),
    INDEX idx_orders_order_id (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS order_history (
    id VARCHAR(255) AUTO_INCREMENT PRIMARY KEY,
    order_id VARCHAR(36) NOT NULL,
    error_reason TEXT,
    failed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
