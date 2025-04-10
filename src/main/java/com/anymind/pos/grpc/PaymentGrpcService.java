package com.anymind.pos.grpc;

import com.anymind.pos.dto.HourlySaleSummary;
import com.anymind.pos.entity.PaymentMethods;
import com.anymind.pos.entity.Payments;
import com.anymind.pos.service.CourierPaymentMapService;
import com.anymind.pos.service.PaymentMethodsService;
import com.anymind.pos.service.PaymentsService;
import com.anymind.pos.validator.PaymentReq;
import com.anymind.pos.validator.SaleReq;
import io.grpc.stub.StreamObserver;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;

@GrpcService
@Slf4j
public class PaymentGrpcService extends PaymentServiceGrpc.PaymentServiceImplBase {
    @Resource
    private CourierPaymentMapService courierPaymentMapService;
    @Resource
    private PaymentMethodsService paymentMethodsService;
    @Resource
    private PaymentsService paymentsService;

    @Override
    public void makePayment(PaymentRequest request, StreamObserver<PaymentResponse> responseObserver) {
        PaymentReq req = new PaymentReq(request);
        PaymentMethods paymentMethods = paymentMethodsService.getPaymentMethodByName(req.getPaymentMethod());
        List<String> courierList = courierPaymentMapService.getCouriersByPaymentMethodId(paymentMethods.getId());
        PaymentReq.validate(req, paymentMethods, courierList);

        // solution 1 QPS < 50-100
        Payments payments = Payments.build(req, paymentMethods);
        paymentsService.save(payments);

        // solution 2 QPS 100-1000
        //@Async or CompletableFuture

        // solution 3 QPS > 1000
        // message queue

        PaymentResponse response = PaymentResponse.newBuilder()
                .setFinalPrice(payments.getFinalPrice().doubleValue())
                .setPoints(payments.getPoints().doubleValue())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void getSales(SalesRequest request, StreamObserver<HourlySale> responseObserver) {

        SaleReq req = new SaleReq(request);


        // Get hourly sales data
        // possible solutions: partition, table sharding, stats scheduling
        List<HourlySaleSummary> hourlySales = paymentsService.getSales(req.getStartDate(), req.getEndDate());


        // Stream each HourlySale to the client
        for (HourlySaleSummary summary : hourlySales) {
            HourlySale hourlySale = HourlySale.newBuilder()
                    .setDatetime(summary.getDatetime())
                    .setSales(summary.getSales().doubleValue())
                    .setPoints(summary.getPoints().doubleValue())
                    .build();
            responseObserver.onNext(hourlySale);
        }

        // Complete the stream
        responseObserver.onCompleted();
    }
}