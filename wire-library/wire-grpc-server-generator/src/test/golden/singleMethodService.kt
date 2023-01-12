package com.foo.bar

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import com.squareup.wire.kotlin.grpcserver.WireBindableService
import com.squareup.wire.kotlin.grpcserver.WireMethodMarshaller
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.ServiceDescriptor
import io.grpc.ServiceDescriptor.newBuilder
import io.grpc.stub.AbstractStub
import io.grpc.stub.ClientCalls
import io.grpc.stub.ClientCalls.blockingUnaryCall
import io.grpc.stub.ServerCalls
import io.grpc.stub.ServerCalls.asyncUnaryCall
import io.grpc.stub.StreamObserver
import java.io.InputStream
import java.lang.Class
import java.lang.UnsupportedOperationException
import java.util.concurrent.ExecutorService
import kotlin.Array
import kotlin.String
import kotlin.Unit
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.jvm.Volatile

public object FooServiceWireGrpc {
  public val SERVICE_NAME: String = "foo.FooService"

  @Volatile
  private var serviceDescriptor: ServiceDescriptor? = null

  private val descriptorMap: Map<String, DescriptorProtos.FileDescriptorProto> = mapOf(
    "service.proto" to descriptorFor(arrayOf(
      "Cg1zZXJ2aWNlLnByb3RvEgNmb28iCQoHUmVxdWVzdCIKCghSZXNwb25zZTJYCgpGb29TZXJ2aWNlEiQK",
      "BUNhbGwxEgwuZm9vLlJlcXVlc3QaDS5mb28uUmVzcG9uc2USJAoFQ2FsbDISDC5mb28uUmVxdWVzdBoN",
      "LmZvby5SZXNwb25zZUINCgtjb20uZm9vLmJhcg==",
    )),
  )


  @Volatile
  private var getCall1Method: MethodDescriptor<Request, Response>? = null

  @Volatile
  private var getCall2Method: MethodDescriptor<Request, Response>? = null

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
      synchronized(FooServiceWireGrpc::class) {
        result = serviceDescriptor
        if (result == null) {
          result = newBuilder(SERVICE_NAME)
          .addMethod(getCall1Method())
          .addMethod(getCall2Method())
          .setSchemaDescriptor(io.grpc.protobuf.ProtoFileDescriptorSupplier {
                fileDescriptor("service.proto", emptySet())
              })
          .build()
          serviceDescriptor = result
        }
      }
    }
    return result
  }

  public fun getCall1Method(): MethodDescriptor<Request, Response> {
    var result: MethodDescriptor<Request, Response>? = getCall1Method
    if (result == null) {
      synchronized(FooServiceWireGrpc::class) {
        result = getCall1Method
        if (result == null) {
          getCall1Method = MethodDescriptor.newBuilder<Request, Response>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(
              MethodDescriptor.generateFullMethodName(
                "foo.FooService", "Call1"
              )
            )
            .setSampledToLocalTracing(true)
            .setRequestMarshaller(FooServiceImplBase.RequestMarshaller())
            .setResponseMarshaller(FooServiceImplBase.ResponseMarshaller())
            .build()
        }
      }
    }
    return getCall1Method!!
  }

  public fun getCall2Method(): MethodDescriptor<Request, Response> {
    var result: MethodDescriptor<Request, Response>? = getCall2Method
    if (result == null) {
      synchronized(FooServiceWireGrpc::class) {
        result = getCall2Method
        if (result == null) {
          getCall2Method = MethodDescriptor.newBuilder<Request, Response>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(
              MethodDescriptor.generateFullMethodName(
                "foo.FooService", "Call2"
              )
            )
            .setSampledToLocalTracing(true)
            .setRequestMarshaller(FooServiceImplBase.RequestMarshaller())
            .setResponseMarshaller(FooServiceImplBase.ResponseMarshaller())
            .build()
        }
      }
    }
    return getCall2Method!!
  }

  public fun newStub(channel: Channel): FooServiceStub = FooServiceStub(channel)

  public fun newBlockingStub(channel: Channel): FooServiceBlockingStub =
      FooServiceBlockingStub(channel)

  public abstract class FooServiceImplBase : WireBindableService {
    public open fun Call1(request: Request, response: StreamObserver<Response>): Unit = throw
        UnsupportedOperationException()

    public open fun Call2(request: Request, response: StreamObserver<Response>): Unit = throw
        UnsupportedOperationException()

    public override fun bindService(): ServerServiceDefinition =
        ServerServiceDefinition.builder(getServiceDescriptor()).addMethod(
              getCall1Method(),
              asyncUnaryCall(this@FooServiceImplBase::Call1)
            ).addMethod(
              getCall2Method(),
              asyncUnaryCall(this@FooServiceImplBase::Call2)
            ).build()

    public class RequestMarshaller : WireMethodMarshaller<Request> {
      public override fun stream(`value`: Request): InputStream =
          Request.ADAPTER.encode(value).inputStream()

      public override fun marshalledClass(): Class<Request> = Request::class.java

      public override fun parse(stream: InputStream): Request = Request.ADAPTER.decode(stream)
    }

    public class ResponseMarshaller : WireMethodMarshaller<Response> {
      public override fun stream(`value`: Response): InputStream =
          Response.ADAPTER.encode(value).inputStream()

      public override fun marshalledClass(): Class<Response> = Response::class.java

      public override fun parse(stream: InputStream): Response = Response.ADAPTER.decode(stream)
    }
  }

  public class BindableAdapter(
    private val streamExecutor: ExecutorService,
    private val Call1: () -> FooServiceCall1BlockingServer,
    private val Call2: () -> FooServiceCall2BlockingServer,
  ) : FooServiceImplBase() {
    public override fun Call1(request: Request, response: StreamObserver<Response>): Unit {
      response.onNext(Call1().Call1(request))
      response.onCompleted()
    }

    public override fun Call2(request: Request, response: StreamObserver<Response>): Unit {
      response.onNext(Call2().Call2(request))
      response.onCompleted()
    }
  }

  public class FooServiceStub : AbstractStub<FooServiceStub> {
    internal constructor(channel: Channel) : super(channel)

    internal constructor(channel: Channel, callOptions: CallOptions) : super(channel, callOptions)

    public override fun build(channel: Channel, callOptions: CallOptions) = FooServiceStub(channel,
        callOptions)

    public fun Call1(request: Request, response: StreamObserver<Response>): Unit {
      ClientCalls.asyncUnaryCall(channel.newCall(getCall1Method(), callOptions), request, response)
    }

    public fun Call2(request: Request, response: StreamObserver<Response>): Unit {
      ClientCalls.asyncUnaryCall(channel.newCall(getCall2Method(), callOptions), request, response)
    }
  }

  public class FooServiceBlockingStub : AbstractStub<FooServiceStub> {
    internal constructor(channel: Channel) : super(channel)

    internal constructor(channel: Channel, callOptions: CallOptions) : super(channel, callOptions)

    public override fun build(channel: Channel, callOptions: CallOptions) = FooServiceStub(channel,
        callOptions)

    public fun Call1(request: Request): Response = blockingUnaryCall(channel, getCall1Method(),
        callOptions, request)

    public fun Call2(request: Request): Response = blockingUnaryCall(channel, getCall2Method(),
        callOptions, request)
  }
}
