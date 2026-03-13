package io.github.cokelee777.a2a.agent.payment.executor;

import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.events.EventQueue;
import io.a2a.server.tasks.TaskUpdater;
import io.a2a.spec.JSONRPCError;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import io.github.cokelee777.a2a.common.A2aMetadataKeys;
import io.github.cokelee777.a2a.common.SkillExecutor;
import io.github.cokelee777.a2a.common.TextExtractor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A2A {@link AgentExecutor} for the Payment Agent.
 *
 * <p>
 * Routes incoming requests to the appropriate {@link SkillExecutor} based on the
 * {@code skillId} present in the message metadata.
 * </p>
 */
@Slf4j
@Component
public class PaymentAgentExecutor implements AgentExecutor {

	private final Map<String, SkillExecutor> skillExecutors;

	/**
	 * Builds the executor by indexing all available {@link SkillExecutor} beans by skill
	 * ID.
	 * @param executors all {@link SkillExecutor} beans registered in the context
	 */
	public PaymentAgentExecutor(List<SkillExecutor> executors) {
		this.skillExecutors = executors.stream().collect(Collectors.toMap(SkillExecutor::skillId, Function.identity()));
	}

	@Override
	public void execute(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
		TaskUpdater updater = new TaskUpdater(context, eventQueue);
		if (context.getTask() == null) {
			updater.submit();
		}
		updater.startWork();

		Message message = Objects.requireNonNull(context.getMessage(), "message must not be null");
		String skillId = resolveSkillId(message);
		String text = TextExtractor.extractFromMessage(message);
		log.debug("PaymentAgentExecutor: skillId={}, text={}", skillId, text);

		try {
			SkillExecutor executor = skillExecutors.get(skillId);
			if (executor == null) {
				throw new IllegalArgumentException("지원하지 않는 스킬 ID: " + skillId);
			}
			String result = executor.execute(text);
			updater.addArtifact(List.of(new TextPart(result)), null, null, null);
			updater.complete();
		}
		catch (Exception e) {
			log.error("PaymentAgentExecutor error: {}", e.getMessage(), e);
			throw new JSONRPCError(-32603, "Agent execution failed: " + e.getMessage(), null);
		}
	}

	@Override
	public void cancel(RequestContext context, EventQueue eventQueue) throws JSONRPCError {
		new TaskUpdater(context, eventQueue).cancel();
	}

	private String resolveSkillId(Message message) {
		if (message.getMetadata() == null) {
			return "";
		}
		Object skillId = message.getMetadata().get(A2aMetadataKeys.SKILL_ID);
		return skillId != null ? skillId.toString() : "";
	}

}
