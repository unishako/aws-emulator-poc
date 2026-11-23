plugins {
    java
    id("org.springframework.boot") version "4.2.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("software.amazon.awssdk:bom:2.59.7")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("software.amazon.awssdk:s3") {
        exclude(group = "software.amazon.awssdk", module = "apache-client")
        exclude(group = "software.amazon.awssdk", module = "netty-nio-client")
    }
    implementation("software.amazon.awssdk:url-connection-client")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.bootJar {
    archiveFileName = "app.jar"
}

tasks.withType<Test> {
    useJUnitPlatform()
}
