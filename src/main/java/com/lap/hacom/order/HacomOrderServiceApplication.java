package com.lap.hacom.order;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
		//(exclude = {MongoAutoConfiguration.class, MongoReactiveAutoConfiguration.class})
@EnableConfigurationProperties
public class HacomOrderServiceApplication {
	private static final Logger logger = LoggerFactory.getLogger(HacomOrderServiceApplication.class);

	public static void main(String[] args) {
		logger.info("Starting Hacom Order Processing System...");

		try {
			SpringApplication.run(HacomOrderServiceApplication.class, args);
			logger.info("Hacom Order Processing System started successfully!");
		} catch (Exception e) {
			logger.error("Failed to start Hacom Order Processing System: {}", e.getMessage(), e);
			System.exit(1);
		}
	}

}
