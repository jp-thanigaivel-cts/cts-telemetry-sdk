import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.include

rootProject.name = "cts-telemetry-sdk"
include("cts-otel-core")
include("cts-otel-spring-boot-adapter")
include("cts-otel-jersey-adapter")
include("examples:demo-app-spring-boot")
include("examples:protos")
include("examples:demo-app-spring-boot-2")
include("telemetry-api")
