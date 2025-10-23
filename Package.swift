// swift-tools-version:5.3

import PackageDescription

let package = Package(
    name: "Wire",
    defaultLocalization: "en",
    platforms: [
        .iOS(.v10),
        .watchOS(.v3),
        .macOS(.v10_15)
    ],
    products: [
        .library(
            name: "Wire",
            targets: ["Wire"]
        ),
    ],
    targets: [
        .target(
            name: "Wire",
            path: "wire-runtime-swift/src/main/swift"
        ),
        // The tests depend on valid protos via gradle
        // ./gradlew generateSwiftProtos generateSwiftTests
        .testTarget(
            name: "WireRuntimeTests",
            dependencies: ["Wire"],
            path: "wire-runtime-swift/src/test/swift"
        ),
        .target(
            name: "WireTests",
            dependencies: ["Wire"],
            path: "wire-tests-swift/no-manifest/src/main/swift"
        ),
        .target(
            name: "module_address",
            dependencies: ["Wire"],
            path: "wire-tests-swift/manifest/module_address"
        ),
        .target(
            name: "module_location",
            dependencies: ["Wire"],
            path: "wire-tests-swift/manifest/module_location"
        ),
        .target(
            name: "module_one",
            dependencies: ["Wire", "module_address", "module_location"],
            path: "wire-tests-swift/manifest/module_one"
        ),
        .target(
            name: "module_two",
            dependencies: ["Wire", "module_one"],
            path: "wire-tests-swift/manifest/module_two"
        ),
        .target(
            name: "module_three",
            dependencies: ["Wire", "module_one"],
            path: "wire-tests-swift/manifest/module_three"
        ),
        .testTarget(
            name: "WireCompilerTests",
            dependencies: ["WireTests", "module_address", "module_location", "module_one", "module_two", "module_three"],
            path: "wire-tests-swift/no-manifest/src/test/swift"
        ),
    ],
    swiftLanguageVersions: [.v5]
)
