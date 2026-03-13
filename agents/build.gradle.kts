// Logical grouping for agent sub-projects. No build logic here.

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
	enabled = false
}
