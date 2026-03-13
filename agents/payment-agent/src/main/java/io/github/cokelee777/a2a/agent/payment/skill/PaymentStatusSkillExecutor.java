package io.github.cokelee777.a2a.agent.payment.skill;

import io.a2a.spec.Message;
import io.github.cokelee777.a2a.common.SkillExecutor;
import org.springframework.stereotype.Component;

/**
 * Handles the {@code payment_status} skill by returning mock payment status.
 *
 * <p>
 * In a production system this would query a real payment gateway; here it returns
 * hard-coded sample data to illustrate the A2A skill-routing pattern.
 * </p>
 */
@Component
public class PaymentStatusSkillExecutor implements SkillExecutor {

	@Override
	public String skillId() {
		return "payment_status";
	}

	@Override
	public Message.Role requiredRole() {
		return Message.Role.AGENT;
	}

	/**
	 * Returns mock payment status for the order referenced in the message.
	 * @param message the message text (e.g., {@code "ORD-1001 결제 상태 확인"})
	 * @return payment status as plain text
	 */
	@Override
	public String execute(String message) {
		if (message.contains("ORD-1001")) {
			return "ORD-1001 결제 상태: 결제완료 — 1,500,000원 (카드결제, 2026-03-01)";
		}
		if (message.contains("ORD-1002")) {
			return "ORD-1002 결제 상태: 결제완료 — 45,000원 (카드결제, 2026-03-10)";
		}
		if (message.contains("ORD-1003")) {
			return "ORD-1003 결제 상태: 결제완료 — 120,000원 (간편결제, 2026-03-12)";
		}
		return "해당 주문번호의 결제 정보를 찾을 수 없습니다.";
	}

}
