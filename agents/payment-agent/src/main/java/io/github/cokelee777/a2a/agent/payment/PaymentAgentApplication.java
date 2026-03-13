package io.github.cokelee777.a2a.agent.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot application entry point for the Payment A2A agent.
 *
 * <p>
 * Handles the {@code payment_status} skill for payment and refund status queries.
 * </p>
 */
@SpringBootApplication
public class PaymentAgentApplication {

	/**
	 * Starts the Payment Agent.
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(PaymentAgentApplication.class, args);
	}

}
