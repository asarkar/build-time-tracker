package org.asarkar.gradle

import java.io.PrintStream
import kotlin.math.round
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime
import kotlin.time.toDuration

class ConsolePrinter(private val out: PrintStream = System.out) : Printer {
    @ExperimentalTime
    override fun print(input: PrinterInput) {
        // find the maxes needed for formatting
        val (maxLabelLen, maxDuration, maxFormattedDurationLen) = input.taskDurations.fold(
                Triple(-1, -1L, -1)) { acc, elem ->
            val maxDuration = maxOf(acc.second, elem.second)
            Triple(maxOf(acc.first, elem.first.length), maxDuration, maxOf(acc.third, maxDuration.format().length))
        }

        // scale the values to max column width so that the corresponding bars don't shoot out of the screen
        val scalingFraction = minOf(input.ext.maxWidth.toLong(), maxDuration) / maxDuration.toDouble()
        val maxNumBlocks = round(maxDuration * scalingFraction).toInt()
        val maxFormattedPercentLen = maxDuration.percentOf(input.buildDuration)
                .format()
                .length

        out.println("== Build time summary ==")
        input.taskDurations.forEach {
            val numBlocks = round(it.second * scalingFraction).toInt()
            val percent = it.second.percentOf(input.buildDuration)

            val common = String.format(
                    "%${maxLabelLen}s | %${maxFormattedDurationLen}s | %${maxFormattedPercentLen}s",
                    it.first, it.second.format(), percent.format()
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

        private fun Long.percentOf(buildDuration: Long): Int = round(this / buildDuration.toDouble() * 100).toInt()
        @ExperimentalTime
        internal fun Long.format(): String {
            val separators = setOf('P', 'D', 'T')
            return this.toDuration(DurationUnit.SECONDS).toIsoString()
                    .filterNot { it in separators }
        }
        private fun Int.format(): String = String.format("%d%%", this)
    }
}
