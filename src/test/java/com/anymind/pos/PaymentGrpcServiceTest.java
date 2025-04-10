package com.anymind.pos;

import com.anymind.pos.grpc.*;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.anymind.pos.config.TestRedisConfiguration;
import com.anymind.pos.entity.Payments;
import com.anymind.pos.service.CourierPaymentMapService;
import com.anymind.pos.service.PaymentMethodsService;
import com.anymind.pos.service.PaymentsService;
import com.anymind.pos.utils.DateUtil;
import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import io.grpc.testing.GrpcCleanupRule;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.apache.http.client.utils.DateUtils.parseDate;
import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@Import(TestRedisConfiguration.class)
@ActiveProfiles("junit")
public class PaymentGrpcServiceTest {

    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    @Resource
    private PaymentMethodsService paymentMethodsService;

    @Resource
    private PaymentsService paymentsService;

    @Resource
    private CourierPaymentMapService courierPaymentMapService;

    @Autowired
    private ApplicationContext applicationContext;

    @GrpcClient("inProcess")
    private PaymentServiceGrpc.PaymentServiceBlockingStub blockingStub;

    @GrpcClient("inProcess")
    private PaymentServiceGrpc.PaymentServiceStub asyncStub;

    @BeforeEach
    public void setup() throws IOException {
        String serverName = InProcessServerBuilder.generateName();
        PaymentGrpcService paymentGrpcService = applicationContext.getBean(PaymentGrpcService.class);

        grpcCleanup.register(
                InProcessServerBuilder.forName(serverName)
                        .directExecutor()
                        .addService(paymentGrpcService)
                        .build()
                        .start()
        );

        ManagedChannel channel = grpcCleanup.register(
                InProcessChannelBuilder.forName(serverName)
                        .directExecutor()
                        .build()
        );

        blockingStub = PaymentServiceGrpc.newBlockingStub(channel);
        asyncStub = PaymentServiceGrpc.newStub(channel);
    }

    @Test
    public void testMakePaymentSuccess() {
        Map<String, String> additionalItems = new HashMap<>();
        additionalItems.put("last4", "1234");

        PaymentRequest request = PaymentRequest.newBuilder()
                .setCustomerId("12345")
                .setPrice(100.00)
                .setPriceModifier(0.95)
                .setPaymentMethod("MASTERCARD")
                .setDatetime("2025-04-10T10:00:00Z")
                .putAllAdditionalItem(additionalItems)
                .build();

        PaymentResponse response = blockingStub.makePayment(request);

        assertNotNull(response);
        assertEquals(95.00, response.getFinalPrice(), 0.00);
        assertEquals(3, response.getPoints(), 0.00);

        Payments payment = paymentsService.getOne(new LambdaUpdateWrapper<Payments>().eq(Payments::getCustomerId, "12345"));
        assertNotNull(payment);
        assertEquals("12345", payment.getCustomerId());
        assertEquals("1234", payment.getInfo().get("last4"));
        assertEquals(0, new BigDecimal("95.00").compareTo(payment.getFinalPrice()));
    }

    @Test
    public void testMakePaymentFail() {
        Map<String, String> additionalItems = new HashMap<>();
        additionalItems.put("hellohello", "1234");
        additionalItems.put("courier", "PAPAPA");

        PaymentRequest request = PaymentRequest.newBuilder()
                .setCustomerId("12345")
                .setPrice(100.00)
                .setPriceModifier(1.3)
                .setPaymentMethod("CASH_ON_DELIVERY")
                .setDatetime("2025-04-10T10:00:00Z")
                .putAllAdditionalItem(additionalItems)
                .build();

        // Expect the gRPC call to throw a StatusRuntimeException
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class, () -> {
            blockingStub.makePayment(request);
        });


        // Assert the gRPC status
        assertEquals(Status.UNKNOWN.getCode(), exception.getStatus().getCode(),
                "Expected UNKNOWN status");
    }

    @Test
    public void testGetSales() throws InterruptedException {
        Payments p1 = new Payments();
        p1.setId("sale-test-1");
        p1.setCustomerId("cust456");
        p1.setPaymentMethodId(1); // CASH
        p1.setOriginalPrice(new BigDecimal("200.00"));
        p1.setPriceModifier(new BigDecimal("1.0"));
        p1.setFinalPrice(new BigDecimal("200.00"));
        p1.setPoints(new BigDecimal("100"));
        p1.setDatetime(DateUtil.parseDateTime("2025-04-10T12:30:00Z"));
        paymentsService.save(p1);

        Payments p2 = new Payments();
        p2.setId("sale-test-2");
        p2.setCustomerId("cust458");
        p2.setPaymentMethodId(1); // CASH
        p2.setOriginalPrice(new BigDecimal("100.00"));
        p2.setPriceModifier(new BigDecimal("1.0"));
        p2.setFinalPrice(new BigDecimal("200.00"));
        p2.setPoints(new BigDecimal("100"));
        p2.setDatetime(DateUtil.parseDateTime("2025-04-10T14:30:00Z"));
        paymentsService.save(p2);

        SalesRequest request = SalesRequest.newBuilder()
                .setStartDateTime("2025-04-10T00:00:00Z")
                .setEndDateTime("2025-04-10T23:59:59Z")
                .build();

        CountDownLatch latch = new CountDownLatch(1);
        List<HourlySale> salesList = Collections.synchronizedList(new ArrayList<>());

        asyncStub.getSales(request, new StreamObserver<HourlySale>() {
            @Override
            public void onNext(HourlySale value) {
                salesList.add(value);
            }

            @Override
            public void onError(Throwable t) {
                fail("Unexpected error in getSales: " + t.getMessage());
                latch.countDown();
            }

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });


        boolean completed = latch.await(5, TimeUnit.SECONDS);
        assertTrue(completed, "getSales did not complete within 5 seconds");

        // Assert: Verify two hourly sales
        assertFalse(salesList.isEmpty(), "Sales list should not be empty");
        assertEquals(2, salesList.size(), "Expected two hourly sales for 12:00 and 14:00 buckets");

        // Sort by datetime for consistent order
        List<HourlySale> sortedSales = salesList.stream()
                .sorted(Comparator.comparing(HourlySale::getDatetime))
                .toList();

        // Check 12:00 bucket
        HourlySale sale12 = sortedSales.getFirst();
        assertEquals("2025-04-10T12:00:00Z", sale12.getDatetime(), "First bucket should be 12:00");
        assertEquals(200.00, sale12.getSales(), 0.01, "12:00 sales should match p1 final price");
        assertEquals(100, sale12.getPoints(), 0.01, "12:00 points should match p1");

        // Check 14:00 bucket
        HourlySale sale14 = sortedSales.get(1);
        assertEquals("2025-04-10T14:00:00Z", sale14.getDatetime(), "Second bucket should be 14:00");
        assertEquals(200.00, sale14.getSales(), 0.01, "14:00 sales should match p2 final price");
        assertEquals(100, sale14.getPoints(), 0.01, "14:00 points should match p2");
    }
}