/*
 * Copyright (C) 2021 Square, Inc.
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
package com.squareup.wire.benchmarks;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static org.openjdk.jmh.annotations.Mode.SampleTime;

import com.squareup.wire.ProtoWriter;
import com.squareup.wire.ReverseProtoWriter;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;
import okio.Source;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import squareup.wire.benchmarks.EmailSearchResponse;
import squareup.wire.benchmarks.EmailSearchResponseProto;

@Fork(1)
@Warmup(iterations = 5, time = 2)
@Measurement(iterations = 5, time = 2)
@State(Scope.Benchmark)
@BenchmarkMode(SampleTime)
@OutputTimeUnit(MICROSECONDS)
public class SimpleMessageBenchmark {
  Buffer buffer = new Buffer();
  byte[] bytes;

  @Setup
  public void setup() throws IOException {
    // `medium_value.bytes` contains bytes for the message
    // [squareup.wire.benchmarks.EmailSearchResponse].
    File file = new File("src/main/resources/medium_value.bytes");
    try (Source fileSource = Okio.source(file);
        BufferedSource bufferedFileSource = Okio.buffer(fileSource)) {
      bytes = bufferedFileSource.readByteArray();
    }
  }

  @Benchmark
  public void encodeWire3x() throws IOException {
    ProtoWriter writer = new ProtoWriter(buffer);
    EmailSearchResponse.ADAPTER.encode(writer, SampleData.newMediumValueWire());
    buffer.clear();
  }

  @Benchmark
  public void encodeWire4x() throws IOException {
    ReverseProtoWriter writer = new ReverseProtoWriter();
    EmailSearchResponse.ADAPTER.encode(writer, SampleData.newMediumValueWire());
    writer.writeTo(buffer);
    buffer.clear();
  }

  @Benchmark
  public void encodeProtobuf() throws IOException {
    SampleData.newMediumValueProtobuf().writeTo(buffer.outputStream());
    buffer.clear();
  }

  @Benchmark
  public void decodeWire() throws IOException {
    EmailSearchResponse.ADAPTER.decode(bytes);
  }

  @Benchmark
  public void decodeProtobuf() throws IOException {
    EmailSearchResponseProto.EmailSearchResponse.parseFrom(bytes);
  }

  /** Run encode for 10 seconds to capture a profile. */
  public static void main(String[] args) throws IOException {
    long now = System.nanoTime();
    long done = now + TimeUnit.SECONDS.toNanos(10L);
    SimpleMessageBenchmark benchmark = new SimpleMessageBenchmark();
    benchmark.setup();
    while (System.nanoTime() < done) {
      System.out.println(".");
      for (int i = 0; i < 1_000_000; i++) {
        benchmark.encodeWire4x();
      }
    }
  }
}
