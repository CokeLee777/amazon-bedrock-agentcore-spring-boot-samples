package io.github.cokelee777.a2a.agent.order.skill;

import io.a2a.spec.Message;
import io.github.cokelee777.a2a.common.SkillExecutor;
import org.springframework.stereotype.Component;

/**
 * Handles the {@code order_cancellability_check} skill.
 *
 * <p>
 * Returns mock cancellability results based on the order number present in the message.
 * In production this would inspect delivery and payment status from real downstream
 * services.
 * </p>
 */
@Component
public class OrderCancellabilitySkillExecutor implements SkillExecutor {

	@Override
	public String skillId() {
		return "order_cancellability_check";
	}

	@Override
	public Message.Role requiredRole() {
		return Message.Role.AGENT;
	}

	/**
	 * Returns mock cancellability status for the order referenced in the message.
	 * @param message the message text (e.g., {@code "ORD-1001 취소 가능 여부 확인"})
	 * @return cancellability verdict as plain text
	 */
	@Override
	public String execute(String message) {
		if (message.contains("ORD-1001")) {
			return "ORD-1001 취소 불가 — 이미 배송이 완료된 주문입니다.";
		}
		if (message.contains("ORD-1002")) {
			return "ORD-1002 취소 가능 — 배송 준비 단계이므로 취소할 수 있습니다.";
		}
		if (message.contains("ORD-1003")) {
			return "ORD-1003 취소 가능 — 아직 결제 완료 단계이므로 취소할 수 있습니다.";
		}
		return "해당 주문번호의 취소 가능 여부를 확인할 수 없습니다.";
	}

}
