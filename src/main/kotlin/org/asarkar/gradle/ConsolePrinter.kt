package org.asarkar.gradle

import java.io.PrintStream
import kotlin.math.round
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class ConsolePrinter(private val out: PrintStream = System.out) : Printer {
    override fun print(input: PrinterInput) {
        // find the maxes needed for formatting
        val maxes = input.taskDurations.fold(Triple(-1, -1.0, -1)) { acc, elem ->
            val maxDuration = maxOf(acc.second, elem.second)
            Triple(maxOf(acc.first, elem.first.length), maxDuration, maxOf(acc.third, formatDuration(maxDuration).length))
        }

        // scale the values to max column width so that the corresponding bars don't shoot out of the screen
        val scalingFraction = minOf(input.ext.maxWidth.toDouble(), maxes.second) / maxes.second
        val maxNumBlocks = round(maxes.second * scalingFraction).toInt()

        out.println("== Build time summary ==")
        input.taskDurations.forEach {
            val numBlocks = round(it.second * scalingFraction).toInt()
            val percent = round(it.second / input.buildDuration * 100).toInt()

            val common = String.format(
                    "%${maxes.first}s | %${maxes.third}s | %s",
                    it.first, formatDuration(it.second), formatPercent(percent)
            )

            if (!input.ext.showBars) {
                out.println(common)
            } else if (input.ext.barPosition == BarPosition.TRAILING) {
                out.printf("%s | %s\n", common, BLOCK_STR.repeat(numBlocks))
            } else {
                out.printf("%${maxNumBlocks}s | %s\n", BLOCK_STR.repeat(numBlocks), common)
            }
        }
    }

    companion object {
        const val BLOCK_STR = "\u2588"

        fun formatDuration(duration: Double): String {
            val separators = setOf('P', 'D', 'T')
            return duration.toDuration(DurationUnit.SECONDS).toIsoString()
                    .filterNot { it in separators }
        }

        fun formatPercent(percent: Int): String = String.format("%2d%%", percent)
    }
}
