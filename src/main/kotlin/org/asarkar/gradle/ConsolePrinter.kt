package org.asarkar.gradle

import java.io.PrintStream
import kotlin.math.round

class ConsolePrinter(private val out: PrintStream = System.out) : Printer {
    override fun print(input: PrinterInput) {
        out.println("== Build time summary ==")
        // find the max length of a task path and the max value
        val max = input.taskDurations.fold(-1 to -1.0) { acc, elem ->
            maxOf(acc.first, elem.first.length) to maxOf(acc.second, elem.second)
        }

        // scale the values to max column width so that the corresponding bars don't shoot out of the screen
        val scalingFraction = minOf(input.ext.maxWidth.toDouble(), max.second) / max.second
        val maxNumBlocks = round(max.second * scalingFraction).toInt()
        val maxDurationLen = formatDuration(max.second).length

        input.taskDurations.forEach {
            val numBlocks = round(it.second * scalingFraction).toInt()
            val percent = round(it.second / input.buildDuration * 100).toInt()

            val common = String.format(
                    "%${max.first}s | %${maxDurationLen}s | %s",
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
        private const val DURATION_FMT_STR = "%.3fs"
        private const val PERCENT_FMT_STR = "%2d%%"

        fun formatDuration(duration: Double): String = String.format(DURATION_FMT_STR, duration)

        fun formatPercent(percent: Int): String = String.format(PERCENT_FMT_STR, percent)
    }
}
