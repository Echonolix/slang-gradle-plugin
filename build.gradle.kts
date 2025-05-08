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
        create("slang") {
            id = "net.echonolix.slang-gradle-plugin"
            displayName = "slang-gradle-plugin"
            description = "Gradle plugin adding support for the Slang Shading language."
            implementationClass = "net.echonolix.slang.SlangPlugin"
        }
    }
}

