package com.asarkar.gradle.buildtimetracker

import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue

@Suppress("UnstableApiUsage")
abstract class TimingRecorder : BuildService<TimingRecorder.Params>, OperationCompletionListener, AutoCloseable {
    interface Params : BuildServiceParameters {
        fun getParams(): Property<BuildTimeTrackerPluginParams>
    }

    private val taskDurations: MutableCollection<Pair<String, Long>> = ConcurrentLinkedQueue()
    private val buildStarted: Instant = Instant.now()

    override fun onFinish(event: FinishEvent) {
        if (event is TaskFinishEvent) {
            val params = parameters.getParams().get()
            val duration = Duration.ofMillis(event.result.endTime - event.result.startTime).seconds
            if (duration >= params.minTaskDuration.seconds) {
                taskDurations.add(event.descriptor.taskPath to duration)
            }
        }
    }

    override fun close() {
        if (taskDurations.isEmpty()) {
            return
        }
        val params = parameters.getParams().get()
        val buildDuration = Duration.between(buildStarted, Instant.now()).seconds
        Printer.newInstance(params)
            .use { printer ->
                val durations = when (params.sortBy) {
                    Sort.NONE -> taskDurations
                    Sort.DESC -> taskDurations.sortedBy { -it.second }
                    Sort.ASC -> taskDurations.sortedBy { it.second }
                }
                val input = PrinterInput(
                    buildDuration,
                    durations,
                    params.maxWidth,
                    params.showBars,
                    params.barPosition
                )
                printer.print(input)
            }
    }
}
