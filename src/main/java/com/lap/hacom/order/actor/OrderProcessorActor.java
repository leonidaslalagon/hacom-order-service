package com.lap.hacom.order.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import com.lap.hacom.order.grpc.CreateOrderResponse;
import com.lap.hacom.order.model.Order;
import com.lap.hacom.order.repository.OrderRepository;
import com.lap.hacom.order.service.EmailService;
import com.lap.hacom.order.service.SmppService;
import io.grpc.stub.StreamObserver;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.List;

public class OrderProcessorActor extends AbstractActor {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcessorActor.class);

    private final OrderRepository orderRepository;
    private final SmppService smppService;
    private final EmailService emailService;
    private final Counter orderCounter;


    public static Props props(OrderRepository orderRepository, SmppService smppService, EmailService emailService, MeterRegistry meterRegistry) {
        return Props.create(OrderProcessorActor.class,
                () -> new OrderProcessorActor(orderRepository, smppService, emailService, meterRegistry));
    }

    public OrderProcessorActor(OrderRepository orderRepository, SmppService smppService, EmailService emailService, MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.smppService = smppService;
        this.emailService = emailService;
        this.orderCounter = Counter.builder("hacom.orders.processed.total")
                .description("Total number of orders processed")
                .register(meterRegistry);
    }

    public static class ProcessOrderMessage {
        private final String orderId;
        private final String customerId;
        private final String customerPhoneNumber;
        private final String customerEmail;
        private final List<String> items;
        private final StreamObserver<CreateOrderResponse> responseObserver;

        public ProcessOrderMessage(String orderId, String customerId, String customerPhoneNumber, String customerEmail,
                                   List<String> items, StreamObserver<CreateOrderResponse> responseObserver) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.customerPhoneNumber = customerPhoneNumber;
            this.customerEmail = customerEmail;
            this.items = items;
            this.responseObserver = responseObserver;
        }

        public String getOrderId() { return orderId; }
        public String getCustomerId() { return customerId; }
        public String getCustomerPhoneNumber() { return customerPhoneNumber; }
        public String getCustomerEmail() { return customerEmail; }
        public List<String> getItems() { return items; }
        public StreamObserver<CreateOrderResponse> getResponseObserver() { return responseObserver; }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ProcessOrderMessage.class, this::processOrder)
                .build();
    }

    private void processOrder(ProcessOrderMessage message) {
        logger.info("Starting order processing for order ID: {}", message.getOrderId());

        try {
            // Create order entity with initial processing status
            Order order = new Order(
                    null,
                    message.getOrderId(),
                    message.getCustomerId(),
                    message.getCustomerPhoneNumber(),
                    message.getCustomerEmail(),
                    "PROCESSING",
                    message.getItems(),
                    OffsetDateTime.now()
            );

            logger.debug("Created order entity: {}", order);

            // Save order to MongoDB
            orderRepository.save(order)
                    .doOnSuccess(savedOrder -> {
                        logger.info("Order saved successfully to MongoDB: {}", savedOrder.getOrderId());

                        // Update order status to completed
                        savedOrder.setStatus("COMPLETED");

                        // Save updated status
                        orderRepository.save(savedOrder)
                                .doOnSuccess(completedOrder -> {
                                    logger.info("Order status updated to COMPLETED: {}", completedOrder.getOrderId());

                                    // Send SMS notification
                                    String smsMessage = "Your order " + completedOrder.getOrderId() + " has been processed";
                                    boolean smsSent = smppService.sendSms(completedOrder.getCustomerPhoneNumber(), smsMessage);

                                    if (smsSent) {
                                        logger.info("SMS notification sent successfully for order: {}", completedOrder.getOrderId());
                                    } else {
                                        logger.warn("Failed to send SMS notification for order: {}", completedOrder.getOrderId());
                                    }

                                    // Send Email notification
                                    String emailSubject = "Your order " + completedOrder.getOrderId() + " has been processed";
                                    String emailBody = "Thank you for your order. Your order with ID " + completedOrder.getOrderId() + " has been successfully processed.";
                                    boolean emailSent = emailService.sendEmail(completedOrder.getCustomerEmail(), emailSubject, emailBody);

                                    if (emailSent) {
                                        logger.info("Email notification sent successfully for order: {}", completedOrder.getOrderId());
                                    } else {
                                        logger.warn("Failed to send email notification for order: {}", completedOrder.getOrderId());
                                    }

                                    // Increment metrics counter
                                    orderCounter.increment();

                                    // Send successful gRPC response
                                    sendSuccessResponse(message.getResponseObserver(), completedOrder.getOrderId(), "COMPLETED");
                                })
                                .doOnError(error -> {
                                    logger.error("Error updating order status for {}: {}", savedOrder.getOrderId(), error.getMessage(), error);
                                    sendErrorResponse(message.getResponseObserver(), message.getOrderId(), "FAILED");
                                })
                                .subscribe();
                    })
                    .doOnError(error -> {
                        logger.error("Error saving order to MongoDB for {}: {}", message.getOrderId(), error.getMessage(), error);
                        sendErrorResponse(message.getResponseObserver(), message.getOrderId(), "FAILED");
                    })
                    .subscribe();

        } catch (Exception e) {
            logger.error("Unexpected error processing order {}: {}", message.getOrderId(), e.getMessage(), e);
            sendErrorResponse(message.getResponseObserver(), message.getOrderId(), "FAILED");
        }
    }

    private void sendSuccessResponse(StreamObserver<CreateOrderResponse> responseObserver, String orderId, String status) {
        CreateOrderResponse response = CreateOrderResponse.newBuilder()
                .setOrderId(orderId)
                .setStatus(status)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        logger.info("Sent successful gRPC response for order: {}", orderId);
    }

    private void sendErrorResponse(StreamObserver<CreateOrderResponse> responseObserver, String orderId, String status) {
        CreateOrderResponse response = CreateOrderResponse.newBuilder()
                .setOrderId(orderId)
                .setStatus(status)
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        logger.error("Sent error gRPC response for order: {}", orderId);
    }
}