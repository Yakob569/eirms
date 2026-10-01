// swift-tools-version: 5.9
import PackageDescription

// EIRSDomain + EIRSData are plain Swift (Foundation only): they build and test on macOS and Linux
// without Xcode. (Named with an EIRS prefix so they never clash with Foundation's `Data`.)
let package = Package(
    name: "EIRSPayKit",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "EIRSDomain", targets: ["EIRSDomain"]),
        .library(name: "EIRSData", targets: ["EIRSData"]),
    ],
    targets: [
        .target(name: "EIRSDomain"),
        .target(name: "EIRSData", dependencies: ["EIRSDomain"]),
        .testTarget(name: "EIRSDataTests", dependencies: ["EIRSData", "EIRSDomain"]),
    ]
)
