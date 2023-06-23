package routeguide

import com.squareup.wire.kotlin.grpcserver.WireBindableService
import com.squareup.wire.kotlin.grpcserver.WireMethodMarshaller
import io.grpc.ServerServiceDefinition
import io.grpc.stub.ServerCalls.asyncBidiStreamingCall
import io.grpc.stub.ServerCalls.asyncClientStreamingCall
import io.grpc.stub.ServerCalls.asyncServerStreamingCall
import io.grpc.stub.ServerCalls.asyncUnaryCall
import io.grpc.stub.StreamObserver
import java.io.InputStream
import java.lang.Class
import java.lang.UnsupportedOperationException
import kotlin.Unit

public class RouteGuideWireGrpc {
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
}
