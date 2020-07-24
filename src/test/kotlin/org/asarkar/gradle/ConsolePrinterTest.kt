package org.asarkar.gradle

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.math.round

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
        } else{
            null
        }
    }
}

private data class Line(val task: String, val duration: String, val percent: String, val bar: String)

private fun String.toLine(barPosition: BarPosition): Line {
    val tokens = this.split("|")
        .map{ it.trim() }
        .takeIf { it.size == 4 } ?: fail("Unexpected line format: $this")
    return if (barPosition == BarPosition.TRAILING) {
        Line(tokens[0], tokens[1], tokens[2], tokens[3])
    } else {
        Line(tokens[3], tokens[1], tokens[2], tokens[0])
    }
}

class ConsolePrinterTest {
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
        val out = ByteArrayOutputStream()
        val ext = BuildTimeTrackerPluginExtension()
        ConsolePrinter(PrintStream(out)).print(
            PrinterInput(
                buildDuration,
                taskDurations,
                ext
            )
        )

        val iterator = out.toByteArray().lines().withIndex().iterator()
        val line = iterator
            .next()
            .value
            .toLine(ext.barPosition)
        assertThat(line.task).isEqualTo(taskDurations[0].first)
        assertThat(line.duration).isEqualTo(String.format("%.3fs", taskDurations[0].second))
        assertThat(line.percent).isEqualTo("${round(taskDurations[0].second / buildDuration * 100).toInt()}%")
        assertThat(line.bar.toCharArray().all { it == '█' }).isTrue()

        iterator.forEach {
            assertThat(it.value.startsWith(taskDurations[it.index].first) && it.value.endsWith("\u2588"))
        }
    }

    @Test
    fun testConsolePrinterLeading() {
        val buildDuration = 28.0
        val out = ByteArrayOutputStream()
        val ext = BuildTimeTrackerPluginExtension()
            .apply { barPosition = BarPosition.LEADING }
        ConsolePrinter(PrintStream(out)).print(
            PrinterInput(
                buildDuration,
                taskDurations,
                ext
            )
        )

        val iterator = out.toByteArray().lines().withIndex().iterator()

        val line = iterator
            .next()
            .value
            .toLine(ext.barPosition)
        assertThat(line.bar.toCharArray().all { it == '█' }).isTrue()
        assertThat(line.duration).isEqualTo(taskDurations[0].first)
        assertThat(line.percent).isEqualTo(String.format("%.3fs", taskDurations[0].second))
        assertThat(line.task).isEqualTo("${round(taskDurations[0].second / buildDuration * 100).toInt()}%")

        iterator.forEach {
            assertThat(it.value.endsWith(taskDurations[it.index].first) && it.value.startsWith("\u2588"))
        }
    }

    @Test
    fun testConsolePrinterScaled() {
        val out = ByteArrayOutputStream()
        val ext = BuildTimeTrackerPluginExtension().apply {
            maxWidth = 5
        }
        ConsolePrinter(PrintStream(out)).print(
            PrinterInput(
                28.0,
                taskDurations,
                ext
            )
        )

        out.toByteArray().lines()
            .map { it.toLine(ext.barPosition) }
            .forEach { assertThat(it.bar.length <= ext.maxWidth) }
    }
}