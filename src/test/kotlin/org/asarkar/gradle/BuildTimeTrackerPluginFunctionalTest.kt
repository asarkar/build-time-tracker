package org.asarkar.gradle

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption.APPEND
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.util.Properties

class BuildTimeTrackerPluginFunctionalTest {
    lateinit var buildFile: Path

    @BeforeEach
    fun beforeEach(@TempDir testProjectDir: Path) {
        val propFile = generateSequence(Paths.get(javaClass.protectionDomain.codeSource.location.path)) {
            val props = it.resolve("gradle.properties")
            if (Files.exists(props)) props else it.parent
        }
                .dropWhile { Files.isDirectory(it) }
                .take(1)
                .iterator()
                .next()

        Files.newInputStream(propFile).use {
            val props = Properties().apply { load(it) }
            buildFile = Files.createFile(testProjectDir.resolve("build.gradle.kts"))
            Files.newBufferedWriter(buildFile, CREATE, WRITE, TRUNCATE_EXISTING).use {
                it.write("""
                    import java.lang.Thread.sleep
                    import org.asarkar.gradle.BuildTimeTrackerPluginExtension
                    import java.time.Duration
                    plugins {
                        id("${props.getProperty("pluginId")}")
                    }
                """.trimIndent())
                it.newLine()
            }
        }
    }

    @Test
    fun testPluginLoads() {
        val taskName = "hello"
        Files.newBufferedWriter(buildFile, APPEND).use {
            it.write("""
                tasks.register("$taskName") {
                    doLast {
                        sleep(2000)
                        println("Hello, World!")
                    }
                }
                
                configure<BuildTimeTrackerPluginExtension> {
                    minTaskDuration = Duration.ofSeconds(1)
                }
            """.trimIndent())
        }

        println(Files.readString(buildFile))

        val result = GradleRunner.create()
                .withProjectDir(buildFile.parent.toFile())
                .withArguments(taskName, "--warning-mode=all", "--stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .withDebug(true)
                .build()

        assertThat(result.task(taskName)?.outcome == SUCCESS)
        assertThat(result.output).contains("== Build time summary ==")
        assertThat(result.output).contains(":hello")
    }
}