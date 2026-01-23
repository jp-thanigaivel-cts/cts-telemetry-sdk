plugins {
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("java")
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("net.devh:grpc-spring-boot-starter:3.1.0.RELEASE")
    implementation(project(":examples:protos"))
    implementation(project(":cts-otel-spring-boot-adapter"))
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}