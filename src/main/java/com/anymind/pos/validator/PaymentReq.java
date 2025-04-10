package com.anymind.pos.validator;

import com.anymind.pos.config.GlobalExceptionHandler;
import com.anymind.pos.entity.PaymentMethods;
import com.anymind.pos.grpc.PaymentRequest;
import com.anymind.pos.utils.DateUtil;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Data
public class PaymentReq {
    private String customerId;
    private BigDecimal price;
    private BigDecimal priceModifier;
    private String paymentMethod;
    private String datetime;
    private Map<String, String> additionalItem;

    public PaymentReq(PaymentRequest grpcRequest) {
        this.customerId = grpcRequest.getCustomerId();
        this.price = grpcRequest.getPrice() > 0
                ? BigDecimal.valueOf(grpcRequest.getPrice())
                : null;
        this.priceModifier = grpcRequest.getPriceModifier() > 0
                ? BigDecimal.valueOf(grpcRequest.getPriceModifier())
                : null;
        this.paymentMethod = grpcRequest.getPaymentMethod();

        this.datetime = grpcRequest.getDatetime();
        this.additionalItem = grpcRequest.getAdditionalItemMap().isEmpty()
                ? Collections.emptyMap()
                : grpcRequest.getAdditionalItemMap();
    }


    public static void validate(PaymentReq req, PaymentMethods paymentMethods, List<String> validCouriers) {
        ValidationErrors errors = new ValidationErrors();

        validateCustomerId(req.getCustomerId(), errors);
        validatePrice(req.getPrice(), errors);
        validatePriceModifier(req, paymentMethods, errors);
        validatePaymentMethod(req.getPaymentMethod(), paymentMethods, errors);
        validateDatetime(req.getDatetime(), errors);
        validateAdditionalItems(req, paymentMethods, validCouriers, errors);

        errors.throwIfNotEmpty();
    }

    private static void validateCustomerId(String customerId, ValidationErrors errors) {
        if (customerId == null || customerId.trim().isEmpty()) {
            errors.add("Customer ID cannot be blank");
        } else if (customerId.length() > 10) {
            errors.add("Customer ID must not exceed 10 characters");
        }
    }

    private static void validatePrice(BigDecimal price, ValidationErrors errors) {
        if (price == null) {
            errors.add("Price cannot be null");
            return;
        }
        if (price.compareTo(BigDecimal.valueOf(0.01)) < 0) {
            errors.add("Price must be greater than 0");
        }
        if (price.compareTo(BigDecimal.valueOf(99999999.99)) > 0) {
            errors.add("Price must not exceed 99999999.99");
        }
    }

    private static void validatePriceModifier(PaymentReq req, PaymentMethods paymentMethods,
                                              ValidationErrors errors) {
        if (paymentMethods == null) {
            return; // Payment method validation will handle this case
        }

        if (req.getPriceModifier() == null) {
            errors.add("Price modifier cannot be null");
            return;
        }

        if (req.getPriceModifier().compareTo(paymentMethods.getMaxModifier()) > 0) {
            errors.add("Price modifier must not exceed " + paymentMethods.getMaxModifier());
        }
        if (req.getPriceModifier().compareTo(paymentMethods.getMinModifier()) < 0) {
            errors.add("Price modifier must be greater than or equal to " +
                    paymentMethods.getMinModifier());
        }
    }

    private static void validatePaymentMethod(String paymentMethod, PaymentMethods paymentMethods,
                                              ValidationErrors errors) {
        if (paymentMethod == null || paymentMethod.trim().isEmpty()) {
            errors.add("Payment method cannot be blank");
        } else if (paymentMethods == null) {
            errors.add("Invalid payment method: " + paymentMethod);
        }
    }

    private static void validateDatetime(String datetime, ValidationErrors errors) {
        if (datetime == null) {
            errors.add("Datetime cannot be null");
        }
        try {
            DateUtil.parseDateTime(datetime);
        } catch (Exception e) {
            errors.add(e.getMessage());
        }
    }


    private static void validateAdditionalItems(PaymentReq req, PaymentMethods paymentMethods,
                                                List<String> validCouriers, ValidationErrors errors) {
        if (paymentMethods == null) {
            return; // Skip if payment method is invalid
        }

        Map<String, String> items = req.getAdditionalItem();

        if (paymentMethods.getRequiresBankInfo()) {
            if (!items.containsKey("account") || !items.containsKey("bank")) {
                errors.add("Payment method " + paymentMethods.getName() +
                        " requires bank and account number");
            }
        }

        if (paymentMethods.getRequiresChequeInfo()) {
            if (!items.containsKey("cheque") || !items.containsKey("bank")) {
                errors.add("Payment method " + paymentMethods.getName() +
                        " requires bank and cheque number");
            }
        }

        if (paymentMethods.getRequiresCourier()) {
            String courier = items.get("courier");
            if (courier == null || !validCouriers.contains(courier)) {
                errors.add("Invalid courier: " + courier);
            }
        }
    }

    // Helper class to collect validation errors
    static class ValidationErrors {
        private final List<String> errors = new ArrayList<>();

        public void add(String error) {
            errors.add(error);
        }

        public void throwIfNotEmpty() {
            if (!errors.isEmpty()) {
                String errorMessage = "Validation failed: " + String.join("; ", errors);
                throw new GlobalExceptionHandler.UserNoticeException(errorMessage);
            }
        }
    }
}

