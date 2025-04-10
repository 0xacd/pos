package com.anymind.pos.validator;

import com.anymind.pos.config.GlobalExceptionHandler;
import com.anymind.pos.entity.PaymentMethods;
import com.anymind.pos.grpc.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
public class PaymentReqTest {

    private PaymentMethods paymentMethods;
    private List<String> validCouriers;

    @BeforeEach
    public void setUp() {
        // Mock PaymentMethods with default settings
        paymentMethods = mock(PaymentMethods.class);
        when(paymentMethods.getName()).thenReturn("CASH");
        when(paymentMethods.getMinModifier()).thenReturn(new BigDecimal("0.9"));
        when(paymentMethods.getMaxModifier()).thenReturn(new BigDecimal("1.02"));
        when(paymentMethods.getRequiresBankInfo()).thenReturn(false);
        when(paymentMethods.getRequiresChequeInfo()).thenReturn(false);
        when(paymentMethods.getRequiresCourier()).thenReturn(false);

        // Default valid couriers
        validCouriers = List.of("DHL", "UPS", "FEDEX");
    }

    @Test
    public void testValidPaymentRequest() {
        // Arrange: Valid PaymentRequest
        PaymentRequest grpcRequest = PaymentRequest.newBuilder()
                .setCustomerId("cust123")
                .setPrice(100.00)
                .setPriceModifier(1.0)
                .setPaymentMethod("CASH")
                .setDatetime("2025-04-10T10:00:00Z")
                .putAdditionalItem("note", "Test payment")
                .build();

        PaymentReq req = new PaymentReq(grpcRequest);

        // Act & Assert: Should not throw an exception
        assertDoesNotThrow(() -> PaymentReq.validate(req, paymentMethods, validCouriers));
    }

    @Test
    public void testCustomerIdBlank() {
        // Arrange: Customer ID is blank
        PaymentRequest grpcRequest = PaymentRequest.newBuilder()
                .setCustomerId("") // Blank
                .setPrice(100.00)
                .setPriceModifier(1.0)
                .setPaymentMethod("CASH")
                .setDatetime("2025-04-10T10:00:00Z")
                .build();

        PaymentReq req = new PaymentReq(grpcRequest);

        // Act & Assert
        GlobalExceptionHandler.UserNoticeException exception = assertThrows(
                GlobalExceptionHandler.UserNoticeException.class,
                () -> PaymentReq.validate(req, paymentMethods, validCouriers)
        );
        assertEquals("Validation failed: Customer ID cannot be blank", exception.getMessage());
    }

    @Test
    public void testPriceNull() {
        // Arrange: Price is 0 (converted to null)
        PaymentRequest grpcRequest = PaymentRequest.newBuilder()
                .setCustomerId("cust123")
                .setPrice(0.0) // Will be null in PaymentReq
                .setPriceModifier(1.0)
                .setPaymentMethod("CASH")
                .setDatetime("2025-04-10T10:00:00Z")
                .build();

        PaymentReq req = new PaymentReq(grpcRequest);

        // Act & Assert
        GlobalExceptionHandler.UserNoticeException exception = assertThrows(
                GlobalExceptionHandler.UserNoticeException.class,
                () -> PaymentReq.validate(req, paymentMethods, validCouriers)
        );
        assertEquals("Validation failed: Price cannot be null", exception.getMessage());
    }

    @Test
    public void testPriceModifierExceedsMax() {
        // Arrange: Price modifier exceeds max (1.02)
        PaymentRequest grpcRequest = PaymentRequest.newBuilder()
                .setCustomerId("cust123")
                .setPrice(100.00)
                .setPriceModifier(1.5) // Exceeds 1.02
                .setPaymentMethod("CASH")
                .setDatetime("2025-04-10T10:00:00Z")
                .build();

        PaymentReq req = new PaymentReq(grpcRequest);

        // Act & Assert
        GlobalExceptionHandler.UserNoticeException exception = assertThrows(
                GlobalExceptionHandler.UserNoticeException.class,
                () -> PaymentReq.validate(req, paymentMethods, validCouriers)
        );
        assertEquals("Validation failed: Price modifier must not exceed 1.02", exception.getMessage());
    }

    @Test
    public void testInvalidPaymentMethod() {
        // Arrange: Invalid payment method (null PaymentMethods)
        PaymentRequest grpcRequest = PaymentRequest.newBuilder()
                .setCustomerId("cust123")
                .setPrice(100.00)
                .setPriceModifier(1.0)
                .setPaymentMethod("INVALID_METHOD")
                .setDatetime("2025-04-10T10:00:00Z")
                .build();

        PaymentReq req = new PaymentReq(grpcRequest);

        // Act & Assert
        GlobalExceptionHandler.UserNoticeException exception = assertThrows(
                GlobalExceptionHandler.UserNoticeException.class,
                () -> PaymentReq.validate(req, null, validCouriers) // Null PaymentMethods
        );
        assertEquals("Validation failed: Invalid payment method: INVALID_METHOD", exception.getMessage());
    }

    @Test
    public void testInvalidDatetime() {
        // Arrange: Invalid datetime format
        PaymentRequest grpcRequest = PaymentRequest.newBuilder()
                .setCustomerId("cust123")
                .setPrice(100.00)
                .setPriceModifier(1.0)
                .setPaymentMethod("CASH")
                .setDatetime("invalid-date") // Invalid format
                .build();

        PaymentReq req = new PaymentReq(grpcRequest);

        // Act & Assert
        GlobalExceptionHandler.UserNoticeException exception = assertThrows(
                GlobalExceptionHandler.UserNoticeException.class,
                () -> PaymentReq.validate(req, paymentMethods, validCouriers)
        );
        assertTrue(exception.getMessage().startsWith("Validation failed: "), "Should contain validation prefix");
        assertTrue(exception.getMessage().contains("Invalid datetime format"),
                "Should contain parsing error");
    }

    @Test
    public void testMissingBankInfo() {
        // Arrange: Payment method requires bank info
        when(paymentMethods.getRequiresBankInfo()).thenReturn(true);

        PaymentRequest grpcRequest = PaymentRequest.newBuilder()
                .setCustomerId("cust123")
                .setPrice(100.00)
                .setPriceModifier(1.0)
                .setPaymentMethod("CASH")
                .setDatetime("2025-04-10T10:00:00Z")
                .build(); // No bank info provided

        PaymentReq req = new PaymentReq(grpcRequest);

        // Act & Assert
        GlobalExceptionHandler.UserNoticeException exception = assertThrows(
                GlobalExceptionHandler.UserNoticeException.class,
                () -> PaymentReq.validate(req, paymentMethods, validCouriers)
        );
        assertEquals("Validation failed: Payment method CASH requires bank and account number",
                exception.getMessage());
    }

    @Test
    public void testInvalidCourier() {
        // Arrange: Payment method requires courier, invalid courier provided
        when(paymentMethods.getRequiresCourier()).thenReturn(true);

        PaymentRequest grpcRequest = PaymentRequest.newBuilder()
                .setCustomerId("cust123")
                .setPrice(100.00)
                .setPriceModifier(1.0)
                .setPaymentMethod("CASH")
                .setDatetime("2025-04-10T10:00:00Z")
                .putAdditionalItem("courier", "INVALID_COURIER")
                .build();

        PaymentReq req = new PaymentReq(grpcRequest);

        // Act & Assert
        GlobalExceptionHandler.UserNoticeException exception = assertThrows(
                GlobalExceptionHandler.UserNoticeException.class,
                () -> PaymentReq.validate(req, paymentMethods, validCouriers)
        );
        assertEquals("Validation failed: Invalid courier: INVALID_COURIER", exception.getMessage());
    }

    @Test
    public void testMultipleValidationErrors() {
        // Arrange: Multiple invalid fields
        PaymentRequest grpcRequest = PaymentRequest.newBuilder()
                .setCustomerId("") // Blank
                .setPrice(-1.0) // Negative
                .setPriceModifier(2.0) // Exceeds max
                .setPaymentMethod("CASH")
                .setDatetime("invalid-date") // Invalid format
                .build();

        PaymentReq req = new PaymentReq(grpcRequest);

        // Act & Assert
        GlobalExceptionHandler.UserNoticeException exception = assertThrows(
                GlobalExceptionHandler.UserNoticeException.class,
                () -> PaymentReq.validate(req, paymentMethods, validCouriers)
        );
        String message = exception.getMessage();
        log.info(message);
        assertTrue(message.contains("Customer ID cannot be blank"));
        assertTrue(message.contains("Price cannot be null"));
        assertTrue(message.contains("Price modifier must not exceed 1.02"));
        assertTrue(message.contains("Invalid datetime format"));
    }
}