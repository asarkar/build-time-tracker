package com.asarkar.gradle

import com.asarkar.gradle.Constants.EXTRA_EXTENSION_NAME
import com.asarkar.gradle.Constants.LOGGER_KEY
import com.asarkar.gradle.Constants.PLUGIN_EXTENSION_NAME
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.reflect.TypeOf
import java.time.Duration

enum class BarPosition {
    LEADING, TRAILING
}

enum class Output {
    CONSOLE
}

open class BuildTimeTrackerPluginExtension {
    var barPosition: BarPosition = BarPosition.TRAILING
    var sort: Boolean = false
    var output: Output = Output.CONSOLE
    var maxWidth: Int = 80
    var minTaskDuration: Duration = Duration.ofSeconds(1)
    var showBars: Boolean = true
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
