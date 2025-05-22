package com.lap.hacom.order.controller;

import com.lap.hacom.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    private final OrderRepository orderRepository;

    @Autowired
    public OrderController(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @GetMapping("/{orderId}/status")
    public Mono<ResponseEntity<Map<String, Object>>> getOrderStatus(@PathVariable String orderId) {
        logger.info("Received request to check status for order: {}", orderId);

        return orderRepository.findByOrderId(orderId)
                .map(order -> {
                    logger.info("Order found: {} with status: {}", orderId, order.getStatus());

                    Map<String, Object> response = Map.of(
                            "orderId", order.getOrderId(),
                            "customerId", order.getCustomerId(),
                            "status", order.getStatus(),
                            "items", order.getItems(),
                            "timestamp", order.getTs(),
                            "totalItems", order.getItems().size()
                    );

                    return ResponseEntity.ok(response);
                })
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnSuccess(response -> {
                    if (response.getStatusCode().is2xxSuccessful()) {
                        logger.info("Successfully returned order status for: {}", orderId);
                    } else {
                        logger.warn("Order not found: {}", orderId);
                    }
                })
                .doOnError(error -> logger.error("Error retrieving order status for {}: {}",
                        orderId, error.getMessage(), error));
    }

    @GetMapping("/count")
    public Mono<ResponseEntity<Map<String, Object>>> getOrderCountByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endDate) {

        logger.info("Received request to count orders between {} and {}", startDate, endDate);

        // Validate date range
        if (startDate.isAfter(endDate)) {
            logger.warn("Invalid date range: start date {} is after end date {}", startDate, endDate);
            Map<String, Object> errorResponse = Map.of(
                    "error", "Invalid date range",
                    "message", "Start date must be before or equal to end date",
                    "startDate", startDate,
                    "endDate", endDate
            );
            return Mono.just(ResponseEntity.badRequest().body(errorResponse));
        }

        return orderRepository.countOrdersByDateRange(startDate, endDate)
                .map(count -> {
                    logger.info("Found {} orders between {} and {}", count, startDate, endDate);

                    Map<String, Object> response = Map.of(
                            "totalOrders", count,
                            "startDate", startDate,
                            "endDate", endDate,
                            "queryTimestamp", OffsetDateTime.now()
                    );

                    return ResponseEntity.ok(response);
                })
                .doOnError(error -> logger.error("Error counting orders by date range: {}",
                        error.getMessage(), error));
    }

    @GetMapping("/health")
    public Mono<ResponseEntity<Map<String, String>>> healthCheck() {
        logger.debug("Health check requested for order service");

        Map<String, String> healthStatus = Map.of(
                "status", "UP",
                "service", "OrderService",
                "timestamp", OffsetDateTime.now().toString(),
                "version", "1.0.0"
        );

        return Mono.just(ResponseEntity.ok(healthStatus));
    }
}