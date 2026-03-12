# Amazon Bedrock AgentCore Runtime — A2A orchestrator container
#
# A2A protocol contract (see AWS docs runtime-a2a-protocol-contract):
#   - Platform: linux/arm64 (required)
#   - Listen: 0.0.0.0:9000
#   - POST /           → JSON-RPC 2.0 (message/send, etc.)
#   - GET /.well-known/agent-card.json → Agent discovery
#   - GET /ping        → health (200 + JSON)
#
# Build for AgentCore (ARM64):
#   docker buildx build --platform linux/arm64 -t a2a-orchestrator:arm64 --load .
# Push to ECR then create_agent_runtime with containerConfiguration.containerUri.
#
# Runtime env (set in AgentCore/ECS/task definition):
#   BEDROCK_REGION, BEDROCK_MODEL_ID, ORCHESTRATOR_URL (public URL of this runtime),
#   ORDER_AGENT_URL, DELIVERY_AGENT_URL, PAYMENT_AGENT_URL, etc.

# ---------- Build stage (JAR is bytecode; any arch) ----------
FROM eclipse-temurin:21-jdk-jammy AS builder

WORKDIR /app

# Gradle wrapper + sources
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY a2a-common ./a2a-common
COPY a2a-spring-boot-autoconfigure ./a2a-spring-boot-autoconfigure
COPY a2a-orchestrator ./a2a-orchestrator

RUN chmod +x gradlew \
	&& ./gradlew :a2a-orchestrator:bootJar --no-daemon -x test \
	&& cp a2a-orchestrator/build/libs/a2a-orchestrator-*-SNAPSHOT.jar /app/app.jar \
	&& ls -la /app/app.jar

# ---------- Runtime stage (must be ARM64 for AgentCore) ----------
FROM --platform=linux/arm64 eclipse-temurin:21-jre-jammy

WORKDIR /app

# Non-root
RUN groupadd --system app && useradd --system --gid app app
USER app

COPY --from=builder --chown=app:app /app/app.jar /app/app.jar

# A2A contract port
EXPOSE 9000

# Spring Boot binds to all interfaces by default; server.port already 9000 in application.yml
ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
