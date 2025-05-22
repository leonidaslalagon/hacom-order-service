package com.lap.hacom.order.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.boot.web.reactive.server.ReactiveWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
@EnableWebFlux
public class WebFluxConfig implements WebFluxConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebFluxConfig.class);

    @Value("${api.port}")
    private int apiPort;

    @Bean
    public ReactiveWebServerFactory reactiveWebServerFactory() {
        logger.info("Configuring WebFlux server on port: {}", apiPort);
        NettyReactiveWebServerFactory factory = new NettyReactiveWebServerFactory();
        factory.setPort(apiPort);
        return factory;
    }
}