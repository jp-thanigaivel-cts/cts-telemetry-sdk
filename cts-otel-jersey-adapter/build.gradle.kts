plugins {
    id("java-library")
    id("maven-publish")
}

dependencies {
    implementation(project(":cts-otel-core"))
    implementation(project(":telemetry-api"))
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
    implementation("org.glassfish.jersey.core:jersey-server:3.1.3")
    implementation("org.slf4j:slf4j-api:2.0.9")
    
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}