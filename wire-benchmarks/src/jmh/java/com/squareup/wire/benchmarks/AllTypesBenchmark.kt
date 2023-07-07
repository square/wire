/*
 * Copyright (C) 2022 Square, Inc.
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
package com.squareup.wire.benchmarks

import com.squareup.moshi.Moshi
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.WireJsonAdapterFactory
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
import squareup.wire.benchmarks.proto2.AllTypes as Proto2Wire
import squareup.wire.benchmarks.proto2.AllTypesProto2.AllTypes as Proto2Protobuf
import squareup.wire.benchmarks.proto3.AllTypes as Proto3Wire
import squareup.wire.benchmarks.proto3.AllTypesProto3.AllTypes as Proto3Protobuf

@Fork(1)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
open class AllTypesBenchmark {
  private val fileSystem = FileSystem.RESOURCES
  private val buffer = Buffer()
  private lateinit var proto2Bytes: ByteArray
  private lateinit var proto3Bytes: ByteArray
  private lateinit var proto2Protobuf: Proto2Protobuf
  private lateinit var proto2Wire: Proto2Wire
  private lateinit var proto3Protobuf: Proto3Protobuf
  private lateinit var proto3Wire: Proto3Wire
  private lateinit var moshi: Moshi

  @Setup fun setup() {
    moshi = Moshi.Builder()
      .add(WireJsonAdapterFactory())
      .build()

    val proto2Adapter = moshi.adapter(Proto2Wire::class.java)
    proto2Wire = proto2Adapter
      .fromJson(fileSystem.read("all_types_proto2.json".toPath(), BufferedSource::readUtf8))!!
    proto2Bytes = proto2Wire.encode()
    proto2Protobuf = Proto2Protobuf.parseFrom(proto2Bytes)
    proto2Protobuf.hashCode()

    val proto3Adapter = moshi.adapter(Proto3Wire::class.java)
    proto3Wire = proto3Adapter
      .fromJson(fileSystem.read("all_types_proto3.json".toPath(), BufferedSource::readUtf8))!!
    proto3Bytes = proto3Wire.encode()
    proto3Protobuf = Proto3Protobuf.parseFrom(proto3Bytes)
    proto3Protobuf.hashCode()
  }

  @Benchmark fun proto2EncodeWire3x() {
    val writer = ProtoWriter(buffer)
    Proto2Wire.ADAPTER.encode(writer, proto2Wire)
    buffer.clear()
  }

  @Benchmark fun proto2EncodeWire4x() {
    val writer = ReverseProtoWriter()
    Proto2Wire.ADAPTER.encode(writer, proto2Wire)
    writer.writeTo(buffer)
    buffer.clear()
  }

  @Benchmark fun proto2EncodeProtobuf() {
    proto2Protobuf.writeTo(buffer.outputStream())
    buffer.clear()
  }

  @Benchmark fun proto2DecodeWire() {
    Proto2Wire.ADAPTER.decode(proto2Bytes)
  }

  @Benchmark fun proto2DecodeProtobufLazyOnly() {
    Proto2Protobuf.parseFrom(proto2Bytes)
  }

  @Benchmark fun proto2DecodeProtobuf() {
    val message = Proto2Protobuf.parseFrom(proto2Bytes)
    message.hashCode()
  }

  @Benchmark fun proto3EncodeWire3x() {
    val writer = ProtoWriter(buffer)
    Proto3Wire.ADAPTER.encode(writer, proto3Wire)
    buffer.clear()
  }

  @Benchmark fun proto3EncodeWire4x() {
    val writer = ReverseProtoWriter()
    Proto3Wire.ADAPTER.encode(writer, proto3Wire)
    writer.writeTo(buffer)
    buffer.clear()
  }

  @Benchmark fun proto3EncodeProtobuf() {
    proto3Protobuf.writeTo(buffer.outputStream())
    buffer.clear()
  }

  @Benchmark fun proto3DecodeWire() {
    Proto3Wire.ADAPTER.decode(proto3Bytes)
  }

  @Benchmark fun proto3DecodeProtobufLazyOnly() {
    Proto3Protobuf.parseFrom(proto3Bytes)
  }

  @Benchmark fun proto3DecodeProtobuf() {
    val message = Proto3Protobuf.parseFrom(proto3Bytes)
    message.hashCode()
  }
}

// We keep this method around for profiling.
fun main() {
  val now = System.nanoTime()
  val done = now + TimeUnit.SECONDS.toNanos(10L)
  val benchmark = AllTypesBenchmark()
  benchmark.setup()
  while (System.nanoTime() < done) {
    println(".")
    for (i in 0 until 1000000) {
      benchmark.proto2DecodeWire()
    }
  }
}
