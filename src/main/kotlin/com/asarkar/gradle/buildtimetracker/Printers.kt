package com.asarkar.gradle.buildtimetracker

import java.io.PrintStream

class ConsolePrinter(
    override val out: PrintStream = System.out,
    override val delimiter: String = " | "
) : Printer {
    override fun print(input: PrinterInput) {
        out.println("== Build time summary ==")
        super.print(input)
    }

    override fun close() {
        // Do nothing
    }
}

class CsvPrinter(
    override val out: PrintStream,
    override val delimiter: String = ","
) : Printer {
    override fun close() {
        out.close()
    }
}

class MarkdownPrinter(
    override val out: PrintStream,
    override val delimiter: String = " | "
) : Printer {

    override fun prefix(): String = "| "
    override fun suffix(): String = " |"

    override fun print(input: PrinterInput) {
        out.println("# Build time summary")
        if (!input.showBars) {
            out.println("| Task name | Time | Percentage |")
            out.println("|:----------|-----:|-----------:|")
        } else if (input.barPosition == BarPosition.TRAILING) {
            out.println("| Task name | Time | Percentage | Bar |")
            out.println("|:----------|-----:|-----------:|----:|")
        } else {
            out.println("| Bar | Task name | Time | Percentage |")
            out.println("|----:|:----------|-----:|-----------:|")
        }
        super.print(input)
    }

    override fun close() {
        // Do nothing
    }
}