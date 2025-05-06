allprojects {
    group = "net.echonolix"
    version = "1.0-SNAPSHOT"

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
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {

}

sourceSets.main {
    java {

    }
    this.kotlin {
        println(this.javaClass)
    }
}

kotlin {
    sourceSets
}
gradlePlugin {
    plugins {
        create("slangn") {
            id = "net.echonolix.slang-gradle-plugin"
            displayName = "slang-gradle-plugin"
            implementationClass = "net.echonolix.slang.SlangPlugin"
        }
    }
}

