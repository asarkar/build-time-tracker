package org.asarkar.gradle

import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.reflect.TypeOf
import org.gradle.api.tasks.TaskState
import java.time.Duration
import java.time.Instant

class TimingRecorder(val ext: BuildTimeTrackerPluginExtension) : TaskExecutionListener, BuildListener {
    private lateinit var taskStarted: Instant
    private lateinit var buildStarted: Instant
    private val taskDurations = mutableListOf<Pair<String, Long>>()

    override fun beforeExecute(task: Task) {
        taskStarted = Instant.now()
    }

    override fun afterExecute(task: Task, state: TaskState) {
        val duration = Duration.between(taskStarted, Instant.now()).seconds
        if (duration >= ext.minTaskDuration.seconds) {
            taskDurations.add(task.path to duration)
        }
    }

    override fun buildFinished(result: BuildResult) {
        if (taskDurations.isEmpty()) {
            val extra = (ext as ExtensionAware).extensions.getByType(
                    object : TypeOf<Map<String, Any>>() {}
            )
            (extra[Constants.LOGGER_KEY] as Logger).lifecycle(
                    "All tasks completed within the minimum threshold: {}s, no build summary to show",
                    ext.minTaskDuration.seconds
            )
            return
        }
        val buildDuration = Duration.between(buildStarted, Instant.now()).seconds
        if (ext.sort) {
            taskDurations.sortBy { -it.second }
        }
        Printer.newInstance(ext.output)
                .print(PrinterInput(buildDuration, taskDurations, ext))
    }

    override fun settingsEvaluated(settings: Settings) {
    }

    override fun projectsLoaded(gradle: Gradle) {
    }

    override fun buildStarted(gradle: Gradle) {

    }

    override fun projectsEvaluated(gradle: Gradle) {
        buildStarted = Instant.now()
    }
}
