package com.asarkar.gradle.buildtimetracker

import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.reporting.ReportingExtension
import java.time.Duration

enum class BarPosition {
    LEADING, TRAILING
}

enum class Output {
    CONSOLE, CSV, MARKDOWN
}

enum class Sort {
    ASC, DESC, NONE
}

open class BuildTimeTrackerPluginExtension(private val project: Project) {
    val barPosition: Property<BarPosition> = project.objects.property(BarPosition::class.java)
        .convention(Constants.DEFAULT_BAR_POSITION)

    @Deprecated("Will be removed in v5, use sortBy")
    val sort: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(false)
    val sortBy: Property<Sort> = project.objects.property(Sort::class.java)
        .convention(Constants.DEFAULT_SORT_BY)
    val output: Property<Output> = project.objects.property(Output::class.java)
        .convention(Constants.DEFAULT_OUTPUT)
    val maxWidth: Property<Int> = project.objects.property(Int::class.java)
        .convention(Constants.DEFAULT_MAX_WIDTH)
    val minTaskDuration: Property<Duration> = project.objects.property(Duration::class.java)
        .convention(Duration.ofSeconds(Constants.DEFAULT_MIN_TASK_DURATION))
    val showBars: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(Constants.DEFAULT_SHOW_BARS)
    val reportsDir: DirectoryProperty = project.objects.directoryProperty()
        .convention(baseReportsDir.map { it.dir(Constants.PLUGIN_EXTENSION_NAME) })

    private val baseReportsDir: DirectoryProperty
        get() = project.extensions.getByType(ReportingExtension::class.java)
            .baseDirectory
}
