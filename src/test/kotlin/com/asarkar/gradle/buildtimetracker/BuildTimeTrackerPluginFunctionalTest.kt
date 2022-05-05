package com.asarkar.gradle.buildtimetracker

import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome.SUCCESS
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.RepetitionInfo
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

    companion object {
        @TempDir
        @JvmStatic
        lateinit var sharedTestProjectDir: Path
    }

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

    private fun newBuildFile(name: String, rootDir: Path) {
        buildFile = rootDir.resolve(name)
        Files.newBufferedWriter(buildFile, CREATE, WRITE, TRUNCATE_EXISTING).use {
            it.write(
                """
                    import ${Thread::class.qualifiedName}
                    import ${Output::class.qualifiedName}
                    import ${Duration::class.qualifiedName}
                    import ${Sort::class.qualifiedName}
                    
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
    fun testConsoleOutputKotlin() {
        newBuildFile("build.gradle.kts", testProjectDir)
        Files.newBufferedWriter(buildFile, APPEND).use {
            it.write(
                """
                ${Constants.PLUGIN_EXTENSION_NAME} {
                    minTaskDuration.set(Duration.ofMillis(100))
                }
                """.trimIndent()
            )
        }

        println(buildFile.readText())

        val result = run(testProjectDir, taskName)

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
    fun testConsoleOutputGroovy() {
        newBuildFile("build.gradle", testProjectDir)
        Files.newBufferedWriter(buildFile, APPEND).use {
            it.write(
                """
                ${Constants.PLUGIN_EXTENSION_NAME} {
                    minTaskDuration = Duration.ofMillis(100)
                }
                """.trimIndent()
            )
        }

        println(buildFile.readText())

        val result = run(testProjectDir, taskName)

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
    fun testCsvOutputKotlin() {
        newBuildFile("build.gradle.kts", testProjectDir)
        Files.newBufferedWriter(buildFile, APPEND).use {
            it.write(
                """
                ${Constants.PLUGIN_EXTENSION_NAME} {
                    minTaskDuration.set(Duration.ofMillis(100))
                    output.set(Output.CSV)
                    reportsDir.set(file("${testProjectDir.absolutePathString()}"))
                }
                """.trimIndent()
            )
        }

        println(buildFile.readText())

        val result = run(testProjectDir, taskName)
        val csvFile = testProjectDir.resolve(Constants.CSV_FILENAME)
        assertThat(result.task(taskName)?.outcome == SUCCESS)
        assertThat(Files.exists(csvFile)).isTrue
        val lines = csvFile.readLines()
        assertThat(lines).hasSize(1)
        assertThat(lines.first()).isEqualTo(":$taskName,0S,0%,")
    }

    @Test
    fun testCsvOutputGroovy() {
        newBuildFile("build.gradle", testProjectDir)
        Files.newBufferedWriter(buildFile, APPEND).use {
            it.write(
                """
                ${Constants.PLUGIN_EXTENSION_NAME} {
                    minTaskDuration = Duration.ofMillis(100)
                    output = Output.CSV
                    reportsDir = file("${testProjectDir.absolutePathString()}")
                }
                """.trimIndent()
            )
        }

        println(buildFile.readText())

        val result = run(testProjectDir, taskName)
        val csvFile = testProjectDir.resolve(Constants.CSV_FILENAME)
        assertThat(result.task(taskName)?.outcome == SUCCESS)
        assertThat(Files.exists(csvFile)).isTrue
        val lines = csvFile.readLines()
        assertThat(lines).hasSize(1)
        assertThat(lines.first()).isEqualTo(":$taskName,0S,0%,")
    }

    @Test
    fun testSort() {
        newBuildFile("build.gradle.kts", testProjectDir)
        Files.newBufferedWriter(buildFile, APPEND).use {
            it.write(
                """
                    tasks.register("a") {
                        doLast {
                            Thread.sleep(1100)
                            println("Hello, World!")
                        }
                    }
                    tasks.register("b") {
                        doLast {
                            Thread.sleep(2100)
                            println("Hi there!")
                        }
                    }
                    ${Constants.PLUGIN_EXTENSION_NAME} {
                        minTaskDuration.set(Duration.ofMillis(100))
                        sort.set(true)
                    }
                """.trimIndent()
            )
        }

        println(buildFile.readText())

        val result = run(testProjectDir, "a", "b")

        assertThat(result.task(taskName)?.outcome == SUCCESS)
        val lines = result.output
            .lines()
            .filter { it.isNotEmpty() }
        assertThat(lines).hasSizeGreaterThanOrEqualTo(4)
        assertThat(lines[0]).isEqualTo("> Task :a")
        assertThat(lines[1]).isEqualTo("Hello, World!")
        assertThat(lines[2]).isEqualTo("> Task :b")
        assertThat(lines[3]).isEqualTo("Hi there!")
        assertThat(lines[4]).isEqualTo("== Build time summary ==")
        assertThat(lines[5]).isEqualTo(":b | 2S | 67% | ${Printer.BLOCK_CHAR.toString().repeat(2)}")
        assertThat(lines[6]).isEqualTo(":a | 1S | 33% | ${Printer.BLOCK_CHAR}")
    }

    @Test
    fun testSortByDesc() {
        newBuildFile("build.gradle.kts", testProjectDir)
        Files.newBufferedWriter(buildFile, APPEND).use {
            it.write(
                """
                    tasks.register("a") {
                        doLast {
                            Thread.sleep(1100)
                            println("Hello, World!")
                        }
                    }
                    tasks.register("b") {
                        doLast {
                            Thread.sleep(2100)
                            println("Hi there!")
                        }
                    }
                    ${Constants.PLUGIN_EXTENSION_NAME} {
                        minTaskDuration.set(Duration.ofMillis(100))
                        sortBy.set(Sort.DESC)
                    }
                """.trimIndent()
            )
        }

        println(buildFile.readText())

        val result = run(testProjectDir, "a", "b")

        assertThat(result.task(taskName)?.outcome == SUCCESS)
        val lines = result.output
            .lines()
            .filter { it.isNotEmpty() }
        assertThat(lines).hasSizeGreaterThanOrEqualTo(4)
        assertThat(lines[0]).isEqualTo("> Task :a")
        assertThat(lines[1]).isEqualTo("Hello, World!")
        assertThat(lines[2]).isEqualTo("> Task :b")
        assertThat(lines[3]).isEqualTo("Hi there!")
        assertThat(lines[4]).isEqualTo("== Build time summary ==")
        assertThat(lines[5]).isEqualTo(":b | 2S | 67% | ${Printer.BLOCK_CHAR.toString().repeat(2)}")
        assertThat(lines[6]).isEqualTo(":a | 1S | 33% | ${Printer.BLOCK_CHAR}")
    }

    @Test
    fun testSortByAsc() {
        newBuildFile("build.gradle.kts", testProjectDir)
        Files.newBufferedWriter(buildFile, APPEND).use {
            it.write(
                """
                    tasks.register("a") {
                        doLast {
                            Thread.sleep(1100)
                            println("Hello, World!")
                        }
                    }
                    tasks.register("b") {
                        doLast {
                            Thread.sleep(2100)
                            println("Hi there!")
                        }
                    }
                    ${Constants.PLUGIN_EXTENSION_NAME} {
                        minTaskDuration.set(Duration.ofMillis(100))
                        sortBy.set(Sort.ASC)
                    }
                """.trimIndent()
            )
        }

        println(buildFile.readText())

        val result = run(testProjectDir, "a", "b")

        assertThat(result.task(taskName)?.outcome == SUCCESS)
        val lines = result.output
            .lines()
            .filter { it.isNotEmpty() }
        assertThat(lines).hasSizeGreaterThanOrEqualTo(4)
        assertThat(lines[0]).isEqualTo("> Task :a")
        assertThat(lines[1]).isEqualTo("Hello, World!")
        assertThat(lines[2]).isEqualTo("> Task :b")
        assertThat(lines[3]).isEqualTo("Hi there!")
        assertThat(lines[4]).isEqualTo("== Build time summary ==")
        assertThat(lines[5]).isEqualTo(":a | 1S | 33% | ${Printer.BLOCK_CHAR}")
        assertThat(lines[6]).isEqualTo(":b | 2S | 67% | ${Printer.BLOCK_CHAR.toString().repeat(2)}")
    }

    @RepeatedTest(3)
    fun testConfigurationCache(repetitionInfo: RepetitionInfo) {
        newBuildFile("build.gradle.kts", sharedTestProjectDir)
        Files.newBufferedWriter(buildFile, APPEND).use {
            it.write(
                """
                    tasks.register("a") {
                        doLast {
                            Thread.sleep(1100)
                            println("Hello, World!")
                        }
                    }
                    tasks.register("b") {
                        doLast {
                            Thread.sleep(2100)
                            println("Hi there!")
                        }
                    }
                    ${Constants.PLUGIN_EXTENSION_NAME} {
                        minTaskDuration.set(Duration.ofMillis(100))
                    }
                """.trimIndent()
            )
        }

        println(buildFile.readText())

        val result = run(sharedTestProjectDir, "-q", "--configuration-cache", "a", "b")

        assertThat(result.task(taskName)?.outcome == SUCCESS)
        val lines = result.output
            .lines()
            .filter { it.isNotEmpty() }
        assertThat(lines).hasSizeGreaterThanOrEqualTo(4)
        assertThat(lines[0]).isEqualTo("Hello, World!")
        assertThat(lines[1]).isEqualTo("Hi there!")
        assertThat(lines[2]).isEqualTo("== Build time summary ==")
        if (repetitionInfo.currentRepetition == 1) {
            assertThat(lines[3]).isEqualTo(":a | 1S | 33% | ${Printer.BLOCK_CHAR}")
            assertThat(lines[4]).isEqualTo(":b | 2S | 67% | ${Printer.BLOCK_CHAR.toString().repeat(2)}")
        } else {
            // https://github.com/asarkar/build-time-tracker/discussions/45
            assertThat(lines[3]).isEqualTo(":a | 1S |  50% | ${Printer.BLOCK_CHAR}")
            assertThat(lines[4]).isEqualTo(":b | 2S | 100% | ${Printer.BLOCK_CHAR.toString().repeat(2)}")
        }
    }

    @RepeatedTest(3)
    fun testSortDescAndConfigurationCache(repetitionInfo: RepetitionInfo) {
        newBuildFile("build.gradle.kts", sharedTestProjectDir)
        Files.newBufferedWriter(buildFile, APPEND).use {
            it.write(
                """
                    tasks.register("a") {
                        doLast {
                            Thread.sleep(1100)
                            println("Hello, World!")
                        }
                    }
                    tasks.register("b") {
                        doLast {
                            Thread.sleep(2100)
                            println("Hi there!")
                        }
                    }
                    ${Constants.PLUGIN_EXTENSION_NAME} {
                        minTaskDuration.set(Duration.ofMillis(100))
                        sortBy.set(Sort.DESC)
                    }
                """.trimIndent()
            )
        }

        println(buildFile.readText())

        val result =
            run(sharedTestProjectDir, "-q", "--configuration-cache", "a", "b")

        assertThat(result.task(taskName)?.outcome == SUCCESS)
        val lines = result.output
            .lines()
            .filter { it.isNotEmpty() }
        assertThat(lines).hasSizeGreaterThanOrEqualTo(4)
        assertThat(lines[0]).isEqualTo("Hello, World!")
        assertThat(lines[1]).isEqualTo("Hi there!")
        assertThat(lines[2]).isEqualTo("== Build time summary ==")
        if (repetitionInfo.currentRepetition == 1) {
            assertThat(lines[3]).isEqualTo(":b | 2S | 67% | ${Printer.BLOCK_CHAR.toString().repeat(2)}")
            assertThat(lines[4]).isEqualTo(":a | 1S | 33% | ${Printer.BLOCK_CHAR}")
        } else {
            // https://github.com/asarkar/build-time-tracker/discussions/45
            assertThat(lines[3]).isEqualTo(":b | 2S | 100% | ${Printer.BLOCK_CHAR.toString().repeat(2)}")
            assertThat(lines[4]).isEqualTo(":a | 1S |  50% | ${Printer.BLOCK_CHAR}")
        }
    }

    private fun run(rootDir: Path, vararg tasks: String): BuildResult {
        return GradleRunner.create()
            .withProjectDir(rootDir.toFile())
            .withArguments(*tasks, "--warning-mode=all", "--stacktrace")
            .withPluginClasspath()
            .withDebug(false)
            .forwardOutput()
            .build()
    }
}
