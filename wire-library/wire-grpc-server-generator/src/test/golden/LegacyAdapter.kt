package routeguide

import com.squareup.wire.kotlin.grpcserver.MessageSinkAdapter
import com.squareup.wire.kotlin.grpcserver.MessageSourceAdapter
import io.grpc.stub.StreamObserver
import java.util.concurrent.ExecutorService
import kotlin.Unit

public class RouteGuideWireGrpc {
  public class RouteGuideImplLegacyAdapter(
    private val streamExecutor: ExecutorService,
    private val GetFeature: () -> RouteGuideGetFeatureBlockingServer,
    private val ListFeatures: () -> RouteGuideListFeaturesBlockingServer,
    private val RecordRoute: () -> RouteGuideRecordRouteBlockingServer,
    private val RouteChat: () -> RouteGuideRouteChatBlockingServer,
  ) : RouteGuideWireGrpc.RouteGuideImplBase() {
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
}
