package routeguide

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import com.squareup.wire.kotlin.grpcserver.MessageSinkAdapter
import com.squareup.wire.kotlin.grpcserver.MessageSourceAdapter
import com.squareup.wire.kotlin.grpcserver.WireBindableService
import com.squareup.wire.kotlin.grpcserver.WireMethodMarshaller
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.ServiceDescriptor
import io.grpc.ServiceDescriptor.newBuilder
import io.grpc.stub.AbstractStub
import io.grpc.stub.ClientCalls.blockingServerStreamingCall
import io.grpc.stub.ClientCalls.blockingUnaryCall
import io.grpc.stub.StreamObserver
import java.io.InputStream
import java.lang.Class
import java.lang.UnsupportedOperationException
import java.util.concurrent.ExecutorService
import kotlin.Array
import kotlin.String
import kotlin.Unit
import kotlin.collections.Iterator
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.jvm.Volatile
import io.grpc.stub.ClientCalls.asyncBidiStreamingCall as clientCallsAsyncBidiStreamingCall
import io.grpc.stub.ClientCalls.asyncClientStreamingCall as clientCallsAsyncClientStreamingCall
import io.grpc.stub.ClientCalls.asyncServerStreamingCall as clientCallsAsyncServerStreamingCall
import io.grpc.stub.ClientCalls.asyncUnaryCall as clientCallsAsyncUnaryCall
import io.grpc.stub.ServerCalls.asyncBidiStreamingCall as serverCallsAsyncBidiStreamingCall
import io.grpc.stub.ServerCalls.asyncClientStreamingCall as serverCallsAsyncClientStreamingCall
import io.grpc.stub.ServerCalls.asyncServerStreamingCall as serverCallsAsyncServerStreamingCall
import io.grpc.stub.ServerCalls.asyncUnaryCall as serverCallsAsyncUnaryCall

public object RouteGuideWireGrpc {
  public val SERVICE_NAME: String = "routeguide.RouteGuide"

  @Volatile
  private var serviceDescriptor: ServiceDescriptor? = null

  private val descriptorMap: Map<String, DescriptorProtos.FileDescriptorProto> = mapOf(
    "src/test/proto/RouteGuideProto.proto" to descriptorFor(arrayOf(
      "CiRzcmMvdGVzdC9wcm90by9Sb3V0ZUd1aWRlUHJvdG8ucHJvdG8SCnJvdXRlZ3VpZGUiLAoFUG9pbnQS",
      "EAoIbGF0aXR1ZGUYASABKAUSEQoJbG9uZ2l0dWRlGAIgASgFIkkKCVJlY3RhbmdsZRIdCgJsbxgBIAEo",
      "CzIRLnJvdXRlZ3VpZGUuUG9pbnQSHQoCaGkYAiABKAsyES5yb3V0ZWd1aWRlLlBvaW50IjwKB0ZlYXR1",
      "cmUSDAoEbmFtZRgBIAEoCRIjCghsb2NhdGlvbhgCIAEoCzIRLnJvdXRlZ3VpZGUuUG9pbnQiNwoPRmVh",
      "dHVyZURhdGFiYXNlEiQKB2ZlYXR1cmUYASADKAsyEy5yb3V0ZWd1aWRlLkZlYXR1cmUiQQoJUm91dGVO",
      "b3RlEiMKCGxvY2F0aW9uGAEgASgLMhEucm91dGVndWlkZS5Qb2ludBIPCgdtZXNzYWdlGAIgASgJImIK",
      "DFJvdXRlU3VtbWFyeRITCgtwb2ludF9jb3VudBgBIAEoBRIVCg1mZWF0dXJlX2NvdW50GAIgASgFEhAK",
      "CGRpc3RhbmNlGAMgASgFEhQKDGVsYXBzZWRfdGltZRgEIAEoBTL9AQoKUm91dGVHdWlkZRI0CgpHZXRG",
      "ZWF0dXJlEhEucm91dGVndWlkZS5Qb2ludBoTLnJvdXRlZ3VpZGUuRmVhdHVyZRI8CgxMaXN0RmVhdHVy",
      "ZXMSFS5yb3V0ZWd1aWRlLlJlY3RhbmdsZRoTLnJvdXRlZ3VpZGUuRmVhdHVyZTABEjwKC1JlY29yZFJv",
      "dXRlEhEucm91dGVndWlkZS5Qb2ludBoYLnJvdXRlZ3VpZGUuUm91dGVTdW1tYXJ5KAESPQoJUm91dGVD",
      "aGF0EhUucm91dGVndWlkZS5Sb3V0ZU5vdGUaFS5yb3V0ZWd1aWRlLlJvdXRlTm90ZSgBMAE=",
    )),
  )


  @Volatile
  private var getGetFeatureMethod: MethodDescriptor<Point, Feature>? = null

  @Volatile
  private var getListFeaturesMethod: MethodDescriptor<Rectangle, Feature>? = null

  @Volatile
  private var getRecordRouteMethod: MethodDescriptor<Point, RouteSummary>? = null

  @Volatile
  private var getRouteChatMethod: MethodDescriptor<RouteNote, RouteNote>? = null

  private fun descriptorFor(`data`: Array<String>): DescriptorProtos.FileDescriptorProto {
    val str = data.fold(java.lang.StringBuilder()) { b, s -> b.append(s) }.toString()
    val bytes = java.util.Base64.getDecoder().decode(str)
    return DescriptorProtos.FileDescriptorProto.parseFrom(bytes)
  }

  private fun fileDescriptor(path: String, visited: Set<String>): Descriptors.FileDescriptor {
    val proto = descriptorMap[path]!!
    val deps = proto.dependencyList.filter { !visited.contains(it) }.map { fileDescriptor(it,
        visited + path) }
    return Descriptors.FileDescriptor.buildFrom(proto, deps.toTypedArray())
  }

  public fun getServiceDescriptor(): ServiceDescriptor? {
    var result = serviceDescriptor
    if (result == null) {
      synchronized(RouteGuideWireGrpc::class) {
        result = serviceDescriptor
        if (result == null) {
          result = newBuilder(SERVICE_NAME)
          .addMethod(getGetFeatureMethod())
          .addMethod(getListFeaturesMethod())
          .addMethod(getRecordRouteMethod())
          .addMethod(getRouteChatMethod())
          .setSchemaDescriptor(io.grpc.protobuf.ProtoFileDescriptorSupplier {
                fileDescriptor("src/test/proto/RouteGuideProto.proto", emptySet())
              })
          .build()
          serviceDescriptor = result
        }
      }
    }
    return result
  }

  public fun getGetFeatureMethod(): MethodDescriptor<Point, Feature> {
    var result: MethodDescriptor<Point, Feature>? = getGetFeatureMethod
    if (result == null) {
      synchronized(RouteGuideWireGrpc::class) {
        result = getGetFeatureMethod
        if (result == null) {
          getGetFeatureMethod = MethodDescriptor.newBuilder<Point, Feature>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(
              MethodDescriptor.generateFullMethodName(
                "routeguide.RouteGuide", "GetFeature"
              )
            )
            .setSampledToLocalTracing(true)
            .setRequestMarshaller(RouteGuideImplBase.PointMarshaller())
            .setResponseMarshaller(RouteGuideImplBase.FeatureMarshaller())
            .build()
        }
      }
    }
    return getGetFeatureMethod!!
  }

  public fun getListFeaturesMethod(): MethodDescriptor<Rectangle, Feature> {
    var result: MethodDescriptor<Rectangle, Feature>? = getListFeaturesMethod
    if (result == null) {
      synchronized(RouteGuideWireGrpc::class) {
        result = getListFeaturesMethod
        if (result == null) {
          getListFeaturesMethod = MethodDescriptor.newBuilder<Rectangle, Feature>()
            .setType(MethodDescriptor.MethodType.SERVER_STREAMING)
            .setFullMethodName(
              MethodDescriptor.generateFullMethodName(
                "routeguide.RouteGuide", "ListFeatures"
              )
            )
            .setSampledToLocalTracing(true)
            .setRequestMarshaller(RouteGuideImplBase.RectangleMarshaller())
            .setResponseMarshaller(RouteGuideImplBase.FeatureMarshaller())
            .build()
        }
      }
    }
    return getListFeaturesMethod!!
  }

  public fun getRecordRouteMethod(): MethodDescriptor<Point, RouteSummary> {
    var result: MethodDescriptor<Point, RouteSummary>? = getRecordRouteMethod
    if (result == null) {
      synchronized(RouteGuideWireGrpc::class) {
        result = getRecordRouteMethod
        if (result == null) {
          getRecordRouteMethod = MethodDescriptor.newBuilder<Point, RouteSummary>()
            .setType(MethodDescriptor.MethodType.CLIENT_STREAMING)
            .setFullMethodName(
              MethodDescriptor.generateFullMethodName(
                "routeguide.RouteGuide", "RecordRoute"
              )
            )
            .setSampledToLocalTracing(true)
            .setRequestMarshaller(RouteGuideImplBase.PointMarshaller())
            .setResponseMarshaller(RouteGuideImplBase.RouteSummaryMarshaller())
            .build()
        }
      }
    }
    return getRecordRouteMethod!!
  }

  public fun getRouteChatMethod(): MethodDescriptor<RouteNote, RouteNote> {
    var result: MethodDescriptor<RouteNote, RouteNote>? = getRouteChatMethod
    if (result == null) {
      synchronized(RouteGuideWireGrpc::class) {
        result = getRouteChatMethod
        if (result == null) {
          getRouteChatMethod = MethodDescriptor.newBuilder<RouteNote, RouteNote>()
            .setType(MethodDescriptor.MethodType.BIDI_STREAMING)
            .setFullMethodName(
              MethodDescriptor.generateFullMethodName(
                "routeguide.RouteGuide", "RouteChat"
              )
            )
            .setSampledToLocalTracing(true)
            .setRequestMarshaller(RouteGuideImplBase.RouteNoteMarshaller())
            .setResponseMarshaller(RouteGuideImplBase.RouteNoteMarshaller())
            .build()
        }
      }
    }
    return getRouteChatMethod!!
  }

  public fun newStub(channel: Channel): RouteGuideStub = RouteGuideStub(channel)

  public fun newBlockingStub(channel: Channel): RouteGuideBlockingStub =
      RouteGuideBlockingStub(channel)

  public abstract class RouteGuideImplBase : WireBindableService {
    public open fun GetFeature(request: Point, response: StreamObserver<Feature>): Unit = throw
        UnsupportedOperationException()

    public open fun ListFeatures(request: Rectangle, response: StreamObserver<Feature>): Unit =
        throw UnsupportedOperationException()

    public open fun RecordRoute(response: StreamObserver<RouteSummary>): StreamObserver<Point> =
        throw UnsupportedOperationException()

    public open fun RouteChat(response: StreamObserver<RouteNote>): StreamObserver<RouteNote> =
        throw UnsupportedOperationException()

    override fun bindService(): ServerServiceDefinition =
        ServerServiceDefinition.builder(getServiceDescriptor()).addMethod(
              getGetFeatureMethod(),
              serverCallsAsyncUnaryCall(this@RouteGuideImplBase::GetFeature)
            ).addMethod(
              getListFeaturesMethod(),
              serverCallsAsyncServerStreamingCall(this@RouteGuideImplBase::ListFeatures)
            ).addMethod(
              getRecordRouteMethod(),
              serverCallsAsyncClientStreamingCall(this@RouteGuideImplBase::RecordRoute)
            ).addMethod(
              getRouteChatMethod(),
              serverCallsAsyncBidiStreamingCall(this@RouteGuideImplBase::RouteChat)
            ).build()

    public class PointMarshaller : WireMethodMarshaller<Point> {
      override fun stream(`value`: Point): InputStream = Point.ADAPTER.encode(value).inputStream()

      override fun marshalledClass(): Class<Point> = Point::class.java

      override fun parse(stream: InputStream): Point = Point.ADAPTER.decode(stream)
    }

    public class FeatureMarshaller : WireMethodMarshaller<Feature> {
      override fun stream(`value`: Feature): InputStream =
          Feature.ADAPTER.encode(value).inputStream()

      override fun marshalledClass(): Class<Feature> = Feature::class.java

      override fun parse(stream: InputStream): Feature = Feature.ADAPTER.decode(stream)
    }

    public class RectangleMarshaller : WireMethodMarshaller<Rectangle> {
      override fun stream(`value`: Rectangle): InputStream =
          Rectangle.ADAPTER.encode(value).inputStream()

      override fun marshalledClass(): Class<Rectangle> = Rectangle::class.java

      override fun parse(stream: InputStream): Rectangle = Rectangle.ADAPTER.decode(stream)
    }

    public class RouteSummaryMarshaller : WireMethodMarshaller<RouteSummary> {
      override fun stream(`value`: RouteSummary): InputStream =
          RouteSummary.ADAPTER.encode(value).inputStream()

      override fun marshalledClass(): Class<RouteSummary> = RouteSummary::class.java

      override fun parse(stream: InputStream): RouteSummary = RouteSummary.ADAPTER.decode(stream)
    }

    public class RouteNoteMarshaller : WireMethodMarshaller<RouteNote> {
      override fun stream(`value`: RouteNote): InputStream =
          RouteNote.ADAPTER.encode(value).inputStream()

      override fun marshalledClass(): Class<RouteNote> = RouteNote::class.java

      override fun parse(stream: InputStream): RouteNote = RouteNote.ADAPTER.decode(stream)
    }
  }

  public class BindableAdapter(
    private val streamExecutor: ExecutorService,
    private val GetFeature: () -> RouteGuideGetFeatureBlockingServer,
    private val ListFeatures: () -> RouteGuideListFeaturesBlockingServer,
    private val RecordRoute: () -> RouteGuideRecordRouteBlockingServer,
    private val RouteChat: () -> RouteGuideRouteChatBlockingServer,
  ) : RouteGuideImplBase() {
    override fun GetFeature(request: Point, response: StreamObserver<Feature>) {
      response.onNext(GetFeature().GetFeature(request))
      response.onCompleted()
    }

    override fun ListFeatures(request: Rectangle, response: StreamObserver<Feature>) {
      ListFeatures().ListFeatures(request, MessageSinkAdapter(response))
    }

    override fun RecordRoute(response: StreamObserver<RouteSummary>): StreamObserver<Point> {
      val requestStream = MessageSourceAdapter<Point>()
      streamExecutor.submit {
        response.onNext(RecordRoute().RecordRoute(requestStream))
        response.onCompleted()
      }
      return requestStream
    }

    override fun RouteChat(response: StreamObserver<RouteNote>): StreamObserver<RouteNote> {
      val requestStream = MessageSourceAdapter<RouteNote>()
      streamExecutor.submit {
        RouteChat().RouteChat(requestStream, MessageSinkAdapter(response))
      }
      return requestStream
    }
  }

  public class RouteGuideStub : AbstractStub<RouteGuideStub> {
    internal constructor(channel: Channel) : super(channel)

    internal constructor(channel: Channel, callOptions: CallOptions) : super(channel, callOptions)

    override fun build(channel: Channel, callOptions: CallOptions): RouteGuideStub =
        RouteGuideStub(channel, callOptions)

    public fun GetFeature(request: Point, response: StreamObserver<Feature>) {
      clientCallsAsyncUnaryCall(channel.newCall(getGetFeatureMethod(), callOptions), request,
          response)
    }

    public fun ListFeatures(request: Rectangle, response: StreamObserver<Feature>) {
      clientCallsAsyncServerStreamingCall(channel.newCall(getListFeaturesMethod(), callOptions),
          request, response)
    }

    public fun RecordRoute(response: StreamObserver<RouteSummary>): StreamObserver<Point> =
        clientCallsAsyncClientStreamingCall(channel.newCall(getRecordRouteMethod(), callOptions),
        response)

    public fun RouteChat(response: StreamObserver<RouteNote>): StreamObserver<RouteNote> =
        clientCallsAsyncBidiStreamingCall(channel.newCall(getRouteChatMethod(), callOptions),
        response)
  }

  public class RouteGuideBlockingStub : AbstractStub<RouteGuideStub> {
    internal constructor(channel: Channel) : super(channel)

    internal constructor(channel: Channel, callOptions: CallOptions) : super(channel, callOptions)

    override fun build(channel: Channel, callOptions: CallOptions): RouteGuideStub =
        RouteGuideStub(channel, callOptions)

    public fun GetFeature(request: Point): Feature = blockingUnaryCall(channel,
        getGetFeatureMethod(), callOptions, request)

    public fun RecordRoute(request: Point): Iterator<RouteSummary> =
        blockingServerStreamingCall(channel, getRecordRouteMethod(), callOptions, request)
  }
}
