plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    id("com.gradle.plugin-publish")
    `maven-publish`
}

val pluginWebsite: String by project
val pluginVcsUrl: String by project
val pluginTags: String by project

pluginBundle {
    website = pluginWebsite
    vcsUrl = pluginVcsUrl
    tags = pluginTags.split(",").map(String::trim)
}

val pluginId: String by project
val pluginDisplayName: String by project
val pluginDescription: String by project
val pluginImplementationClass: String by project
val pluginDeclarationName: String by project

gradlePlugin {
    plugins {
        create(pluginDeclarationName) {
            id = pluginId
            displayName = pluginDisplayName
            description = pluginDescription
            implementationClass = pluginImplementationClass
        }
    }
}

val projectGroup: String by project
val projectVersion: String by project
group = projectGroup
version = projectVersion

repositories {
    mavenCentral()
}

val jUnit5Version: String by project
val assertJVersion: String by project
dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter-api:$jUnit5Version")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$jUnit5Version")
    testImplementation("org.assertj:assertj-core:$assertJVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$jUnit5Version")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

publishing {
    publications {
        repositories {
            mavenLocal()
        }
    }
}
