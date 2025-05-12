import org.gradle.kotlin.dsl.withType
import org.jlleitschuh.gradle.ktlint.tasks.KtLintCheckTask
import org.jlleitschuh.gradle.ktlint.tasks.KtLintFormatTask

plugins {
    `kotlin-dsl`
    alias(libs.plugins.ktlint)
    alias(libs.plugins.pluginPublish)
    jacoco
}

val javaVersion =
    providers
        .fileContents(layout.projectDirectory.file(".java-version"))
        .asText
        .map { v -> JavaLanguageVersion.of(v.trim()) }

kotlin {
    jvmToolchain {
        languageVersion = javaVersion
    }
    compilerOptions {
        freeCompilerArgs = listOf("-Werror", "-opt-in=kotlin.RequiresOptIn")
    }
}

val projectGroup: String by project
val projectVersion: String by project

group = projectGroup
version = projectVersion

val pluginWebsite =
    providers.environmentVariable("GITHUB_SERVER_URL")
        .zip(providers.environmentVariable("GITHUB_REPOSITORY"), { x, y -> "$x/$y" })

val pluginTags: String by project
val pluginId: String by project
val pluginDescription: String by project
val pluginImplementationClass: String by project
val pluginDeclarationName: String by project

gradlePlugin {
    website = pluginWebsite
    vcsUrl = pluginWebsite.map { "$it.git" }
    plugins {
        register(pluginDeclarationName) {
            id = pluginId
            displayName = rootProject.name
            description = pluginDescription
            implementationClass = pluginImplementationClass
            tags = pluginTags.split(',').map(String::trim)
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation(libs.assertj)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

val ci: Boolean by lazy { System.getenv("CI") != null }

tasks {
    wrapper {
        distributionType = Wrapper.DistributionType.BIN
    }
    test {
        useJUnitPlatform()
        testLogging {
            showStandardStreams = true
        }
        environment("PROJECT_DIR", rootDir.path)
    }
    jacocoTestReport {
        reports {
            xml.required = true
            csv.required = false
            html.required = false
        }
    }

    withType<KtLintFormatTask> {
        enabled = !ci
    }

    // https://github.com/JLLeitschuh/ktlint-gradle/issues/886
    withType<KtLintCheckTask> {
        val fmtTaskName = name.replace("Check", "Format")
        val fmtTask by named(fmtTaskName)
        dependsOn(fmtTask)
    }
}
