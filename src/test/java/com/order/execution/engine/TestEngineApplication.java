package com.order.execution.engine;

import org.springframework.boot.SpringApplication;
import org.testcontainers.utility.TestcontainersConfiguration;

public class TestEngineApplication {

	public static void main(String[] args) {
		SpringApplication.from(OrderExecutionEngineApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
