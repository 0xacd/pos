CREATE TABLE payment_methods
(
    id                   SERIAL PRIMARY KEY,
    name                 TEXT UNIQUE             NOT NULL,
    min_modifier         NUMERIC(5, 2),
    max_modifier         NUMERIC(5, 2),
    point_rate           NUMERIC(5, 4),
    requires_last4       BOOLEAN   DEFAULT FALSE,
    requires_bank_info   BOOLEAN   DEFAULT FALSE,
    requires_cheque_info BOOLEAN   DEFAULT FALSE,
    requires_courier     BOOLEAN   DEFAULT FALSE,
    created_at           TIMESTAMP DEFAULT now() NOT NULL,
    updated_at           TIMESTAMP DEFAULT now() NOT NULL
);



CREATE TABLE payments
(
    id                VARCHAR(40) PRIMARY KEY,
    customer_id       VARCHAR(10),
    payment_method_id INTEGER REFERENCES payment_methods (id),
    original_price    NUMERIC(10, 2) NOT NULL,
    price_modifier    NUMERIC(5, 2)  NOT NULL,
    final_price       NUMERIC(10, 2) NOT NULL,
    points            NUMERIC(10, 0) NOT NULL,
    datetime          TIMESTAMP      NOT NULL,
    info              TEXT           NULL,
    created_at        TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_payments_datetime ON payments (datetime);


CREATE TABLE courier_payment_map
(
    id                SERIAL PRIMARY KEY,
    courier           VARCHAR(10),
    payment_method_id INTEGER REFERENCES payment_methods (id),
    created_at        TIMESTAMP DEFAULT now()
);
CREATE INDEX idx_courier_payment_payment_method_id ON courier_payment_map (payment_method_id);


