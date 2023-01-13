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
            path: "wire-library/wire-runtime-swift/src/main/swift"
        ),
        // The tests depend on valid protos via gradle
        // ./gradlew -p wire-library :wire-runtime-swift:generateTestProtos
        .testTarget(
            name: "WireTests",
            dependencies: ["Wire"],
            path: "wire-library/wire-runtime-swift/src/test/swift"
        ),
    ],
    swiftLanguageVersions: [.v5]
)
