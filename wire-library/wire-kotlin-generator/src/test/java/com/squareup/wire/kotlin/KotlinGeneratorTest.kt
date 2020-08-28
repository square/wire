/*
 * Copyright (C) 2018 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.kotlin

import com.squareup.kotlinpoet.FileSpec
import com.squareup.wire.kotlin.KotlinGenerator.Companion.sanitizeKdoc
import com.squareup.wire.schema.PruningRules
import com.squareup.wire.schema.RepoBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.text.RegexOption.DOT_MATCHES_ALL

class KotlinGeneratorTest {
  @Test fun basic() {
    val repoBuilder = RepoBuilder()
        .add("message.proto", """
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
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("Person").replace("\n", "")
    assertTrue(code.contains("class Person"))
    assertTrue(code.contains("object : ProtoAdapter<PhoneNumber>("))
    assertTrue(code.contains("FieldEncoding.LENGTH_DELIMITED"))
    assertTrue(code.contains("PhoneNumber::class"))
    assertTrue(code.contains("override fun encode(writer: ProtoWriter, value: Person)"))
    assertTrue(code.contains("enum class PhoneType(    override val value: Int  ) : WireEnum"))
    assertTrue(code.contains("fun fromValue(value: Int): PhoneType?"))
    assertTrue(code.contains("WORK(1),"))
  }

  @Test fun defaultValues() {
    val repoBuilder = RepoBuilder()
        .add("message.proto", """
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
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("Message")
    assertTrue(code.contains("const val DEFAULT_A: Int = 10"))
    assertTrue(code.contains("const val DEFAULT_B: Int = 32"))
    assertTrue(code.contains("const val DEFAULT_C: Long = 11L"))
    assertTrue(code.contains("const val DEFAULT_D: Long = 33L"))
    assertTrue(code.contains("const val DEFAULT_E: Float = 806f"))
    assertTrue(code.contains("const val DEFAULT_F: Double = -0.0"))
    assertTrue(code.contains("const val DEFAULT_G: Double = 0.0"))
    assertTrue(code.contains("const val DEFAULT_H: Double = -1.0"))
    assertTrue(code.contains("const val DEFAULT_I: Double = 1.0E10"))
    assertTrue(code.contains("const val DEFAULT_J: Double = -0.01"))
    assertTrue(code.contains("const val DEFAULT_K: Double = -1.23E45"))
    assertTrue(code.contains("const val DEFAULT_L: Double = 255.0"))
  }

  @Test fun nameAllocatorIsUsed() {
    val repoBuilder = RepoBuilder()
        .add("message.proto", """
        |message Message {
        |  required float when = 1;
        |  required int32 ADAPTER = 2;
        |  optional int64 adapter = 3;
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("Message")
    assertTrue(code.contains("val when_: Float"))
    assertTrue(code.contains("val ADAPTER_: Int"))
    assertTrue(code.contains("val adapter_: Long?"))
    assertTrue(code.contains("var size = value.unknownFields.size"))
    assertTrue(code.contains("size += ProtoAdapter.FLOAT.encodedSizeWithTag(1, value.when_)"))
    assertTrue(code.contains("ProtoAdapter.FLOAT.encodeWithTag(writer, 1, value.when_)"))
    assertTrue(code.contains("ProtoAdapter.FLOAT.encodeWithTag(writer, 1, value.when_)"))
    assertTrue(code.contains("1 -> when_ = ProtoAdapter.FLOAT.decode(reader)"))
  }

  @Test fun enclosing() {
    val schema = RepoBuilder()
        .add("message.proto", """
          |message A {
          |  message B {
          |  }
          |  optional B b = 1;
          |}""".trimMargin())
        .schema()

    val pruned = schema.prune(PruningRules.Builder().addRoot("A.B").build())

    val kotlinGenerator = KotlinGenerator.invoke(pruned)
    val typeSpec = kotlinGenerator.generateType(pruned.getType("A")!!)
    val code = FileSpec.get("", typeSpec).toString()
    assertTrue(code.contains("class A private constructor() {"))
    assertTrue(code.contains("class B(.*) : Message<B, Nothing>".toRegex(DOT_MATCHES_ALL)))
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
          |interface RouteGuideClient : Service {
          |  /**
          |   * Returns the [Feature] for a [Point].
          |   */
          |  fun GetFeature(): GrpcCall<Point, Feature>
          |}
          |""".trimMargin()
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
          |class GrpcRouteGuideClient(
          |  private val client: GrpcClient
          |) : RouteGuideClient {
          |  /**
          |   * Returns the [Feature] for a [Point].
          |   */
          |  override fun GetFeature(): GrpcCall<Point, Feature> = client.newCall(GrpcMethod(
          |      path = "/routeguide.RouteGuide/GetFeature",
          |      requestAdapter = Point.ADAPTER,
          |      responseAdapter = Feature.ADAPTER
          |  ))
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |package routeguide;
          |
          |// RouteGuide service interface.
          |service RouteGuide {
          |  // Returns the [Feature] for a [Point].
          |  rpc GetFeature(Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |""".trimMargin())
    assertEquals(listOf(expectedInterface, expectedImplementation),
        repoBuilder.generateGrpcKotlin("routeguide.RouteGuide"))
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
        interface RouteGuideBlockingServer : Service {
          /**
           * Returns the [Feature] for a [Point].
           */
          @WireRpc(
            path = "/routeguide.RouteGuide/GetFeature",
            requestAdapter = "routeguide.Point#ADAPTER",
            responseAdapter = "routeguide.Feature#ADAPTER"
          )
          fun GetFeature(request: Point): Feature
        }
        
        """.trimIndent()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |package routeguide;
          |
          |// RouteGuide service interface.
          |service RouteGuide {
          |  // Returns the [Feature] for a [Point].
          |  rpc GetFeature(Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |""".trimMargin())
    assertEquals(listOf(expected), repoBuilder.generateGrpcKotlin("routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING, rpcRole = RpcRole.SERVER))
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
        interface RouteGuideBlockingServer : Service {
          /**
           * Creates a [RouteSummary] based on the provided [Point]s.
           */
          @WireRpc(
            path = "/routeguide.RouteGuide/RecordRoute",
            requestAdapter = "routeguide.Point#ADAPTER",
            responseAdapter = "routeguide.RouteSummary#ADAPTER"
          )
          fun RecordRoute(request: MessageSource<Point>): RouteSummary
        }
        
        """.trimIndent()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |package routeguide;
          |
          |// RouteGuide service interface.
          |service RouteGuide {
          |  // Creates a [RouteSummary] based on the provided [Point]s.
          |  rpc RecordRoute(stream Point) returns (RouteSummary) {}
          |}
          |$pointMessage
          |$routeSummaryMessage
          |""".trimMargin())
    assertEquals(listOf(expected), repoBuilder.generateGrpcKotlin("routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING, rpcRole = RpcRole.SERVER))
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
        interface RouteGuideBlockingServer : Service {
          /**
           * Returns the [Feature]s within a [Rectangle].
           */
          @WireRpc(
            path = "/routeguide.RouteGuide/ListFeatures",
            requestAdapter = "routeguide.Rectangle#ADAPTER",
            responseAdapter = "routeguide.Feature#ADAPTER"
          )
          fun ListFeatures(request: Rectangle, response: MessageSink<Feature>)
        }
        
        """.trimIndent()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
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
          |""".trimMargin())
    assertEquals(listOf(expected), repoBuilder.generateGrpcKotlin("routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING, rpcRole = RpcRole.SERVER))
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
        interface RouteGuideBlockingServer : Service {
          /**
           * Chat with someone using a [RouteNote].
           */
          @WireRpc(
            path = "/routeguide.RouteGuide/RouteChat",
            requestAdapter = "routeguide.RouteNote#ADAPTER",
            responseAdapter = "routeguide.RouteNote#ADAPTER"
          )
          fun RouteChat(request: MessageSource<RouteNote>, response: MessageSink<RouteNote>)
        }
        
        """.trimIndent()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
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
          |""".trimMargin())
    assertEquals(listOf(expected), repoBuilder.generateGrpcKotlin("routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING, rpcRole = RpcRole.SERVER))
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
        interface RouteGuideBlockingServer : Service {
          /**
           * Returns the [Feature] for a [Point].
           */
          @WireRpc(
            path = "/routeguide.RouteGuide/GetFeature",
            requestAdapter = "com.squareup.routeguide.Point#ADAPTER",
            responseAdapter = "com.squareup.routeguide.Feature#ADAPTER"
          )
          fun GetFeature(request: Point): Feature
        }
        
        """.trimIndent()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
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
          |""".trimMargin())
    assertEquals(listOf(expected), repoBuilder.generateGrpcKotlin("routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING, rpcRole = RpcRole.SERVER))
  }

  @Test fun noPackage() {
    val expectedInterface = """
          |import com.squareup.wire.GrpcCall
          |import com.squareup.wire.Service
          |
          |/**
          | * RouteGuide service interface.
          | */
          |interface RouteGuideClient : Service {
          |  /**
          |   * Returns the [Feature] for a [Point].
          |   */
          |  fun GetFeature(): GrpcCall<Point, Feature>
          |}
          |""".trimMargin()
    val expectedImplementation = """
          |import com.squareup.wire.GrpcCall
          |import com.squareup.wire.GrpcClient
          |import com.squareup.wire.GrpcMethod
          |
          |/**
          | * RouteGuide service interface.
          | */
          |class GrpcRouteGuideClient(
          |  private val client: GrpcClient
          |) : RouteGuideClient {
          |  /**
          |   * Returns the [Feature] for a [Point].
          |   */
          |  override fun GetFeature(): GrpcCall<Point, Feature> = client.newCall(GrpcMethod(
          |      path = "/RouteGuide/GetFeature",
          |      requestAdapter = Point.ADAPTER,
          |      responseAdapter = Feature.ADAPTER
          |  ))
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |/** RouteGuide service interface. */
          |service RouteGuide {
          |  /**
          |   * Returns the [Feature] for a [Point].
          |   */
          |  rpc GetFeature(Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |""".trimMargin())
    assertEquals(listOf(expectedInterface, expectedImplementation),
        repoBuilder.generateGrpcKotlin("RouteGuide"))
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
          |interface RouteGuideClient : Service {
          |  /**
          |   * Returns the [Feature] at the given [Point].
          |   */
          |  fun GetFeature(): GrpcCall<Point, Feature>
          |}
          |""".trimMargin()
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
          |class GrpcRouteGuideClient(
          |  private val client: GrpcClient
          |) : RouteGuideClient {
          |  /**
          |   * Returns the [Feature] at the given [Point].
          |   */
          |  override fun GetFeature(): GrpcCall<Point, Feature> = client.newCall(GrpcMethod(
          |      path = "/routeguide.grpc.RouteGuide/GetFeature",
          |      requestAdapter = Point.ADAPTER,
          |      responseAdapter = Feature.ADAPTER
          |  ))
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
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
          |""".trimMargin())
    assertEquals(listOf(expectedInterface, expectedImplementation),
        repoBuilder.generateGrpcKotlin("routeguide.grpc.RouteGuide"))
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
          |interface RouteGuideClient : Service {
          |  /**
          |   * Records a route made up of the provided [Point]s.
          |   */
          |  fun RecordRoute(): GrpcStreamingCall<Point, RouteSummary>
          |}
          |""".trimMargin()
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
          |class GrpcRouteGuideClient(
          |  private val client: GrpcClient
          |) : RouteGuideClient {
          |  /**
          |   * Records a route made up of the provided [Point]s.
          |   */
          |  override fun RecordRoute(): GrpcStreamingCall<Point, RouteSummary> =
          |      client.newStreamingCall(GrpcMethod(
          |      path = "/routeguide.RouteGuide/RecordRoute",
          |      requestAdapter = Point.ADAPTER,
          |      responseAdapter = RouteSummary.ADAPTER
          |  ))
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
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
          |""".trimMargin())
    assertEquals(listOf(expectedInterface, expectedImplementation),
        repoBuilder.generateGrpcKotlin("routeguide.RouteGuide"))
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
          |interface RouteGuideClient : Service {
          |  /**
          |   * List the features available in the area defined by [Rectangle].
          |   */
          |  fun ListFeatures(): GrpcStreamingCall<Rectangle, Feature>
          |}
          |""".trimMargin()
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
          |class GrpcRouteGuideClient(
          |  private val client: GrpcClient
          |) : RouteGuideClient {
          |  /**
          |   * List the features available in the area defined by [Rectangle].
          |   */
          |  override fun ListFeatures(): GrpcStreamingCall<Rectangle, Feature> =
          |      client.newStreamingCall(GrpcMethod(
          |      path = "/routeguide.RouteGuide/ListFeatures",
          |      requestAdapter = Rectangle.ADAPTER,
          |      responseAdapter = Feature.ADAPTER
          |  ))
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
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
          |""".trimMargin())
    assertEquals(listOf(expectedInterface, expectedImplementation),
        repoBuilder.generateGrpcKotlin("routeguide.RouteGuide"))
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
        interface RouteGuideClient : Service {
          /**
           * Chat with someone using a [RouteNote].
           */
          fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote>
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
        class GrpcRouteGuideClient(
          private val client: GrpcClient
        ) : RouteGuideClient {
          /**
           * Chat with someone using a [RouteNote].
           */
          override fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote> =
              client.newStreamingCall(GrpcMethod(
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
        interface RouteGuideBlockingServer : Service {
          /**
           * Chat with someone using a [RouteNote].
           */
          @WireRpc(
            path = "/routeguide.RouteGuide/RouteChat",
            requestAdapter = "routeguide.RouteNote#ADAPTER",
            responseAdapter = "routeguide.RouteNote#ADAPTER"
          )
          fun RouteChat(request: MessageSource<RouteNote>, response: MessageSink<RouteNote>)
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
         interface RouteGuideClient : Service {
           /**
            * Chat with someone using a [RouteNote].
            */
           fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote>
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
         class GrpcRouteGuideClient(
           private val client: GrpcClient
         ) : RouteGuideClient {
           /**
            * Chat with someone using a [RouteNote].
            */
           override fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote> =
               client.newStreamingCall(GrpcMethod(
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
         interface RouteGuideServer : Service {
           /**
            * Chat with someone using a [RouteNote].
            */
           @WireRpc(
             path = "/routeguide.RouteGuide/RouteChat",
             requestAdapter = "routeguide.RouteNote#ADAPTER",
             responseAdapter = "routeguide.RouteNote#ADAPTER"
           )
           suspend fun RouteChat(request: SendChannel<RouteNote>, response: ReceiveChannel<RouteNote>)
         }
         
         """.trimIndent()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
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
          |""".trimMargin())

    assertEquals(listOf(blockingClientInterface, blockingClientImplementation),
        repoBuilder.generateGrpcKotlin("routeguide.RouteGuide",
            rpcCallStyle = RpcCallStyle.BLOCKING, rpcRole = RpcRole.CLIENT))
    assertEquals(listOf(blockingServer), repoBuilder.generateGrpcKotlin("routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING, rpcRole = RpcRole.SERVER))
    assertEquals(listOf(suspendingClientInterface, suspendingClientImplementation),
        repoBuilder.generateGrpcKotlin("routeguide.RouteGuide",
            rpcCallStyle = RpcCallStyle.SUSPENDING, rpcRole = RpcRole.CLIENT))
    assertEquals(listOf(suspendingServer), repoBuilder.generateGrpcKotlin("routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.SUSPENDING, rpcRole = RpcRole.SERVER))
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
          |interface RouteGuideClient : Service {
          |  /**
          |   * Returns the [Feature] for a [Point].
          |   */
          |  fun GetFeature(): GrpcCall<Point, Feature>
          |
          |  /**
          |   * Chat with someone using a [RouteNote].
          |   */
          |  fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote>
          |}
          |""".trimMargin()
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
          |class GrpcRouteGuideClient(
          |  private val client: GrpcClient
          |) : RouteGuideClient {
          |  /**
          |   * Returns the [Feature] for a [Point].
          |   */
          |  override fun GetFeature(): GrpcCall<Point, Feature> = client.newCall(GrpcMethod(
          |      path = "/routeguide.RouteGuide/GetFeature",
          |      requestAdapter = Point.ADAPTER,
          |      responseAdapter = Feature.ADAPTER
          |  ))
          |
          |  /**
          |   * Chat with someone using a [RouteNote].
          |   */
          |  override fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote> =
          |      client.newStreamingCall(GrpcMethod(
          |      path = "/routeguide.RouteGuide/RouteChat",
          |      requestAdapter = RouteNote.ADAPTER,
          |      responseAdapter = RouteNote.ADAPTER
          |  ))
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
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
          |""".trimMargin())
    assertEquals(listOf(expectedInterface, expectedImplementation),
        repoBuilder.generateGrpcKotlin("routeguide.RouteGuide"))
  }

  @Test fun multipleRpcsAsSingleMethodInterface() {
    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |package routeguide;
          |
          |service RouteGuide {
          |  rpc GetFeature(Point) returns (Feature) {}
          |  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {}
          |}
          |$pointMessage
          |$featureMessage
          |$routeNoteMessage
          |""".trimMargin())

    val expectedGetFeatureInterface = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcCall
          |import com.squareup.wire.Service
          |
          |interface RouteGuideGetFeatureClient : Service {
          |  fun GetFeature(): GrpcCall<Point, Feature>
          |}
          |""".trimMargin()
    val expectedGetFeatureImplementation = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcCall
          |import com.squareup.wire.GrpcClient
          |import com.squareup.wire.GrpcMethod
          |
          |class GrpcRouteGuideGetFeatureClient(
          |  private val client: GrpcClient
          |) : RouteGuideGetFeatureClient {
          |  override fun GetFeature(): GrpcCall<Point, Feature> = client.newCall(GrpcMethod(
          |      path = "/routeguide.RouteGuide/GetFeature",
          |      requestAdapter = Point.ADAPTER,
          |      responseAdapter = Feature.ADAPTER
          |  ))
          |}
          |""".trimMargin()

    assertEquals(listOf(expectedGetFeatureInterface, expectedGetFeatureImplementation),
        repoBuilder.generateGrpcKotlin("routeguide.RouteGuide", "GetFeature"))

    val expectedRouteChatInterface = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcStreamingCall
          |import com.squareup.wire.Service
          |
          |interface RouteGuideRouteChatClient : Service {
          |  fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote>
          |}
          |""".trimMargin()
    val expectedRouteChatImplementation = """
          |package routeguide
          |
          |import com.squareup.wire.GrpcClient
          |import com.squareup.wire.GrpcMethod
          |import com.squareup.wire.GrpcStreamingCall
          |
          |class GrpcRouteGuideRouteChatClient(
          |  private val client: GrpcClient
          |) : RouteGuideRouteChatClient {
          |  override fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote> =
          |      client.newStreamingCall(GrpcMethod(
          |      path = "/routeguide.RouteGuide/RouteChat",
          |      requestAdapter = RouteNote.ADAPTER,
          |      responseAdapter = RouteNote.ADAPTER
          |  ))
          |}
          |""".trimMargin()

    assertEquals(listOf(expectedRouteChatInterface, expectedRouteChatImplementation),
        repoBuilder.generateGrpcKotlin("routeguide.RouteGuide", "RouteChat"))
  }

  @Test fun nameAllocatorIsUsedInDecodeForReaderTag() {
    val repoBuilder = RepoBuilder()
        .add("message.proto", """
        |message Message {
        |  required float tag = 1;
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("Message")
    assertTrue(code.contains("val unknownFields = reader.forEachTag { tag_ ->"))
    assertTrue(code.contains("when (tag_)"))
    assertTrue(code.contains("1 -> tag = ProtoAdapter.FLOAT.decode(reader)"))
    assertTrue(code.contains("else -> reader.readUnknownField(tag_)"))
  }

  @Test fun someFieldNameIsKeyword() {
    val repoBuilder = RepoBuilder()
        .add("message.proto", """
        |message Message {
        |  required float var  = 1;
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("Message")
    assertTrue(code.contains("throw missingRequiredFields(var_, \"var\")"))
  }

  @Test fun generateTypeUsesPackageNameOnFieldAndClassNameClash() {
    val repoBuilder = RepoBuilder()
        .add("person.proto", """
        |package common.proto;
        |enum Gender {
        |  Gender_Male = 0;
        |  Gender_Female = 1;
        |}
        |message Person {
        |  optional Gender Gender = 1;
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("common.proto.Person")
    assertTrue(code.contains("val common_proto_Gender: Gender"))
  }

  @Test fun generateTypeUsesPackageNameOnFieldAndClassNameClashWithinPackage() {
    val repoBuilder = RepoBuilder()
        .add("a.proto", """
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
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("common.proto.A")
    assertTrue(code.contains("val common_proto_Status: AnotherStatus"))
  }

  @Test fun usesAny() {
    val repoBuilder = RepoBuilder()
        .add("a.proto", """
        |package common.proto;
        |import "google/protobuf/any.proto";
        |message Message {
        |  optional google.protobuf.Any just_one = 1;
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("common.proto.Message")
    assertTrue(code.contains("import com.squareup.wire.AnyMessage"))
  }

  @Test fun wildCommentsAreEscaped() {
    val repoBuilder = RepoBuilder()
        .add("message.proto", """
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
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("Person").replace("\n", "")
    assertTrue(code.contains("class Person"))
    assertTrue(code.contains("object : ProtoAdapter<PhoneNumber>("))
    assertTrue(code.contains("FieldEncoding.LENGTH_DELIMITED"))
    assertTrue(code.contains("PhoneNumber::class"))
    assertTrue(code.contains("override fun encode(writer: ProtoWriter, value: Person)"))
    assertTrue(code.contains("enum class PhoneType(    override val value: Int  ) : WireEnum"))
    assertTrue(code.contains("fun fromValue(value: Int): PhoneType?"))
    assertTrue(code.contains("WORK(1),"))
  }

  @Test fun sanitizeStringOnPrinting() {
    val repoBuilder = RepoBuilder()
        .add("message.proto", """
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
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("Person")
    assertTrue(code.contains("import com.squareup.wire.internal.sanitize"))
    assertTrue(code.contains("result += \"\"\"name=\${sanitize(name)}\"\"\""))
    assertTrue(code.contains("result += \"\"\"id=\$id\"\"\""))
    assertTrue(code.contains("result += \"\"\"phone=\$phone\"\"\""))
    assertTrue(code.contains("result += \"\"\"aliases=\${sanitize(aliases)}\"\"\""))
    assertTrue(code.contains("result += \"\"\"number=\${sanitize(number)}\"\"\""))
    assertTrue(code.contains("result += \"\"\"type=\$type\"\"\""))
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
    val longType = "MessageWithNameLongerThan100Chars00000000000000000000000000000000000000000000000000000000000000000000"
    val longMember = "member_with_name_which_is_longer_then_100_chars_00000000000000000000000000000000000000000000000000000"
    val repoBuilder = RepoBuilder()
        .add("$longType.proto", """
        |message $longType {
        |  required string $longMember = 1;
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin(longType)
    val expectedEqualsConditionImplementation = """
          |        ($longMember
          |        !=
          |        other.$longMember)
          """.trimMargin()

    assertTrue(code.contains("return false"))
    assertTrue(code.contains("return $longType("))
    assertTrue(code.contains(expectedEqualsConditionImplementation))
    assertTrue(code.contains("$longMember =\n"))
  }

  @Test fun constructorForProto3() {
    val repoBuilder = RepoBuilder()
        .add("label.proto", """
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
        |""".trimMargin())
    val code = repoBuilder.generateKotlin("common.proto.LabelMessage")
    assertTrue(code.contains("""val text: String = "","""))
    assertTrue(code.contains("""val author: Author? = null,"""))
    assertTrue(code.contains("""val enum: Enum = Enum.UNKNOWN,"""))
    assertTrue(code.contains("""val foo: Int? = null,"""))
    assertTrue(code.contains("""val bar: String? = null,"""))
    assertTrue(code.contains("""val baz: Baz? = null,"""))
    assertTrue(code.contains("""val count: Long = 0"""))
  }

  @Test fun wirePackageTakesPrecedenceOverJavaPackage() {
    val repoBuilder = RepoBuilder()
        .add("proto_package/person.proto", """
        |package proto_package;
        |import "wire/extensions.proto";
        |
        |option java_package = "java_package";
        |option (wire.wire_package) = "wire_package";
        |
        |message Person {
        |	required string name = 1;
        |}
        |""".trimMargin())
    val code = repoBuilder.generateKotlin("proto_package.Person")
    assertTrue(code.contains("package wire_package"))
    assertTrue(code.contains("class Person"))
  }

  @Test fun wirePackageTakesPrecedenceOverProtoPackage() {
    val repoBuilder = RepoBuilder()
        .add("proto_package/person.proto", """
        |package proto_package;
        |import "wire/extensions.proto";
        |
        |option (wire.wire_package) = "wire_package";
        |
        |message Person {
        |	required string name = 1;
        |}
        |""".trimMargin())
    val code = repoBuilder.generateKotlin("proto_package.Person")
    assertTrue(code.contains("package wire_package"))
    assertTrue(code.contains("class Person"))
  }

  @Test fun wirePackageUsedInImport() {
    val repoBuilder = RepoBuilder()
        .add("proto_package/person.proto", """
        |package proto_package;
        |import "wire/extensions.proto";
        |
        |option (wire.wire_package) = "wire_package";
        |
        |message Person {
        |	required string name = 1;
        |}
        |""".trimMargin())
        .add("city_package/home.proto", """
        |package city_package;
        |import "proto_package/person.proto";
        |
        |message Home {
        |	repeated proto_package.Person person = 1;
        |}
        |""".trimMargin())
    val code = repoBuilder.generateKotlin("city_package.Home")
    assertTrue(code.contains("package city_package"))
    assertTrue(code.contains("import wire_package.Person"))
  }

  companion object {
    private val pointMessage = """
          |message Point {
          |  optional int32 latitude = 1;
          |  optional int32 longitude = 2;
          |}""".trimMargin()

    private val rectangeMessage = """
          |message Rectangle {
          |  optional Point lo = 1;
          |  optional Point hi = 2;
          |}""".trimMargin()

    private val featureMessage = """
          |message Feature {
          |  optional string name = 1;
          |  optional Point location = 2;
          |}""".trimMargin()

    private val routeNoteMessage = """
          |message RouteNote {
          |  optional Point location = 1;
          |  optional string message = 2;
          |}""".trimMargin()

    private val routeSummaryMessage = """
          |message RouteSummary {
          |  optional int32 point_count = 1;
          |  optional int32 feature_count = 2;
          |  optional int32 distance = 3;
          |  optional int32 elapsed_time = 4;
          |}""".trimMargin()
  }
}
