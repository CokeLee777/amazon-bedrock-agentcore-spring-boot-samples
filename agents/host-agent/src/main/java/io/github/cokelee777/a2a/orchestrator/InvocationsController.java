package io.github.cokelee777.a2a.orchestrator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for AgentCore Runtime invocations.
 *
 * <p>
 * Amazon Bedrock AgentCore Runtime forwards user messages to {@code POST /invocations}.
 * This controller delegates each request to the {@link ChatClient}, which routes to
 * downstream A2A agents via {@link RemoteAgentConnections}.
 * </p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class InvocationsController {

	private final ChatClient chatClient;

	/**
	 * Processes a user message and returns the orchestrator's response.
	 * @param userMessage the raw user message from AgentCore Runtime
	 * @return the LLM-generated response text
	 */
	@PostMapping("/invocations")
	public String invoke(@RequestBody String userMessage) {
		log.info("Received: {}", userMessage);
		String response = this.chatClient.prompt().user(userMessage).call().content();
		log.info("Response: {}", response);
		return response;
	}

}
