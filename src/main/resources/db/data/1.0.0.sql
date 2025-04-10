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