package com.lap.hacom.order.grpc;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.lap.hacom.order.actor.OrderProcessorActor;
import com.lap.hacom.order.repository.OrderRepository;
import com.lap.hacom.order.service.SmppService;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.MeterRegistry;
import net.devh.boot.grpc.server.service.GrpcService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@GrpcService
public class OrderServiceImpl extends OrderServiceGrpc.OrderServiceImplBase {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final ActorSystem actorSystem;
    private final OrderRepository orderRepository;
    private final SmppService smppService;
    private final ActorRef orderProcessorActor;

    @Autowired
    public OrderServiceImpl(ActorSystem actorSystem, OrderRepository orderRepository,
                            SmppService smppService, MeterRegistry meterRegistry) {
        this.actorSystem = actorSystem;
        this.orderRepository = orderRepository;
        this.smppService = smppService;

        // Create order processor actor
        this.orderProcessorActor = actorSystem.actorOf(
                OrderProcessorActor.props(orderRepository, smppService, meterRegistry),
                "order-processor-actor"
        );

        logger.info("OrderService gRPC implementation initialized with actor system");
    }

    @Override
    public void createOrder(CreateOrderRequest request, StreamObserver<CreateOrderResponse> responseObserver) {
        logger.info("Received gRPC order creation request for order ID: {}", request.getOrderId());
        logger.debug("Order details - Customer: {}, Phone: {}, Items: {}",
                request.getCustomerId(), request.getCustomerPhoneNumber(), request.getItemsList());

        try {
            // Validate request
            if (!isValidRequest(request)) {
                logger.warn("Invalid order request received: {}", request.getOrderId());
                sendInvalidRequestResponse(responseObserver, request.getOrderId());
                return;
            }

            // gRPC repeated field to List
            List<String> items = new ArrayList<>(request.getItemsList());

            // Create message for actor
            OrderProcessorActor.ProcessOrderMessage message =
                    new OrderProcessorActor.ProcessOrderMessage(
                            request.getOrderId(),
                            request.getCustomerId(),
                            request.getCustomerPhoneNumber(),
                            items,
                            responseObserver
                    );

            // Send message
            // async process
            orderProcessorActor.tell(message, ActorRef.noSender());

            logger.info("Order processing message sent to actor for order: {}", request.getOrderId());

        } catch (Exception e) {
            logger.error("Unexpected error handling gRPC request for order {}: {}",
                    request.getOrderId(), e.getMessage(), e);
            sendErrorResponse(responseObserver, request.getOrderId(), "FAILED");
        }
    }

    private boolean isValidRequest(CreateOrderRequest request) {
        if (request.getOrderId() == null || request.getOrderId().trim().isEmpty()) {
            logger.warn("Order ID is missing or empty");
            return false;
        }

        if (request.getCustomerId() == null || request.getCustomerId().trim().isEmpty()) {
            logger.warn("Customer ID is missing or empty for order: {}", request.getOrderId());
            return false;
        }

        if (request.getCustomerPhoneNumber() == null || request.getCustomerPhoneNumber().trim().isEmpty()) {
            logger.warn("Customer phone number is missing or empty for order: {}", request.getOrderId());
            return false;
        }

        if (request.getItemsList() == null || request.getItemsList().isEmpty()) {
            logger.warn("Items list is missing or empty for order: {}", request.getOrderId());
            return false;
        }

        return true;
    }

    private void sendInvalidRequestResponse(StreamObserver<CreateOrderResponse> responseObserver, String orderId) {
        CreateOrderResponse response = CreateOrderResponse.newBuilder()
                .setOrderId(orderId)
                .setStatus("INVALID_REQUEST")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    private void sendErrorResponse(StreamObserver<CreateOrderResponse> responseObserver, String orderId, String status) {
        CreateOrderResponse response = CreateOrderResponse.newBuilder()
                .setOrderId(orderId)
                .setStatus(status)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}