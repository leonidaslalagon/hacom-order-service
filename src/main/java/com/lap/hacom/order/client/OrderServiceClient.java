package com.lap.hacom.order.client;

import com.lap.hacom.order.grpc.CreateOrderRequest;
import com.lap.hacom.order.grpc.CreateOrderResponse;
import com.lap.hacom.order.grpc.OrderServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class OrderServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceClient.class);

    public static void main(String[] args) throws InterruptedException {
        logger.info("Starting Order Service gRPC client test");

        // Create gRPC channel
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9090)
                .usePlaintext()
                .build();

        try {
            // Create blocking stub
            OrderServiceGrpc.OrderServiceBlockingStub stub = OrderServiceGrpc.newBlockingStub(channel);

            // Test multiple orders
            for (int i = 1; i <= 3; i++) {
                testOrderCreation(stub, i);
                Thread.sleep(2000); // Wait between requests
            }

        } finally {
            // Shutdown channel
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
            logger.info("gRPC client shutdown completed");
        }
    }

    private static void testOrderCreation(OrderServiceGrpc.OrderServiceBlockingStub stub, int orderNumber) {
        String orderId = UUID.randomUUID().toString();
        String customerId = "CUST" + String.format("%03d", orderNumber);
        String phoneNumber = "+521234567" + String.format("%03d", orderNumber);

        logger.info("Creating test order #{} with ID: {}", orderNumber, orderId);

        // Create request
        CreateOrderRequest request = CreateOrderRequest.newBuilder()
                .setOrderId(orderId)
                .setCustomerId(customerId)
                .setCustomerPhoneNumber(phoneNumber)
                .addAllItems(Arrays.asList(
                        "Product A - Laptop",
                        "Product B - Mouse",
                        "Product C - Keyboard"
                ))
                .build();

        try {
            // Make gRPC call
            CreateOrderResponse response = stub.createOrder(request);

            logger.info("Order #{} Response - ID: {}, Status: {}",
                    orderNumber, response.getOrderId(), response.getStatus());

        } catch (Exception e) {
            logger.error("Error creating order #{}: {}", orderNumber, e.getMessage(), e);
        }
    }
}