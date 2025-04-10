package com.anymind.pos.validator;

import com.anymind.pos.config.GlobalExceptionHandler;
import com.anymind.pos.grpc.SalesRequest;
import com.anymind.pos.utils.DateUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockStatic;

public class SaleReqTest {

    private Date validStartDate;
    private Date validEndDate;

    @BeforeEach
    public void setUp() {
        // Sample valid dates for mocking
        validStartDate = new Date(1746595200000L); // Approx 2025-04-10T00:00:00Z
        validEndDate = new Date(1746681599000L);   // Approx 2025-04-10T23:59:59Z
    }

    @Test
    public void testValidSaleRequest() {
        // Arrange: Mock DateUtil to return valid dates
        try (MockedStatic<DateUtil> dateUtilMock = mockStatic(DateUtil.class)) {
            dateUtilMock.when(() -> DateUtil.parseDateTime("2025-04-10T00:00:00Z"))
                    .thenReturn(validStartDate);
            dateUtilMock.when(() -> DateUtil.parseDateTime("2025-04-10T23:59:59Z"))
                    .thenReturn(validEndDate);

            SalesRequest grpcRequest = SalesRequest.newBuilder()
                    .setStartDateTime("2025-04-10T00:00:00Z")
                    .setEndDateTime("2025-04-10T23:59:59Z")
                    .build();

            // Act
            SaleReq saleReq = new SaleReq(grpcRequest);

            // Assert
            assertEquals(validStartDate, saleReq.getStartDate(), "Start date should match parsed value");
            assertEquals(validEndDate, saleReq.getEndDate(), "End date should match parsed value");
        }
    }

    @Test
    public void testInvalidStartDateFormat() {
        // Arrange: Mock DateUtil to throw exception for start date
        try (MockedStatic<DateUtil> dateUtilMock = mockStatic(DateUtil.class)) {
            dateUtilMock.when(() -> DateUtil.parseDateTime("invalid-start-date"))
                    .thenThrow(new GlobalExceptionHandler.UserNoticeException("Invalid start date format"));
            dateUtilMock.when(() -> DateUtil.parseDateTime("2025-04-10T23:59:59Z"))
                    .thenReturn(validEndDate);

            SalesRequest grpcRequest = SalesRequest.newBuilder()
                    .setStartDateTime("invalid-start-date")
                    .setEndDateTime("2025-04-10T23:59:59Z")
                    .build();

            // Act & Assert
            GlobalExceptionHandler.UserNoticeException exception = assertThrows(
                    GlobalExceptionHandler.UserNoticeException.class,
                    () -> new SaleReq(grpcRequest),
                    "Should throw UserNoticeException for invalid start date"
            );
            assertEquals("Invalid start date format", exception.getMessage());
        }
    }

    @Test
    public void testInvalidEndDateFormat() {
        // Arrange: Mock DateUtil to throw exception for end date
        try (MockedStatic<DateUtil> dateUtilMock = mockStatic(DateUtil.class)) {
            dateUtilMock.when(() -> DateUtil.parseDateTime("2025-04-10T00:00:00Z"))
                    .thenReturn(validStartDate);
            dateUtilMock.when(() -> DateUtil.parseDateTime("invalid-end-date"))
                    .thenThrow(new GlobalExceptionHandler.UserNoticeException("Invalid end date format"));

            SalesRequest grpcRequest = SalesRequest.newBuilder()
                    .setStartDateTime("2025-04-10T00:00:00Z")
                    .setEndDateTime("invalid-end-date")
                    .build();

            // Act & Assert
            GlobalExceptionHandler.UserNoticeException exception = assertThrows(
                    GlobalExceptionHandler.UserNoticeException.class,
                    () -> new SaleReq(grpcRequest),
                    "Should throw UserNoticeException for invalid end date"
            );
            assertEquals("Invalid end date format", exception.getMessage());
        }
    }

    @Test
    public void testEndDateBeforeStartDate() {
        // Arrange: Mock DateUtil with dates where end is before start
        try (MockedStatic<DateUtil> dateUtilMock = mockStatic(DateUtil.class)) {
            dateUtilMock.when(() -> DateUtil.parseDateTime("2025-04-10T23:59:59Z"))
                    .thenReturn(validEndDate);   // Later date
            dateUtilMock.when(() -> DateUtil.parseDateTime("2025-04-10T00:00:00Z"))
                    .thenReturn(validStartDate); // Earlier date, but swapped in request

            SalesRequest grpcRequest = SalesRequest.newBuilder()
                    .setStartDateTime("2025-04-10T23:59:59Z") // Start is later
                    .setEndDateTime("2025-04-10T00:00:00Z")   // End is earlier
                    .build();

            // Act & Assert
            GlobalExceptionHandler.UserNoticeException exception = assertThrows(
                    GlobalExceptionHandler.UserNoticeException.class,
                    () -> new SaleReq(grpcRequest),
                    "Should throw UserNoticeException when end date is before start date"
            );
            assertEquals("End date must be after start date", exception.getMessage());
        }
    }

    @Test
    public void testSameStartAndEndDate() {
        // Arrange: Mock DateUtil with same start and end dates
        try (MockedStatic<DateUtil> dateUtilMock = mockStatic(DateUtil.class)) {
            dateUtilMock.when(() -> DateUtil.parseDateTime("2025-04-10T12:00:00Z"))
                    .thenReturn(validStartDate);

            SalesRequest grpcRequest = SalesRequest.newBuilder()
                    .setStartDateTime("2025-04-10T12:00:00Z")
                    .setEndDateTime("2025-04-10T12:00:00Z")
                    .build();

            // Act
            SaleReq saleReq = new SaleReq(grpcRequest);

            // Assert
            assertEquals(validStartDate, saleReq.getStartDate());
            assertEquals(validStartDate, saleReq.getEndDate());
        }
    }
}