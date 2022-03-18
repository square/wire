package routeguide

import io.grpc.ServiceDescriptor
import io.grpc.ServiceDescriptor.newBuilder
import kotlin.String
import kotlin.jvm.Volatile

public class RouteGuideWireGrpc {
  public val SERVICE_NAME: String = "routeguide.RouteGuide"

  @Volatile
  private var serviceDescriptor: ServiceDescriptor? = null

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
}
