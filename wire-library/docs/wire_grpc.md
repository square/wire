Wire gRPC
=============================

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

 [gradlePlugin]: https://square.github.io/wire/wire_compiler/
 [grpcCall]: https://square.github.io/wire/3.x/wire-grpc-client/com.squareup.wire/-grpc-call/
 [grpcClient]: https://square.github.io/wire/3.x/wire-grpc-client/com.squareup.wire/-grpc-client/
 [grpcStreamingCall]: https://square.github.io/wire/3.x/wire-grpc-client/com.squareup.wire/-grpc-streaming-call/
 [messageSink]: https://square.github.io/wire/3.x/wire-runtime/com.squareup.wire/-message-sink/
 [messageSource]: https://square.github.io/wire/3.x/wire-runtime/com.squareup.wire/-message-source/
 [sampleProject]: https://github.com/square/wire/tree/master/wire-grpc-sample
