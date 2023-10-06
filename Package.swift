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
        .testTarget(
            name: "WireCompilerTests",
            dependencies: ["WireTests"],
            path: "wire-tests-swift/no-manifest/src/test/swift"
        ),
    ],
    swiftLanguageVersions: [.v5]
)
