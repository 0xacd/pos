package com.anymind.pos.config;

import io.grpc.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.advice.GrpcAdvice;
import net.devh.boot.grpc.server.advice.GrpcExceptionHandler;
import org.springframework.context.annotation.Configuration;

@Slf4j
@GrpcAdvice
@Configuration
public class GlobalExceptionHandler {

    // Response object to mimic your original MSG structure
    @Data
    @AllArgsConstructor
    public static class MSG {
        int code;
        String message;

        public static MSG error400(String message) {
            return new MSG(400, message);
        }

        public static MSG error500(String message) {
            return new MSG(500, message);
        }
    }

    // Custom exception class (unchanged)
    public static class UserNoticeException extends IllegalStateException {
        public UserNoticeException(String s) {
            super(s);
        }
    }

    // Handle UserNoticeException and map it to gRPC Status.INVALID_ARGUMENT (equivalent to HTTP 400)
    @GrpcExceptionHandler(UserNoticeException.class)
    public StatusException handleUserNoticeException(UserNoticeException e) {
        log.error("gRPC error: {}", e.getMessage());
        MSG errorResponse = MSG.error400(e.getMessage());

        // Attach metadata with custom error details
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("code", Metadata.ASCII_STRING_MARSHALLER), String.valueOf(errorResponse.getCode()));
        metadata.put(Metadata.Key.of("message", Metadata.ASCII_STRING_MARSHALLER), errorResponse.getMessage());

        return Status.INVALID_ARGUMENT
                .withDescription(e.getMessage())
                .withCause(e)
                .asException(metadata);
    }

    // Handle generic exceptions and map to gRPC Status.INTERNAL (equivalent to HTTP 500)
    @GrpcExceptionHandler(Exception.class)
    public StatusException handleGenericException(Exception e) {
        log.error("gRPC internal error: {}", e.getMessage());
        MSG errorResponse = MSG.error500("Internal server error");

        // Attach metadata with custom error details
        Metadata metadata = new Metadata();
        metadata.put(Metadata.Key.of("code", Metadata.ASCII_STRING_MARSHALLER), String.valueOf(errorResponse.getCode()));
        metadata.put(Metadata.Key.of("message", Metadata.ASCII_STRING_MARSHALLER), errorResponse.getMessage());

        return Status.INTERNAL
                .withDescription("Internal server error")
                .withCause(e)
                .asException(metadata);
    }
}