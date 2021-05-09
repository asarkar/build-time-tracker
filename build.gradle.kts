plugins {
    `java-gradle-plugin`
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
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

val junitVersion: String by project
val assertjVersion: String by project
dependencies {
    implementation(kotlin("stdlib-jdk8"))
    testImplementation(platform("org.junit:junit-bom:$junitVersion"))
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}

plugins.withType<JavaPlugin> {
    extensions.configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showStandardStreams = true
    }
}

publishing {
    publications {
        repositories {
            mavenLocal()
        }
    }
}
