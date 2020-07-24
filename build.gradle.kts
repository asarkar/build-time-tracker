plugins {
    `java-gradle-plugin`
    kotlin("jvm") version Versions.KOTLIN_VERSION
    id("com.gradle.plugin-publish") version Versions.GRADLE_PLUGIN_PUBLISH_VERSION
    `maven-publish`
}

pluginBundle {
    // please change these URLs to point to your own website/repository
    website = "https://github.com/asarkar/build-time-tracker"
    vcsUrl = "https://github.com/asarkar/build-time-tracker.git"
    tags = listOf("performance", "buildtimes", "metrics")
}

gradlePlugin {
    plugins {
        create("buildTimeTrackerPlugin") {
            id = "org.asarkar.gradle.build-time-tracker"
            displayName = "build-time-tracker"
            description = "Gradle plugin that prints the time taken by the tasks in a build"
            implementationClass = "org.asarkar.gradle.BuildTimeTrackerPlugin"
        }
    }
}

group = "org.asarkar.gradle"
version = Versions.PROJECT

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:${Dependencies.JUNIT5_VERSION}")
    testImplementation("org.assertj:assertj-core:${Dependencies.ASSERTJ_VERSION}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${Dependencies.JUNIT5_VERSION}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

publishing {
    publications {
        repositories {
            mavenLocal()
        }
    }
}