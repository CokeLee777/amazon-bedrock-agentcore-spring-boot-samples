package io.github.cokelee777.agent.common;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Auto-configuration for agent-common shared components.
 *
 * <p>
 * Registers {@link PingController} for any Spring Boot application that includes the
 * {@code agent-common} module on its classpath.
 * </p>
 */
@AutoConfiguration
@Import(PingController.class)
public class AgentCommonAutoConfiguration {

}
