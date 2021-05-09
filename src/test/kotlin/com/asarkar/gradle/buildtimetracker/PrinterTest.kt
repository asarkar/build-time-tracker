package com.asarkar.gradle.buildtimetracker

import com.asarkar.gradle.buildtimetracker.Printer.Companion.BLOCK_CHAR
import com.asarkar.gradle.buildtimetracker.Printer.Companion.format
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.readLines

private class PrinterWrapper(val output: Output) {
    private val out = ByteArrayOutputStream()
    val delegate = when (output) {
        Output.CONSOLE -> ConsolePrinter(PrintStream(out))
        Output.CSV -> CsvPrinter(PrintStream(out))
    }

    fun lines(): List<String> {
        val list = out.use { baos ->
            String(baos.toByteArray())
                .lines()
                .filter { it.isNotEmpty() }
        }

        if (output == Output.CONSOLE) {
            return list.drop(1)
        }
        return list
    }
}

private data class Line(val task: String, val duration: String, val percent: String, val bar: String)

private fun String.toLine(barPosition: BarPosition, delimiter: String): Line {
    val tokens = this.split(delimiter)
        .map { it.trim() }
        .takeIf { it.size == 4 } ?: fail("Unexpected line format: $this")
    return if (barPosition == BarPosition.TRAILING) {
        Line(tokens[0], tokens[1], tokens[2], tokens[3])
    } else {
        Line(tokens[1], tokens[2], tokens[3], tokens[0])
    }
}

class PrinterTest {
    private lateinit var ext: BuildTimeTrackerPluginExtension
    private lateinit var printerWrapper: PrinterWrapper

    @BeforeEach
    fun beforeEach() {
        ext = BuildTimeTrackerPluginExtension()
    }

    @AfterEach
    fun afterEach() {
        if (this::printerWrapper.isInitialized) {
            printerWrapper.delegate.close()
        }
    }

    private val taskDurations = listOf(
        ":commons:extractIncludeProto" to 4L,
        ":commons:compileKotlin" to 2L,
        ":commons:compileJava" to 6L,
        ":service-client:compileKotlin" to 1L,
        ":webapp:compileKotlin" to 1L,
        ":webapp:dockerBuildImage" to 4L,
        ":webapp:dockerPushImage" to 4L
    )

    @ParameterizedTest
    @EnumSource(Output::class)
    fun testPrintDefault(output: Output) {
        printerWrapper = PrinterWrapper(output)
        printerWrapper.delegate.print(
            PrinterInput(
                28L,
                taskDurations,
                ext
            )
        )

        val lines = printerWrapper.lines()
        val line = lines
            .first()
            .toLine(ext.barPosition, printerWrapper.delegate.delimiter)
        assertThat(line.task).isEqualTo(taskDurations[0].first)
        assertThat(line.duration).isEqualTo("4S")
        assertThat(line.percent).isEqualTo("14%")
        assertThat(line.bar.toCharArray().all { it == BLOCK_CHAR }).isTrue

        lines
            .drop(1)
            .withIndex()
            .forEach {
                assertThat(it.value.startsWith(taskDurations[it.index].first) && it.value.endsWith(BLOCK_CHAR))
            }
    }

    @ParameterizedTest
    @EnumSource(Output::class)
    fun testPrintLeadingBar(output: Output) {
        printerWrapper = PrinterWrapper(output)
        printerWrapper.delegate.print(
            PrinterInput(
                28L,
                taskDurations,
                ext.apply { barPosition = BarPosition.LEADING }
            )
        )

        val lines = printerWrapper.lines()
        val line = lines
            .first()
            .toLine(ext.barPosition, printerWrapper.delegate.delimiter)
        assertThat(line.bar.toCharArray().all { it == BLOCK_CHAR }).isTrue
        assertThat(line.duration).isEqualTo("4S")
        assertThat(line.percent).isEqualTo("14%")
        assertThat(line.task).isEqualTo(taskDurations[0].first)

        lines
            .drop(1)
            .withIndex()
            .forEach {
                assertThat(it.value.endsWith(taskDurations[it.index].first) && it.value.startsWith(BLOCK_CHAR))
            }
    }

    @ParameterizedTest
    @EnumSource(Output::class)
    fun testPrintScaled(output: Output) {
        printerWrapper = PrinterWrapper(output)
        printerWrapper.delegate.print(
            PrinterInput(
                28L,
                taskDurations,
                ext.apply { maxWidth = 5 }
            )
        )

        printerWrapper.lines()
            .forEach {
                val line = it.toLine(ext.barPosition, printerWrapper.delegate.delimiter)
                assertThat(line.bar.length <= ext.maxWidth)
            }
    }

    @ParameterizedTest
    @EnumSource(Output::class)
    // https://github.com/asarkar/build-time-tracker/issues/1
    fun testPrintHideBars(output: Output) {
        printerWrapper = PrinterWrapper(output)
        printerWrapper.delegate.print(
            PrinterInput(
                28L,
                taskDurations,
                ext.apply { showBars = false }
            )
        )

        printerWrapper.lines()
            .forEach {
                assertThat(it).doesNotContain("$BLOCK_CHAR")
            }
    }

    @ParameterizedTest
    @MethodSource("argsProvider")
    // https://github.com/asarkar/build-time-tracker/issues/3
    fun testFormatting(output: Output, position: BarPosition) {
        printerWrapper = PrinterWrapper(output)
        printerWrapper.delegate.print(
            PrinterInput(
                18L,
                listOf(
                    ":service-client:compileKotlin" to 1L,
                    "webapp:test" to 13L
                ),
                ext.apply { barPosition = position }
            )
        )

        val lines = printerWrapper.lines()
        assertThat(lines).hasSize(2)
        val pattern = printerWrapper.delegate.delimiter
            .trim()
            .toRegex(RegexOption.LITERAL)
        val firstDelimiterRanges = pattern.findAll(lines.first())
            .map { it.range.first to it.range.last }
            .toList()
        val secondDelimiterRanges = pattern.findAll(lines.last())
            .map { it.range.first to it.range.last }
            .toList()
        assertThat(firstDelimiterRanges).containsExactlyElementsOf(secondDelimiterRanges)
    }

    @ParameterizedTest
    @EnumSource(Output::class)
    fun testFormattingHideBars(output: Output) {
        printerWrapper = PrinterWrapper(output)
        printerWrapper.delegate.print(
            PrinterInput(
                18L,
                listOf(
                    ":service-client:compileKotlin" to 1L,
                    "webapp:test" to 13L
                ),
                ext.apply { showBars = false }
            )
        )

        val lines = printerWrapper.lines()
        assertThat(lines).hasSize(2)
        val pattern = printerWrapper.delegate.delimiter
            .trim()
            .toRegex(RegexOption.LITERAL)
        val firstDelimiterRanges = pattern.findAll(lines.first())
            .map { it.range.first to it.range.last }
            .toList()
        val secondDelimiterRanges = pattern.findAll(lines.last())
            .map { it.range.first to it.range.last }
            .toList()
        assertThat(firstDelimiterRanges).containsExactlyElementsOf(secondDelimiterRanges)
    }

    @ParameterizedTest
    @EnumSource(Output::class)
    fun testFormatHundredPercent(output: Output) {
        printerWrapper = PrinterWrapper(output)
        printerWrapper.delegate.print(
            PrinterInput(
                10L,
                listOf(
                    ":service-client:compileKotlin" to 10L
                ),
                ext
            )
        )

        val lines = printerWrapper.lines()
        assertThat(lines).hasSize(1)
        assertThat(lines.last().toLine(ext.barPosition, printerWrapper.delegate.delimiter).percent)
            .isEqualTo("100%")
    }

    @ParameterizedTest
    @MethodSource("durationProvider")
    fun testDurationFormatting(duration: Long, formatted: String) {
        assertThat(duration.format()).isEqualTo(formatted)
    }

    @Test
    fun testCreateConsolePrinter() {
        Printer.newInstance(ext)
            .use {
                assertThat(it).isInstanceOf(ConsolePrinter::class.java)
            }
    }

    @OptIn(ExperimentalPathApi::class)
    @Test
    fun testCsvPrinter(@TempDir testProjectDir: Path) {
        val testFilePath = testProjectDir.resolve(ext.csvFilePath.resolveSibling("test.csv"))
        val extCopy = ext.apply {
            output = Output.CSV
            csvFilePath = testFilePath
        }

        Printer.newInstance(extCopy).use { printer ->
            assertThat(printer).isInstanceOf(CsvPrinter::class.java)
            printer.print(
                PrinterInput(
                    28L,
                    taskDurations,
                    extCopy
                )
            )
            assertThat(Files.exists(testFilePath)).isTrue
            val lines = testFilePath.readLines()
            val line = lines
                .first()
                .toLine(ext.barPosition, printer.delimiter)
            assertThat(line.task).isEqualTo(taskDurations[0].first)
            assertThat(line.duration).isEqualTo("4S")
            assertThat(line.percent).isEqualTo("14%")
            assertThat(line.bar.toCharArray().all { it == BLOCK_CHAR }).isTrue

            lines
                .drop(1)
                .withIndex()
                .forEach {
                    assertThat(it.value.startsWith(taskDurations[it.index].first) && it.value.endsWith(BLOCK_CHAR))
                }
        }
    }

    companion object {
        @JvmStatic
        fun argsProvider(): List<Arguments> {
            return Output.values()
                .flatMap { out ->
                    BarPosition.values()
                        .map { pos ->
                            Arguments.arguments(out, pos)
                        }
                }
        }

        @JvmStatic
        fun durationProvider(): List<Arguments> {
            return listOf(
                Arguments.arguments(14, "14S"),
                Arguments.arguments(7200, "2H"),
                Arguments.arguments(120, "2M"),
                Arguments.arguments(3905, "1H5M5S"),
                Arguments.arguments(3900, "1H5M"),
                Arguments.arguments(61, "1M1S"),
                Arguments.arguments(172800, "48H"),
                Arguments.arguments(172859, "48H59S")
            )
        }
    }
}
