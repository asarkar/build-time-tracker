package com.asarkar.gradle.buildtimetracker

import com.asarkar.gradle.buildtimetracker.Constants.EXTRA_EXTENSION_NAME
import com.asarkar.gradle.buildtimetracker.Constants.LOGGER_KEY
import com.asarkar.gradle.buildtimetracker.Constants.PLUGIN_EXTENSION_NAME
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.api.reflect.TypeOf

class BuildTimeTrackerPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(ReportingBasePlugin::class.java)
        val ext = project.extensions.create(
            PLUGIN_EXTENSION_NAME, BuildTimeTrackerPluginExtension::class.java, project
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
