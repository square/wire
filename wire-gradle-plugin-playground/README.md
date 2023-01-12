## Wire Gradle Plugin Playground

This module is to allow a simple usage of the Wire Gradle plugin for debug purposes. This is useful
when developing a new feature on the plugin or debugging a bug. Unit tests should not live in this
module but in the Wire Gradle plugin own module' test suite.

The `protos.jar` file under `src/main/proto` contains two files:
```
squareup/geology/period.proto
squareup/dinosaurs/dinosaur.proto
```
