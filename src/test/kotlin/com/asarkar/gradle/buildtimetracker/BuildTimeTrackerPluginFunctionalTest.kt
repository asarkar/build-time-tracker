package com.asarkar.gradle.buildtimetracker

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
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
import java.time.Duration
import java.util.Properties
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.absolutePathString
import kotlin.io.path.inputStream
import kotlin.io.path.readLines
import kotlin.io.path.readText

@OptIn(ExperimentalPathApi::class)
class BuildTimeTrackerPluginFunctionalTest {
    private lateinit var buildFile: Path
    private val taskName = "hello"

    @TempDir
    lateinit var testProjectDir: Path

    private val props = generateSequence(Paths.get(javaClass.protectionDomain.codeSource.location.path)) {
        val props = it.resolve("gradle.properties")
        if (Files.exists(props)) props else it.parent
    }
        .dropWhile { Files.isDirectory(it) }
        .take(1)
        .iterator()
        .next()
        .inputStream()
        .use {
            Properties().apply { load(it) }
        }

    @BeforeEach
    fun beforeEach() {
        buildFile = Files.createFile(testProjectDir.resolve("build.gradle.kts"))
        Files.newBufferedWriter(buildFile, CREATE, WRITE, TRUNCATE_EXISTING).use {
            it.write(
                """
                    import ${Thread::class.qualifiedName}
                    import ${BuildTimeTrackerPluginExtension::class.qualifiedName}
                    import ${Output::class.qualifiedName}
                    import ${Duration::class.qualifiedName}
                    import ${Paths::class.qualifiedName}
                    
                    plugins {
                        id("${props.getProperty("pluginId")}")
                    }
                    
                    tasks.register("$taskName") {
                        doLast {
                            Thread.sleep(200)
                            println("Hello, World!")
                        }
                    }
                """.trimIndent()
            )
            it.newLine()
        }
    }

    @Test
    fun testConsoleOutput() {
        Files.newBufferedWriter(buildFile, APPEND).use {
            it.write(
                """
                configure<${BuildTimeTrackerPluginExtension::class.simpleName}> {
                    minTaskDuration = Duration.ofMillis(100)
                }
                """.trimIndent()
            )
        }

        println(buildFile.readText())

        val result = run()

        assertThat(result.task(taskName)?.outcome == SUCCESS)
        val lines = result.output
            .lines()
            .filter { it.isNotEmpty() }
        assertThat(lines).hasSizeGreaterThanOrEqualTo(4)
        assertThat(lines[0]).isEqualTo("> Task :$taskName")
        assertThat(lines[1]).isEqualTo("Hello, World!")
        assertThat(lines[2]).isEqualTo("== Build time summary ==")
        assertThat(lines[3]).isEqualTo(":$taskName | 0S | 0% | ")
    }

    @Test
    fun testCsvOutput() {
        val csvFilePath = testProjectDir.resolve(BuildTimeTrackerPluginExtension().csvFilePath)

        Files.newBufferedWriter(buildFile, APPEND).use {
            it.write(
                """
                configure<${BuildTimeTrackerPluginExtension::class.simpleName}> {
                    minTaskDuration = Duration.ofMillis(100)
                    output = Output.CSV
                    csvFilePath = Paths.get("${csvFilePath.absolutePathString()}")
                }
                """.trimIndent()
            )
        }

        println(buildFile.readText())

        val result = run()

        assertThat(result.task(taskName)?.outcome == SUCCESS)
        assertThat(Files.exists(csvFilePath)).isTrue
        val lines = csvFilePath.readLines()
        assertThat(lines).hasSize(1)
        assertThat(lines.first()).isEqualTo(":$taskName,0S,0%,")
    }

    private fun run(): BuildResult {
        return GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withArguments(taskName, "--warning-mode=all", "--stacktrace")
            .withPluginClasspath()
            .withDebug(false)
            .forwardOutput()
            .build()
    }
}
