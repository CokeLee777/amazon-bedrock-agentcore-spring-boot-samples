package io.github.cokelee777.a2a.agent.delivery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point for the Delivery A2A agent.
 *
 * <p>
 * Handles the {@code track_delivery} skill for shipment status queries.
 * </p>
 */
@SpringBootApplication
public class DeliveryAgentApplication {

	/**
	 * Starts the Delivery Agent.
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(DeliveryAgentApplication.class, args);
	}

}
