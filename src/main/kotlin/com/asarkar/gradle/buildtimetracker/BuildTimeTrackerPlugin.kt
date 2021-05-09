package com.asarkar.gradle.buildtimetracker

import com.asarkar.gradle.buildtimetracker.Constants.EXTRA_EXTENSION_NAME
import com.asarkar.gradle.buildtimetracker.Constants.LOGGER_KEY
import com.asarkar.gradle.buildtimetracker.Constants.PLUGIN_EXTENSION_NAME
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.reflect.TypeOf
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Duration

enum class BarPosition {
    LEADING, TRAILING
}

enum class Output {
    CONSOLE, CSV
}

open class BuildTimeTrackerPluginExtension {
    var barPosition: BarPosition = BarPosition.TRAILING
    var sort: Boolean = false
    var output: Output = Output.CONSOLE
    var maxWidth: Int = 80
    var minTaskDuration: Duration = Duration.ofSeconds(1)
    var showBars: Boolean = true
    var csvFilePath: Path = Paths.get("build")
        .resolve("reports")
        .resolve(PLUGIN_EXTENSION_NAME)
        .resolve("build.csv")
}

class BuildTimeTrackerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val ext = project.extensions.create(
            PLUGIN_EXTENSION_NAME, BuildTimeTrackerPluginExtension::class.java
        )
        (ext as ExtensionAware).extensions.add(
            object : TypeOf<Map<String, Any>>() {},
            EXTRA_EXTENSION_NAME,
            mapOf<String, Any>(LOGGER_KEY to project.logger)
        )
        val timingRecorder = TimingRecorder(ext)
        project.gradle.addListener(timingRecorder)
    }
}
