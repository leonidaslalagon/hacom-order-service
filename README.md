# Hacom Order Processing System

A comprehensive order processing system built with Spring Boot, integrating multiple technologies for high-performance, scalable order management.

## Technologies

- **Spring Boot 3.2** Java 17
- **Spring WebFlux** reactive web services
- **MongoDB Reactive** for data persistence
- **gRPC** for high-performance RPC communication
- **Akka Classic Actors** for asynchronous processing
- **SMPP** for SMS notifications
- **Prometheus** metrics for monitoring
- **Log4j2** Logging


### API Rest Endpoints

#### Order Status
*GET /api/orders/{orderId}/status*

Returns detailed order information including status, items, and timestamps.

#### Order Statistics
*GET /api/orders/count?startDate={ISO_DATE}&endDate={ISO_DATE}*

Returns total order count within the specified date range.

#### Health Check
*GET /api/orders/health*

Returns service health status.

### gRPC Service
- **Port**: 9090
- **Service**: `OrderService`
- **Method**: `CreateOrder`

### Prometheus Metrics
- **Endpoint**: `/actuator/prometheus`
- **Custom Metrics**:
    - `hacom.orders.processed.total` - Total orders processed
    - `hacom.sms.sent.total` - Total SMS messages sent
    - `hacom.grpc.requests.total` - Total gRPC requests
    - `hacom.api.requests.total` - Total API requests


## Running the Application

### Prerequisites
1. **Java 17** installed, Developed on Temurin 17
2. **MongoDB** running on localhost:27017
3. **SMPP Server** (optional)

### Build and Run
```bash
# Build
./gradlew build

# Run
./gradlew bootRun
```

### Testing
```bash
# Run the gRPC test client
./gradlew run --main-class=com.lap.hacom.order.client.OrderServiceClient
```

## Monitoring

### Health Checks
- **Application**: http://localhost:9898/actuator/health
- **Order Service**: http://localhost:9898/api/orders/health

### Metrics
- **Prometheus**: http://localhost:9898/actuator/prometheus
- **All Actuator**: http://localhost:9898/actuator