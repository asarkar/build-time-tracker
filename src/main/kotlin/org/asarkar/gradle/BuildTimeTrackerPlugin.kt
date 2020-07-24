package org.asarkar.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import kotlin.time.ExperimentalTime

enum class BarPosition {
    LEADING, TRAILING
}

enum class Output {
    CONSOLE
}

open class BuildTimeTrackerPluginExtension {
    var barPosition: BarPosition =
        BarPosition.TRAILING
    var sort: Boolean = false
    var output: Output = Output.CONSOLE
    var maxWidth: Int = 80
    var minTaskDuration: Int = 1
}

class BuildTimeTrackerPlugin : Plugin<Project> {
    @ExperimentalTime
    override fun apply(project: Project) {
        val extension = project.extensions.create(
            "buildTimeTracker", BuildTimeTrackerPluginExtension::class.java
        )
        val timingRecorder = TimingRecorder(extension)
        project.gradle.addListener(timingRecorder)
    }
}