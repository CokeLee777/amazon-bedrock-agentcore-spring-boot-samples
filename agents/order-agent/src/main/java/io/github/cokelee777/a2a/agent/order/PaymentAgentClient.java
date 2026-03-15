package io.github.cokelee777.a2a.agent.order;

import io.a2a.A2A;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.github.cokelee777.agent.common.A2ATransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * A2A client for communicating with the Payment Agent.
 *
 * <p>
 * Fetches and caches the Payment Agent's {@link AgentCard} on startup, then delegates
 * payment status queries via {@link A2ATransport}.
 * </p>
 */
@Slf4j
@Component
public class PaymentAgentClient {

	private final AgentCard agentCard;

	/**
	 * Resolves the Payment Agent's {@link AgentCard} from the given URL.
	 * @param properties the remote agent connection properties
	 */
	public PaymentAgentClient(RemoteAgentProperties properties) {
		String agentUrl = properties.agents().get("payment-agent").url();
		AgentCard card = null;
		try {
			String path = new URI(agentUrl).getPath();
			card = A2A.getAgentCard(agentUrl, path + ".well-known/agent-card.json", null);
			log.info("Connected to Payment Agent at {}", agentUrl);
		}
		catch (Exception e) {
			log.error("Failed to connect to Payment Agent at {}: {}", agentUrl, e.getMessage());
		}
		this.agentCard = card;
	}

	/**
	 * Sends {@code task} to the Payment Agent and returns the text response.
	 * @param task the task description to send
	 * @return the payment agent's response, or an error message if unavailable
	 */
	public String send(String task) {
		if (this.agentCard == null) {
			return "결제 에이전트에 연결할 수 없습니다.";
		}
		Message message = A2A.toUserMessage(task);
		return A2ATransport.send(this.agentCard, message);
	}

}
