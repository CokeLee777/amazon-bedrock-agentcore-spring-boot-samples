package io.github.cokelee777.agentcore.autoconfigure.controller;

import org.springframework.util.Assert;

/**
 * Response body for the {@code GET /ping} health check endpoint.
 *
 * @param status the current health status of the agent (e.g., {@code "healthy"})
 */
public record PingResponse(String status) {

	public PingResponse {
		Assert.notNull(status, "status must not be null");
	}
}
