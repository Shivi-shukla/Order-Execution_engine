package com.order.execution.engine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.order.execution.engine.repository.jpa")
public class OrderExecutionEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(OrderExecutionEngineApplication.class, args);
	}

}
