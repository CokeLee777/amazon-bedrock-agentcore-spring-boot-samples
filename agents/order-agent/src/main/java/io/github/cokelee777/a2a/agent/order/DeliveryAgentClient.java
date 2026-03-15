package io.github.cokelee777.a2a.agent.order;

import io.a2a.A2A;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.github.cokelee777.agent.common.A2ATransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;

/**
 * A2A client for communicating with the Delivery Agent.
 *
 * <p>
 * Fetches and caches the Delivery Agent's {@link AgentCard} on startup, then delegates
 * delivery status queries via {@link A2ATransport}.
 * </p>
 */
@Slf4j
@Component
public class DeliveryAgentClient {

	private final AgentCard agentCard;

	/**
	 * Resolves the Delivery Agent's {@link AgentCard} from the given URL.
	 * @param properties the remote agent connection properties
	 */
	public DeliveryAgentClient(RemoteAgentProperties properties) {
		String agentUrl = properties.agents().get("delivery-agent").url();
		AgentCard card = null;
		try {
			String path = new URI(agentUrl).getPath();
			card = A2A.getAgentCard(agentUrl, path + ".well-known/agent-card.json", null);
			log.info("Connected to Delivery Agent at {}", agentUrl);
		}
		catch (Exception e) {
			log.error("Failed to connect to Delivery Agent at {}: {}", agentUrl, e.getMessage());
		}
		this.agentCard = card;
	}

	/**
	 * Sends {@code task} to the Delivery Agent and returns the text response.
	 * @param task the task description to send
	 * @return the delivery agent's response, or an error message if unavailable
	 */
	public String send(String task) {
		if (this.agentCard == null) {
			return "배송 에이전트에 연결할 수 없습니다.";
		}
		Message message = A2A.toUserMessage(task);
		return A2ATransport.send(this.agentCard, message);
	}

}
