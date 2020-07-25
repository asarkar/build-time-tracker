# build-time-tracker

Like [passy/build-time-tracker-plugin](https://github.com/passy/build-time-tracker-plugin), but actively maintained.
Requires Java 11 or later, because, it's 2020.

[![](https://github.com/asarkar/build-time-tracker/workflows/CI%20Pipeline/badge.svg)](https://github.com/asarkar/build-time-tracker/actions?query=workflow%3A%22CI+Pipeline%22)

```
== Build time summary ==
 :commons:extractIncludeProto | 4.000s | 14% | ████
       :commons:compileKotlin | 2.000s |  7% | ██
         :commons:compileJava | 6.000s | 21% | ██████
:service-client:compileKotlin | 1.000s |  4% | █
        :webapp:compileKotlin | 1.000s |  4% | █
     :webapp:dockerBuildImage | 4.000s | 14% | ████
      :webapp:dockerPushImage | 4.000s | 14% | ████
```

> Due to a [Gradle limitation](https://docs.gradle.org/6.5.1/userguide/upgrading_version_5.html#apis_buildlistener_buildstarted_and_gradle_buildstarted_have_been_deprecated), the build duration can't be calculated precisely.
Thus, the bars and percentages provide a good indication of how long individual tasks took to complete relative to the build,
but are not meant to be correct upto the 9th decimal place.

See [Gradle Plugin Portal](https://plugins.gradle.org/plugin/org.asarkar.gradle.build-time-tracker) for usage instructions.

If you are the fiddling type, you can customize the plugin as follows:

```
buildTimeTracker {
    barPosition = TRAILING or LEADING, default is TRAILING
    sort = false or true, default is false
    output = CONSOLE, other options may be added in the future
    maxWidth = 80, so that your build logs don't look like Craigslist
    minTaskDuration = 1, don't worry about tasks that take less than a second to execute
    showBars = false or true, default is true
}
```

## Contribute

This project is a volunteer effort. You are welcome to send pull requests, ask questions, or create issues.
If you like it, you can help by spreading the word!

## License

Copyright 2020 Abhijit Sarkar - Released under [Apache License v2.0](LICENSE).
