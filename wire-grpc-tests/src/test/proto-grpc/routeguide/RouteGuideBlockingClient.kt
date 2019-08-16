// Draft of a file the Wire compiler should be able to generate.
package routeguide

import com.squareup.wire.MessageSink
import com.squareup.wire.MessageSource
import com.squareup.wire.Service
import com.squareup.wire.WireRpc

interface RouteGuideBlockingClient : Service {
  @WireRpc(
    path = "/routeguide.RouteGuide/GetFeature",
    requestAdapter = "routeguide.Point#ADAPTER",
    responseAdapter = "routeguide.Feature#ADAPTER"
  )
  fun GetFeature(request: Point): Feature

  @WireRpc(
    path = "/routeguide.RouteGuide/ListFeatures",
    requestAdapter = "routeguide.Rectangle#ADAPTER",
    responseAdapter = "routeguide.Feature#ADAPTER"
  )
  fun ListFeatures(request: Rectangle): MessageSource<Feature>

  @WireRpc(
    path = "/routeguide.RouteGuide/RecordRoute",
    requestAdapter = "routeguide.Point#ADAPTER",
    responseAdapter = "routeguide.RouteSummary#ADAPTER"
  )
  fun RecordRoute(): Pair<MessageSink<Point>, MessageSource<RouteSummary>>

  @WireRpc(
    path = "/routeguide.RouteGuide/RouteChat",
    requestAdapter = "routeguide.RouteNote#ADAPTER",
    responseAdapter = "routeguide.RouteNote#ADAPTER"
  )
  fun RouteChat(): Pair<MessageSink<RouteNote>, MessageSource<RouteNote>>
}
