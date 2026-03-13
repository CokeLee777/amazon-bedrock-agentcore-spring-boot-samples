package io.github.cokelee777.a2a.agent.order;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import io.a2a.spec.TransportProtocol;
import io.github.cokelee777.a2a.server.executor.ChatClientExecutorHandler;
import io.github.cokelee777.a2a.server.executor.DefaultAgentExecutor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Spring Boot application entry point for the Order A2A agent.
 *
 * <p>
 * Handles order-related skills: order list retrieval and cancellability check. Declares
 * {@link AgentCard} and {@link AgentExecutor} beans directly, following the reference
 * weather-agent style.
 * </p>
 */
@SpringBootApplication
public class OrderAgentApplication {

	private static final String SYSTEM_PROMPT = "당신은 주문 조회 전문 에이전트입니다. 주문 내역 조회와 취소 가능 여부를 확인합니다.";

	/**
	 * Starts the Order Agent.
	 * @param args command-line arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(OrderAgentApplication.class, args);
	}

	/**
	 * Builds the agent card advertising the order agent's skills.
	 * @param agentUrl base URL of this agent; injected from {@code a2a.agent-url}
	 * @return the fully constructed {@link AgentCard}
	 */
	@Bean
	public AgentCard agentCard(@Value("${a2a.agent-url}") String agentUrl) {
		return new AgentCard.Builder().name("Order Agent")
			.description("주문 내역 조회 및 취소 가능 여부를 확인하는 에이전트")
			.url(agentUrl)
			.additionalInterfaces(List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(), agentUrl)))
			.version("1.0.0")
			.capabilities(new AgentCapabilities.Builder().streaming(false).pushNotifications(false).build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of(
					new AgentSkill.Builder().id("order_list")
						.name("주문 조회")
						.description("회원의 주문 목록을 조회합니다")
						.tags(List.of("order", "list"))
						.build(),
					new AgentSkill.Builder().id("order_cancellability_check")
						.name("주문 취소 가능 여부 확인")
						.description("배송/결제 상태를 확인하여 취소 가능 여부를 판단합니다")
						.tags(List.of("order", "cancellability"))
						.build()))
			.build();
	}

	/**
	 * Creates the {@link AgentExecutor} using {@link DefaultAgentExecutor} with
	 * {@link OrderTools} as the registered tool.
	 * @param builder the Spring AI auto-configured {@link ChatClient.Builder}
	 * @param tools the order tool methods
	 * @return a configured {@link AgentExecutor}
	 */
	@Bean
	public AgentExecutor agentExecutor(ChatClient.Builder builder, OrderTools tools) {
		ChatClient chatClient = builder.clone().defaultSystem(SYSTEM_PROMPT).defaultTools(tools).build();
		ChatClientExecutorHandler handler = (chat, ctx) -> {
			String msg = DefaultAgentExecutor.extractTextFromMessage(ctx.getMessage());
			return chat.prompt().user(msg).call().content();
		};
		return new DefaultAgentExecutor(chatClient, handler);
	}

}
