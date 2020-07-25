package org.asarkar.gradle

data class PrinterInput(val buildDuration: Double, val taskDurations: List<Pair<String, Double>>, val ext: BuildTimeTrackerPluginExtension)

interface Printer {
    fun print(input: PrinterInput)

    companion object {
        fun newInstance(output: Output): Printer {
            return when (output) {
                Output.CONSOLE -> ConsolePrinter()
            }
        }
    }
}