plugins {
    id("java-library")
    id("maven-publish")
}
dependencies {

    api(project(":cts-otel-core"))
    implementation(project(":telemetry-api"))
    
    // Spring Boot dependencies (compileOnly to avoid transitivity issues in consumers if they use different versions, 
    // but implementation is usually fine for starters. Using implementation for simplicity)
//    implementation("org.springframework.boot:spring-boot-starter-kafka:3.3.0")

    implementation("org.springframework.boot:spring-boot-starter-web:3.3.0")
    implementation("org.springframework.boot:spring-boot-starter-web:3.3.0")
    implementation("org.springframework.boot:spring-boot-starter-aop:3.3.0")
    implementation("org.springframework.kafka:spring-kafka:3.1.8")
    compileOnly("org.springframework.kafka:spring-kafka:4.0.1")
    compileOnly("org.springframework.boot:spring-boot-starter-webflux:3.3.0")
    compileOnly("net.devh:grpc-spring-boot-starter:3.1.0.RELEASE")
    compileOnly("io.grpc:grpc-api:1.63.0")
    compileOnly("io.grpc:grpc-stub:1.63.0")
    
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
}

tasks.test {
    useJUnitPlatform()
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}