package io.github.cokelee777.a2a.agent.payment.config;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.TransportProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Provides the {@link AgentCard} bean that describes the Payment Agent to callers.
 *
 * <p>
 * Declares the {@code payment_status} skill supported by this agent.
 * </p>
 */
@Configuration
public class AgentCardConfig {

	/**
	 * Builds the agent card advertising the payment agent's skills.
	 * @param agentUrl base URL of this agent; injected from {@code a2a.payment-agent-url}
	 * @return the fully constructed {@link AgentCard}
	 */
	@Bean
	public AgentCard agentCard(@Value("${a2a.payment-agent-url}") String agentUrl) {
		return new AgentCard.Builder().name("Payment Agent")
			.description("주문번호로 결제 및 환불 상태를 확인하는 에이전트")
			.url(agentUrl)
			.additionalInterfaces(List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(), agentUrl)))
			.version("1.0.0")
			.capabilities(new AgentCapabilities.Builder().streaming(false).pushNotifications(false).build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of(new AgentSkill.Builder().id("payment_status")
				.name("결제/환불 상태 확인")
				.description("주문번호로 결제 또는 환불 상태를 반환합니다")
				.tags(List.of("payment", "refund"))
				.build()))
			.build();
	}

}
