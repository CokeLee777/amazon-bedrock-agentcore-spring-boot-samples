package io.github.cokelee777.a2a.agent.order.skill;

import io.a2a.spec.Message;
import io.github.cokelee777.a2a.common.SkillExecutor;
import org.springframework.stereotype.Component;

/**
 * Handles the {@code order_list} skill by returning mock order history.
 *
 * <p>
 * In a production system this would query a real order database; here it returns
 * hard-coded sample data to illustrate the A2A skill-routing pattern.
 * </p>
 */
@Component
public class OrderListSkillExecutor implements SkillExecutor {

	@Override
	public String skillId() {
		return "order_list";
	}

	@Override
	public Message.Role requiredRole() {
		return Message.Role.AGENT;
	}

	/**
	 * Returns mock order history for the given message.
	 * @param message the message text (e.g., {@code "MEMBER-001 주문내역 조회"})
	 * @return formatted order list as plain text
	 */
	@Override
	public String execute(String message) {
		return """
				[주문 내역]
				- ORD-1001 | 상품: 노트북 | 금액: 1,500,000원 | 상태: 배송완료 | 주문일: 2026-03-01
				- ORD-1002 | 상품: 마우스 | 금액: 45,000원 | 상태: 배송중 | 주문일: 2026-03-10
				- ORD-1003 | 상품: 키보드 | 금액: 120,000원 | 상태: 결제완료 | 주문일: 2026-03-12
				""";
	}

}
