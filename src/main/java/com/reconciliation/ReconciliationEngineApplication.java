package com.reconciliation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.reconciliation.shared.BatchProperties;

@SpringBootApplication
@EnableConfigurationProperties(BatchProperties.class)
public class ReconciliationEngineApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReconciliationEngineApplication.class, args);
	}

}
