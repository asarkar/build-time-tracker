package com.asarkar.gradle.buildtimetracker

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

@Suppress("UnstableApiUsage")
abstract class TimingRecorder : BuildService<TimingRecorder.Params>, OperationCompletionListener, AutoCloseable {
    interface Params : BuildServiceParameters {
        val barPosition: Property<BarPosition>
        val sort: Property<Boolean>
        val sortBy: Property<Sort>
        val output: Property<Output>
        val maxWidth: Property<Int>
        val minTaskDuration: Property<Duration>
        val showBars: Property<Boolean>
        val reportsDir: DirectoryProperty
    }

    private val taskDurations: MutableCollection<Pair<String, Long>> = ConcurrentLinkedQueue()
    private val buildStart = AtomicReference(Instant.EPOCH)

    override fun onFinish(event: FinishEvent) {
        if (event is TaskFinishEvent) {
            val eventStart = Instant.ofEpochMilli(event.result.startTime)
            buildStart.accumulateAndGet(eventStart) { curr, newVal ->
                if (curr == Instant.EPOCH) newVal else minOf(curr, newVal)
            }
            val duration = Duration.ofMillis(event.result.endTime - event.result.startTime).seconds
            if (duration >= parameters.minTaskDuration.get().seconds) {
                taskDurations.add(event.descriptor.taskPath to duration)
            }
        }
    }

    override fun close() {
        if (taskDurations.isEmpty()) {
            return
        }

        val buildDuration = Duration.between(buildStart.get(), Instant.now()).seconds
        Printer.newInstance(parameters.output.get(), parameters.reportsDir.get().asFile)
            .use { printer ->
                val sort: Sort = if (parameters.sort.get()) Sort.DESC else parameters.sortBy.get()
                val durations = when (sort) {
                    Sort.NONE -> taskDurations
                    Sort.DESC -> taskDurations.sortedBy { -it.second }
                    Sort.ASC -> taskDurations.sortedBy { it.second }
                }
                val input = PrinterInput(
                    buildDuration,
                    durations,
                    parameters.maxWidth.get(),
                    parameters.showBars.get(),
                    parameters.barPosition.get()
                )
                printer.print(input)
            }
    }
}
