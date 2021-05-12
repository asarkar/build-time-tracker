package com.asarkar.gradle.buildtimetracker

object Constants {
    const val PLUGIN_EXTENSION_NAME = "buildTimeTracker"
    const val EXTRA_EXTENSION_NAME = "extra"
    const val LOGGER_KEY = "logger"
    val DEFAULT_BAR_POSITION = BarPosition.TRAILING
    const val DEFAULT_SORT = false
    val DEFAULT_OUTPUT = Output.CONSOLE
    const val DEFAULT_MAX_WIDTH = 80
    const val DEFAULT_MIN_TASK_DURATION = 1L
    const val DEFAULT_SHOW_BARS = true
    const val CSV_FILENAME = "build.csv"
}
