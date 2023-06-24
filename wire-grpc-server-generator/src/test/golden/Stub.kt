package routeguide

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.stub.AbstractStub
import io.grpc.stub.ClientCalls.asyncBidiStreamingCall
import io.grpc.stub.ClientCalls.asyncClientStreamingCall
import io.grpc.stub.ClientCalls.asyncServerStreamingCall
import io.grpc.stub.ClientCalls.asyncUnaryCall
import io.grpc.stub.StreamObserver

public class RouteGuideWireGrpc {
  public fun newStub(channel: Channel): RouteGuideStub = RouteGuideStub(channel)

  public class RouteGuideStub : AbstractStub<RouteGuideStub> {
    internal constructor(channel: Channel) : super(channel)

    internal constructor(channel: Channel, callOptions: CallOptions) : super(channel, callOptions)

    override fun build(channel: Channel, callOptions: CallOptions): RouteGuideStub =
        RouteGuideStub(channel, callOptions)

    public fun GetFeature(request: Point, response: StreamObserver<Feature>) {
      asyncUnaryCall(channel.newCall(getGetFeatureMethod(), callOptions), request, response)
    }

    public fun ListFeatures(request: Rectangle, response: StreamObserver<Feature>) {
      asyncServerStreamingCall(channel.newCall(getListFeaturesMethod(), callOptions), request,
          response)
    }

    public fun RecordRoute(response: StreamObserver<RouteSummary>): StreamObserver<Point> =
        asyncClientStreamingCall(channel.newCall(getRecordRouteMethod(), callOptions), response)

    public fun RouteChat(response: StreamObserver<RouteNote>): StreamObserver<RouteNote> =
        asyncBidiStreamingCall(channel.newCall(getRouteChatMethod(), callOptions), response)
  }
}
