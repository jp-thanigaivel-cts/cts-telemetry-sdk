plugins {
    id("java-library")
    id("maven-publish")
}

dependencies {
    api(platform("io.opentelemetry:opentelemetry-bom:1.36.0"))
    api("io.opentelemetry:opentelemetry-api")
    api("io.opentelemetry:opentelemetry-sdk")
    api("io.opentelemetry:opentelemetry-exporter-otlp")
    api("io.opentelemetry:opentelemetry-exporter-logging")
    api("io.opentelemetry:opentelemetry-sdk-trace")
//    api("io.opentelemetry:opentelemetry-sdk-metrics")
    
    // For semantic conventions if needed, though we are using custom names mostly
    implementation("io.opentelemetry.semconv:opentelemetry-semconv:1.23.1-alpha")

    implementation(project(":telemetry-api"))

    implementation("org.slf4j:slf4j-api:2.0.9")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.7.0")
    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testImplementation("org.assertj:assertj-core:3.24.2")
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