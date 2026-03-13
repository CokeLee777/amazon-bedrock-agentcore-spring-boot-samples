package io.github.cokelee777.a2a.agent.order.config;

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
 * Provides the {@link AgentCard} bean that describes the Order Agent to callers.
 *
 * <p>
 * Declares the {@code order_list} and {@code order_cancellability_check} skills that this
 * agent supports.
 * </p>
 */
@Configuration
public class AgentCardConfig {

	/**
	 * Builds the agent card advertising the order agent's skills.
	 * @param agentUrl base URL of this agent; injected from {@code a2a.order-agent-url}
	 * @return the fully constructed {@link AgentCard}
	 */
	@Bean
	public AgentCard agentCard(@Value("${a2a.order-agent-url}") String agentUrl) {
		return new AgentCard.Builder().name("Order Agent")
			.description("주문 내역 조회 및 취소 가능 여부를 처리하는 에이전트")
			.url(agentUrl)
			.additionalInterfaces(List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(), agentUrl)))
			.version("1.0.0")
			.capabilities(new AgentCapabilities.Builder().streaming(false).pushNotifications(false).build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of(
					new AgentSkill.Builder().id("order_list")
						.name("주문 목록 조회")
						.description("회원의 주문 내역 목록을 반환합니다")
						.tags(List.of("order", "list"))
						.build(),
					new AgentSkill.Builder().id("order_cancellability_check")
						.name("주문 취소 가능 여부 확인")
						.description("주문번호로 취소 가능 여부를 확인합니다")
						.tags(List.of("order", "cancellability"))
						.build()))
			.build();
	}

}
