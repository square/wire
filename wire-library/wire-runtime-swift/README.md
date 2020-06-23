# Swift Wire Runtime

## Developing

### Developing in Xcode

To develop the runtime or run tests using Xcode, run the following commands from the root of the rep:

```
pod gen ./Wire.podspec
open gen/Wire/Wire.xcworkspace
```

For these commands to work you'll need the following installed:

- Ruby
- The CocoaPods Ruby gem
- Xcode

### Running Tests On the Command Line

To build the runtime and run tests from the command line using Gradle, run the following command from the root of the repo:

```
./gradlew -p wire-library :wire-runtime-swift:build
```

