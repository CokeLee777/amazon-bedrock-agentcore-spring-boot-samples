package io.github.cokelee777.a2a.agent.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point for the Order A2A agent.
 *
 * <p>
 * Handles order-related skills: {@code order_list} and
 * {@code order_cancellability_check}.
 * </p>
 */
@SpringBootApplication
public class OrderAgentApplication {

	/**
	 * Starts the Order Agent.
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(OrderAgentApplication.class, args);
	}

}
