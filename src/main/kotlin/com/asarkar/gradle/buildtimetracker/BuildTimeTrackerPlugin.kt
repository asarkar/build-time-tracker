package com.asarkar.gradle.buildtimetracker

import com.asarkar.gradle.buildtimetracker.Constants.PLUGIN_EXTENSION_NAME
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ReportingBasePlugin
import org.gradle.build.event.BuildEventsListenerRegistry
import javax.inject.Inject

class BuildTimeTrackerPlugin
    @Inject
    constructor(
        private val registry: BuildEventsListenerRegistry,
    ) : Plugin<Project> {
        override fun apply(project: Project) {
            if (project != project.rootProject) {
                project.logger.warn(
                    "build-time-tracker should be applied to the root project only; also applied to ${project.path}.",
                )
            }
            project.pluginManager.apply(ReportingBasePlugin::class.java)
            val ext =
                project.extensions.create(
                    PLUGIN_EXTENSION_NAME,
                    BuildTimeTrackerPluginExtension::class.java,
                    project,
                )
            val clazz = TimingRecorder::class.java
            val timingRecorder =
                project.gradle.sharedServices.registerIfAbsent(clazz.simpleName, clazz) {
                    with(parameters) {
                        barPosition.set(ext.barPosition)
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
