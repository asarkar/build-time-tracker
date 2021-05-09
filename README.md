# build-time-tracker

Like [passy/build-time-tracker-plugin](https://github.com/passy/build-time-tracker-plugin), but actively maintained.
Requires Java 8 or later.

[![](https://github.com/asarkar/build-time-tracker/workflows/CI%20Pipeline/badge.svg)](https://github.com/asarkar/build-time-tracker/actions?query=workflow%3A%22CI+Pipeline%22)

```
== Build time summary ==
 :commons:extractIncludeProto | 4S | 14% | ████
       :commons:compileKotlin | 2S |  7% | ██
         :commons:compileJava | 6S | 21% | ██████
:service-client:compileKotlin | 1S |  4% | █
        :webapp:compileKotlin | 1S |  4% | █
     :webapp:dockerBuildImage | 4S | 14% | ████
      :webapp:dockerPushImage | 4S | 14% | ████
```

See [Gradle Plugin Portal](https://plugins.gradle.org/plugin/com.asarkar.gradle.build-time-tracker) for usage 
instructions.

If you are the fiddling type, you can customize the plugin as follows:

```
import com.asarkar.gradle.BuildTimeTrackerPluginExtension
// bunch of code
configure<BuildTimeTrackerPluginExtension> { // or buildTimeTracker {...}, for Groovy
    barPosition = TRAILING or LEADING, default is TRAILING
    sort = false or true, default is false
    output = CONSOLE, other options may be added in the future
    maxWidth = 80, so that your build logs don't look like Craigslist
    minTaskDuration = Duration.ofSeconds(1), don't worry about tasks that take less than a second to execute
    showBars = false or true, default is true
}
```

:information_source: Due to a [Gradle limitation](https://docs.gradle.org/6.5.1/userguide/upgrading_version_5.html#apis_buildlistener_buildstarted_and_gradle_buildstarted_have_been_deprecated),
the build duration can't be calculated precisely.
The bars and percentages are rounded off such that the output provides a good indication of how long individual
tasks took to complete relative to the build, but are not meant to be correct up to the milliseconds.

:information_source: It is sufficient to apply the plugin to the root project; applying to subprojects will result 
in duplication of the report.

:warning: If the output console does not support UTF-8 encoding, the bars may appear as weird characters. If you are 
running Windows, make sure the terminal encoding is set to UTF-8, or turn off the bars as explained above.

## Contribute

This project is a volunteer effort. You are welcome to send pull requests, ask questions, or create issues. If you like
it, you can help by spreading the word and "Starring" the GitHub repo!

## License

Copyright 2021 Abhijit Sarkar - Released under [Apache License v2.0](LICENSE).
