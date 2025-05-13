# build-time-tracker

Gradle plugin that prints the time taken by the tasks in a build.

[![Sponsor](https://img.shields.io/badge/Sponsor-%E2%9D%A4-black?style=for-the-badge&logo=github&color=%23fe8e86)](https://github.com/sponsors/asarkar)
[![CI](<https://img.shields.io/github/actions/workflow/status/asarkar/build-time-tracker/ci.yml?branch=main&style=for-the-badge&logo=github>)](https://github.com/asarkar/build-time-tracker/actions?query=workflow%3A%22CI%22)
[![Gradle](https://img.shields.io/maven-metadata/v/https/plugins.gradle.org/m2/com/asarkar/gradle/build-time-tracker/com.asarkar.gradle.build-time-tracker.gradle.plugin/maven-metadata.xml.svg?style=for-the-badge&label=gradle)](https://plugins.gradle.org/plugin/com.asarkar.gradle.build-time-tracker)
[![License](https://img.shields.io/github/license/asarkar/build-time-tracker?style=for-the-badge&logo=apache&color=brightgreen)](https://www.apache.org/licenses/LICENSE-2.0)
[![COC](https://img.shields.io/badge/COC-Code%20Of%20Conduct-blue?style=for-the-badge&logo=asterisk&logoColor=white)](https://github.com/asarkar/.github/blob/main/CODE_OF_CONDUCT.md)
[![JVM](https://img.shields.io/badge/dynamic/regex?style=for-the-badge&logo=openjdk&color=brightgreen&label=JVM&url=https%3A%2F%2Fraw.githubusercontent.com%2Fasarkar%2Fbuild-time-tracker%2Frefs%2Fheads%2Fmain%2F.java-version&search=%5Cd%2B)](https://github.com/asarkar/build-time-tracker/blob/main/.java-version)

```
== Build time summary ==
 :commons:extractIncludeProto | 4S | 14% | ████
       :commons:compileKotlin | 2S |  7% | ██
         :commons:compileJava | 6S | 21% | ██████
       :service:compileKotlin | 1S |  4% | █
        :webapp:compileKotlin | 1S |  4% | █
     :webapp:dockerBuildImage | 4S | 14% | ████
      :webapp:dockerPushImage | 4S | 14% | ████
```

See [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.asarkar.gradle.build-time-tracker) for usage
instructions.

You can customize the plugin as follows:

```
buildTimeTracker {
    barPosition = BarPosition.TRAILING or BarPosition.LEADING, default is TRAILING
    sortBy = Sort.ASC, Sort.DESC, or Sort.NONE, default is NONE
    output = Output.CONSOLE or Output.CSV, default is CONSOLE
    maxWidth = 120, default is 80
    minTaskDuration = Duration.ofSeconds(1), don't show tasks that take less than a second
    showBars = false or true, default is true
    reportsDir = only relevant if output = CSV, default $buildDir/reports/buildTimeTracker
}
```

> If you are using Kotlin build script, set the configuration properties using `property.set()` method.

> `BarPosition`, `Sort`, and `Output` are enums, so, they need to be imported or fully-qualified with `com.asarkar.gradle.buildtimetracker`.

:information_source: The bars and percentages are rounded off such that the output provides a good indication 
of how long individual tasks took to complete relative to the build, but are not meant to be correct up to the 
milliseconds. [Read this](https://github.com/asarkar/build-time-tracker/discussions/45) for details.

:information_source: It is sufficient to apply the plugin to the root project; also applying to subprojects will result in
duplication of the report.

:warning: If the output terminal does not support UTF-8 encoding, the bars may appear as weird characters. If you are
running Windows, make sure the terminal encoding is set to UTF-8, or turn off the bars as explained above.

:warning: If exporting to CSV, and bars are enabled, the resulting file must be imported as UTF-8 encoded CSV data in
Microsoft Excel. How to do this depends on the Operating System, and Excel version, but
[here](https://answers.microsoft.com/en-us/msoffice/forum/msoffice_excel-mso_mac-mso_365hp/how-to-open-utf-8-csv-file-in-excel-without-mis/1eb15700-d235-441e-8b99-db10fafff3c2)
is one way.
