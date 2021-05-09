pluginManagement {
    val kotlinPluginVersion: String by settings
    val gradlePluginPublishVersion: String by settings
    val ktlintVersion: String by settings
    plugins {
        kotlin("jvm") version kotlinPluginVersion
        id("com.gradle.plugin-publish") version gradlePluginPublishVersion
        id("org.jlleitschuh.gradle.ktlint") version ktlintVersion
    }
}

rootProject.name = "build-time-tracker"
