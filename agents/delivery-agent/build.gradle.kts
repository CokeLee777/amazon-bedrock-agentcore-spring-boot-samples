val a2aVersion = rootProject.extra["a2aVersion"] as String

dependencies {
	implementation(project(":a2a-common"))
	implementation(project(":spring-ai-a2a-server"))
	implementation(project(":spring-ai-a2a-server-autoconfigure"))
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("io.github.a2asdk:a2a-java-sdk-spec:$a2aVersion")
	implementation("io.github.a2asdk:a2a-java-sdk-server-common:$a2aVersion")
}
