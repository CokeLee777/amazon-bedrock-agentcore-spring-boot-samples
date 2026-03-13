dependencies {
	implementation(project(":a2a-common"))
	implementation(project(":spring-ai-a2a-server"))
	implementation(project(":spring-ai-a2a-server-autoconfigure"))
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.ai:spring-ai-starter-model-bedrock-converse")
	implementation("org.springframework.ai:spring-ai-client-chat")
}
