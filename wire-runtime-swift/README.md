# Swift Wire Runtime

## Developing

### Developing in Xcode

To develop the runtime or run tests using Xcode, ensure you have the following dependencies installed:

- Xcode
- Ruby
- The [`CocoaPods` Ruby gem](https://guides.cocoapods.org/using/getting-started.html)
- The [`cocoapods-generate` Ruby gem](https://github.com/square/cocoapods-generate)

Then, run the following commands from the root of the repo:

```
pod gen ./Wire.podspec
open gen/Wire/Wire.xcworkspace
```

You can also just `open Package.swift`.

### Codegen

To generate all swift protos, there are a few different gradle jobs:

```
./gradlew generateSwiftProtos generateSwiftTests
```

This implicitly will happen as part of the `Running Tests On the Command Line`

### Running Tests On the Command Line

To build the runtime and run the [Swift tests](./src/test) from the command line using Gradle,
run the following command **from the root of the repo**:

```
./gradlew :wire-runtime-swift:build
```

### Measuring Performance

In order to ensure that Wire is performant and stays performant, there are performance tests included in the test suite. For any major change to the runtime that has the potential to impact Wire's speed of encoding/decoding, run the test suite and include the corresponding numbers as part of the change's commit comment.

To run the performance tests:

- Close as many other programs on your computer as possible.
- Enable the tests in `./wire-runtime-swift/src/test/swift/PerformanceTests.swift` by changing `#if false` to `#if true`. The tests are disabled by default so that they don't run on CI.
- Run the tests once without your new changes in order to establish a baseline. Baselines are relative to your computer and are not checked in.
- Use Xcode to actually set these values as the baseline for each test. This can be done by tapping the gray diamond next to the "No baseline set" message for each test, then tapping the gray diamond again to bring up the Baseline popover, then tapping "Set Baseline". This should be done for each test in the file.
- Make your changes.
- Re-run the tests and compare the results. A variance of up to 10% isn't uncommon. If numbers are varying more than that on each run, trying closing more programs, disabling WiFi, or otherwise limiting outside influences.

Include the baseline metrics as well as the updated metrics in the commit message.
