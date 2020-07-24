package org.asarkar.gradle

import java.io.PrintStream
import kotlin.math.round

class ConsolePrinter(private val out: PrintStream = System.out): Printer {
    override fun print(input: PrinterInput) {
        println("== Build time summary ==")
        // find the max length of a task path and the max value
        val max = input.taskDurations.fold(-1 to -1.0) { acc, elem ->
            maxOf(acc.first, elem.first.length) to maxOf(acc.second, elem.second)
        }

        // scale the values to max column width so that the corresponding bars don't shoot out of the screen
        val scalingFraction = minOf(input.ext.maxWidth.toDouble(), max.second) / max.second
        val maxNumBlocks = round(max.second * scalingFraction).toInt()
        val commonFmtStr = "%${max.first}s | %.3fs | %2d%%"
        input.taskDurations.forEach {
            val numBlocks = round(it.second * scalingFraction).toInt()
            val percent = round(it.second / input.buildDuration * 100).toInt()
            if (input.ext.barPosition == BarPosition.TRAILING) {
                out.printf("$commonFmtStr | %s\n", it.first, it.second, percent, "\u2588".repeat(numBlocks))
            } else {
                out.printf("%${maxNumBlocks}s | $commonFmtStr\n", "\u2588".repeat(numBlocks), it.first, it.second, percent)
            }
        }
    }
}
