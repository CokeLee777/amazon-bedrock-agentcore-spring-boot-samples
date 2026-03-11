package io.github.cokelee777.agentcore.orchestrator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChatOrchestrator}.
 */
@ExtendWith(MockitoExtension.class)
class ChatOrchestratorTest {

	@Mock
	private ChatClient chatClient;

	@Mock
	private ChatClient.ChatClientRequestSpec requestSpec;

	@Mock
	private ChatClient.CallResponseSpec callResponseSpec;

	@InjectMocks
	private ChatOrchestrator chatOrchestrator;

	@Test
	@SuppressWarnings("unchecked")
	void handle_normalResponse_returnsContent() {
		when(chatClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
		when(requestSpec.user(anyString())).thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(callResponseSpec);
		when(callResponseSpec.content()).thenReturn("주문 조회 결과입니다.");

		String result = chatOrchestrator.handle("주문 조회", "session-1");

		assertThat(result).isEqualTo("주문 조회 결과입니다.");
	}

	@Test
	@SuppressWarnings("unchecked")
	void handle_contentNull_returnsFallbackMessage() {
		when(chatClient.prompt()).thenReturn(requestSpec);
		when(requestSpec.advisors(any(Consumer.class))).thenReturn(requestSpec);
		when(requestSpec.user(anyString())).thenReturn(requestSpec);
		when(requestSpec.call()).thenReturn(callResponseSpec);
		when(callResponseSpec.content()).thenReturn(null);

		String result = chatOrchestrator.handle("주문 조회", "session-1");

		assertThat(result).isEqualTo("응답을 생성하지 못했습니다.");
	}

	@Test
	void handle_promptThrows_returnsErrorMessage() {
		when(chatClient.prompt()).thenThrow(new RuntimeException("connection failed"));

		String result = chatOrchestrator.handle("주문 조회", "session-1");

		assertThat(result).startsWith("처리 중 오류가 발생했습니다:");
	}

}
