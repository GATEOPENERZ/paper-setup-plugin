plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.1"
}

group = "io.github.gateopenerz"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.13.1")
}

gradlePlugin {
    website.set("https://github.com/gateopenerz/paper-setup-plugin")
    vcsUrl.set("https://github.com/gateopenerz/paper-setup-plugin.git")

    plugins {
        create("paperSetupServer") {
            id = "io.github.gateopenerz.paper-server"
            implementationClass = "com.gateopenerz.paperserver.PaperServerPlugin"
            displayName = "Paper Setup Server helper plugin"
            description = "Downloads & runs PaperMC with configurable jvm arguments and hooks"
            tags.set(listOf("minecraft", "paper", "server"))
        }
    }
}

publishing {
    repositories {
        maven {
            name = "internal"
            url = uri("https://maven.gateopenerz.com/releases")
            credentials {
                username = findProperty("mavenUser") as String?
                password = findProperty("mavenPw")   as String?
            }
        }
    }
}
