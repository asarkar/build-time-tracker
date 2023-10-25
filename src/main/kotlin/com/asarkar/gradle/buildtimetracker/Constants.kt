package com.asarkar.gradle.buildtimetracker

object Constants {
    const val PLUGIN_EXTENSION_NAME = "buildTimeTracker"
    val DEFAULT_BAR_POSITION = BarPosition.TRAILING
    val DEFAULT_OUTPUT = Output.CONSOLE
    val DEFAULT_SORT_BY = Sort.NONE
    const val DEFAULT_MAX_WIDTH = 80
    const val DEFAULT_MIN_TASK_DURATION = 1L
    const val DEFAULT_SHOW_BARS = true
    const val CSV_FILENAME = "build.csv"
    const val MARKDOWN_FILENAME = "build.md"
}
