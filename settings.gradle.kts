pluginManagement {
    val kotlinPluginVersion: String by settings
    val gradlePluginPublishVersion : String by settings

    plugins {
        `java-gradle-plugin`
        kotlin("jvm") version kotlinPluginVersion
        id("com.gradle.plugin-publish") version gradlePluginPublishVersion
        `maven-publish`
    }
}

rootProject.name = "build-time-tracker"

