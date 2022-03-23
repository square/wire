package com.squareup.wire.benchmarks

import com.google.protobuf.benchmarks.proto2.BenchmarkMessage1Proto2 as Message1Proto2Protobuf
import com.google.protobuf.benchmarks.proto2.GoogleMessage1 as Message1Proto2Wire
import com.google.protobuf.benchmarks.proto3.BenchmarkMessage1Proto3 as Message1Proto3Protobuf
import com.google.protobuf.benchmarks.proto3.GoogleMessage1 as Message1Proto3Wire
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import java.util.concurrent.TimeUnit
import okio.Buffer
import okio.BufferedSource
import okio.FileSystem
import okio.Path.Companion.toPath
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.Setup
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup

@Fork(1)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class GoogleMessageBenchmark {
  private val fileSystem = FileSystem.RESOURCES
  private val buffer = Buffer()
  private lateinit var message1Proto2Bytes: ByteArray
  private lateinit var message1Proto3Bytes: ByteArray
  private lateinit var message1Proto2Protobuf: Message1Proto2Protobuf.GoogleMessage1
  private lateinit var message1Proto2Wire: Message1Proto2Wire
  private lateinit var message1Proto3Protobuf: Message1Proto3Protobuf.GoogleMessage1
  private lateinit var message1Proto3Wire: Message1Proto3Wire

  @Setup fun setup() {
    message1Proto2Bytes = fileSystem.read("dataset.google_message1_proto2.bytes".toPath(), BufferedSource::readByteArray)
    message1Proto2Protobuf = Message1Proto2Protobuf.GoogleMessage1.parseFrom(message1Proto2Bytes)
    message1Proto2Wire = Message1Proto2Wire.ADAPTER.decode(message1Proto2Bytes)
    message1Proto3Bytes = fileSystem.read("dataset.google_message1_proto3.bytes".toPath(), BufferedSource::readByteArray)
    message1Proto3Protobuf = Message1Proto3Protobuf.GoogleMessage1.parseFrom(message1Proto3Bytes)
    message1Proto3Wire = Message1Proto3Wire.ADAPTER.decode(message1Proto3Bytes)
  }

  @Benchmark fun message1Proto2EncodeWire3x() {
    val writer = ProtoWriter(buffer)
    Message1Proto2Wire.ADAPTER.encode(writer, message1Proto2Wire)
    buffer.clear()
  }

  @Benchmark fun message1Proto2EncodeWire4x() {
    val writer = ReverseProtoWriter()
    Message1Proto2Wire.ADAPTER.encode(writer, message1Proto2Wire)
    writer.writeTo(buffer)
    buffer.clear()
  }

  @Benchmark fun message1Proto2EncodeProtobuf() {
    message1Proto2Protobuf.writeTo(buffer.outputStream())
    buffer.clear()
  }

  @Benchmark fun message1Proto2DecodeWire() {
    Message1Proto2Wire.ADAPTER.decode(message1Proto2Bytes)
  }

  @Benchmark fun message1Proto2DecodeProtobuf() {
    Message1Proto2Protobuf.GoogleMessage1.parseFrom(message1Proto2Bytes)
  }

  @Benchmark fun message1Proto3EncodeWire3x() {
    val writer = ProtoWriter(buffer)
    Message1Proto3Wire.ADAPTER.encode(writer, message1Proto3Wire)
    buffer.clear()
  }

  @Benchmark fun message1Proto3EncodeWire4x() {
    val writer = ReverseProtoWriter()
    Message1Proto3Wire.ADAPTER.encode(writer, message1Proto3Wire)
    writer.writeTo(buffer)
    buffer.clear()
  }

  @Benchmark fun message1Proto3EncodeProtobuf() {
    message1Proto3Protobuf.writeTo(buffer.outputStream())
    buffer.clear()
  }

  @Benchmark fun message1Proto3DecodeWire() {
    Message1Proto3Wire.ADAPTER.decode(message1Proto3Bytes)
  }

  @Benchmark fun message1Proto3DecodeProtobuf() {
    Message1Proto3Protobuf.GoogleMessage1.parseFrom(message1Proto3Bytes)
  }
}
