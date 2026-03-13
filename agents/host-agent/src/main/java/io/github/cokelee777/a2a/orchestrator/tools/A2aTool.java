package io.github.cokelee777.a2a.orchestrator.tools;

import io.a2a.A2A;
import io.a2a.spec.Message;
import io.github.cokelee777.a2a.common.A2aTransport;
import org.springframework.beans.factory.annotation.Value;

/**
 * Abstract base class for Spring AI {@code @Tool} implementations that delegate to
 * downstream A2A agents.
 *
 * <p>
 * Subclasses supply the target agent URL via the constructor and expose one or more
 * {@code @Tool}-annotated methods that call {@link #sendRequest} with the appropriate
 * skill ID and message text.
 * </p>
 */
public abstract class A2aTool {

	private final A2aTransport transport;

	/**
	 * Timeout in seconds for downstream A2A calls, read from
	 * {@code a2a.client.timeout-seconds}.
	 */
	@Value("${a2a.client.timeout-seconds}")
	private int timeoutSeconds;

	/**
	 * Initialises the underlying {@link A2aTransport} for the given agent URL.
	 * @param agentUrl base URL of the downstream A2A agent
	 */
	protected A2aTool(String agentUrl) {
		this.transport = new A2aTransport(agentUrl);
	}

	/**
	 * Sends a {@code message/send} request to the downstream agent and returns the text
	 * response.
	 * @param text the natural-language text to include in the message body
	 * @return the agent's response text, or a Korean error message if the call fails
	 */
	protected String sendRequest(String text) {
		Message message = A2A.toUserMessage(text);
		return transport.send(message, timeoutSeconds).orElse("에이전트 호출 중 오류가 발생했습니다.");
	}

}
