/*
 * Copyright (C) 2026 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import Foundation
import Wire
import XCTest
@testable import WireTests

/// Decodes random but mostly well-formed wire bytes, and checks the decode contract.
///
/// `ProtoDecoder.decode` may only throw `ProtoDecoder.Error`, and it must not crash.
/// A message that decodes must also encode again, and the encoded bytes must decode.
/// A crash prints the type, the seed, the index and the input in hex on stderr.
/// Every other failure names the type, the seed and the index, and shows the input in hex.
///
/// The input is deterministic. Each input comes from the base seed and the input index.
/// Two environment variables change a run without a code change:
///
/// - `WIRE_FUZZ_SCALE` multiplies the number of inputs. Use `50` for a long local run, for example
///   `WIRE_FUZZ_SCALE=50 swift test --filter DecodeFuzzTests`.
/// - `WIRE_FUZZ_SEED` replaces the base seed, to explore new inputs.
final class DecodeFuzzTests: XCTestCase {
    private static let inputsPerType = 3_000

    func testDecodingRandomBytesThrowsOnlyProtoDecoderError() {
        FuzzCrashReporter.install()
        defer { FuzzCrashReporter.uninstall() }

        let environment = ProcessInfo.processInfo.environment
        let scale = environment["WIRE_FUZZ_SCALE"].flatMap { Double($0) }.flatMap { $0 > 0 ? $0 : nil } ?? 1.0
        let baseSeed = environment["WIRE_FUZZ_SEED"].flatMap { UInt64($0) } ?? 1
        let inputs = Int(Double(Self.inputsPerType) * scale)

        // The wire type of each field of `AllTypes` follows from its tag.
        // Fields 1 to 18 and the ranges that end in 1 to 18 hold one scalar, enum or message each.
        let scalarWireTypes = [0, 0, 0, 5, 5, 0, 0, 0, 1, 1, 0, 5, 1, 2, 2, 0, 2, 2]
        let arrayWireTypes = [0, 0, 0, 5, 5, 0, 0, 0, 1, 1, 5, 1]
        let allTypes = FuzzSpec(
            tags: Array(1...18) + Array(101...117) + Array(201...218) + Array(301...316)
                + Array(401...416) + Array(501...505) + Array(601...612),
            requiredTags: Array(101...117),
            wireType: { tag, coin in
                switch tag {
                case 1...18, 101...117, 201...218, 401...416: return scalarWireTypes[(tag - 1) % 100]
                case 601...612: return coin == 0 ? 2 : arrayWireTypes[tag - 601]
                default: return 2
                }
            },
            // The enum fields hold the case with the number 1.
            validVarint: { tag in (tag - 1) % 100 == 15 ? 1 : nil }
        )
        // `Person` has required fields 1 (a string) and 2 (an int32).
        let person = FuzzSpec(
            tags: Array(1...9),
            requiredTags: [1, 2],
            wireType: { tag, _ in tag == 1 ? 2 : (tag == 2 ? 0 : nil) }
        )
        let small = FuzzSpec(tags: Array(1...12))

        var failures: [String] = []
        // Messages from the generated code of the test protos.
        failures += fuzz(AllTypes.self, "AllTypes", allTypes, inputs, baseSeed)
        failures += fuzz(Person.self, "Person", person, inputs, baseSeed)
        failures += fuzz(Form.self, "Form", small, inputs, baseSeed)
        failures += fuzz(Screen.self, "Screen", small, inputs, baseSeed)
        failures += fuzz(OneOfMessage.self, "OneOfMessage", small, inputs, baseSeed)
        failures += fuzz(OneOfMessageWithMessages.self, "OneOfMessageWithMessages", small, inputs, baseSeed)
        failures += fuzz(Mappy.self, "Mappy", small, inputs, baseSeed)
        failures += fuzz(MappyTwo.self, "MappyTwo", small, inputs, baseSeed)
        failures += fuzz(ModelEvaluation.self, "ModelEvaluation", small, inputs, baseSeed)
        failures += fuzz(OuterMessage.self, "OuterMessage", small, inputs, baseSeed)
        failures += fuzz(FooBar.self, "FooBar", small, inputs, baseSeed)
        failures += fuzz(SwiftEdgeCases.self, "SwiftEdgeCases", FuzzSpec(tags: Array(1...24)), inputs, baseSeed)
        failures += fuzz(NegativeValueMessage.self, "NegativeValueMessage", small, inputs, baseSeed)
        failures += fuzz(MessageWithStatus.self, "MessageWithStatus", small, inputs, baseSeed)
        // Well-known types from the runtime.
        failures += fuzz(AnyMessage.self, "AnyMessage", small, inputs, baseSeed)
        failures += fuzz(StructMessage.self, "StructMessage", small, inputs, baseSeed)
        failures += fuzz(StructValue.self, "StructValue", small, inputs, baseSeed)
        failures += fuzz(ListValue.self, "ListValue", small, inputs, baseSeed)
        failures += fuzz(FieldMask.self, "FieldMask", small, inputs, baseSeed)
        failures += fuzz(Timestamp.self, "Timestamp", small, inputs, baseSeed)
        failures += fuzz(Wire.Duration.self, "Duration", small, inputs, baseSeed)

        XCTAssertTrue(failures.isEmpty, failures.joined(separator: "\n\n"))
    }

    private func fuzz<T: ProtoCodable>(
        _ messageType: T.Type,
        _ name: String,
        _ spec: FuzzSpec,
        _ inputs: Int,
        _ baseSeed: UInt64
    ) -> [String] {
        var failures: [String: String] = [:]
        var order: [String] = []
        var decoded = 0
        let start = Date()

        func record(_ stage: String, _ error: Error, _ index: Int, _ bytes: [UInt8]) {
            let key = "\(stage) \(type(of: error))"
            guard failures[key] == nil else { return }
            order.append(key)
            let hex = bytes.map { String(format: "%02x", $0) }.joined()
            let shown = hex.count > 8192 ? String(hex.prefix(8192)) + "... (\(bytes.count) bytes in total)" : hex
            failures[key] = "\(name): \(stage) threw \(error)\n  seed=\(baseSeed) index=\(index) (set WIRE_FUZZ_SEED=\(baseSeed) to repeat it)\n  input=\(shown)"
        }

        for index in 0..<inputs {
            var random = FuzzRandom(seed: baseSeed &* 1_000_003 &+ UInt64(index))
            let bytes = spec.next(&random)
            FuzzCrashReporter.begin(name: name, seed: baseSeed, index: index, bytes: bytes)

            // Both strategies for unknown enum values must keep the contract.
            let strategy: ProtoDecoder.UnknownEnumValueDecodingStrategy = index % 2 == 0 ? .throwError : .returnNil
            let decoder = ProtoDecoder(enumDecodingStrategy: strategy)
            let message: T
            do {
                message = try decoder.decode(messageType, from: Data(bytes))
            } catch is ProtoDecoder.Error {
                continue
            } catch {
                record("decode", error, index, bytes)
                continue
            }
            decoded += 1

            do {
                let encoded = try ProtoEncoder().encode(message)
                _ = try decoder.decode(messageType, from: encoded)
            } catch {
                record("encode after decode", error, index, bytes)
            }
        }
        FuzzCrashReporter.end()

        let seconds = Date().timeIntervalSince(start)
        print("DecodeFuzzTests \(name): inputs=\(inputs) decoded=\(decoded) seconds=\(String(format: "%.2f", seconds))")
        if inputs > 0 && decoded == 0 {
            return ["\(name): no generated input decoded, so the fuzzer proves nothing"]
        }
        return order.compactMap { failures[$0] }
    }
}

// MARK: - Input generation

/// A small deterministic generator (SplitMix64), so that a seed repeats an input.
private struct FuzzRandom {
    private var state: UInt64

    init(seed: UInt64) { state = seed &+ 0x9E37_79B9_7F4A_7C15 }

    mutating func next() -> UInt64 {
        state = state &+ 0x9E37_79B9_7F4A_7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58_476D_1CE4_E5B9
        z = (z ^ (z >> 27)) &* 0x94D0_49BB_1331_11EB
        return z ^ (z >> 31)
    }

    mutating func below(_ n: Int) -> Int { Int(next() % UInt64(n)) }
}

/// Describes which field numbers a message type uses, and how to build random bytes for it.
private struct FuzzSpec {
    /// Field numbers to use.
    let tags: [Int]
    /// Field numbers that most inputs hold, so that proto2 required fields are present.
    var requiredTags: [Int] = []
    /// The wire type of a field number, when it is known. The second argument is 0 or 1 at random.
    var wireType: (Int, Int) -> Int? = { _, _ in nil }
    /// A valid varint value for a field number, for example the number of an enum case.
    var validVarint: (Int) -> UInt64? = { _ in nil }

    private static let maxDepth = 6

    func next(_ random: inout FuzzRandom) -> [UInt8] {
        switch random.below(40) {
        case 0: return deepNesting(&random)
        case 1...6: return mutate(&random, message(&random, depth: 0))
        default: return message(&random, depth: 0)
        }
    }

    private func message(_ random: inout FuzzRandom, depth: Int) -> [UInt8] {
        var out: [UInt8] = []
        if depth == 0 {
            for tag in requiredTags where random.below(40) != 0 {
                field(&out, &random, tag: tag, wireType: wireType(tag, random.below(2)) ?? 0, depth: depth)
            }
        }
        for _ in 0..<random.below(8) {
            let tag: Int
            switch random.below(30) {
            case 0: tag = 1 + random.below(1 << 20)
            case 1: tag = (1 << 29) - 1
            case 2: tag = 0
            default: tag = tags[random.below(tags.count)]
            }
            var type = wireType(tag, random.below(2)) ?? random.below(6)
            if random.below(16) == 0 { type = random.below(8) }
            field(&out, &random, tag: tag, wireType: type, depth: depth)
        }
        return out
    }

    private func field(_ out: inout [UInt8], _ random: inout FuzzRandom, tag: Int, wireType: Int, depth: Int) {
        out += varint(UInt64(tag) << 3 | UInt64(wireType))
        switch wireType {
        case 0:
            if let valid = validVarint(tag), random.below(10) != 0 {
                out += varint(valid)
            } else {
                out += varint(varintValue(&random))
            }
        case 1:
            for _ in 0..<(random.below(12) == 0 ? random.below(9) : 8) { out.append(UInt8(truncatingIfNeeded: random.next())) }
        case 5:
            for _ in 0..<(random.below(12) == 0 ? random.below(5) : 4) { out.append(UInt8(truncatingIfNeeded: random.next())) }
        case 2:
            let payload = lengthDelimitedPayload(&random, depth: depth)
            var length = UInt64(payload.count)
            switch random.below(30) {
            case 0: length = length &+ UInt64(1 + random.below(5))
            case 1: length = UInt64(UInt32.max)
            case 2: length = UInt64.max
            case 3: length = 0x8000_0000
            case 4: length = UInt64(Int32.max)
            case 5: if length > 0 { length -= 1 }
            default: break
            }
            out += varint(length)
            out += payload
        case 3:
            // A group: nested fields, then the matching end key.
            out += depth < Self.maxDepth ? message(&random, depth: depth + 1) : []
            if random.below(10) != 0 { out += varint(UInt64(tag) << 3 | 4) }
        default:
            break
        }
    }

    private func lengthDelimitedPayload(_ random: inout FuzzRandom, depth: Int) -> [UInt8] {
        switch random.below(8) {
        case 0, 1: return depth < Self.maxDepth ? message(&random, depth: depth + 1) : []
        case 2: return (0..<random.below(12)).map { _ in UInt8(truncatingIfNeeded: random.next()) }
        case 3, 4: return Array(["hello", "h\u{e9}llo", "\u{1F600}", ""][random.below(4)].utf8)
        case 5: return (0..<random.below(6)).flatMap { _ in varint(varintValue(&random)) }
        case 6: return (0..<random.below(4)).flatMap { _ in (0..<4).map { _ in UInt8(truncatingIfNeeded: random.next()) } }
        default: return []
        }
    }

    private func varintValue(_ random: inout FuzzRandom) -> UInt64 {
        switch random.below(7) {
        case 0: return 0
        case 1: return 1
        case 2: return UInt64.max
        case 3: return UInt64(UInt32.max)
        case 4: return 0x8000_0000
        case 5: return UInt64(random.below(300))
        default: return random.next() >> UInt64(random.below(64))
        }
    }

    /// Nests a message or a group thousands of levels deep, to find recursion limits.
    private func deepNesting(_ random: inout FuzzRandom) -> [UInt8] {
        let tag = UInt64(tags[random.below(tags.count)])
        let levels = [10, 100, 1_000, 5_000][random.below(4)]
        if random.below(2) == 0 {
            return (0..<levels).flatMap { _ in varint(tag << 3 | 3) }
                + (0..<levels).flatMap { _ in varint(tag << 3 | 4) }
        }
        var sizes = [Int](repeating: 0, count: levels + 1)
        for level in 1...levels {
            sizes[level] = varint(tag << 3 | 2).count + varint(UInt64(sizes[level - 1])).count + sizes[level - 1]
        }
        var out: [UInt8] = []
        for level in stride(from: levels, through: 1, by: -1) {
            out += varint(tag << 3 | 2)
            out += varint(UInt64(sizes[level - 1]))
        }
        return out
    }

    private func mutate(_ random: inout FuzzRandom, _ bytes: [UInt8]) -> [UInt8] {
        var bytes = bytes
        for _ in 0..<(1 + random.below(4)) {
            switch random.below(5) {
            case 0 where !bytes.isEmpty: bytes[random.below(bytes.count)] = UInt8(truncatingIfNeeded: random.next())
            case 1 where !bytes.isEmpty: bytes.remove(at: random.below(bytes.count))
            case 2: bytes.insert(UInt8(truncatingIfNeeded: random.next()), at: random.below(bytes.count + 1))
            case 3 where !bytes.isEmpty: bytes = Array(bytes.prefix(random.below(bytes.count)))
            default: if !bytes.isEmpty { bytes[random.below(bytes.count)] ^= UInt8(1 << random.below(8)) }
            }
        }
        return bytes
    }

    private func varint(_ value: UInt64) -> [UInt8] {
        var value = value
        var out: [UInt8] = []
        while value > 0x7f {
            out.append(UInt8(value & 0x7f) | 0x80)
            value >>= 7
        }
        out.append(UInt8(value))
        return out
    }
}

// MARK: - Crash report

/// Remembers the current input. A trap such as an overflow or an index out of range kills the
/// process, so the signal handler prints the input before the process ends.
private enum FuzzCrashReporter {
    private static let capacity = 1 << 16
    nonisolated(unsafe) private static var buffer = UnsafeMutablePointer<UInt8>.allocate(capacity: capacity)
    nonisolated(unsafe) private static var length = 0
    nonisolated(unsafe) private static var index = 0
    nonisolated(unsafe) private static var seed: UInt64 = 0
    nonisolated(unsafe) private static var name = UnsafeMutablePointer<UInt8>.allocate(capacity: 128)
    nonisolated(unsafe) private static var nameLength = 0

    private static let signals = [SIGTRAP, SIGILL, SIGSEGV, SIGBUS, SIGABRT]

    static func install() {
        for signalNumber in signals {
            signal(signalNumber) { received in
                FuzzCrashReporter.report()
                signal(received, SIG_DFL)
                raise(received)
            }
        }
    }

    static func uninstall() {
        for signalNumber in signals { signal(signalNumber, SIG_DFL) }
    }

    static func begin(name typeName: String, seed baseSeed: UInt64, index inputIndex: Int, bytes: [UInt8]) {
        let nameBytes = Array(typeName.utf8.prefix(127))
        for (offset, byte) in nameBytes.enumerated() { name[offset] = byte }
        nameLength = nameBytes.count
        seed = baseSeed
        index = inputIndex
        length = min(bytes.count, capacity)
        for offset in 0..<length { buffer[offset] = bytes[offset] }
    }

    static func end() {
        length = 0
    }

    private static func report() {
        var text = Array("\nDecodeFuzzTests crash: type=".utf8)
        text += UnsafeBufferPointer(start: name, count: nameLength)
        text += Array(" seed=\(seed) index=\(index) bytes=\(length) input=".utf8)
        let digits: [UInt8] = Array("0123456789abcdef".utf8)
        for offset in 0..<length {
            text.append(digits[Int(buffer[offset] >> 4)])
            text.append(digits[Int(buffer[offset] & 15)])
        }
        text.append(10)
        _ = text.withUnsafeBufferPointer { write(2, $0.baseAddress, $0.count) }
    }
}
