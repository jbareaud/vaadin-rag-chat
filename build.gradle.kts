plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.5.3"
	id("io.spring.dependency-management") version "1.1.7"
	id("com.vaadin") version "24.8.2"
}

group = "org.jbareaud"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

extra["vaadinVersion"] = "24.8.2"
extra["langchain4jVersion"] = "1.1.0-beta7"
extra["lineAwesomeVersion"] = "2.1.0"
extra["viritinVersion"] = "2.16.0"

dependencies {
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("com.vaadin:vaadin-spring-boot-starter")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("jakarta.validation:jakarta.validation-api")
	developmentOnly("org.springframework.boot:spring-boot-devtools")

	// vaadin extras
	implementation("org.parttio:line-awesome:${property("lineAwesomeVersion")}")
	implementation("in.virit:viritin:${property("viritinVersion")}")

	// langchain4j
	implementation("dev.langchain4j:langchain4j-spring-boot-starter:${property("langchain4jVersion")}")
	implementation("dev.langchain4j:langchain4j-easy-rag:${property("langchain4jVersion")}")
	implementation("dev.langchain4j:langchain4j-ollama-spring-boot-starter:${property("langchain4jVersion")}")
	implementation("dev.langchain4j:langchain4j-onnx-scoring:${property("langchain4jVersion")}")

	// Jackson
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")

	// tests
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("com.vaadin:vaadin-bom:${property("vaadinVersion")}")
	}
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
