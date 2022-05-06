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
        project.gradle.taskGraph.whenReady {
            val clazz = TimingRecorder::class.java
            val timingRecorder =
                project.gradle.sharedServices.registerIfAbsent(clazz.simpleName, clazz) { spec ->
                    with(spec.parameters) {
                        barPosition.set(ext.barPosition)
                        sort.set(ext.sort)
                        sortBy.set(ext.sortBy)
                        output.set(ext.output)
                        maxWidth.set(ext.maxWidth)
                        minTaskDuration.set(ext.minTaskDuration)
                        showBars.set(ext.showBars)
                        reportsDir.set(ext.reportsDir)
                    }
                }

            registry.onTaskCompletion(timingRecorder)
        }
    }
}
