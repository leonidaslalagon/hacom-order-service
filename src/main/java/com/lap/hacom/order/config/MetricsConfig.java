package com.lap.hacom.order.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter grpcRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("hacom.grpc.requests.total")
                .description("Total number of gRPC requests received")
                .register(meterRegistry);
    }

    @Bean
    public Counter apiRequestCounter(MeterRegistry meterRegistry) {
        return Counter.builder("hacom.api.requests.total")
                .description("Total number of API requests received")
                .register(meterRegistry);
    }
}
