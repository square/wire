Wire gRPC
=========

The Wire compiler will generate interfaces for your service RPCs defined in your protobuf schema;
under the condition that the target is set to Kotlin.

Getting Started
---------------

You can configure how your services are generated via the [Gradle plugin][gradlePlugin]:

```groovy
wire {
  // Kotlin target will generate code for services.
  kotlin {
    // `client` to generate interfaces best suited to sending outbound calls.
    // `server` to generate interfaces best suited to receiving inbound calls.
    rpcRole = 'server'
    // Server only
    // `suspending` to generate coroutines APIs that require a Kotlin coroutines context.
    // `blocking` to generate blocking APIs callable by Java and Kotlin.
    rpcCallStyle = 'suspending'
    // Server only
    // True for emitted services to generate one interface per RPC.
    singleMethodServices = false
  }
}
```

The generated code varies depending on your RPC role, namely client or server.
We'll use the following schema to demonstrate how they differ:

```proto
syntax = "proto2";

package routeguide;

service RouteGuide {
  // A simple RPC.
  rpc GetFeature(Point) returns (Feature) {}

  // A server-to-client streaming RPC.
  rpc ListFeatures(Rectangle) returns (stream Feature) {}

  // A client-to-server streaming RPC.
  rpc RecordRoute(stream Point) returns (RouteSummary) {}

  // A Bidirectional streaming RPC.
  rpc RouteChat(stream RouteNote) returns (stream RouteNote) {}
}
```

gRPC for Clients
----------------
For the schema `RouteGuide`, when `rpcRole` is `client`, the Wire compiler will generate the
following interface:
```kotlin
interface RouteGuideClient : Service {
  fun GetFeature(): GrpcCall<Point, Feature>

  fun ListFeatures(): GrpcStreamingCall<Rectangle, Feature>

  fun RecordRoute(): GrpcStreamingCall<Point, RouteSummary>

  fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote>
}
```

For streaming APIs, we return a [GrpcStreamingCall][grpcStreamingCall], and a [GrpcCall][grpcCall]
otherwise. With these objects, the caller can communicate with the server. Both blocking and
suspending APIs are provided.

### Wire runtime

To help you instantiate your service, we provide a small runtime, namely
[GrpcClient][grpcClient]:

```kotlin
val grpcClient = GrpcClient.Builder()
  .client(OkHttpClient.Builder().protocols(listOf(Protocol.H2_PRIOR_KNOWLEDGE)).build())
  .baseUrl(serverUrl)
  .build()
val routeGuideClient = grpcClient.create(RouteGuideClient::class)
```

gRPC for Servers
----------------

### Blocking APIs

Following this configuration:
```groovy
wire {
  kotlin {
    rpcRole = 'server'
    rpcCallStyle = 'blocking'
  }
}
```
the Wire compiler will generate the following interface for the server to implement:
```kotlin
interface RouteGuideBlockingServer : Service {
  fun GetFeature(request: Point): Feature

  fun ListFeatures(request: Rectangle, response: MessageSink<Feature>)

  fun RecordRoute(request: MessageSource<Point>): RouteSummary

  fun RouteChat(request: MessageSource<RouteNote>, response: MessageSink<RouteNote>)
}
```
For streaming connections, the server can use Wire's blocking APIs: [MessageSource][messageSource]
and [MessageSink][messageSink].

### Suspending APIs

Following this configuration:
```groovy
wire {
  kotlin {
    rpcRole = 'server'
    rpcCallStyle = 'suspending'
  }
}
```
the Wire compiler will generate the following interface for the server to implement:
```kotlin
interface RouteGuideServer : Service {
  suspend fun GetFeature(request: Point): Feature

  fun ListFeatures(request: Rectangle, response: SendChannel<Feature>)

  fun RecordRoute(request: ReceiveChannel<Point>): RouteSummary

  fun RouteChat(request: ReceiveChannel<RouteNote>, response: SendChannel<RouteNote>)
}
```
The server can use Kotlin coroutines' suspend and Channels mechanisms to execute suspending network
calls.

Implementing Client Interfaces
------------------------------

Wire has helper functions to make it easier to implement its client interfaces. This can be
particularly useful for testing. It supports both streaming and non-streaming APIs in Kotlin:

```kotlin
class FakeRouteGuideClient : RouteGuideClient {
  override fun GetFeature(): GrpcCall<Point, Feature> {
    return GrpcCall { request: Point ->
      return@GrpcCall Feature(name = "test", location = request)
    }
  }

  override fun RouteChat(): GrpcStreamingCall<RouteNote, RouteNote> {
    return GrpcStreamingCall { requests: ReceiveChannel<RouteNote>, responses: SendChannel<RouteNote> ->
      try {
        requests.consumeEach { routeNote: RouteNote ->
          responses.send(RouteNote(message = "ACK: ${routeNote.message}"))
        }
      } finally {
        responses.close()
      }
    }
  }

  ...
}
```

The `GrpcCall` and `GrpcStreamingCall` functions are well suited to expression functions:

```kotlin
class FakeRouteGuideClient : RouteGuideClient {
  override fun GetFeature() =
      GrpcCall<Point, Feature> { request ->
        return@GrpcCall Feature(name = "test", location = request)
      }

  override fun RouteChat() =
      GrpcStreamingCall<RouteNote, RouteNote> { requests, responses ->
        try {
          requests.consumeEach { routeNote ->
            responses.send(RouteNote(message = "ACK: ${routeNote.message}"))
          }
        } finally {
          responses.close()
        }
      }

  ...
}
```

The client interface may also be implemented in Java. Wire only offers a non-streaming helper
function. The `GrpcStreamingCall` above uses coroutines which is Kotlin-only.

```java
public class FakeRouteGuideClient implements RouteGuideClient {
  @Override public GrpcCall<Point, Feature> GetFeature() {
    return GrpcCalls.grpcCall(new Function1<Point, Feature>() {
      @Override public Feature invoke(Point request) {
        return new Feature.Builder()
            .name("test")
            .location(request)
            .build();
      }
    });
  }

  ...
}
```

These similarly interact nicely with Java lambdas.


Sample
------

Check out our [sample project][sampleProject] demonstrating bidirectional communication between an
Android application and a server.

Get it
------

Use Wire runtime Gradle coordinates:
```groovy
implementation("com.squareup.wire:wire-runtime:LATEST_VERSION")
```

In addition, to use Wire gRPC for clients, use these new Gradle coordinates:

```groovy
implementation("com.squareup.wire:wire-grpc-client:LATEST_VERSION")
```

wire-grpc-server
----------------

The modules `wire-grpc-server` and `wire-grpc-server-generator` have been extracted out of Wire 5.
They now live as a standalone repository [square/wire-grpc-server/](https://github.com/square/wire-grpc-server/).

Here are the steps for a smooth migration:

### Maven Coordinates

Update the coordinates for `com.squareup.wire:wire-grpc-server`.
```diff
-com.squareup.wire:wire-grpc-server:<wire-version>
+com.squareup.wiregrpcserver:server:<new-repo-version>
```

Add a new dependency on the classpath Wire will run on.

```kotlin
classpath("com.squareup.wiregrpcserver:server-generator:<new-repo-version>")
```

### Configuration

Before
```kotlin
wire {
  kotlin {
    rpcRole = "server"
    grpcServerCompatible = true
    singleMethodServices = false
    rpcCallStyle = "suspending"
  }
}
```

`grpcServerCompatible` does not exist anymore. You are to pass the new `GrpcServerSchemaHandler` to
Wire in a custom block.

After
```kotlin
wire {
  custom {
    // Be sure that `server-generator` is on the classpath for Gradle to resolve
    // `GrpcServerSchemaHandler`.
    schemaHandlerFactory = com.squareup.wire.kotlin.grpcserver.GrpcServerSchemaHandler.Factory()
    options = mapOf(
      // Defaults to `true` if absent. Any other value than `true` is considered false.
      "singleMethodServices" to "false",
      // Defaults to `suspending` if absent. Any other value than `suspending` is considered
      // non-suspending.
      "rpcCallStyle" to "suspending",
    )
    // We set the custom block exclusivity to false so that the next `kotlin {}` block can also
    // generate the protobuf Messages.
    exclusive = false
  }

  kotlin {
    rpcRole = "server"
    singleMethodServices = false
    rpcCallStyle = "suspending"
  }
}
```

For any problem with the migration, please ask on [wire-grpc-server](https://github.com/square/wire-grpc-server/issues).


 [gradlePlugin]: https://square.github.io/wire/wire_compiler/
 [grpcCall]: https://square.github.io/wire/3.x/wire-grpc-client/wire-grpc-client/com.squareup.wire/-grpc-call/
 [grpcClient]: https://square.github.io/wire/3.x/wire-grpc-client/wire-grpc-client/com.squareup.wire/-grpc-client/
 [grpcStreamingCall]: https://square.github.io/wire/3.x/wire-grpc-client/wire-grpc-client/com.squareup.wire/-grpc-streaming-call/
 [messageSink]: https://square.github.io/wire/3.x/wire-runtime/wire-runtime/com.squareup.wire/-message-sink/
 [messageSource]: https://square.github.io/wire/3.x/wire-runtime/wire-runtime/com.squareup.wire/-message-source/
 [sampleProject]: https://github.com/square/wire/tree/master/samples/wire-grpc-sample
