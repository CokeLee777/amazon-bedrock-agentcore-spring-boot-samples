dependencies {
    implementation(project(":a2a-common"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.ai:spring-ai-starter-model-bedrock-converse")
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation("software.amazon.awssdk:bedrockagentcore")
}