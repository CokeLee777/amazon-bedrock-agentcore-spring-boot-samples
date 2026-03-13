package io.github.cokelee777.a2a.agent.delivery.skill;

import io.a2a.spec.Message;
import io.github.cokelee777.a2a.common.SkillExecutor;
import org.springframework.stereotype.Component;

/**
 * Handles the {@code track_delivery} skill by returning mock delivery status.
 *
 * <p>
 * In a production system this would query a real logistics API; here it returns
 * hard-coded sample data to illustrate the A2A skill-routing pattern.
 * </p>
 */
@Component
public class TrackDeliverySkillExecutor implements SkillExecutor {

	@Override
	public String skillId() {
		return "track_delivery";
	}

	@Override
	public Message.Role requiredRole() {
		return Message.Role.AGENT;
	}

	/**
	 * Returns mock delivery status for the tracking number referenced in the message.
	 * @param message the message text (e.g., {@code "TRACK-1001 배송 조회"})
	 * @return delivery status as plain text
	 */
	@Override
	public String execute(String message) {
		if (message.contains("TRACK-1001")) {
			return "TRACK-1001 배송 상태: 배송완료 — 2026-03-05 14:32 수령인 인도 완료";
		}
		if (message.contains("TRACK-1002")) {
			return "TRACK-1002 배송 상태: 배송중 — 현재 서울 물류센터에서 출발 (2026-03-12 09:15)";
		}
		if (message.contains("TRACK-1003")) {
			return "TRACK-1003 배송 상태: 배송준비중 — 아직 발송되지 않았습니다";
		}
		return "해당 운송장번호의 배송 정보를 찾을 수 없습니다.";
	}

}
