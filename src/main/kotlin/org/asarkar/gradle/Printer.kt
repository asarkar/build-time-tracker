package org.asarkar.gradle

data class PrinterInput(
        val buildDuration: Long,
        val taskDurations: List<Pair<String, Long>>,
        val ext: BuildTimeTrackerPluginExtension
)

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