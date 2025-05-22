package com.lap.hacom.order.config;

import akka.actor.ActorSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AkkaConfig {

    private static final Logger logger = LoggerFactory.getLogger(AkkaConfig.class);

    @Bean
    public ActorSystem actorSystem() {
        logger.info("Initializing Akka Actor System for order processing");
        ActorSystem system = ActorSystem.create("HacomOrderProcessingSystem");
        logger.info("Akka Actor System initialized successfully");
        return system;
    }
}