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
    ],
    swiftLanguageVersions: [.v5]
)
