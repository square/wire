/*
 * Copyright (C) 2018 Square, Inc.
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
package com.squareup.wire.kotlin

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsMatch
import assertk.assertions.doesNotContain
import com.squareup.kotlinpoet.FileSpec
import com.squareup.wire.buildSchema
import com.squareup.wire.kotlin.EnumMode.SEALED_CLASS
import com.squareup.wire.kotlin.KotlinGenerator.Companion.sanitizeKdoc
import com.squareup.wire.schema.PruningRules
import com.squareup.wire.schema.addFromTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import okio.Path.Companion.toPath

class KotlinGeneratorTest {
  @Test fun basic() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |message Person {
        |	required string name = 1;
        |	required int32 id = 2;
        |	optional string email = 3;
        |	enum PhoneType {
        |		HOME = 0;
        |		WORK = 1;
        |		MOBILE = 2;
        |	}
        |	message PhoneNumber {
        |		required string number = 1;
        |		optional PhoneType type = 2 [default = HOME];
        |	}
        |	repeated PhoneNumber phone = 4;
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("Person").replace("\n", "")
    assertThat(code).contains("class Person")
    assertThat(code).contains("object : ProtoAdapter<PhoneNumber>(")
    assertThat(code).contains("FieldEncoding.LENGTH_DELIMITED")
    assertThat(code).contains("PhoneNumber::class")
    assertThat(code).contains("override fun encode(writer: ProtoWriter, `value`: Person)")
    assertTrue(
      code.contains("enum class PhoneType(    override val `value`: Int,  ) : WireEnum"),
    )
    assertThat(code).contains("fun fromValue(`value`: Int): PhoneType?")
    assertThat(code).contains("WORK(1),")
  }

  @Test fun generateSealedClassEnumForProto2() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |enum PhoneType {
        |	HOME = 0;
        |	WORK = 1;
        |	MOBILE = 2;
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema)
      .generateKotlin("PhoneType", enumMode = EnumMode.SEALED_CLASS)
    assertThat(code).contains("data object HOME")
    assertThat(code).contains("data object WORK")
    assertThat(code).contains("data object MOBILE")
    assertThat(code).contains("data class Unrecognized internal constructor(")
  }

  @Test fun generateSealedClassEnumForProto3() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto3";
        |enum PhoneType {
        |	HOME = 0;
        |	WORK = 1;
        |	MOBILE = 2;
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema)
      .generateKotlin("PhoneType", enumMode = EnumMode.SEALED_CLASS)
    assertThat(code).contains("data object HOME")
    assertThat(code).contains("data object WORK")
    assertThat(code).contains("data object MOBILE")
    assertThat(code).contains("data class Unrecognized internal constructor(")
  }

  @Test fun defaultValues() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |message Message {
        |  optional int32 a = 1 [default = 10 ];
        |  optional int32 b = 2 [default = 0x20 ];
        |  optional int64 c = 3 [default = 11 ];
        |  optional int64 d = 4 [default = 0x21 ];
        |  optional float e = 5 [default = 806 ];
        |  optional double f = 6 [default = -0];
        |  optional double g = 7 [default = 0.0];
        |  optional double h = 8 [default = -1.0];
        |  optional double i = 9 [default = 1e10];
        |  optional double j = 10 [default = -1e-2];
        |  optional double k = 11 [default = -1.23e45];
        |  optional double l = 12 [default = 255];
        |  optional double m = 13 [default = inf];
        |  optional double n = 14 [default = -inf];
        |  optional double o = 15 [default = nan];
        |  optional double p = 16 [default = -nan];
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("Message")
    assertThat(code).contains("const val DEFAULT_A: Int = 10")
    assertThat(code).contains("const val DEFAULT_B: Int = 32")
    assertThat(code).contains("const val DEFAULT_C: Long = 11L")
    assertThat(code).contains("const val DEFAULT_D: Long = 33L")
    assertThat(code).contains("const val DEFAULT_E: Float = 806f")
    assertThat(code).contains("const val DEFAULT_F: Double = -0.0")
    assertThat(code).contains("const val DEFAULT_G: Double = 0.0")
    assertThat(code).contains("const val DEFAULT_H: Double = -1.0")
    assertThat(code).contains("const val DEFAULT_I: Double = 10_000_000_000.0")
    assertThat(code).contains("const val DEFAULT_J: Double = -0.01")
    assertThat(code).contains(
      "public const val DEFAULT_K: Double =\n" +
        "        -1_230_000_000_000_000_000_000_000_000_000_000_000_000_000_000.0",
    )
    assertThat(code).contains("const val DEFAULT_L: Double = 255.0")
    assertThat(code).contains("const val DEFAULT_M: Double = Double.POSITIVE_INFINITY")
    assertThat(code).contains("const val DEFAULT_N: Double = Double.NEGATIVE_INFINITY")
    assertThat(code).contains("const val DEFAULT_O: Double = Double.NaN")
    assertThat(code).contains("const val DEFAULT_P: Double = Double.NaN")
  }

  @Test fun nameAllocatorIsUsed() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |message Message {
        |  required float when = 1;
        |  required int32 ADAPTER = 2;
        |  optional int64 adapter = 3;
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("Message")
    assertThat(code).contains("val when_: Float")
    assertThat(code).contains("val ADAPTER_: Int")
    assertThat(code).contains("val adapter_: Long?")
    assertThat(code).contains("var size = value.unknownFields.size")
    assertThat(code).contains("size += ProtoAdapter.FLOAT.encodedSizeWithTag(1, value.when_)")
    assertThat(code).contains("ProtoAdapter.FLOAT.encodeWithTag(writer, 1, value.when_)")
    assertThat(code).contains("ProtoAdapter.FLOAT.encodeWithTag(writer, 1, value.when_)")
    assertThat(code).contains("1 -> when_ = ProtoAdapter.FLOAT.decode(reader)")
  }

  @Test fun enclosing() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
          |message A {
          |  message B {
          |  }
          |  optional B b = 1;
          |}
        """.trimMargin(),
      )
    }

    val pruned = schema.prune(PruningRules.Builder().addRoot("A.B").build())

    val kotlinGenerator = KotlinGenerator.invoke(pruned)
    val typeSpec = kotlinGenerator.generateType(pruned.getType("A")!!)
    assertThat(FileSpec.get("", typeSpec).toString()).all {
      contains(
        """
      |@WireEnclosingType
      |public class A private constructor() {
        """.trimMargin(),
      )
      containsMatch(
        "class B(.*) : Message<B, Nothing>".toRegex(DOT_MATCHES_ALL),
      )
    }
  }

  @Test fun requestResponse() {
    val expectedInterface = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcCall
          |import com.squareup.wire.Service
          |
          |/**
          | * RouteGuide service interface.
          | */
          |public interface RouteGuideClient : Service {
          |  /**
          |   * Returns the \[Feature\] for a \[Point\].
          |   */
          |  public fun GetFeature(): GrpcCall<Point, Feature>
          |}
          |
    """.trimMargin()
    val expectedImplementation = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcCall
          |import com.squareup.wire.GrpcClient
          |import com.squareup.wire.GrpcMethod
          |
          |/**
          | * RouteGuide service interface.
          | */
          |public class GrpcRouteGuideClient(
          |  private val client: GrpcClient,
          |) : RouteGuideClient {
          |  /**
          |   * Returns the \[Feature\] for a \[Point\].
          |   */
          |  override fun GetFeature(): GrpcCall<Point, Feature> = client.newCall(GrpcMethod(
          |      path = "/routeguide.RouteGuide/GetFeature",
          |      requestAdapter = Point.ADAPTER,
          |      responseAdapter = Feature.ADAPTER
          |  ))
          |}
          |
    """.trimMargin()

    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide;
          |
          |// RouteGuide service interface.
          |service RouteGuide {
          |  // Returns the [Feature] for a [Point].
          |  rpc GetFeature(Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |
        """.trimMargin(),
      )
    }
    assertEquals(
      listOf(expectedInterface, expectedImplementation),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin("routeguide.RouteGuide"),
    )
  }

  @Test fun blockingSingleRequestSingleResponse() {
    //language=kotlin
    val expected = """
        package routeguide

        import com.squareup.wire.Service
        import com.squareup.wire.WireRpc

        /**
         * RouteGuide service interface.
         */
        public interface RouteGuideBlockingServer : Service {
          /**
           * Returns the \[Feature\] for a \[Point\].
           */
          @WireRpc(
            path = "/routeguide.RouteGuide/GetFeature",
            requestAdapter = "routeguide.Point#ADAPTER",
            responseAdapter = "routeguide.Feature#ADAPTER",
            sourceFile = "routeguide.proto",
          )
          public fun GetFeature(request: Point): Feature
        }

    """.trimIndent()

    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide;
          |
          |// RouteGuide service interface.
          |service RouteGuide {
          |  // Returns the [Feature] for a [Point].
          |  rpc GetFeature(Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |
        """.trimMargin(),
      )
    }
    assertEquals(
      listOf(expected),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin(
        "routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING,
        rpcRole = RpcRole.SERVER,
      ),
    )
  }

  @Test fun blockingStreamingRequestSingleResponse() {
    //language=kotlin
    val expected = """
        package routeguide

        import com.squareup.wire.MessageSource
        import com.squareup.wire.Service
        import com.squareup.wire.WireRpc

        /**
         * RouteGuide service interface.
         */
        public interface RouteGuideBlockingServer : Service {
          /**
           * Creates a \[RouteSummary\] based on the provided \[Point\]s.
           */
          @WireRpc(
            path = "/routeguide.RouteGuide/RecordRoute",
            requestAdapter = "routeguide.Point#ADAPTER",
            responseAdapter = "routeguide.RouteSummary#ADAPTER",
            sourceFile = "routeguide.proto",
          )
          public fun RecordRoute(request: MessageSource<Point>): RouteSummary
        }

    """.trimIndent()

    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide;
          |
          |// RouteGuide service interface.
          |service RouteGuide {
          |  // Creates a [RouteSummary] based on the provided [Point]s.
          |  rpc RecordRoute(stream Point) returns (RouteSummary) {}
          |}
          |$pointMessage
          |$routeSummaryMessage
          |
        """.trimMargin(),
      )
    }
    assertEquals(
      listOf(expected),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin(
        "routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING,
        rpcRole = RpcRole.SERVER,
      ),
    )
  }

  @Test fun blockingSingleRequestStreamingResponse() {
    //language=kotlin
    val expected = """
        package routeguide

        import com.squareup.wire.MessageSink
        import com.squareup.wire.Service
        import com.squareup.wire.WireRpc

        /**
         * RouteGuide service interface.
         */
        public interface RouteGuideBlockingServer : Service {
          /**
           * Returns the \[Feature\]s within a \[Rectangle\].
           */
          @WireRpc(
            path = "/routeguide.RouteGuide/ListFeatures",
            requestAdapter = "routeguide.Rectangle#ADAPTER",
            responseAdapter = "routeguide.Feature#ADAPTER",
            sourceFile = "routeguide.proto",
          )
          public fun ListFeatures(request: Rectangle, response: MessageSink<Feature>)
        }

    """.trimIndent()

    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide;
          |
          |// RouteGuide service interface.
          |service RouteGuide {
          |  // Returns the [Feature]s within a [Rectangle].
          |  rpc ListFeatures(Rectangle) returns (stream Feature) {}
          |}
          |$pointMessage
          |$rectangeMessage
          |$featureMessage
          |
        """.trimMargin(),
      )
    }
    assertEquals(
      listOf(expected),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin(
        "routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING,
        rpcRole = RpcRole.SERVER,
      ),
    )
  }

  @Test fun blockingStreamingRequestStreamingResponse() {
    //language=kotlin
    val expected = """
        package routeguide

        import com.squareup.wire.MessageSink
        import com.squareup.wire.MessageSource
        import com.squareup.wire.Service
        import com.squareup.wire.WireRpc

        /**
         * RouteGuide service interface.
         */
        public interface RouteGuideBlockingServer : Service {
          /**
           * Chat with someone using a \[RouteNote\].
           */
          @WireRpc(
            path = "/routeguide.RouteGuide/RouteChat",
            requestAdapter = "routeguide.RouteNote#ADAPTER",
            responseAdapter = "routeguide.RouteNote#ADAPTER",
            sourceFile = "routeguide.proto",
          )
          public fun RouteChat(request: MessageSource<RouteNote>, response: MessageSink<RouteNote>)
        }

    """.trimIndent()

    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide;
          |
          |/**
          | * RouteGuide service interface.
          | */
          |service RouteGuide {
          |  // Chat with someone using a [RouteNote].
          |  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {}
          |}
          |$pointMessage
          |$rectangeMessage
          |$routeNoteMessage
          |
        """.trimMargin(),
      )
    }
    assertEquals(
      listOf(expected),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin(
        "routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING,
        rpcRole = RpcRole.SERVER,
      ),
    )
  }

  @Test fun javaPackageOption() {
    // Note that the @WireRpc path does not use the Java package option.
    //language=kotlin
    val expected = """
        package com.squareup.routeguide

        import com.squareup.wire.Service
        import com.squareup.wire.WireRpc

        /**
         * RouteGuide service interface.
         */
        public interface RouteGuideBlockingServer : Service {
          /**
           * Returns the \[Feature\] for a \[Point\].
           */
          @WireRpc(
            path = "/routeguide.RouteGuide/GetFeature",
            requestAdapter = "com.squareup.routeguide.Point#ADAPTER",
            responseAdapter = "com.squareup.routeguide.Feature#ADAPTER",
            sourceFile = "routeguide.proto",
          )
          public fun GetFeature(request: Point): Feature
        }

    """.trimIndent()

    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide;
          |
          |option java_package = "com.squareup.routeguide";
          |
          |/**
          | * RouteGuide service interface.
          | */
          |service RouteGuide {
          |  /**
          |   * Returns the [Feature] for a [Point].
          |   */
          |  rpc GetFeature(Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |
        """.trimMargin(),
      )
    }
    assertEquals(
      listOf(expected),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin(
        "routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING,
        rpcRole = RpcRole.SERVER,
      ),
    )
  }

  @Test fun noPackage() {
    val expectedInterface = """
          |import com.squareup.wire.GrpcCall
          |import com.squareup.wire.Service
          |
          |/**
          | * RouteGuide service interface.
          | */
          |public interface RouteGuideClient : Service {
          |  /**
          |   * Returns the \[Feature\] for a \[Point\].
          |   */
          |  public fun GetFeature(): GrpcCall<Point, Feature>
          |}
          |
    """.trimMargin()
    val expectedImplementation = """
          |import com.squareup.wire.GrpcCall
          |import com.squareup.wire.GrpcClient
          |import com.squareup.wire.GrpcMethod
          |
          |/**
          | * RouteGuide service interface.
          | */
          |public class GrpcRouteGuideClient(
          |  private val client: GrpcClient,
          |) : RouteGuideClient {
          |  /**
          |   * Returns the \[Feature\] for a \[Point\].
          |   */
          |  override fun GetFeature(): GrpcCall<Point, Feature> = client.newCall(GrpcMethod(
          |      path = "/RouteGuide/GetFeature",
          |      requestAdapter = Point.ADAPTER,
          |      responseAdapter = Feature.ADAPTER
          |  ))
          |}
          |
    """.trimMargin()

    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |/** RouteGuide service interface. */
          |service RouteGuide {
          |  /**
          |   * Returns the [Feature] for a [Point].
          |   */
          |  rpc GetFeature(Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |
        """.trimMargin(),
      )
    }
    assertEquals(
      listOf(expectedInterface, expectedImplementation),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin("RouteGuide"),
    )
  }

  @Test fun multiDepthPackage() {
    val expectedInterface = """
          |package routeguide.grpc
          |
          |import com.squareup.wire.GrpcCall
          |import com.squareup.wire.Service
          |
          |/**
          | * RouteGuide service interface.
          | */
          |public interface RouteGuideClient : Service {
          |  /**
          |   * Returns the \[Feature\] at the given \[Point\].
          |   */
          |  public fun GetFeature(): GrpcCall<Point, Feature>
          |}
          |
    """.trimMargin()
    val expectedImplementation = """
          |package routeguide.grpc
          |
          |import com.squareup.wire.GrpcCall
          |import com.squareup.wire.GrpcClient
          |import com.squareup.wire.GrpcMethod
          |
          |/**
          | * RouteGuide service interface.
          | */
          |public class GrpcRouteGuideClient(
          |  private val client: GrpcClient,
          |) : RouteGuideClient {
          |  /**
          |   * Returns the \[Feature\] at the given \[Point\].
          |   */
          |  override fun GetFeature(): GrpcCall<Point, Feature> = client.newCall(GrpcMethod(
          |      path = "/routeguide.grpc.RouteGuide/GetFeature",
          |      requestAdapter = Point.ADAPTER,
          |      responseAdapter = Feature.ADAPTER
          |  ))
          |}
          |
    """.trimMargin()

    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide.grpc;
          |
          |/**
          | * RouteGuide service interface.
          | */
          |service RouteGuide {
          |  // Returns the [Feature] at the given [Point].
          |  rpc GetFeature(Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |
        """.trimMargin(),
      )
    }
    assertEquals(
      listOf(expectedInterface, expectedImplementation),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin("routeguide.grpc.RouteGuide"),
    )
  }

  @Test fun streamingRequest() {
    val expectedInterface = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcStreamingCall
          |import com.squareup.wire.Service
          |
          |/**
          | * RouteGuide service interface.
          | */
          |public interface RouteGuideClient : Service {
          |  /**
          |   * Records a route made up of the provided \[Point\]s.
          |   */
          |  public fun RecordRoute(): GrpcStreamingCall<Point, RouteSummary>
          |}
          |
    """.trimMargin()
    val expectedImplementation = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcClient
          |import com.squareup.wire.GrpcMethod
          |import com.squareup.wire.GrpcStreamingCall
          |
          |/**
          | * RouteGuide service interface.
          | */
          |public class GrpcRouteGuideClient(
          |  private val client: GrpcClient,
          |) : RouteGuideClient {
          |  /**
          |   * Records a route made up of the provided \[Point\]s.
          |   */
          |  override fun RecordRoute(): GrpcStreamingCall<Point, RouteSummary> = client.newStreamingCall(GrpcMethod(
          |      path = "/routeguide.RouteGuide/RecordRoute",
          |      requestAdapter = Point.ADAPTER,
          |      responseAdapter = RouteSummary.ADAPTER
          |  ))
          |}
          |
    """.trimMargin()

    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide;
          |
          |/**
          | * RouteGuide service interface.
          | */
          |service RouteGuide {
          |  /** Records a route made up of the provided [Point]s. */
          |  rpc RecordRoute(stream Point) returns (RouteSummary) {}
          |}
          |$pointMessage
          |$routeSummaryMessage
          |
        """.trimMargin(),
      )
    }
    assertEquals(
      listOf(expectedInterface, expectedImplementation),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin("routeguide.RouteGuide"),
    )
  }

  @Test fun streamingResponse() {
    val expectedInterface = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcStreamingCall
          |import com.squareup.wire.Service
          |
          |/**
          | * RouteGuide service interface.
          | */
          |public interface RouteGuideClient : Service {
          |  /**
          |   * List the features available in the area defined by \[Rectangle\].
          |   */
          |  public fun ListFeatures(): GrpcStreamingCall<Rectangle, Feature>
          |}
          |
    """.trimMargin()
    val expectedImplementation = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcClient
          |import com.squareup.wire.GrpcMethod
          |import com.squareup.wire.GrpcStreamingCall
          |
          |/**
          | * RouteGuide service interface.
          | */
          |public class GrpcRouteGuideClient(
          |  private val client: GrpcClient,
          |) : RouteGuideClient {
          |  /**
          |   * List the features available in the area defined by \[Rectangle\].
          |   */
          |  override fun ListFeatures(): GrpcStreamingCall<Rectangle, Feature> = client.newStreamingCall(GrpcMethod(
          |      path = "/routeguide.RouteGuide/ListFeatures",
          |      requestAdapter = Rectangle.ADAPTER,
          |      responseAdapter = Feature.ADAPTER
          |  ))
          |}
          |
    """.trimMargin()

    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide;
          |
          |/**
          | * RouteGuide service interface.
          | */
          |service RouteGuide {
          |  // List the features available in the area defined by [Rectangle].
          |  rpc ListFeatures(Rectangle) returns (stream Feature) {}
          |}
          |$rectangeMessage
          |$pointMessage
          |$featureMessage
          |
        """.trimMargin(),
      )
    }
    assertEquals(
      listOf(expectedInterface, expectedImplementation),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin("routeguide.RouteGuide"),
    )
  }

  @Test fun bidirectional() {
    //language=kotlin
    val blockingClientInterface = """
        package routeguide

        import com.squareup.wire.GrpcStreamingCall
        import com.squareup.wire.Service

        /**
         * RouteGuide service interface.
         */
        public interface RouteGuideClient : Service {
          /**
           * Chat with someone using a \[RouteNote\].
           */
          public fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote>
        }

    """.trimIndent()
    //language=kotlin
    val blockingClientImplementation = """
        package routeguide

        import com.squareup.wire.GrpcClient
        import com.squareup.wire.GrpcMethod
        import com.squareup.wire.GrpcStreamingCall

        /**
         * RouteGuide service interface.
         */
        public class GrpcRouteGuideClient(
          private val client: GrpcClient,
        ) : RouteGuideClient {
          /**
           * Chat with someone using a \[RouteNote\].
           */
          override fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote> = client.newStreamingCall(GrpcMethod(
              path = "/routeguide.RouteGuide/RouteChat",
              requestAdapter = RouteNote.ADAPTER,
              responseAdapter = RouteNote.ADAPTER
          ))
        }

    """.trimIndent()
    //language=kotlin
    val blockingServer = """
        package routeguide

        import com.squareup.wire.MessageSink
        import com.squareup.wire.MessageSource
        import com.squareup.wire.Service
        import com.squareup.wire.WireRpc

        /**
         * RouteGuide service interface.
         */
        public interface RouteGuideBlockingServer : Service {
          /**
           * Chat with someone using a \[RouteNote\].
           */
          @WireRpc(
            path = "/routeguide.RouteGuide/RouteChat",
            requestAdapter = "routeguide.RouteNote#ADAPTER",
            responseAdapter = "routeguide.RouteNote#ADAPTER",
            sourceFile = "routeguide.proto",
          )
          public fun RouteChat(request: MessageSource<RouteNote>, response: MessageSink<RouteNote>)
        }

    """.trimIndent()
    //language=kotlin
    val suspendingClientInterface = """
         package routeguide

         import com.squareup.wire.GrpcStreamingCall
         import com.squareup.wire.Service

         /**
          * RouteGuide service interface.
          */
         public interface RouteGuideClient : Service {
           /**
            * Chat with someone using a \[RouteNote\].
            */
           public fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote>
         }

    """.trimIndent()
    //language=kotlin
    val suspendingClientImplementation = """
         package routeguide

         import com.squareup.wire.GrpcClient
         import com.squareup.wire.GrpcMethod
         import com.squareup.wire.GrpcStreamingCall

         /**
          * RouteGuide service interface.
          */
         public class GrpcRouteGuideClient(
           private val client: GrpcClient,
         ) : RouteGuideClient {
           /**
            * Chat with someone using a \[RouteNote\].
            */
           override fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote> = client.newStreamingCall(GrpcMethod(
               path = "/routeguide.RouteGuide/RouteChat",
               requestAdapter = RouteNote.ADAPTER,
               responseAdapter = RouteNote.ADAPTER
           ))
         }

    """.trimIndent()
    //language=kotlin
    val suspendingServer = """
         package routeguide

         import com.squareup.wire.Service
         import com.squareup.wire.WireRpc
         import kotlinx.coroutines.channels.ReceiveChannel
         import kotlinx.coroutines.channels.SendChannel

         /**
          * RouteGuide service interface.
          */
         public interface RouteGuideServer : Service {
           /**
            * Chat with someone using a \[RouteNote\].
            */
           @WireRpc(
             path = "/routeguide.RouteGuide/RouteChat",
             requestAdapter = "routeguide.RouteNote#ADAPTER",
             responseAdapter = "routeguide.RouteNote#ADAPTER",
             sourceFile = "routeguide.proto",
           )
           public suspend fun RouteChat(request: ReceiveChannel<RouteNote>, response: SendChannel<RouteNote>)
         }

    """.trimIndent()

    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide;
          |
          |/**
          | * RouteGuide service interface.
          | */
          |service RouteGuide {
          |  // Chat with someone using a [RouteNote].
          |  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {}
          |}
          |$pointMessage
          |$routeNoteMessage
          |
        """.trimMargin(),
      )
    }

    assertEquals(
      listOf(blockingClientInterface, blockingClientImplementation),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin(
        "routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING,
        rpcRole = RpcRole.CLIENT,
      ),
    )
    assertEquals(
      listOf(blockingServer),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin(
        "routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING,
        rpcRole = RpcRole.SERVER,
      ),
    )
    assertEquals(
      listOf(suspendingClientInterface, suspendingClientImplementation),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin(
        "routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.SUSPENDING,
        rpcRole = RpcRole.CLIENT,
      ),
    )
    assertEquals(
      listOf(suspendingServer),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin(
        "routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.SUSPENDING,
        rpcRole = RpcRole.SERVER,
      ),
    )
  }

  @Test fun multipleRpcs() {
    val expectedInterface = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcCall
          |import com.squareup.wire.GrpcStreamingCall
          |import com.squareup.wire.Service
          |
          |/**
          | * RouteGuide service interface.
          | */
          |public interface RouteGuideClient : Service {
          |  /**
          |   * Returns the \[Feature\] for a \[Point\].
          |   */
          |  public fun GetFeature(): GrpcCall<Point, Feature>
          |
          |  /**
          |   * Chat with someone using a \[RouteNote\].
          |   */
          |  public fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote>
          |}
          |
    """.trimMargin()
    val expectedImplementation = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcCall
          |import com.squareup.wire.GrpcClient
          |import com.squareup.wire.GrpcMethod
          |import com.squareup.wire.GrpcStreamingCall
          |
          |/**
          | * RouteGuide service interface.
          | */
          |public class GrpcRouteGuideClient(
          |  private val client: GrpcClient,
          |) : RouteGuideClient {
          |  /**
          |   * Returns the \[Feature\] for a \[Point\].
          |   */
          |  override fun GetFeature(): GrpcCall<Point, Feature> = client.newCall(GrpcMethod(
          |      path = "/routeguide.RouteGuide/GetFeature",
          |      requestAdapter = Point.ADAPTER,
          |      responseAdapter = Feature.ADAPTER
          |  ))
          |
          |  /**
          |   * Chat with someone using a \[RouteNote\].
          |   */
          |  override fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote> = client.newStreamingCall(GrpcMethod(
          |      path = "/routeguide.RouteGuide/RouteChat",
          |      requestAdapter = RouteNote.ADAPTER,
          |      responseAdapter = RouteNote.ADAPTER
          |  ))
          |}
          |
    """.trimMargin()

    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide;
          |
          |/**
          | * RouteGuide service interface.
          | */
          |service RouteGuide {
          |  // Returns the [Feature] for a [Point].
          |  rpc GetFeature(Point) returns (Feature) {}
          |  // Chat with someone using a [RouteNote].
          |  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {}
          |}
          |$pointMessage
          |$featureMessage
          |$routeNoteMessage
          |
        """.trimMargin(),
      )
    }
    assertEquals(
      listOf(expectedInterface, expectedImplementation),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin("routeguide.RouteGuide"),
    )
  }

  @Test fun multipleRpcsAsSingleMethodInterface() {
    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide;
          |
          |service RouteGuide {
          |  rpc GetFeature(Point) returns (Feature) {}
          |  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {}
          |}
          |$pointMessage
          |$featureMessage
          |$routeNoteMessage
          |
        """.trimMargin(),
      )
    }

    val expectedGetFeatureInterface = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcCall
          |import com.squareup.wire.Service
          |
          |public interface RouteGuideGetFeatureClient : Service {
          |  public fun GetFeature(): GrpcCall<Point, Feature>
          |}
          |
    """.trimMargin()
    val expectedGetFeatureImplementation = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcCall
          |import com.squareup.wire.GrpcClient
          |import com.squareup.wire.GrpcMethod
          |
          |public class GrpcRouteGuideGetFeatureClient(
          |  private val client: GrpcClient,
          |) : RouteGuideGetFeatureClient {
          |  override fun GetFeature(): GrpcCall<Point, Feature> = client.newCall(GrpcMethod(
          |      path = "/routeguide.RouteGuide/GetFeature",
          |      requestAdapter = Point.ADAPTER,
          |      responseAdapter = Feature.ADAPTER
          |  ))
          |}
          |
    """.trimMargin()

    assertEquals(
      listOf(expectedGetFeatureInterface, expectedGetFeatureImplementation),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin("routeguide.RouteGuide", "GetFeature"),
    )

    val expectedRouteChatInterface = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcStreamingCall
          |import com.squareup.wire.Service
          |
          |public interface RouteGuideRouteChatClient : Service {
          |  public fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote>
          |}
          |
    """.trimMargin()
    val expectedRouteChatImplementation = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcClient
          |import com.squareup.wire.GrpcMethod
          |import com.squareup.wire.GrpcStreamingCall
          |
          |public class GrpcRouteGuideRouteChatClient(
          |  private val client: GrpcClient,
          |) : RouteGuideRouteChatClient {
          |  override fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote> = client.newStreamingCall(GrpcMethod(
          |      path = "/routeguide.RouteGuide/RouteChat",
          |      requestAdapter = RouteNote.ADAPTER,
          |      responseAdapter = RouteNote.ADAPTER
          |  ))
          |}
          |
    """.trimMargin()

    assertEquals(
      listOf(expectedRouteChatInterface, expectedRouteChatImplementation),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin("routeguide.RouteGuide", "RouteChat"),
    )
  }

  @Test fun nameSuffixes() {
    //language=kotlin
    val suspendingInterface = """
          |package routeguide
          |
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |
          |/**
          | * RouteGuide service interface.
          | */
          |public interface RouteGuide : Service {
          |  /**
          |   * Returns the \[Feature\] for a \[Point\].
          |   */
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/GetFeature",
          |    requestAdapter = "routeguide.Point#ADAPTER",
          |    responseAdapter = "routeguide.Feature#ADAPTER",
          |    sourceFile = "routeguide.proto",
          |  )
          |  public suspend fun GetFeature(request: Point): Feature
          |}
          |
    """.trimMargin()

    //language=kotlin
    val blockingInterface = """
          |package routeguide
          |
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |
          |/**
          | * RouteGuide service interface.
          | */
          |public interface RouteGuideThing : Service {
          |  /**
          |   * Returns the \[Feature\] for a \[Point\].
          |   */
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/GetFeature",
          |    requestAdapter = "routeguide.Point#ADAPTER",
          |    responseAdapter = "routeguide.Feature#ADAPTER",
          |    sourceFile = "routeguide.proto",
          |  )
          |  public fun GetFeature(request: Point): Feature
          |}
          |
    """.trimMargin()

    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide;
          |
          |/**
          | * RouteGuide service interface.
          | */
          |service RouteGuide {
          |  // Returns the [Feature] for a [Point].
          |  rpc GetFeature(Point) returns (Feature);
          |}
          |$pointMessage
          |$featureMessage
          |
        """.trimMargin(),
      )
    }

    assertEquals(
      listOf(suspendingInterface),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin(
        serviceName = "routeguide.RouteGuide",
        rpcRole = RpcRole.SERVER,
        rpcCallStyle = RpcCallStyle.SUSPENDING,
        nameSuffix = "",
      ),
    )

    assertEquals(
      listOf(blockingInterface),
      KotlinWithProfilesGenerator(schema).generateGrpcKotlin(
        serviceName = "routeguide.RouteGuide",
        rpcRole = RpcRole.SERVER,
        rpcCallStyle = RpcCallStyle.BLOCKING,
        nameSuffix = "Thing",
      ),
    )
  }

  @Test fun nameAllocatorIsUsedInDecodeForReaderTag() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |message Message {
        |  required float tag = 1;
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("Message")
    assertThat(code).contains("val unknownFields = reader.forEachTag { tag_ ->")
    assertThat(code).contains("when (tag_)")
    assertThat(code).contains("1 -> tag = ProtoAdapter.FLOAT.decode(reader)")
    assertThat(code).contains("else -> reader.readUnknownField(tag_)")
  }

  @Test fun someFieldNameIsKeyword() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |message Message {
        |  required float var  = 1;
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("Message")
    assertThat(code).contains("throw missingRequiredFields(var_, \"var\")")
  }

  @Test fun generateTypeUsesPackageNameOnFieldAndClassNameClash() {
    val schema = buildSchema {
      add(
        "person.proto".toPath(),
        """
        |package common.proto;
        |enum Gender {
        |  Gender_Male = 0;
        |  Gender_Female = 1;
        |}
        |message Person {
        |  optional Gender Gender = 1;
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("common.proto.Person")
    assertThat(code).contains("val common_proto_Gender: Gender")
  }

  @Test fun generateTypeUsesPackageNameOnFieldAndClassNameClashWithinPackage() {
    val schema = buildSchema {
      add(
        "a.proto".toPath(),
        """
        |package common.proto;
        |enum Status {
        |  Status_Approved = 0;
        |  Status_Denied = 1;
        |}
        |enum AnotherStatus {
        |  AnotherStatus_Processing = 0;
        |  AnotherStatus_Completed = 1;
        |}
        |message A {
        |  message B {
        |    optional Status Status = 1;
        |  }
        |  repeated B b = 1;
        |  optional AnotherStatus Status = 2;
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("common.proto.A")
    assertThat(code).contains("val common_proto_Status: AnotherStatus")
  }

  @Test fun usesAny() {
    val schema = buildSchema {
      add(
        "a.proto".toPath(),
        """
        |package common.proto;
        |import "google/protobuf/any.proto";
        |message Message {
        |  optional google.protobuf.Any just_one = 1;
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("common.proto.Message")
    assertThat(code).contains("import com.squareup.wire.AnyMessage")
  }

  @Test fun wildCommentsAreEscaped() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |message Person {
        |	required string name = 1;
        |	required int32 id = 2;
        |	optional string email = 3;
        |	enum PhoneType {
        |		HOME = 0;
        |		WORK = 1;
        |		MOBILE = 2;
        |	}
        |	message PhoneNumber {
        |		required string number = 1;
        |		optional PhoneType type = 2 [default = HOME];
        |	}
        |	repeated PhoneNumber phone = 4;
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("Person").replace("\n", "")
    assertThat(code).contains("class Person")
    assertThat(code).contains("object : ProtoAdapter<PhoneNumber>(")
    assertThat(code).contains("FieldEncoding.LENGTH_DELIMITED")
    assertThat(code).contains("PhoneNumber::class")
    assertThat(code).contains("override fun encode(writer: ProtoWriter, `value`: Person)")
    assertTrue(
      code.contains("enum class PhoneType(    override val `value`: Int,  ) : WireEnum"),
    )
    assertThat(code).contains("fun fromValue(`value`: Int): PhoneType?")
    assertThat(code).contains("WORK(1),")
  }

  @Test fun sanitizeStringOnPrinting() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |message Person {
        |	required string name = 1;
        |	required int32 id = 2;
        |	repeated PhoneNumber phone = 3;
        |	repeated string aliases = 4;
        |
        |	message PhoneNumber {
        |		required string number = 1;
        |		optional PhoneType type = 2 [default = HOME];
        |	}
        |	enum PhoneType {
        |		HOME = 0;
        |		WORK = 1;
        |		MOBILE = 2;
        |	}
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("Person")
    assertThat(code).contains("import com.squareup.wire.`internal`.sanitize")
    assertThat(code).contains("result += \"\"\"name=\${sanitize(name)}\"\"\"")
    assertThat(code).contains("result += \"\"\"id=\$id\"\"\"")
    assertThat(code).contains("result += \"\"\"phone=\$phone\"\"\"")
    assertThat(code).contains("result += \"\"\"aliases=\${sanitize(aliases)}\"\"\"")
    assertThat(code).contains("result += \"\"\"number=\${sanitize(number)}\"\"\"")
    assertThat(code).contains("result += \"\"\"type=\$type\"\"\"")
  }

  @Test fun sanitizeJavadocStripsTrailingWhitespace() {
    val input = "The quick brown fox  \nJumps over  \n\t \t\nThe lazy dog  "
    val expected = "The quick brown fox\nJumps over\n\nThe lazy dog"
    assertEquals(expected, input.sanitizeKdoc())
  }

  @Test fun sanitizeJavadocStarSlash() {
    val input = "/* comment inside comment. */"
    val expected = "/&#42; comment inside comment. &#42;/"
    assertEquals(expected, input.sanitizeKdoc())
  }

  @Test fun handleLongIdentifiers() {
    val longType =
      "MessageWithNameLongerThan100Chars00000000000000000000000000000000000000000000000000000000000000000000"
    val longMember =
      "member_with_name_which_is_longer_then_100_chars_00000000000000000000000000000000000000000000000000000"
    val schema = buildSchema {
      add(
        "$longType.proto".toPath(),
        """
        |message $longType {
        |  required string $longMember = 1;
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin(longType)
    val expectedEqualsConditionImplementation = "if ($longMember != other.$longMember) return false"

    assertThat(code).contains("return false")
    assertThat(code).contains("return $longType(")
    assertThat(code).contains(expectedEqualsConditionImplementation)
    assertThat(code).contains("$longMember = ")
  }

  @Test fun constructorForProto3() {
    val schema = buildSchema {
      add(
        "label.proto".toPath(),
        """
        |syntax = "proto3";
        |package common.proto;
        |message LabelMessage {
        |  string text = 1;
        |  Author author = 2;
        |  Enum enum = 3;
        |  oneof choice {
        |    int32 foo = 4;
        |    string bar = 5;
        |    Baz baz = 6;
        |  }
        |  int64 count = 7;
        |  enum Enum {
        |    UNKNOWN = 0;
        |    A = 1;
        |  }
        |  message Author {
        |    string name = 1;
        |  }
        |  message Baz {}
        |}
        |
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("common.proto.LabelMessage")
    assertThat(code).contains("""val text: String = "",""")
    assertThat(code).contains("""val author: Author? = null,""")
    assertThat(code).contains("""val enum_: Enum = Enum.UNKNOWN,""")
    assertThat(code).contains("""val foo: Int? = null,""")
    assertThat(code).contains("""val bar: String? = null,""")
    assertThat(code).contains("""val baz: Baz? = null,""")
    assertThat(code).contains("""val count: Long = 0""")
  }

  @Test fun wirePackageTakesPrecedenceOverJavaPackage() {
    val schema = buildSchema {
      add(
        "proto_package/person.proto".toPath(),
        """
        |package proto_package;
        |import "wire/extensions.proto";
        |
        |option java_package = "java_package";
        |option (wire.wire_package) = "wire_package";
        |
        |message Person {
        |	required string name = 1;
        |}
        |
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("proto_package.Person")
    assertThat(code).contains("package wire_package")
    assertThat(code).contains("class Person")
  }

  @Test fun wirePackageTakesPrecedenceOverProtoPackage() {
    val schema = buildSchema {
      add(
        "proto_package/person.proto".toPath(),
        """
        |package proto_package;
        |import "wire/extensions.proto";
        |
        |option (wire.wire_package) = "wire_package";
        |
        |message Person {
        |	required string name = 1;
        |}
        |
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("proto_package.Person")
    assertThat(code).contains("package wire_package")
    assertThat(code).contains("class Person")
  }

  @Test fun wirePackageUsedInImport() {
    val schema = buildSchema {
      add(
        "proto_package/person.proto".toPath(),
        """
        |package proto_package;
        |import "wire/extensions.proto";
        |
        |option (wire.wire_package) = "wire_package";
        |
        |message Person {
        |	required string name = 1;
        |}
        |
        """.trimMargin(),
      )
      add(
        "city_package/home.proto".toPath(),
        """
        |package city_package;
        |import "proto_package/person.proto";
        |
        |message Home {
        |	repeated proto_package.Person person = 1;
        |}
        |
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("city_package.Home")
    assertThat(code).contains("package city_package")
    assertThat(code).contains("import wire_package.Person")
  }

  @Test fun useArrayUsesTheCorrectType() {
    val schema = buildSchema {
      add(
        "proto_package/person.proto".toPath(),
        """
        |package proto_package;
        |import "wire/extensions.proto";
        |
        |message Person {
        |	 repeated float info = 1 [packed = true, (wire.use_array) = true];
        |}
        |
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("proto_package.Person")
    assertContains(code, "public val info: FloatArray = floatArrayOf()")
    assertContains(code, "ProtoAdapter.FLOAT_ARRAY.encodeWithTag(writer, 1, value.info)")
  }

  @Test fun documentationEscapesBrackets() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto3";
        |
        |// a [b]
        |message Message {
        |  // c [d..e]
        |  string a = 1;
        |}
        |
        |// [f]
        |enum Enum {
        |  // g [h.i.j] k
        |  CONSTANT = 0;
        |}
        """.trimMargin(),
      )
    }
    val messageCode = KotlinWithProfilesGenerator(schema).generateKotlin("Message")
    assertThat(messageCode).contains("""* a \[b\]""")
    assertThat(messageCode).contains("""* c \[d..e\]""")
    val enumCode = KotlinWithProfilesGenerator(schema).generateKotlin("Enum")
    assertThat(enumCode).contains("""* \[f\]""")
    assertThat(enumCode).contains("""* g \[h.i.j\] k""")
  }

  @Test fun profileHonoredInRpcInterface() {
    val expectedInterface = """
        |package routeguide
        |
        |import com.squareup.wire.GrpcCall
        |import com.squareup.wire.Service
        |import java.lang.String
        |import java.util.Properties
        |
        |/**
        | * RouteGuide service interface.
        | */
        |public interface RouteGuideClient : Service {
        |  /**
        |   * Returns the \[Feature\] for a \[Point\].
        |   */
        |  public fun GetFeature(): GrpcCall<String, Properties>
        |}
        |
    """.trimMargin()
    val expectedImplementation = """
        |package routeguide
        |
        |import com.example.PropertiesFeatureAdapter
        |import com.example.StringPointAdapter
        |import com.squareup.wire.GrpcCall
        |import com.squareup.wire.GrpcClient
        |import com.squareup.wire.GrpcMethod
        |import java.lang.String
        |import java.util.Properties
        |
        |/**
        | * RouteGuide service interface.
        | */
        |public class GrpcRouteGuideClient(
        |  private val client: GrpcClient,
        |) : RouteGuideClient {
        |  /**
        |   * Returns the \[Feature\] for a \[Point\].
        |   */
        |  override fun GetFeature(): GrpcCall<String, Properties> = client.newCall(GrpcMethod(
        |      path = "/routeguide.RouteGuide/GetFeature",
        |      requestAdapter = StringPointAdapter.INSTANCE,
        |      responseAdapter = PropertiesFeatureAdapter.ADAPTER
        |  ))
        |}
        |
    """.trimMargin()

    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide;
          |
          |// RouteGuide service interface.
          |service RouteGuide {
          |  // Returns the [Feature] for a [Point].
          |  rpc GetFeature(Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |
        """.trimMargin(),
      )
    }
    assertEquals(
      listOf(expectedInterface, expectedImplementation),
      KotlinWithProfilesGenerator(schema)
        .withProfile(
          "java.wire",
          """
          |syntax = "wire2";
          |import "routeguide.proto";
          |
          |type routeguide.Point {
          |  target java.lang.String using com.example.StringPointAdapter#INSTANCE;
          |}
          |
          |type routeguide.Feature {
          |  target java.util.Properties using com.example.PropertiesFeatureAdapter#ADAPTER;
          |}
          |
          """.trimMargin(),
        )
        .generateGrpcKotlin("routeguide.RouteGuide", profileName = "java"),
    )
  }

  @Test fun profileHonoredInMessage() {
    val schema = buildSchema {
      add(
        "routeguide.proto".toPath(),
        """
          |package routeguide;
          |
          |$pointMessage
          |$featureMessage
          |
        """.trimMargin(),
      )
    }

    val kotlin = KotlinWithProfilesGenerator(schema)
      .withProfile(
        "java.wire",
        """
          |syntax = "wire2";
          |import "routeguide.proto";
          |
          |type routeguide.Point {
          |  target kotlin.String using com.example.StringPointAdapter#INSTANCE;
          |}
          |
        """.trimMargin(),
      )
      .generateKotlin("routeguide.Feature", profileName = "java")
    assertThat(kotlin).contains(
      """
        |  @field:WireField(
        |    tag = 2,
        |    adapter = "com.example.StringPointAdapter#INSTANCE",
        |    schemaIndex = 1,
        |  )
        |  public val location: String? = null,
      """.trimMargin(),
    )
    assertThat(kotlin).contains(
      """
        |      override fun encodedSize(`value`: Feature): Int {
        |        var size = value.unknownFields.size
        |        size += ProtoAdapter.STRING.encodedSizeWithTag(1, value.name)
        |        size += StringPointAdapter.INSTANCE.encodedSizeWithTag(2, value.location)
        |        return size
        |      }
      """.trimMargin(),
    )
    assertThat(kotlin).contains(
      """
        |      override fun encode(writer: ProtoWriter, `value`: Feature) {
        |        ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name)
        |        StringPointAdapter.INSTANCE.encodeWithTag(writer, 2, value.location)
        |        writer.writeBytes(value.unknownFields)
        |      }
      """.trimMargin(),
    )
    assertThat(kotlin).contains(
      """
        |      override fun decode(reader: ProtoReader): Feature {
        |        var name: String? = null
        |        var location: String? = null
        |        val unknownFields = reader.forEachTag { tag ->
        |          when (tag) {
        |            1 -> name = ProtoAdapter.STRING.decode(reader)
        |            2 -> location = StringPointAdapter.INSTANCE.decode(reader)
        |            else -> reader.readUnknownField(tag)
        |          }
        |        }
        |        return Feature(
        |          name = name,
        |          location = location,
        |          unknownFields = unknownFields
        |        )
        |      }
        |
      """.trimMargin(),
    )
  }

  @Test fun deprecatedEnum() {
    val schema = buildSchema {
      add(
        "proto_package/person.proto".toPath(),
        """
         |package proto_package;
         |enum Direction {
         |  option deprecated = true;
         |  NORTH = 1;
         |  EAST = 2;
         |  SOUTH = 3;
         |  WEST = 4;
         |}
         |
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("proto_package.Direction")
    assertThat(code).contains(
      """|@Deprecated(message = "Direction is deprecated")
         |public enum class Direction(
      """.trimMargin(),
    )
  }

  @Test fun deprecatedEnumConstant() {
    val schema = buildSchema {
      add(
        "proto_package/person.proto".toPath(),
        """
        |package proto_package;
        |enum Direction {
        |  NORTH = 1;
        |  EAST = 2 [deprecated = true];
        |  SOUTH = 3;
        |  WEST = 4;
        |}
        |
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("proto_package.Direction")
    assertThat(code).contains(
      """|  @Deprecated(message = "EAST is deprecated")
         |  EAST(2),
      """.trimMargin(),
    )
    assertThat(code).contains(
      """|  2 -> @Suppress("DEPRECATION") EAST
      """.trimMargin(),
    )
  }

  @Test fun deprecatedField() {
    val schema = buildSchema {
      add(
        "proto_package/person.proto".toPath(),
        """
        |package proto_package;
        |message Person {
        |  optional string name = 1 [deprecated = true];
        |}
        |
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("proto_package.Person")
    assertThat(code).contains(
      """|  @Deprecated(message = "name is deprecated")
         |  @field:WireField(
         |    tag = 1,
         |    adapter = "com.squareup.wire.ProtoAdapter#STRING",
         |    schemaIndex = 0,
         |  )
         |  public val name: String? = null,
      """.trimMargin(),
    )
  }

  @Test fun deprecatedMessage() {
    val schema = buildSchema {
      add(
        "proto_package/person.proto".toPath(),
        """
        |package proto_package;
        |message Person {
        |  option deprecated = true;
        |  optional string name = 1;
        |}
        |
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("proto_package.Person")
    assertThat(code).contains(
      """|@Deprecated(message = "Person is deprecated")
         |public class Person(
      """.trimMargin(),
    )
  }

  @Test
  fun redactedNonNullableFieldsForProto3() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto3";
        |import "option_redacted.proto";
        |message RedactedFields {
        |  string a = 1 [(squareup.protos.redacted_option.redacted) = true];
        |  int32  b = 2 [(squareup.protos.redacted_option.redacted) = true];
        |  oneof choice {
        |    string c = 3 [(squareup.protos.redacted_option.redacted) = true];
        |    int32  d = 4 [(squareup.protos.redacted_option.redacted) = true];
        |  }
        |  SecretData secret_data = 5 [(squareup.protos.redacted_option.redacted) = true];
        |}
        |
        |message SecretData {}
        |
        """.trimMargin(),
      )
      addFromTest("option_redacted.proto".toPath())
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("RedactedFields")
    assertThat(code).contains("""public val a: String = "",""")
    assertThat(code).contains("""public val b: Int = 0,""")
    assertThat(code).contains("""public val c: String? = null,""")
    assertThat(code).contains("""public val d: Int? = null,""")
    assertThat(code).contains("""public val secret_data: SecretData? = null,""")
    assertThat(code).contains(
      """
      |      override fun redact(`value`: RedactedFields): RedactedFields = value.copy(
      |        a = "",
      |        b = 0,
      |        c = null,
      |        d = null,
      |        secret_data = null,
      |        unknownFields = ByteString.EMPTY
      |      )
      """.trimMargin(),
    )
  }

  @Test
  fun buildersOnlyGeneratesNonPublicConstructors() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |message SomeMessage {
        |  optional string a = 1;
        |  optional string b = 2;
        |  message InnerMessage {
        |    optional string c = 3;
        |    optional string d = 8;
        |  }
        |}
        |
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema)
      .generateKotlin("SomeMessage", buildersOnly = true)
    assertThat(code).contains("SomeMessage private constructor(")
    assertThat(code).contains("InnerMessage private constructor(")
    assertThat(code).doesNotContain("SomeMessage public constructor(")
    assertThat(code).doesNotContain("InnerMessage public constructor(")
  }

  @Test
  fun buildersOnlyOrJavaInteropGeneratesKotlinBuildClosure() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |message SomeMessage {
        |  optional string a = 1;
        |  optional string b = 2;
        |}
        |
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema)
      .generateKotlin("SomeMessage", buildersOnly = false, javaInterop = false)
    assertThat(code).doesNotContain("inline fun build(body: Builder.() -> Unit): SomeMessage")
    val buildersOnlyCode = KotlinWithProfilesGenerator(schema)
      .generateKotlin("SomeMessage", buildersOnly = true, javaInterop = false)
    assertThat(buildersOnlyCode).contains("@JvmSynthetic\n    public inline fun build(body: Builder.() -> Unit): SomeMessage")
    val javaInteropCode = KotlinWithProfilesGenerator(schema)
      .generateKotlin("SomeMessage", buildersOnly = false, javaInterop = true)
    assertThat(javaInteropCode).contains("@JvmSynthetic\n    public inline fun build(body: Builder.() -> Unit): SomeMessage")
    val bothCode = KotlinWithProfilesGenerator(schema)
      .generateKotlin("SomeMessage", buildersOnly = true, javaInterop = true)
    assertThat(bothCode).contains("@JvmSynthetic\n    public inline fun build(body: Builder.() -> Unit): SomeMessage")
  }

  @Test
  fun javaInteropAndBuildersOnly() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |message SomeMessage {
        |  optional string a = 1;
        |}
        |
        """.trimMargin(),
      )
    }
    assertThat(
      KotlinWithProfilesGenerator(schema)
        .generateKotlin("SomeMessage", javaInterop = false),
    )
      .contains("Builders are deprecated and only available in a javaInterop build")
    assertThat(
      KotlinWithProfilesGenerator(schema)
        .generateKotlin("SomeMessage", javaInterop = true),
    )
      .doesNotContain("Builders are deprecated and only available in a javaInterop build")
    // If `buildersOnly` is set to true, it takes precedence over `javaInterop` for it would
    // otherwise create non-instantiable types.
    assertThat(
      KotlinWithProfilesGenerator(schema)
        .generateKotlin("SomeMessage", javaInterop = false, buildersOnly = true),
    )
      .doesNotContain("Builders are deprecated and only available in a javaInterop build")
  }

  @Test
  fun fieldsDeclarationOrderIsRespected() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |syntax = "proto2";
        |message SomeMessage {
        |  optional string a = 1;
        |  optional string b = 2;
        |  oneof choice {
        |    string c = 3;
        |    string d = 8;
        |  }
        |  optional SecretData secret_data = 4;
        |  optional string e = 5;
        |  oneof decision {
        |    string f = 6;
        |    string g = 7;
        |    string h = 9;
        |  }
        |  optional string i = 10;
        |  oneof unique {
        |    string j = 12;
        |  }
        |  optional string k = 11;
        |}
        |
        |message SecretData {}
        |
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("SomeMessage", boxOneOfsMinSize = 3)
    assertThat(code).contains(
      """
      |public class SomeMessage(
      |  @field:WireField(
      |    tag = 1,
      |    adapter = "com.squareup.wire.ProtoAdapter#STRING",
      |    schemaIndex = 0,
      |  )
      |  public val a: String? = null,
      |  @field:WireField(
      |    tag = 2,
      |    adapter = "com.squareup.wire.ProtoAdapter#STRING",
      |    schemaIndex = 1,
      |  )
      |  public val b: String? = null,
      |  @field:WireField(
      |    tag = 3,
      |    adapter = "com.squareup.wire.ProtoAdapter#STRING",
      |    oneofName = "choice",
      |    schemaIndex = 2,
      |  )
      |  public val c: String? = null,
      |  @field:WireField(
      |    tag = 8,
      |    adapter = "com.squareup.wire.ProtoAdapter#STRING",
      |    oneofName = "choice",
      |    schemaIndex = 3,
      |  )
      |  public val d: String? = null,
      |  @field:WireField(
      |    tag = 4,
      |    adapter = "SecretData#ADAPTER",
      |    schemaIndex = 4,
      |  )
      |  public val secret_data: SecretData? = null,
      |  @field:WireField(
      |    tag = 5,
      |    adapter = "com.squareup.wire.ProtoAdapter#STRING",
      |    schemaIndex = 5,
      |  )
      |  public val e: String? = null,
      |  public val decision: OneOf<Decision<*>, *>? = null,
      |  @field:WireField(
      |    tag = 10,
      |    adapter = "com.squareup.wire.ProtoAdapter#STRING",
      |    schemaIndex = 6,
      |  )
      |  public val i: String? = null,
      |  @field:WireField(
      |    tag = 12,
      |    adapter = "com.squareup.wire.ProtoAdapter#STRING",
      |    oneofName = "unique",
      |    schemaIndex = 7,
      |  )
      |  public val j: String? = null,
      |  @field:WireField(
      |    tag = 11,
      |    adapter = "com.squareup.wire.ProtoAdapter#STRING",
      |    schemaIndex = 8,
      |  )
      |  public val k: String? = null,
      |  unknownFields: ByteString = ByteString.EMPTY,
      """.trimMargin(),
    )
  }

  @Test fun hashCodeFunctionImplementation() {
    val schema = buildSchema {
      add(
        "text.proto".toPath(),
        """
        |syntax = "proto3";
        |
        |message SomeText {
        |  string stringValue = 1;
        |  int32 intValue=2;
        |  bool boolValue=3;
        |  OtherText other_text = 4;
        |  oneof test_oneof {
        |    string string_oneof = 5;
        |    int32 int_oneof = 6;
        |  }
        |
        |  repeated string list = 7;
        |}
        |
        |message OtherText {
        |  string otherValue = 1;
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("SomeText")
    assertContains(code, "result = result * 37 + stringValue.hashCode()")
    assertContains(code, "result = result * 37 + intValue.hashCode()")
    assertContains(code, "result = result * 37 + boolValue.hashCode()")
    assertContains(code, "result = result * 37 + (other_text?.hashCode() ?: 0)")
    assertContains(code, "result = result * 37 + (string_oneof?.hashCode() ?: 0)")
    assertContains(code, "result = result * 37 + (string_oneof?.hashCode() ?: 0)")
    assertContains(code, "result = result * 37 + (int_oneof?.hashCode() ?: 0)")
    assertContains(code, "result = result * 37 + list.hashCode()")
  }

  @Test
  fun enumConstantConflictingDeclaration() {
    val schema = buildSchema {
      add(
        "text.proto".toPath(),
        """
        |enum ConflictingEnumConstants {
        |  hello = 0;
        |  name = 1;
        |  ordinal = 2;
        |  open = 3;
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("ConflictingEnumConstants")
    assertThat(code).contains(
      """
       |public enum class ConflictingEnumConstants(
       |  override val `value`: Int,
       |) : WireEnum {
       |  hello(0),
       |  @WireEnumConstant(declaredName = "name")
       |  name_(1),
       |  @WireEnumConstant(declaredName = "ordinal")
       |  ordinal_(2),
       |  @WireEnumConstant(declaredName = "open")
       |  open_(3),
       |  ;
      """.trimMargin(),
    )
    assertThat(code).contains(
      """
       |    public fun fromValue(`value`: Int): ConflictingEnumConstants? = when (`value`) {
       |      0 -> hello
       |      1 -> name_
       |      2 -> ordinal_
       |      3 -> open_
       |      else -> null
       |    }
      """.trimMargin(),
    )
  }

  @Test fun packedFieldsUsePresizedLists() {
    val schema = buildSchema {
      add(
        "message.proto".toPath(),
        """
        |message Embedding {
        |	repeated float values = 1 [packed = true];
        |}
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin("Embedding")
    assertContains(code, "var values: MutableList<Float>? = null")
    assertContains(code, "val minimumByteSize = 4")
    assertContains(code, "values = ArrayList(initialCapacity)")
  }

  /**
   * We had a bug where java_package and wire_package were asymmetric. We would lose the
   * wire_package when it was used on the protoPath.
   */
  @Test fun wirePackageInProtoPathHonoredWhenGeneratingCode() {
    val schema = buildSchema {
      addProtoPath(
        "person_proto_package/person.proto".toPath(),
        """
        |package person_proto_package;
        |import "wire/extensions.proto";
        |
        |option (wire.wire_package) = "wire_package";
        |
        |message Person {
        |	required string name = 1;
        |}
        |
        """.trimMargin(),
      )
      add(
        "employer_proto_package/employer.proto".toPath(),
        """
        |package employer_proto_package;
        |import "person_proto_package/person.proto";
        |
        |message Employer {
        |	repeated person_proto_package.Person employees = 1;
        |}
        |
        """.trimMargin(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema)
      .generateKotlin("employer_proto_package.Employer")
    assertThat(code).contains("import wire_package.Person")
  }

  @Test
  fun generatesMutableMessages() {
    val schema = buildSchema {
      add(
        "packet.proto".toPath(),
        """
          message Header {
            optional uint64 id = 1;
          }

          message Payload {
            enum Type {
              TYPE_TEXT_PLAIN = 1;
              TYPE_TEXT_HTML = 2;
              TYPE_IMAGE_JPEG = 3;
              TYPE_IMAGE_PNG = 4;
              TYPE_UNKNOWN = 10;
            }
            optional bytes content = 1;
            optional Type type = 2;
          }

          message Packet {
            optional Header header = 1;
            optional Payload payload = 2;
          }
        """.trimIndent(),
      )
    }
    val code = KotlinWithProfilesGenerator(schema).generateKotlin(
      typeName = "Packet",
      mutableTypes = true,
    )
    assertThat(code).contains("class MutablePacket")
    assertThat(code).contains("public var header_: MutableHeader? = null")
    assertThat(code).contains("public var payload: MutablePayload? = null")
    assertThat(code).contains("override var unknownFields: ByteString = ByteString.EMPTY")
    assertThat(code).contains("MutableHeader#ADAPTER") // should refer to adapters of Mutable message types.
    assertThat(code).contains("MutablePayload#ADAPTER")
    assertThat(code).contains("var result = 0") // hashCode() is no longer calling super.hashCode().
    assertThat(code).contains(
      "throw UnsupportedOperationException(\"newBuilder() is unsupported for mutable message types\")",
    )
    assertThat(code).contains(
      "throw UnsupportedOperationException(\"redact() is unsupported for mutable message types\")",
    )
  }

  companion object {
    private val pointMessage = """
          |message Point {
          |  optional int32 latitude = 1;
          |  optional int32 longitude = 2;
          |}
    """.trimMargin()

    private val rectangeMessage = """
          |message Rectangle {
          |  optional Point lo = 1;
          |  optional Point hi = 2;
          |}
    """.trimMargin()

    private val featureMessage = """
          |message Feature {
          |  optional string name = 1;
          |  optional Point location = 2;
          |}
    """.trimMargin()

    private val routeNoteMessage = """
          |message RouteNote {
          |  optional Point location = 1;
          |  optional string message = 2;
          |}
    """.trimMargin()

    private val routeSummaryMessage = """
          |message RouteSummary {
          |  optional int32 point_count = 1;
          |  optional int32 feature_count = 2;
          |  optional int32 distance = 3;
          |  optional int32 elapsed_time = 4;
          |}
    """.trimMargin()
  }
}
