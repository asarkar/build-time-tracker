package com.asarkar.gradle.buildtimetracker

import org.gradle.BuildAdapter
import org.gradle.BuildResult
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.invocation.Gradle
import org.gradle.api.logging.Logger
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.reflect.TypeOf
import org.gradle.api.tasks.TaskState
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

class TimingRecorder(private val ext: BuildTimeTrackerPluginExtension) : TaskExecutionListener, BuildAdapter() {
    private val taskStartTimings: MutableMap<String, Instant> = ConcurrentHashMap()
    private val taskDurations: MutableCollection<Pair<String, Long>> = ConcurrentLinkedQueue()
    private lateinit var buildStarted: Instant

    override fun beforeExecute(task: Task) {
        taskStartTimings[task.path] = Instant.now()
    }

    override fun afterExecute(task: Task, state: TaskState) {
        check(taskStartTimings.contains(task.path)) { "No start timing for task ${task.path}" }
        val duration = Duration.between(taskStartTimings[task.path], Instant.now()).seconds
        if (duration >= ext.minTaskDuration.get().seconds) {
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
                ext.minTaskDuration.get().seconds
            )
            return
        }
        val buildDuration = Duration.between(buildStarted, Instant.now()).seconds
        Printer.newInstance(ext)
            .use { printer ->
                val input = PrinterInput(
                    buildDuration,
                    if (ext.sort.get()) taskDurations.sortedBy { -it.second } else taskDurations,
                    ext.maxWidth.get(),
                    ext.showBars.get(),
                    ext.barPosition.get()
                )
                printer.print(input)
            }
    }

    override fun projectsEvaluated(gradle: Gradle) {
        buildStarted = Instant.now()
    }
}
