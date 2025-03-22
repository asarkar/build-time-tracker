package com.asarkar.gradle.buildtimetracker

import java.io.PrintStream

class ConsolePrinter(
    override val out: PrintStream = System.out,
    override val delimiter: String = " | ",
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
    override val delimiter: String = ",",
) : Printer {
    override fun close() {
        out.close()
    }
}
