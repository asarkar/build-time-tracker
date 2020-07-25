package org.asarkar.gradle

import org.asarkar.gradle.ConsolePrinter.Companion.BLOCK_STR
import org.asarkar.gradle.ConsolePrinter.Companion.format
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.stream.Stream


private fun ByteArray.lines(): Sequence<String> {
    val iterator = this.iterator()

    return generateSequence {
        val buffer = mutableListOf<Byte>()
        if (iterator.hasNext()) {
            var next = iterator.next()
            while (next != '\n'.toByte()) {
                buffer.add(next)
                next = iterator.next()
            }
            String(buffer.toByteArray())
        } else {
            null
        }
    }
}

private data class Line(val task: String, val duration: String, val percent: String, val bar: String)

private fun String.toLine(barPosition: BarPosition): Line {
    val tokens = this.split("|")
            .map { it.trim() }
            .takeIf { it.size == 4 } ?: fail("Unexpected line format: $this")
    return if (barPosition == BarPosition.TRAILING) {
        Line(tokens[0], tokens[1], tokens[2], tokens[3])
    } else {
        Line(tokens[1], tokens[2], tokens[3], tokens[0])
    }
}

class ConsolePrinterTest {
    private lateinit var out: ByteArrayOutputStream
    private lateinit var ext: BuildTimeTrackerPluginExtension

    @BeforeEach
    fun beforeEach() {
        out = ByteArrayOutputStream()
        ext = BuildTimeTrackerPluginExtension()
    }

    private val taskDurations = listOf(
            ":commons:extractIncludeProto" to 4.0,
            ":commons:compileKotlin" to 2.0,
            ":commons:compileJava" to 6.0,
            ":service-client:compileKotlin" to 1.0,
            ":webapp:compileKotlin" to 1.0,
            ":webapp:dockerBuildImage" to 4.0,
            ":webapp:dockerPushImage" to 4.0
    )

    @Test
    fun testConsolePrinterDefault() {
        val buildDuration = 28.0

        ConsolePrinter(PrintStream(out)).print(
                PrinterInput(
                        buildDuration,
                        taskDurations,
                        ext
                )
        )

        val iterator = out.toByteArray().lines().withIndex().iterator()
        iterator.next() // ignore summary header
        val line = iterator
                .next()
                .value
                .toLine(ext.barPosition)
        assertThat(line.task).isEqualTo(taskDurations[0].first)
        assertThat(line.duration).isEqualTo("4S")
        assertThat(line.percent).isEqualTo("14%")
        assertThat(line.bar.toCharArray().all { it == '█' }).isTrue()

        iterator.forEach {
            assertThat(it.value.startsWith(taskDurations[it.index - 1].first) && it.value.endsWith(BLOCK_STR))
        }
    }

    @Test
    fun testConsolePrinterLeading() {
        val buildDuration = 28.0
        ConsolePrinter(PrintStream(out)).print(
                PrinterInput(
                        buildDuration,
                        taskDurations,
                        ext.apply { barPosition = BarPosition.LEADING }
                )
        )

        val iterator = out.toByteArray().lines().withIndex().iterator()
        iterator.next() // ignore summary header

        val line = iterator
                .next()
                .value
                .toLine(ext.barPosition)
        assertThat(line.bar.toCharArray().all { it == '█' }).isTrue()
        assertThat(line.duration).isEqualTo("4S")
        assertThat(line.percent).isEqualTo("14%")
        assertThat(line.task).isEqualTo(taskDurations[0].first)

        iterator.forEach {
            assertThat(it.value.endsWith(taskDurations[it.index - 1].first) && it.value.startsWith(BLOCK_STR))
        }
    }

    @Test
    fun testConsolePrinterScaled() {
        ConsolePrinter(PrintStream(out)).print(
                PrinterInput(
                        28.0,
                        taskDurations,
                        ext.apply { maxWidth = 5 }
                )
        )

        out.toByteArray().lines()
                .drop(1) // ignore summary header
                .map { it.toLine(ext.barPosition) }
                .forEach { assertThat(it.bar.length <= ext.maxWidth) }
    }

    @Test
    // https://github.com/asarkar/build-time-tracker/issues/1
    fun testConsolePrinterHideBars() {
        ConsolePrinter(PrintStream(out)).print(
                PrinterInput(
                        28.0,
                        taskDurations,
                        ext.apply { showBars = false }
                )
        )

        assertThat(out.toByteArray().lines()
                .all { !it.contains(BLOCK_STR) }
        )
    }

    @ParameterizedTest
    @EnumSource(BarPosition::class)
    // https://github.com/asarkar/build-time-tracker/issues/3
    fun testFormatting(position: BarPosition) {
        ConsolePrinter(PrintStream(out)).print(
                PrinterInput(
                        18.0,
                        listOf(
                                ":service-client:compileKotlin" to 1.432,
                                "webapp:test" to 13.882
                        ),
                        ext.apply { barPosition = position }
                )
        )

        val lines = out.toByteArray().lines().toList()
        assertThat(lines).hasSize(3)
        val first = lines[1].mapIndexed { i, ch -> if (ch == '|') i else -1 }
                .filterNot { it == -1 }
        val second = lines[2].mapIndexed { i, ch -> if (ch == '|') i else -1 }
                .filterNot { it == -1 }
        assertThat(first).containsExactlyElementsOf(second)
    }

    @Test
    fun testFormattingHideBars() {
        ConsolePrinter(PrintStream(out)).print(
                PrinterInput(
                        18.0,
                        listOf(
                                ":service-client:compileKotlin" to 1.432,
                                "webapp:test" to 13.882
                        ),
                        ext.apply { showBars = false }
                )
        )

        val lines = out.toByteArray().lines().toList()
        assertThat(lines).hasSize(3)
        val first = lines[1].mapIndexed { i, ch -> if (ch == '|') i else -1 }
                .filterNot { it == -1 }
        val second = lines[2].mapIndexed { i, ch -> if (ch == '|') i else -1 }
                .filterNot { it == -1 }
        assertThat(first).containsExactlyElementsOf(second)
    }

    // The following "tests" do not verify anything, but prints the output for manual inspection

    @Test
    fun testConsoleOutputDefault() {
        ConsolePrinter().print(
                PrinterInput(
                        28.0,
                        taskDurations,
                        ext
                )
        )
    }

    @Test
    fun testConsoleOutputLeading() {
        ConsolePrinter().print(
                PrinterInput(
                        28.0,
                        taskDurations,
                        ext.apply { barPosition = BarPosition.LEADING }
                )
        )
    }

    @Test
    fun testConsoleOutputHideBars() {
        ConsolePrinter().print(
                PrinterInput(
                        28.0,
                        taskDurations,
                        ext.apply { showBars = false }
                )
        )
    }


    @ParameterizedTest
    @MethodSource("durationProvider")
    fun testDurationFormatting(duration: Double, formatted: String) {
        assertThat(duration.format()).isEqualTo(formatted)
    }

    companion object {
        @JvmStatic
        fun durationProvider(): Stream<Arguments> {
            return Stream.of(
                    arguments(14, "14S"),
                    arguments(14.23, "14.230S"),
                    arguments(7200, "2H"),
                    arguments(120, "2M"),
                    arguments(3905.3, "1H5M5.300S"),
                    arguments(3900, "1H5M"),
                    arguments(61, "1M1S"),
                    arguments(172800, "48H"), // should be 2D
                    arguments(172859, "48H0M59S")
            )
        }
    }

    @Test
    fun testFormatHundredPercent() {
        ConsolePrinter(PrintStream(out)).print(
                PrinterInput(
                        10.0,
                        listOf(
                                ":service-client:compileKotlin" to 10.0
                        ),
                        ext
                )
        )

        val lines = out.toByteArray().lines().toList()
        assertThat(lines).hasSize(2)
        assertThat(lines.last().toLine(ext.barPosition).percent).isEqualTo("100%")
    }
}