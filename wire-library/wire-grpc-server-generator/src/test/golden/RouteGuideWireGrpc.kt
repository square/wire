package routeguide

import com.squareup.wire.kotlin.grpcserver.MessageSinkAdapter
import com.squareup.wire.kotlin.grpcserver.MessageSourceAdapter
import io.grpc.BindableService
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.ServiceDescriptor
import io.grpc.ServiceDescriptor.newBuilder
import io.grpc.stub.AbstractStub
import io.grpc.stub.ClientCalls
import io.grpc.stub.ClientCalls.blockingServerStreamingCall
import io.grpc.stub.ClientCalls.blockingUnaryCall
import io.grpc.stub.ServerCalls.asyncBidiStreamingCall
import io.grpc.stub.ServerCalls.asyncClientStreamingCall
import io.grpc.stub.ServerCalls.asyncServerStreamingCall
import io.grpc.stub.ServerCalls.asyncUnaryCall
import io.grpc.stub.StreamObserver
import java.io.InputStream
import java.lang.UnsupportedOperationException
import java.util.concurrent.ExecutorService
import kotlin.String
import kotlin.Unit
import kotlin.collections.Iterator
import kotlin.jvm.Volatile

public object RouteGuideWireGrpc {
  public val SERVICE_NAME: String = "routeguide.RouteGuide"

  @Volatile
  private var serviceDescriptor: ServiceDescriptor? = null

  @Volatile
  private var getGetFeatureMethod: MethodDescriptor<Point, Feature>? = null

  @Volatile
  private var getListFeaturesMethod: MethodDescriptor<Rectangle, Feature>? = null

  @Volatile
  private var getRecordRouteMethod: MethodDescriptor<Point, RouteSummary>? = null

  @Volatile
  private var getRouteChatMethod: MethodDescriptor<RouteNote, RouteNote>? = null

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

  public abstract class RouteGuideImplBase : BindableService {
    public open fun GetFeature(request: Point, response: StreamObserver<Feature>): Unit = throw
        UnsupportedOperationException()

    public open fun ListFeatures(request: Rectangle, response: StreamObserver<Feature>): Unit =
        throw UnsupportedOperationException()

    public open fun RecordRoute(response: StreamObserver<RouteSummary>): StreamObserver<Point> =
        throw UnsupportedOperationException()

    public open fun RouteChat(response: StreamObserver<RouteNote>): StreamObserver<RouteNote> =
        throw UnsupportedOperationException()

    public override fun bindService(): ServerServiceDefinition =
        ServerServiceDefinition.builder(getServiceDescriptor()).addMethod(
              getGetFeatureMethod(),
              asyncUnaryCall(this@RouteGuideImplBase::GetFeature)
            ).addMethod(
              getListFeaturesMethod(),
              asyncServerStreamingCall(this@RouteGuideImplBase::ListFeatures)
            ).addMethod(
              getRecordRouteMethod(),
              asyncClientStreamingCall(this@RouteGuideImplBase::RecordRoute)
            ).addMethod(
              getRouteChatMethod(),
              asyncBidiStreamingCall(this@RouteGuideImplBase::RouteChat)
            ).build()

    public class PointMarshaller : MethodDescriptor.Marshaller<Point> {
      public override fun stream(`value`: Point): InputStream =
          Point.ADAPTER.encode(value).inputStream()

      public override fun parse(stream: InputStream): Point = Point.ADAPTER.decode(stream)
    }

    public class FeatureMarshaller : MethodDescriptor.Marshaller<Feature> {
      public override fun stream(`value`: Feature): InputStream =
          Feature.ADAPTER.encode(value).inputStream()

      public override fun parse(stream: InputStream): Feature = Feature.ADAPTER.decode(stream)
    }

    public class RectangleMarshaller : MethodDescriptor.Marshaller<Rectangle> {
      public override fun stream(`value`: Rectangle): InputStream =
          Rectangle.ADAPTER.encode(value).inputStream()

      public override fun parse(stream: InputStream): Rectangle = Rectangle.ADAPTER.decode(stream)
    }

    public class RouteSummaryMarshaller : MethodDescriptor.Marshaller<RouteSummary> {
      public override fun stream(`value`: RouteSummary): InputStream =
          RouteSummary.ADAPTER.encode(value).inputStream()

      public override fun parse(stream: InputStream): RouteSummary =
          RouteSummary.ADAPTER.decode(stream)
    }

    public class RouteNoteMarshaller : MethodDescriptor.Marshaller<RouteNote> {
      public override fun stream(`value`: RouteNote): InputStream =
          RouteNote.ADAPTER.encode(value).inputStream()

      public override fun parse(stream: InputStream): RouteNote = RouteNote.ADAPTER.decode(stream)
    }
  }

  public class RouteGuideImplLegacyAdapter(
    private val streamExecutor: ExecutorService,
    private val GetFeature: () -> RouteGuideGetFeatureBlockingServer,
    private val ListFeatures: () -> RouteGuideListFeaturesBlockingServer,
    private val RecordRoute: () -> RouteGuideRecordRouteBlockingServer,
    private val RouteChat: () -> RouteGuideRouteChatBlockingServer
  ) : RouteGuideImplBase() {
    public override fun GetFeature(request: Point, response: StreamObserver<Feature>): Unit {
      response.onNext(GetFeature().GetFeature(request))
      response.onCompleted()
    }

    public override fun ListFeatures(request: Rectangle, response: StreamObserver<Feature>): Unit {
      ListFeatures().ListFeatures(request, MessageSinkAdapter(response))
    }

    public override fun RecordRoute(response: StreamObserver<RouteSummary>): StreamObserver<Point> {
      val requestStream = MessageSourceAdapter<Point>()
      streamExecutor.submit {
        response.onNext(RecordRoute().RecordRoute(requestStream))
        response.onCompleted()
      }
      return requestStream
    }

    public override fun RouteChat(response: StreamObserver<RouteNote>): StreamObserver<RouteNote> {
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

    public override fun build(channel: Channel, callOptions: CallOptions) = RouteGuideStub(channel,
        callOptions)

    public fun GetFeature(request: Point, response: StreamObserver<Feature>): Unit {
      ClientCalls.asyncUnaryCall(channel.newCall(getGetFeatureMethod(), callOptions), request,
          response)
    }

    public fun ListFeatures(request: Rectangle, response: StreamObserver<Feature>): Unit {
      ClientCalls.asyncServerStreamingCall(channel.newCall(getListFeaturesMethod(), callOptions),
          request, response)
    }

    public fun RecordRoute(response: StreamObserver<RouteSummary>): StreamObserver<Point> =
        ClientCalls.asyncClientStreamingCall(channel.newCall(getRecordRouteMethod(), callOptions),
        response)

    public fun RouteChat(response: StreamObserver<RouteNote>): StreamObserver<RouteNote> =
        ClientCalls.asyncBidiStreamingCall(channel.newCall(getRouteChatMethod(), callOptions),
        response)
  }

  public class RouteGuideBlockingStub : AbstractStub<RouteGuideStub> {
    internal constructor(channel: Channel) : super(channel)

    internal constructor(channel: Channel, callOptions: CallOptions) : super(channel, callOptions)

    public override fun build(channel: Channel, callOptions: CallOptions) = RouteGuideStub(channel,
        callOptions)

    public fun GetFeature(request: Point): Feature = blockingUnaryCall(channel,
        getGetFeatureMethod(), callOptions, request)

    public fun RecordRoute(request: Point): Iterator<RouteSummary> =
        blockingServerStreamingCall(channel, getRecordRouteMethod(), callOptions, request)
  }
}
