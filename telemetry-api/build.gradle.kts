plugins {
    id("java-library")
    id("maven-publish")
}

dependencies {
    // No dependencies for now, pure Java API
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
}