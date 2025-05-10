allprojects {
    group = "net.echonolix"
    version = "0.0.1"

    repositories {
        mavenCentral()
        mavenLocal()
    }
}

plugins {
    `kotlin-dsl`
    `maven-publish`
}

java {
    withSourcesJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
    }
}

gradlePlugin {
    plugins {
        create("slang-gradle-plugin") {
            id = "net.echonolix.slang-gradle-plugin"
            implementationClass = "net.echonolix.slang.SlangPlugin"
            displayName = "slang-gradle-plugin"
            description = "A Gradle plugin adding support for the Slang Shading language."
            tags.addAll("slang", "shader", "vulkan", "toolchain")
        }
    }
}

