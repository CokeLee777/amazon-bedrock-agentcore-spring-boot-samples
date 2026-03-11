package io.github.cokelee777.agentcore.autoconfigure;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application used as a bootstrap anchor for {@code @WebMvcTest}
 * slices in this library module.
 *
 * <p>
 * {@code @WebMvcTest} requires a {@code @SpringBootApplication} class in the source tree.
 * Since {@code a2a-spring-boot-autoconfigure} is a library with no main application, this
 * class fulfils that requirement for tests only.
 * </p>
 */
@SpringBootApplication
class TestAutoConfigureApplication {

}
