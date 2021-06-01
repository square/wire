package routeguide

import io.grpc.BindableService
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.stub.ServerCalls.asyncBidiStreamingCall
import io.grpc.stub.ServerCalls.asyncClientStreamingCall
import io.grpc.stub.ServerCalls.asyncServerStreamingCall
import io.grpc.stub.ServerCalls.asyncUnaryCall
import io.grpc.stub.StreamObserver
import java.io.InputStream
import java.lang.UnsupportedOperationException

public class RouteGuideWireGrpc {
  public abstract class RouteGuideImplBase : BindableService {
    public open fun GetFeature(request: Point, response: StreamObserver<Feature>) = throw
        UnsupportedOperationException()

    public open fun ListFeatures(request: Rectangle, response: StreamObserver<Feature>) = throw
        UnsupportedOperationException()

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
}
