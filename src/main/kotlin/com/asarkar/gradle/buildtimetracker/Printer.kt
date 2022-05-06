package com.asarkar.gradle.buildtimetracker

import java.io.Closeable
import java.io.File
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.nio.file.StandardOpenOption.WRITE
import java.time.Duration
import kotlin.math.round

data class PrinterInput(
    val buildDuration: Long,
    val taskDurations: Collection<Pair<String, Long>>,
    val maxWidth: Int,
    val showBars: Boolean,
    val barPosition: BarPosition
)

interface Printer : Closeable {
    fun print(input: PrinterInput) {
        // find the maxes needed for formatting
        val (maxLabelLen, maxDuration, maxFormattedDurationLen) = input.taskDurations.fold(
            Triple(-1, -1L, -1)
        ) { acc, elem ->
            val maxDuration = maxOf(acc.second, elem.second)
            Triple(maxOf(acc.first, elem.first.length), maxDuration, maxOf(acc.third, maxDuration.format().length))
        }

        // scale the values to max column width so that the corresponding bars don't shoot out of the screen
        val scalingFraction = minOf(input.maxWidth.toLong(), maxDuration) / maxDuration.toDouble()
        val maxNumBlocks = round(maxDuration * scalingFraction).toInt()
        val maxFormattedPercentLen = maxDuration.percentOf(input.buildDuration)
            .format()
            .length

        input.taskDurations.forEach {
            val numBlocks = round(it.second * scalingFraction).toInt()
            val percent = it.second.percentOf(input.buildDuration)

            val common = String.format(
                "%${maxLabelLen}s%s%${maxFormattedDurationLen}s%s%${maxFormattedPercentLen}s",
                it.first, delimiter, it.second.format(), delimiter, percent.format()
            )

            if (!input.showBars) {
                out.println(common)
            } else if (input.barPosition == BarPosition.TRAILING) {
                out.printf("%s%s%s\n", common, delimiter, "$BLOCK_CHAR".repeat(numBlocks))
            } else {
                out.printf("%${maxNumBlocks}s%s%s\n", "$BLOCK_CHAR".repeat(numBlocks), delimiter, common)
            }
        }
    }

    val out: PrintStream
    val delimiter: String

    companion object {
        const val BLOCK_CHAR = '\u2588'

        private fun Long.percentOf(buildDuration: Long): Int = round(this / buildDuration.toDouble() * 100).toInt()
        internal fun Long.format(): String {
            val separators = setOf('P', 'D', 'T')
            return Duration.ofSeconds(this).toString()
                .filterNot { it in separators }
        }

        private fun Int.format(): String = String.format("%d%%", this)

        fun newInstance(output: Output, reportsDir: File): Printer {
            return when (output) {
                Output.CONSOLE -> ConsolePrinter()
                Output.CSV -> {
                    val csvFile = reportsDir
                        .resolve(Constants.CSV_FILENAME)
                    CsvPrinter(newOutputStream(csvFile))
                }
            }
        }

        internal fun newOutputStream(csvFile: File): PrintStream {
            csvFile.parentFile.mkdirs()
            return PrintStream(
                Files.newOutputStream(csvFile.toPath(), CREATE, WRITE, TRUNCATE_EXISTING),
                false,
                StandardCharsets.UTF_8.name()
            )
        }
    }
}
