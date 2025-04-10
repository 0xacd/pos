-- Create payment_methods table
CREATE TABLE payment_methods (
                                 id INTEGER PRIMARY KEY AUTO_INCREMENT,
                                 name VARCHAR UNIQUE NOT NULL,
                                 min_modifier NUMERIC(5, 2),
                                 max_modifier NUMERIC(5, 2),
                                 point_rate NUMERIC(5, 4),
                                 requires_last4 BOOLEAN DEFAULT FALSE,
                                 requires_bank_info BOOLEAN DEFAULT FALSE,
                                 requires_cheque_info BOOLEAN DEFAULT FALSE,
                                 requires_courier BOOLEAN DEFAULT FALSE,
                                 created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                                 updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Create payments table
CREATE TABLE payments (
                          id VARCHAR(40) PRIMARY KEY,
                          customer_id VARCHAR(10),
                          payment_method_id INTEGER REFERENCES payment_methods (id),
                          original_price NUMERIC(10, 2) NOT NULL,
                          price_modifier NUMERIC(5, 2) NOT NULL,
                          final_price NUMERIC(10, 2) NOT NULL,
                          points NUMERIC(10, 0) NOT NULL,
                          datetime TIMESTAMP NOT NULL,
                          info VARCHAR,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_payments_datetime ON payments (datetime);

-- Create courier_payment_map table
CREATE TABLE courier_payment_map (
                                     id INTEGER AUTO_INCREMENT PRIMARY KEY,
                                     courier VARCHAR(10),
                                     payment_method_id INTEGER REFERENCES payment_methods (id),
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_courier_payment_payment_method_id ON courier_payment_map (payment_method_id);

-- Insert payment methods
INSERT INTO payment_methods (
    name,
    min_modifier,
    max_modifier,
    point_rate,
    requires_last4,
    requires_bank_info,
    requires_cheque_info,
    requires_courier
) VALUES
      ('CASH', 0.90, 1.00, 0.0500, FALSE, FALSE, FALSE, FALSE),
      ('CASH_ON_DELIVERY', 1.00, 1.02, 0.0500, FALSE, FALSE, FALSE, TRUE),
      ('VISA', 0.95, 1.00, 0.0300, TRUE, FALSE, FALSE, FALSE),
      ('MASTERCARD', 0.95, 1.00, 0.0300, TRUE, FALSE, FALSE, FALSE),
      ('AMEX', 0.98, 1.01, 0.0200, TRUE, FALSE, FALSE, FALSE),
      ('JCB', 0.95, 1.00, 0.0500, TRUE, FALSE, FALSE, FALSE),
      ('LINE_PAY', 1.00, 1.00, 0.0100, FALSE, FALSE, FALSE, FALSE),
      ('PAYPAY', 1.00, 1.00, 0.0100, FALSE, FALSE, FALSE, FALSE),
      ('POINTS', 1.00, 1.00, 0.0000, FALSE, FALSE, FALSE, FALSE),
      ('GRAB_PAY', 1.00, 1.00, 0.0100, FALSE, FALSE, FALSE, FALSE),
      ('BANK_TRANSFER', 1.00, 1.00, 0.0000, FALSE, TRUE, FALSE, FALSE),
      ('CHEQUE', 0.90, 1.00, 0.0000, FALSE, FALSE, TRUE, FALSE);

-- Insert courier mappings for CASH_ON_DELIVERY
INSERT INTO courier_payment_map (courier, payment_method_id)
SELECT 'YAMATO', id FROM payment_methods WHERE name = 'CASH_ON_DELIVERY'
UNION
SELECT 'SAGAWA', id FROM payment_methods WHERE name = 'CASH_ON_DELIVERY';