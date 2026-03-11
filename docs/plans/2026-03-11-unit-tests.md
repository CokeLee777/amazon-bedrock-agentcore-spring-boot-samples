# Unit Tests Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 세 모듈(a2a-common → a2a-spring-boot-autoconfigure → a2a-orchestrator) 순서로 단위 테스트를 작성한다.

**Architecture:** 외부 의존성은 Mockito로 Mock 처리, Spring MVC 계층은 @WebMvcTest 슬라이스, 나머지는 순수 JUnit 5. A2aTransport는 최소 리팩터링(A2ACardResolver 주입 + protected executeMessage 추출)으로 테스트 가능하게 만든다.

**Tech Stack:** JUnit 5, Mockito 5 (spring-boot-starter-test 포함), MockMvc, MockHttpServletRequest, ReflectionTestUtils

---

## Task 1: A2aTransport 최소 리팩터링

**Files:**
- Modify: `a2a-common/src/main/java/io/github/cokelee777/agentcore/common/transport/A2aTransport.java`

### 변경 내용

1. `A2ACardResolver`를 필드로 추출 — public 생성자는 `new A2ACardResolver(agentUrl)` 유지, package-private 생성자로 주입 허용
2. Client 실행 로직을 `protected Optional<String> executeMessage(AgentCard card, Message message)` 메서드로 추출 — Mockito spy로 mock 가능하게

```java
package io.github.cokelee777.agentcore.common.transport;

import io.a2a.client.Client;
import io.a2a.client.TaskEvent;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.github.cokelee777.agentcore.common.util.TextExtractor;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reusable A2A client transport that sends a {@link Message} to a downstream agent and
 * collects the full text response.
 *
 * <p>
 * The agent card is resolved lazily on the first call and cached for subsequent
 * invocations. Thread-safe initialization is done via double-checked locking with
 * {@link AtomicReference}.
 * </p>
 */
public class A2aTransport {

	private final A2ACardResolver cardResolver;

	private final AtomicReference<AgentCard> agentCardRef = new AtomicReference<>();

	/**
	 * Creates a transport targeting the given agent base URL.
	 * @param agentUrl base URL of the downstream A2A agent (e.g.,
	 * {@code "http://order-agent:8080/"})
	 */
	public A2aTransport(String agentUrl) {
		this.cardResolver = new A2ACardResolver(agentUrl);
	}

	/**
	 * Package-private constructor for testing — allows injecting a mock
	 * {@link A2ACardResolver}.
	 * @param cardResolver the card resolver to use
	 */
	A2aTransport(A2ACardResolver cardResolver) {
		this.cardResolver = cardResolver;
	}

	/**
	 * Sends {@code message} to the downstream agent and waits up to
	 * {@code timeoutSeconds} for the response.
	 *
	 * <p>
	 * Returns {@link Optional#empty()} when the agent returns no text, the timeout
	 * elapses, or any exception occurs.
	 * </p>
	 * @param message the A2A {@link Message} to send
	 * @param timeoutSeconds maximum seconds to wait for a response
	 * @return the concatenated text from all response parts, or empty if the call fails
	 */
	public Optional<String> send(Message message, int timeoutSeconds) {
		try {
			CompletableFuture<Optional<String>> future = CompletableFuture.supplyAsync(() -> {
				try {
					return executeMessage(resolveAgentCard(), message);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
			});
			return future.get(timeoutSeconds, TimeUnit.SECONDS);
		}
		catch (Exception e) {
			return Optional.empty();
		}
	}

	/**
	 * Executes the actual A2A message send and collects the response.
	 *
	 * <p>
	 * Protected to allow spy-based overriding in unit tests without a real HTTP
	 * connection.
	 * </p>
	 * @param card the resolved {@link AgentCard}
	 * @param message the message to send
	 * @return the text response, or empty if no text was produced
	 * @throws Exception on client-level errors
	 */
	protected Optional<String> executeMessage(AgentCard card, Message message) throws Exception {
		CompletableFuture<String> resultFuture = new CompletableFuture<>();
		try (Client client = Client.builder(card)
			.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
			.addConsumer((event, c) -> {
				if (event instanceof TaskEvent taskEvent) {
					Task task = taskEvent.getTask();
					if (TaskState.TASK_STATE_FAILED.equals(task.status().state())) {
						resultFuture.complete(null);
						return;
					}
					resultFuture.complete(TextExtractor.extractFromTask(task));
				}
			})
			.streamingErrorHandler(resultFuture::completeExceptionally)
			.build()) {
			client.sendMessage(message);
		}
		return Optional.ofNullable(resultFuture.getNow(null));
	}

	/**
	 * Resolves and caches the {@link AgentCard} for this transport's target agent.
	 *
	 * <p>
	 * Uses double-checked locking with {@link AtomicReference} for thread-safe lazy
	 * initialization.
	 * </p>
	 * @return the resolved {@link AgentCard}
	 */
	private AgentCard resolveAgentCard() {
		AgentCard card = agentCardRef.get();
		if (card == null) {
			synchronized (this) {
				card = agentCardRef.get();
				if (card == null) {
					card = cardResolver.getAgentCard();
					agentCardRef.set(card);
				}
			}
		}
		return card;
	}

}
```

**Step 1: 기존 A2aTransport를 위 코드로 교체**

**Step 2: 빌드 확인**

```bash
./gradlew :a2a-common:compileJava
```

Expected: BUILD SUCCESSFUL

---

## Task 2: TextExtractorTest 작성

**Files:**
- Create: `a2a-common/src/test/java/io/github/cokelee777/agentcore/common/util/TextExtractorTest.java`

### 테스트 디렉터리 생성

```bash
mkdir -p a2a-common/src/test/java/io/github/cokelee777/agentcore/common/util
mkdir -p a2a-common/src/test/java/io/github/cokelee777/agentcore/common/transport
```

### 테스트 코드

```java
package io.github.cokelee777.agentcore.common.util;

import io.a2a.spec.Artifact;
import io.a2a.spec.FilePart;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextExtractorTest {

    // ── extractFromMessage ──────────────────────────────────────────────────

    @Test
    void extractFromMessage_singleTextPart_returnsText() {
        Message message = Message.builder()
            .messageId("m1")
            .role(Message.Role.ROLE_USER)
            .parts(List.of(new TextPart("hello")))
            .build();

        assertThat(TextExtractor.extractFromMessage(message)).isEqualTo("hello");
    }

    @Test
    void extractFromMessage_multipleTextParts_concatenatesAll() {
        Message message = Message.builder()
            .messageId("m2")
            .role(Message.Role.ROLE_USER)
            .parts(List.of(new TextPart("foo"), new TextPart("bar")))
            .build();

        assertThat(TextExtractor.extractFromMessage(message)).isEqualTo("foobar");
    }

    @Test
    void extractFromMessage_noTextParts_returnsEmpty() {
        Message message = Message.builder()
            .messageId("m3")
            .role(Message.Role.ROLE_USER)
            .parts(List.of())
            .build();

        assertThat(TextExtractor.extractFromMessage(message)).isEmpty();
    }

    // ── extractFromTask ─────────────────────────────────────────────────────

    @Test
    void extractFromTask_nullArtifacts_returnsEmpty() {
        Task task = Task.builder()
            .id("t1")
            .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED, null, null))
            .build();

        assertThat(TextExtractor.extractFromTask(task)).isEmpty();
    }

    @Test
    void extractFromTask_emptyArtifacts_returnsEmpty() {
        Task task = Task.builder()
            .id("t2")
            .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED, null, null))
            .artifacts(List.of())
            .build();

        assertThat(TextExtractor.extractFromTask(task)).isEmpty();
    }

    @Test
    void extractFromTask_singleTextPartArtifact_returnsText() {
        Artifact artifact = new Artifact(null, null, List.of(new TextPart("result")), null);
        Task task = Task.builder()
            .id("t3")
            .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED, null, null))
            .artifacts(List.of(artifact))
            .build();

        assertThat(TextExtractor.extractFromTask(task)).isEqualTo("result");
    }

    @Test
    void extractFromTask_multipleArtifactsWithTextParts_concatenatesAll() {
        Artifact a1 = new Artifact(null, null, List.of(new TextPart("part1")), null);
        Artifact a2 = new Artifact(null, null, List.of(new TextPart("part2")), null);
        Task task = Task.builder()
            .id("t4")
            .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED, null, null))
            .artifacts(List.of(a1, a2))
            .build();

        assertThat(TextExtractor.extractFromTask(task)).isEqualTo("part1part2");
    }

}
```

**Step 1: 위 파일 생성 후 테스트 실행**

```bash
./gradlew :a2a-common:test
```

Expected: TextExtractorTest — 6 tests PASSED

> **주의:** `Artifact`, `TaskStatus`, `FilePart` 등의 실제 생성자/빌더 시그니처는 A2A SDK 버전(1.0.0.Alpha3)에 따라 다를 수 있다. 컴파일 오류 발생 시 SDK 소스 또는 Javadoc을 확인하여 생성자 인자를 수정한다.

---

## Task 3: A2aTransportTest 작성

**Files:**
- Create: `a2a-common/src/test/java/io/github/cokelee777/agentcore/common/transport/A2aTransportTest.java`

### 테스트 코드

```java
package io.github.cokelee777.agentcore.common.transport;

import io.a2a.client.http.A2ACardResolver;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class A2aTransportTest {

    @Mock
    private A2ACardResolver cardResolver;

    @Mock
    private AgentCard agentCard;

    private Message testMessage;

    @BeforeEach
    void setUp() {
        testMessage = Message.builder()
            .messageId("msg-1")
            .role(Message.Role.ROLE_USER)
            .parts(List.of(new TextPart("test")))
            .build();
        when(cardResolver.getAgentCard()).thenReturn(agentCard);
    }

    @Test
    void send_executeMessageReturnsText_returnsText() throws Exception {
        A2aTransport transport = spy(new A2aTransport(cardResolver));
        doReturn(Optional.of("response")).when(transport).executeMessage(agentCard, testMessage);

        Optional<String> result = transport.send(testMessage, 5);

        assertThat(result).contains("response");
    }

    @Test
    void send_executeMessageReturnsEmpty_returnsEmpty() throws Exception {
        A2aTransport transport = spy(new A2aTransport(cardResolver));
        doReturn(Optional.empty()).when(transport).executeMessage(agentCard, testMessage);

        Optional<String> result = transport.send(testMessage, 5);

        assertThat(result).isEmpty();
    }

    @Test
    void send_executeMessageThrows_returnsEmpty() throws Exception {
        A2aTransport transport = spy(new A2aTransport(cardResolver));
        doThrow(new RuntimeException("connection error")).when(transport).executeMessage(agentCard, testMessage);

        Optional<String> result = transport.send(testMessage, 5);

        assertThat(result).isEmpty();
    }

    @Test
    void send_timeout_returnsEmpty() throws Exception {
        A2aTransport transport = spy(new A2aTransport(cardResolver));
        doAnswer(inv -> {
            Thread.sleep(200);
            return Optional.of("too late");
        }).when(transport).executeMessage(agentCard, testMessage);

        // timeoutSeconds=0 → 즉시 TimeoutException
        Optional<String> result = transport.send(testMessage, 0);

        assertThat(result).isEmpty();
    }

    @Test
    void send_cardResolverThrows_returnsEmpty() {
        when(cardResolver.getAgentCard()).thenThrow(new RuntimeException("unreachable"));
        A2aTransport transport = new A2aTransport(cardResolver);

        Optional<String> result = transport.send(testMessage, 5);

        assertThat(result).isEmpty();
    }

    @Test
    void send_calledTwice_cardResolverInvokedOnce() throws Exception {
        A2aTransport transport = spy(new A2aTransport(cardResolver));
        doReturn(Optional.of("ok")).when(transport).executeMessage(any(), any());

        transport.send(testMessage, 5);
        transport.send(testMessage, 5);

        verify(cardResolver, times(1)).getAgentCard();
    }

}
```

**Step 1: 위 파일 생성 후 테스트 실행**

```bash
./gradlew :a2a-common:test
```

Expected: A2aTransportTest — 6 tests PASSED

---

## Task 4: A2AJsonRpcControllerTest 작성

**Files:**
- Create: `a2a-spring-boot-autoconfigure/src/test/java/io/github/cokelee777/agentcore/autoconfigure/controller/A2AJsonRpcControllerTest.java`

### 테스트 디렉터리 생성

```bash
mkdir -p a2a-spring-boot-autoconfigure/src/test/java/io/github/cokelee777/agentcore/autoconfigure/controller
```

### 테스트 코드

```java
package io.github.cokelee777.agentcore.autoconfigure.controller;

import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(A2AJsonRpcController.class)
class A2AJsonRpcControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestHandler requestHandler;

    private static final String SEND_MESSAGE_REQUEST = """
            {
              "jsonrpc": "2.0",
              "id": "1",
              "method": "message/send",
              "params": {
                "message": {
                  "messageId": "msg-1",
                  "role": "user",
                  "parts": [{"kind": "text", "text": "hello"}]
                }
              }
            }
            """;

    @Test
    void handle_validSendMessageRequest_returns200() throws Exception {
        Task task = Task.builder()
            .id("task-1")
            .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED, null, null))
            .build();
        when(requestHandler.onMessageSend(any(), any())).thenReturn(task);

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(SEND_MESSAGE_REQUEST))
            .andExpect(status().isOk());
    }

    @Test
    void handle_malformedJson_returns500() throws Exception {
        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid-json}"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void handle_unknownMethod_returns500() throws Exception {
        String unknownMethodRequest = """
                {
                  "jsonrpc": "2.0",
                  "id": "1",
                  "method": "unknown/method",
                  "params": {}
                }
                """;

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(unknownMethodRequest))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void handle_missingId_returns500() throws Exception {
        String missingIdRequest = """
                {
                  "jsonrpc": "2.0",
                  "method": "message/send",
                  "params": {}
                }
                """;

        mockMvc.perform(post("/")
                .contentType(MediaType.APPLICATION_JSON)
                .content(missingIdRequest))
            .andExpect(status().isInternalServerError());
    }

}
```

**Step 1: 위 파일 생성 후 테스트 실행**

```bash
./gradlew :a2a-spring-boot-autoconfigure:test
```

Expected: A2AJsonRpcControllerTest — 4 tests PASSED

> **주의:** `@MockitoBean`은 Spring Boot 3.4+에서 `@MockBean` 대신 권장되는 애너테이션이다. `RequestHandler`는 인터페이스이므로 mock 주입이 가능하다. 실제 JSON-RPC 포맷은 A2A SDK의 파싱 로직에 의존하므로, 테스트 실패 시 `JSONRPCUtils.parseRequestBody` 소스를 확인하여 요청 바디 형식을 수정한다.

---

## Task 5: OrchestratorAgentExecutorTest 작성

**Files:**
- Create: `a2a-orchestrator/src/test/java/io/github/cokelee777/agentcore/orchestrator/OrchestratorAgentExecutorTest.java`

### 테스트 디렉터리 생성

```bash
mkdir -p a2a-orchestrator/src/test/java/io/github/cokelee777/agentcore/orchestrator
```

### 테스트 코드

```java
package io.github.cokelee777.agentcore.orchestrator;

import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.tasks.AgentEmitter;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrchestratorAgentExecutorTest {

    @Mock
    private ChatOrchestrator chatOrchestrator;

    @Mock
    private RequestContext requestContext;

    @Mock
    private AgentEmitter emitter;

    @InjectMocks
    private OrchestratorAgentExecutor executor;

    private Message testMessage;

    @BeforeEach
    void setUp() {
        testMessage = Message.builder()
            .messageId("msg-1")
            .role(Message.Role.ROLE_USER)
            .parts(List.of(new TextPart("주문 조회해줘")))
            .build();
        when(requestContext.getMessage()).thenReturn(testMessage);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    // ── 세션 ID 해석 ─────────────────────────────────────────────────────────

    @Test
    void execute_withSessionHeader_usesHeaderValueAsSessionId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(OrchestratorAgentExecutor.SESSION_HEADER, "session-abc");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(chatOrchestrator.handle(anyString(), eq("session-abc"))).thenReturn("응답");

        executor.execute(requestContext, emitter);

        verify(chatOrchestrator).handle("주문 조회해줘", "session-abc");
    }

    @Test
    void execute_withBlankSessionHeader_fallsBackToUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(OrchestratorAgentExecutor.SESSION_HEADER, "   ");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(chatOrchestrator.handle(anyString(), anyString())).thenReturn("응답");

        executor.execute(requestContext, emitter);

        verify(chatOrchestrator).handle(eq("주문 조회해줘"), anyString());
    }

    @Test
    void execute_withoutServletContext_fallsBackToUuid() throws Exception {
        RequestContextHolder.resetRequestAttributes();
        when(chatOrchestrator.handle(anyString(), anyString())).thenReturn("응답");

        executor.execute(requestContext, emitter);

        verify(chatOrchestrator).handle(eq("주문 조회해줘"), anyString());
    }

    // ── 성공 / 실패 흐름 ─────────────────────────────────────────────────────

    @Test
    void execute_success_callsAddArtifactAndComplete() throws Exception {
        RequestContextHolder.resetRequestAttributes();
        when(chatOrchestrator.handle(anyString(), anyString())).thenReturn("성공 응답");

        executor.execute(requestContext, emitter);

        verify(emitter).startWork();
        verify(emitter).addArtifact(any());
        verify(emitter).complete();
        verify(emitter, never()).fail();
    }

    @Test
    void execute_orchestratorThrows_callsFail() throws Exception {
        RequestContextHolder.resetRequestAttributes();
        when(chatOrchestrator.handle(anyString(), anyString())).thenThrow(new RuntimeException("LLM error"));

        executor.execute(requestContext, emitter);

        verify(emitter).startWork();
        verify(emitter).fail();
        verify(emitter, never()).complete();
    }

    @Test
    void cancel_callsEmitterCancel() throws Exception {
        executor.cancel(requestContext, emitter);

        verify(emitter).cancel();
    }

}
```

**Step 1: 위 파일 생성 후 테스트 실행**

```bash
./gradlew :a2a-orchestrator:test
```

Expected: OrchestratorAgentExecutorTest — 6 tests PASSED

---

## Task 6: ChatOrchestratorTest 작성

**Files:**
- Create: `a2a-orchestrator/src/test/java/io/github/cokelee777/agentcore/orchestrator/ChatOrchestratorTest.java`

### 테스트 코드

```java
package io.github.cokelee777.agentcore.orchestrator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ChatClient.AdvisorSpec;
import org.springframework.ai.chat.client.ChatClient.CallResponseSpec;
import org.springframework.ai.chat.client.ChatClient.ChatClientRequestSpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatOrchestratorTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClientRequestSpec requestSpec;

    @Mock
    private CallResponseSpec callResponseSpec;

    private ChatOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        orchestrator = new ChatOrchestrator(chatClient);

        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
    }

    @Test
    void handle_normalResponse_returnsContent() {
        when(callResponseSpec.content()).thenReturn("주문 조회 결과입니다.");

        String result = orchestrator.handle("주문 보여줘", "session-1");

        assertThat(result).isEqualTo("주문 조회 결과입니다.");
    }

    @Test
    void handle_nullContent_returnsFallbackMessage() {
        when(callResponseSpec.content()).thenReturn(null);

        String result = orchestrator.handle("주문 보여줘", "session-1");

        assertThat(result).isEqualTo("응답을 생성하지 못했습니다.");
    }

    @Test
    void handle_chatClientThrows_returnsErrorMessage() {
        when(chatClient.prompt()).thenThrow(new RuntimeException("Bedrock timeout"));

        String result = orchestrator.handle("주문 보여줘", "session-1");

        assertThat(result).startsWith("처리 중 오류가 발생했습니다:");
        assertThat(result).contains("Bedrock timeout");
    }

}
```

**Step 1: 위 파일 생성 후 전체 테스트 실행**

```bash
./gradlew test
```

Expected: 전체 테스트 PASSED (a2a-common 12개, a2a-spring-boot-autoconfigure 4개, a2a-orchestrator 9개)

---

## 최종 검증

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL (모든 테스트 포함)
