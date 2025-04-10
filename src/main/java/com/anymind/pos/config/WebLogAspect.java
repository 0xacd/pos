package com.anymind.pos.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.grpc.*;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.interceptor.GrpcGlobalServerInterceptor;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;

@Slf4j
@Component
@GrpcGlobalServerInterceptor
public class WebLogAspect implements ServerInterceptor {

    private ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> call,
            Metadata headers,
            ServerCallHandler<ReqT, RespT> next) {
        long startTime = System.currentTimeMillis();
        GrpcLog grpcLog = new GrpcLog();
        grpcLog.setStartTime(startTime);

        String fullMethodName = call.getMethodDescriptor().getFullMethodName();
        grpcLog.setMethod(fullMethodName);

        InetSocketAddress clientAddress = (InetSocketAddress) call.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        grpcLog.setIp(clientAddress != null ? clientAddress.getAddress().getHostAddress() : "unknown");

        return new ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT>(next.startCall(
                new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
                    @Override
                    public void sendMessage(RespT message) {
                        grpcLog.setResult(safeToStr(message, "response"));
                        super.sendMessage(message);
                    }
                }, headers)) {

            @Override
            public void onMessage(ReqT request) {
                grpcLog.setParameter(safeToStr(request, "request"));
                log.info("gRPC Start {}", safeToStr(grpcLog, "GrpcLog"));
                super.onMessage(request);
            }

            @Override
            public void onComplete() {
                long endTime = System.currentTimeMillis();
                int spendTime = (int) (endTime - startTime);
                grpcLog.setSpendTime(spendTime);
                if (spendTime < 5 * 1000) {
                    log.info("gRPC END {}", safeToStr(grpcLog, "GrpcLog"));
                } else {
                    log.warn("gRPC END overtime {}", safeToStr(grpcLog, "GrpcLog"));
                }
                super.onComplete();
            }

            @Override
            public void onCancel() {
                log.warn("gRPC Call Cancelled: {}", fullMethodName);
                super.onCancel();
            }
        };
    }

    private String safeToStr(Object o, String context) {
        if (o == null) {
            return "null";
        }
        if (o instanceof Message) {
            try {
                String body = JsonFormat.printer()
                        .omittingInsignificantWhitespace()
                        .print((Message) o);
                if (body.length() > 1 * 1024) {
                    return body.substring(0, 1023) + ".....ignore....";
                }
                return body;
            } catch (Exception e) {
                log.error("Failed to serialize {} to JSON with Protobuf: {}", context, e.getMessage());
                return "ProtobufSerializationError: " + o.getClass().getSimpleName();
            }
        }
        try {
            String body = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(o)
                    .replace("\n", "")
                    .replace(" ", "");
            if (body.length() > 1 * 1024) {
                return body.substring(0, 1023) + ".....ignore....";
            }
            return body;
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} to JSON: {}", context, e.getMessage());
            return "SerializationError: " + o.getClass().getSimpleName();
        }
    }

    @Data
    public class GrpcLog {
        @JsonIgnore
        private Long startTime;
        private String method;
        private String ip;
        private Integer spendTime;
        private String parameter;
        private String result;
    }
}