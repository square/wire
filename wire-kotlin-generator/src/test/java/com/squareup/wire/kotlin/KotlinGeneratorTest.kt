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
import com.squareup.wire.schema.IdentifierSet
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
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("Message")
    assertTrue(code.contains("const val DEFAULT_A: Int = 10"))
    assertTrue(code.contains("const val DEFAULT_B: Int = 32"))
    assertTrue(code.contains("const val DEFAULT_C: Long = 11L"))
    assertTrue(code.contains("const val DEFAULT_D: Long = 33L"))
  }

  @Test fun nameAllocatorIsUsed() {
    val repoBuilder = RepoBuilder()
        .add("message.proto", """
        |message Message {
        |  required float when = 1;
        |  required int32 ADAPTER = 2;
        |}""".trimMargin())
    val code = repoBuilder.generateKotlin("Message")
    assertTrue(code.contains("val when_: Float"))
    assertTrue(code.contains("val ADAPTER_: Int"))
    assertTrue(code.contains("ProtoAdapter.FLOAT.encodedSizeWithTag(1, value.when_) +"))
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

    val pruned = schema.prune(IdentifierSet.Builder().include("A.B").build())

    val kotlinGenerator = KotlinGenerator.invoke(pruned)
    val typeSpec = kotlinGenerator.generateType(pruned.getType("A"))
    val code = FileSpec.get("", typeSpec).toString()
    assertTrue(code.contains("object A {"))
    assertTrue(code.contains("class B(.*) : Message<B, Nothing>".toRegex(DOT_MATCHES_ALL)))
  }

  @Test fun requestResponse() {
    val expected = """
          |package routeguide
          |
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |
          |interface RouteGuideClient : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/GetFeature",
          |    requestAdapter = "routeguide.Point#ADAPTER",
          |    responseAdapter = "routeguide.Feature#ADAPTER"
          |  )
          |  suspend fun GetFeature(request: Point): Feature
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |package routeguide;
          |
          |service RouteGuide {
          |  rpc GetFeature(Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |""".trimMargin())
    assertEquals(expected, repoBuilder.generateGrpcKotlin("routeguide.RouteGuide"))
  }

  @Test fun blockingSingleRequestSingleResponse() {
    val expected = """
          |package routeguide
          |
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |
          |interface RouteGuideBlockingServer : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/GetFeature",
          |    requestAdapter = "routeguide.Point#ADAPTER",
          |    responseAdapter = "routeguide.Feature#ADAPTER"
          |  )
          |  fun GetFeature(request: Point): Feature
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |package routeguide;
          |
          |service RouteGuide {
          |  rpc GetFeature(Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |""".trimMargin())
    assertEquals(expected, repoBuilder.generateGrpcKotlin(
        "routeguide.RouteGuide", rpcCallStyle = RpcCallStyle.BLOCKING, rpcRole = RpcRole.SERVER))
  }

  @Test fun blockingStreamingRequestSingleResponse() {
    val expected = """
          |package routeguide
          |
          |import com.squareup.wire.MessageSource
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |
          |interface RouteGuideBlockingServer : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/GetFeature",
          |    requestAdapter = "routeguide.Point#ADAPTER",
          |    responseAdapter = "routeguide.Feature#ADAPTER"
          |  )
          |  fun GetFeature(request: MessageSource<Point>): Feature
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |package routeguide;
          |
          |service RouteGuide {
          |  rpc GetFeature(stream Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |""".trimMargin())
    assertEquals(expected, repoBuilder.generateGrpcKotlin(
        "routeguide.RouteGuide", rpcCallStyle = RpcCallStyle.BLOCKING, rpcRole = RpcRole.SERVER))
  }

  @Test fun blockingSingleRequestStreamingResponse() {
    val expected = """
          |package routeguide
          |
          |import com.squareup.wire.MessageSink
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |
          |interface RouteGuideBlockingServer : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/GetFeature",
          |    requestAdapter = "routeguide.Point#ADAPTER",
          |    responseAdapter = "routeguide.Feature#ADAPTER"
          |  )
          |  fun GetFeature(request: Point, response: MessageSink<Feature>)
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |package routeguide;
          |
          |service RouteGuide {
          |  rpc GetFeature(Point) returns (stream Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |""".trimMargin())
    assertEquals(expected, repoBuilder.generateGrpcKotlin(
        "routeguide.RouteGuide", rpcCallStyle = RpcCallStyle.BLOCKING, rpcRole = RpcRole.SERVER))
  }

  @Test fun blockingStreamingRequestStreamingResponse() {
    val expected = """
          |package routeguide
          |
          |import com.squareup.wire.MessageSink
          |import com.squareup.wire.MessageSource
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |
          |interface RouteGuideBlockingServer : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/GetFeature",
          |    requestAdapter = "routeguide.Point#ADAPTER",
          |    responseAdapter = "routeguide.Feature#ADAPTER"
          |  )
          |  fun GetFeature(request: MessageSource<Point>, response: MessageSink<Feature>)
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |package routeguide;
          |
          |service RouteGuide {
          |  rpc GetFeature(stream Point) returns (stream Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |""".trimMargin())
    assertEquals(expected, repoBuilder.generateGrpcKotlin(
        "routeguide.RouteGuide", rpcCallStyle = RpcCallStyle.BLOCKING, rpcRole = RpcRole.SERVER))
  }

  @Test fun javaPackageOption() {
    // Note that the @WireRpc path does not use the Java package option.
    val expected = """
          |package com.squareup.routeguide
          |
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |
          |interface RouteGuideBlockingServer : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/GetFeature",
          |    requestAdapter = "routeguide.Point#ADAPTER",
          |    responseAdapter = "routeguide.Feature#ADAPTER"
          |  )
          |  fun GetFeature(request: Point): Feature
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |package routeguide;
          |
          |option java_package = "com.squareup.routeguide";
          |
          |service RouteGuide {
          |  rpc GetFeature(Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |""".trimMargin())
    assertEquals(expected, repoBuilder.generateGrpcKotlin(
        "routeguide.RouteGuide", rpcCallStyle = RpcCallStyle.BLOCKING, rpcRole = RpcRole.SERVER))
  }

  @Test fun noPackage() {
    val expected = """
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |
          |interface RouteGuideClient : Service {
          |  @WireRpc(
          |    path = "/RouteGuide/GetFeature",
          |    requestAdapter = "Point#ADAPTER",
          |    responseAdapter = "Feature#ADAPTER"
          |  )
          |  suspend fun GetFeature(request: Point): Feature
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |service RouteGuide {
          |  rpc GetFeature(Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |""".trimMargin())
    assertEquals(expected, repoBuilder.generateGrpcKotlin("RouteGuide"))
  }

  @Test fun multiDepthPackage() {
    val expected = """
          |package routeguide.grpc
          |
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |
          |interface RouteGuideClient : Service {
          |  @WireRpc(
          |    path = "/routeguide.grpc.RouteGuide/GetFeature",
          |    requestAdapter = "routeguide.grpc.Point#ADAPTER",
          |    responseAdapter = "routeguide.grpc.Feature#ADAPTER"
          |  )
          |  suspend fun GetFeature(request: Point): Feature
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |package routeguide.grpc;
          |
          |service RouteGuide {
          |  rpc GetFeature(Point) returns (Feature) {}
          |}
          |$pointMessage
          |$featureMessage
          |""".trimMargin())
    assertEquals(expected, repoBuilder.generateGrpcKotlin("routeguide.grpc.RouteGuide"))
  }

  @Test fun streamingRequest() {
    val expected = """
          |package routeguide
          |
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |import kotlin.Pair
          |import kotlinx.coroutines.Deferred
          |import kotlinx.coroutines.channels.SendChannel
          |
          |interface RouteGuideClient : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/RecordRoute",
          |    requestAdapter = "routeguide.Point#ADAPTER",
          |    responseAdapter = "routeguide.RouteSummary#ADAPTER"
          |  )
          |  fun RecordRoute(): Pair<SendChannel<Point>, Deferred<RouteSummary>>
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |package routeguide;
          |
          |service RouteGuide {
          |  rpc RecordRoute(stream Point) returns (RouteSummary) {}
          |}
          |$pointMessage
          |$routeSummaryMessage
          |""".trimMargin())
    assertEquals(expected, repoBuilder.generateGrpcKotlin("routeguide.RouteGuide"))
  }

  @Test fun streamingResponse() {
    val expected = """
          |package routeguide
          |
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |import kotlinx.coroutines.channels.ReceiveChannel
          |
          |interface RouteGuideClient : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/ListFeatures",
          |    requestAdapter = "routeguide.Rectangle#ADAPTER",
          |    responseAdapter = "routeguide.Feature#ADAPTER"
          |  )
          |  fun ListFeatures(request: Rectangle): ReceiveChannel<Feature>
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |package routeguide;
          |
          |service RouteGuide {
          |  rpc ListFeatures(Rectangle) returns (stream Feature) {}
          |}
          |$rectangeMessage
          |$pointMessage
          |$featureMessage
          |""".trimMargin())
    assertEquals(expected, repoBuilder.generateGrpcKotlin("routeguide.RouteGuide"))
  }

  @Test fun bidirectional() {
    val blockingClient = """
          |package routeguide
          |
          |import com.squareup.wire.MessageSink
          |import com.squareup.wire.MessageSource
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |import kotlin.Pair
          |
          |interface RouteGuideBlockingClient : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/RouteChat",
          |    requestAdapter = "routeguide.RouteNote#ADAPTER",
          |    responseAdapter = "routeguide.RouteNote#ADAPTER"
          |  )
          |  fun RouteChat(): Pair<MessageSource<RouteNote>, MessageSink<RouteNote>>
          |}
          |""".trimMargin()
    val blockingServer = """
          |package routeguide
          |
          |import com.squareup.wire.MessageSink
          |import com.squareup.wire.MessageSource
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |
          |interface RouteGuideBlockingServer : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/RouteChat",
          |    requestAdapter = "routeguide.RouteNote#ADAPTER",
          |    responseAdapter = "routeguide.RouteNote#ADAPTER"
          |  )
          |  fun RouteChat(request: MessageSource<RouteNote>, response: MessageSink<RouteNote>)
          |}
          |""".trimMargin()
    val suspendingClient = """
          |package routeguide
          |
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |import kotlin.Pair
          |import kotlinx.coroutines.channels.ReceiveChannel
          |import kotlinx.coroutines.channels.SendChannel
          |
          |interface RouteGuideClient : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/RouteChat",
          |    requestAdapter = "routeguide.RouteNote#ADAPTER",
          |    responseAdapter = "routeguide.RouteNote#ADAPTER"
          |  )
          |  fun RouteChat(): Pair<SendChannel<RouteNote>, ReceiveChannel<RouteNote>>
          |}
          |""".trimMargin()
    val suspendingServer = """
          |package routeguide
          |
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |import kotlinx.coroutines.channels.ReceiveChannel
          |import kotlinx.coroutines.channels.SendChannel
          |
          |interface RouteGuideServer : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/RouteChat",
          |    requestAdapter = "routeguide.RouteNote#ADAPTER",
          |    responseAdapter = "routeguide.RouteNote#ADAPTER"
          |  )
          |  suspend fun RouteChat(request: SendChannel<RouteNote>, response: ReceiveChannel<RouteNote>)
          |}
          |""".trimMargin()

    val repoBuilder = RepoBuilder()
        .add("routeguide.proto", """
          |package routeguide;
          |
          |service RouteGuide {
          |  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {}
          |}
          |$pointMessage
          |$routeNoteMessage
          |""".trimMargin())
    assertEquals(blockingClient, repoBuilder.generateGrpcKotlin("routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING, rpcRole = RpcRole.CLIENT))
    assertEquals(blockingServer, repoBuilder.generateGrpcKotlin("routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.BLOCKING, rpcRole = RpcRole.SERVER))
    assertEquals(suspendingClient, repoBuilder.generateGrpcKotlin("routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.SUSPENDING, rpcRole = RpcRole.CLIENT))
    assertEquals(suspendingServer, repoBuilder.generateGrpcKotlin("routeguide.RouteGuide",
        rpcCallStyle = RpcCallStyle.SUSPENDING, rpcRole = RpcRole.SERVER))
  }

  @Test fun multipleRpcs() {
    val expected = """
          |package routeguide
          |
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |import kotlin.Pair
          |import kotlinx.coroutines.channels.ReceiveChannel
          |import kotlinx.coroutines.channels.SendChannel
          |
          |interface RouteGuideClient : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/GetFeature",
          |    requestAdapter = "routeguide.Point#ADAPTER",
          |    responseAdapter = "routeguide.Feature#ADAPTER"
          |  )
          |  suspend fun GetFeature(request: Point): Feature
          |
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/RouteChat",
          |    requestAdapter = "routeguide.RouteNote#ADAPTER",
          |    responseAdapter = "routeguide.RouteNote#ADAPTER"
          |  )
          |  fun RouteChat(): Pair<SendChannel<RouteNote>, ReceiveChannel<RouteNote>>
          |}
          |""".trimMargin()

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
    assertEquals(expected, repoBuilder.generateGrpcKotlin("routeguide.RouteGuide"))
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

    val expectedGetFeature = """
          |package routeguide
          |
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |
          |interface RouteGuideGetFeatureClient : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/GetFeature",
          |    requestAdapter = "routeguide.Point#ADAPTER",
          |    responseAdapter = "routeguide.Feature#ADAPTER"
          |  )
          |  suspend fun GetFeature(request: Point): Feature
          |}
          |""".trimMargin()

    assertEquals(expectedGetFeature,
        repoBuilder.generateGrpcKotlin("routeguide.RouteGuide", "GetFeature"))

    val expectedRouteChat = """
          |package routeguide
          |
          |import com.squareup.wire.Service
          |import com.squareup.wire.WireRpc
          |import kotlin.Pair
          |import kotlinx.coroutines.channels.ReceiveChannel
          |import kotlinx.coroutines.channels.SendChannel
          |
          |interface RouteGuideRouteChatClient : Service {
          |  @WireRpc(
          |    path = "/routeguide.RouteGuide/RouteChat",
          |    requestAdapter = "routeguide.RouteNote#ADAPTER",
          |    responseAdapter = "routeguide.RouteNote#ADAPTER"
          |  )
          |  fun RouteChat(): Pair<SendChannel<RouteNote>, ReceiveChannel<RouteNote>>
          |}
          |""".trimMargin()
    assertEquals(expectedRouteChat,
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
