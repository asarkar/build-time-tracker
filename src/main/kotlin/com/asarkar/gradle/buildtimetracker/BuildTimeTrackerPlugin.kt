package com.asarkar.gradle.buildtimetracker

import com.asarkar.gradle.buildtimetracker.Constants.PLUGIN_EXTENSION_NAME
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

@Suppress("UnstableApiUsage")
class BuildTimeTrackerPlugin @Inject constructor(private val registry: BuildEventsListenerRegistry) : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager.apply(ReportingBasePlugin::class.java)
        val ext = project.extensions.create(
            PLUGIN_EXTENSION_NAME, BuildTimeTrackerPluginExtension::class.java, project
        )
        val clazz = TimingRecorder::class.java
        val timingRecorder =
            project.gradle.sharedServices.registerIfAbsent(clazz.simpleName, clazz) { spec ->
                val params = BuildTimeTrackerPluginParams(ext.reportsDir.get().asFile)
                spec.parameters.getParams().set(params)
            }

        project.gradle.projectsEvaluated {
            copyParams(ext, timingRecorder.get().parameters.getParams().get())
        }

        registry.onTaskCompletion(timingRecorder)
    }

    private fun copyParams(src: BuildTimeTrackerPluginExtension, dest: BuildTimeTrackerPluginParams) {
        dest.barPosition = src.barPosition.get()
        dest.sort = src.sort.get()
        dest.output = src.output.get()
        dest.maxWidth = src.maxWidth.get()
        dest.minTaskDuration = src.minTaskDuration.get()
        dest.showBars = src.showBars.get()
        dest.reportsDir = src.reportsDir.get().asFile
    }
}
