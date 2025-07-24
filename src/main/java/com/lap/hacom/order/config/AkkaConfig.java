package com.lap.hacom.order.config;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import com.lap.hacom.order.actor.OrderProcessorActor;
import com.lap.hacom.order.repository.OrderRepository;
import com.lap.hacom.order.service.EmailService;
import com.lap.hacom.order.service.SmppService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AkkaConfig {

    private static final Logger logger = LoggerFactory.getLogger(AkkaConfig.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private SmppService smppService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Bean
    public ActorSystem actorSystem() {
        logger.info("Initializing Akka Actor System for order processing");
        ActorSystem system = ActorSystem.create("HacomOrderProcessingSystem");
        logger.info("Akka Actor System initialized successfully");
        return system;
    }

    @Bean
    public ActorRef orderProcessorActor(ActorSystem actorSystem) {
        Props props = OrderProcessorActor.props(orderRepository, smppService, emailService, meterRegistry);
        return actorSystem.actorOf(props, "orderProcessorActor");
    }
}