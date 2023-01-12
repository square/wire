package routeguide

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import io.grpc.ServiceDescriptor
import io.grpc.ServiceDescriptor.newBuilder
import kotlin.Array
import kotlin.String
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.jvm.Volatile

public class RouteGuideWireGrpc {
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
}
