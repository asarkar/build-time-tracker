# build-time-tracker

Like [passy/build-time-tracker-plugin](https://github.com/passy/build-time-tracker-plugin), but actively maintained.
Requires Java 11, because, it's 2020.

[![Build Status](https://github.com/asarkar/build-time-tracker/workflows/CI%20Pipeline/badge.svg)](https://github.com/asarkar/build-time-tracker/actions?query=workflow%3A%22CI+Pipeline%22)

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

Simply declare the plugin in the `plugins` block, and you are good to go:
```
plugins {
  id "org.asarkar.gradle.build-time-tracker" version "1.0-SNAPSHOT"
}
```

If you are the fiddling type, you can customize the plugin as follows:

```
buildTimeTracker {
  barPosition = TRAILING or LEADING, default is TRAILING
  sort = false or true, default is false
  output = CONSOLE, other options are coming
  maxWidth = 80, such that your build logs don't look like Craigslist
  minTaskDuration = 1, don't worry about tasks that take less than a second to execute
}
```

## Contribute

This project is a volunteer effort. You are welcome to send pull requests, ask questions, or create issues.
If you like it, you can help by spreading the word!

## License

Copyright 2020 Abhijit Sarkar - Released under [Apache License v2.0](LICENSE).
