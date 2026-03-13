package io.github.cokelee777.a2a.agent.delivery.config;

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
 * Provides the {@link AgentCard} bean that describes the Delivery Agent to callers.
 *
 * <p>
 * Declares the {@code track_delivery} skill supported by this agent.
 * </p>
 */
@Configuration
public class AgentCardConfig {

	/**
	 * Builds the agent card advertising the delivery agent's skills.
	 * @param agentUrl base URL of this agent; injected from
	 * {@code a2a.delivery-agent-url}
	 * @return the fully constructed {@link AgentCard}
	 */
	@Bean
	public AgentCard agentCard(@Value("${a2a.delivery-agent-url}") String agentUrl) {
		return new AgentCard.Builder().name("Delivery Agent")
			.description("운송장번호로 배송 상태를 추적하는 에이전트")
			.url(agentUrl)
			.additionalInterfaces(List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(), agentUrl)))
			.version("1.0.0")
			.capabilities(new AgentCapabilities.Builder().streaming(false).pushNotifications(false).build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of(new AgentSkill.Builder().id("track_delivery")
				.name("배송 조회")
				.description("운송장번호로 현재 배송 상태를 반환합니다")
				.tags(List.of("delivery", "tracking"))
				.build()))
			.build();
	}

}
