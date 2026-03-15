package com.asarkar.gradle.buildtimetracker

import org.gradle.api.logging.Logging
import java.io.PrintStream

class ConsolePrinter(
    private val lineSink: (String) -> Unit = { LOGGER.quiet(it) },
    override val delimiter: String = " | ",
) : Printer {
    override fun print(input: PrinterInput) {
        emit("== Build time summary ==")
        super.print(input)
    }

    override fun emit(line: String) {
        lineSink(line)
    }

    override fun close() {
        // Do nothing
    }

    private companion object {
        val LOGGER = Logging.getLogger(ConsolePrinter::class.java)
    }
}

class CsvPrinter(
    private val out: PrintStream,
    override val delimiter: String = ",",
) : Printer {
    override fun emit(line: String) {
        out.println(line)
    }

    override fun close() {
        out.close()
    }
}
