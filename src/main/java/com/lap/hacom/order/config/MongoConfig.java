package com.lap.hacom.order.config;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractReactiveMongoConfiguration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableReactiveMongoRepositories;

@Configuration
@EnableReactiveMongoRepositories(basePackages = "com.lap.hacom.order.repository")
public class MongoConfig extends AbstractReactiveMongoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(MongoConfig.class);

    @Value("${mongodb.database}")
    private String databaseName;

    @Value("${mongodb.uri}")
    private String mongoUri;

    @Override
    protected String getDatabaseName() {
        logger.info("Configuring MongoDB database: {}", databaseName);
        return databaseName;
    }

    @Override
    @Bean
    public MongoClient reactiveMongoClient() {
        logger.info("Creating MongoDB reactive client with URI: {}", mongoUri);
        return MongoClients.create(mongoUri);
    }

    @Bean
    public ReactiveMongoTemplate reactiveMongoTemplate() {
        logger.info("Creating ReactiveMongoTemplate for database: {}", databaseName);
        return new ReactiveMongoTemplate(reactiveMongoClient(), getDatabaseName());
    }
}
