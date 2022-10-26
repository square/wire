package com.foo.bar

import io.grpc.BindableService
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.MethodDescriptor
import io.grpc.ServerServiceDefinition
import io.grpc.ServiceDescriptor
import io.grpc.ServiceDescriptor.newBuilder
import io.grpc.stub.AbstractStub
import io.grpc.stub.ClientCalls
import io.grpc.stub.ClientCalls.blockingUnaryCall
import io.grpc.stub.ServerCalls.asyncUnaryCall
import io.grpc.stub.StreamObserver
import java.io.InputStream
import java.lang.UnsupportedOperationException
import java.util.concurrent.ExecutorService
import kotlin.String
import kotlin.Unit
import kotlin.jvm.Volatile

public object FooServiceWireGrpc {
  public val SERVICE_NAME: String = "foo.FooService"

  @Volatile
  private var serviceDescriptor: ServiceDescriptor? = null

  @Volatile
  private var getCallMethod: MethodDescriptor<Request, Response>? = null

  public fun getServiceDescriptor(): ServiceDescriptor? {
    var result = serviceDescriptor
    if (result == null) {
      synchronized(FooServiceWireGrpc::class) {
        result = serviceDescriptor
        if (result == null) {
          result = newBuilder(SERVICE_NAME)
          .addMethod(getCallMethod())
          .build()
          serviceDescriptor = result
        }
      }
    }
    return result
  }

  public fun getCallMethod(): MethodDescriptor<Request, Response> {
    var result: MethodDescriptor<Request, Response>? = getCallMethod
    if (result == null) {
      synchronized(FooServiceWireGrpc::class) {
        result = getCallMethod
        if (result == null) {
          getCallMethod = MethodDescriptor.newBuilder<Request, Response>()
            .setType(MethodDescriptor.MethodType.UNARY)
            .setFullMethodName(
              MethodDescriptor.generateFullMethodName(
                "foo.FooService", "Call"
              )
            )
            .setSampledToLocalTracing(true)
            .setRequestMarshaller(FooServiceImplBase.RequestMarshaller())
            .setResponseMarshaller(FooServiceImplBase.ResponseMarshaller())
            .build()
        }
      }
    }
    return getCallMethod!!
  }

  public fun newStub(channel: Channel): FooServiceStub = FooServiceStub(channel)

  public fun newBlockingStub(channel: Channel): FooServiceBlockingStub =
      FooServiceBlockingStub(channel)

  public abstract class FooServiceImplBase : BindableService {
    public open fun Call(request: Request, response: StreamObserver<Response>): Unit = throw
        UnsupportedOperationException()

    public override fun bindService(): ServerServiceDefinition =
        ServerServiceDefinition.builder(getServiceDescriptor()).addMethod(
              getCallMethod(),
              asyncUnaryCall(this@FooServiceImplBase::Call)
            ).build()

    public class RequestMarshaller : MethodDescriptor.Marshaller<Request> {
      public override fun stream(`value`: Request): InputStream =
          Request.ADAPTER.encode(value).inputStream()

      public override fun parse(stream: InputStream): Request = Request.ADAPTER.decode(stream)
    }

    public class ResponseMarshaller : MethodDescriptor.Marshaller<Response> {
      public override fun stream(`value`: Response): InputStream =
          Response.ADAPTER.encode(value).inputStream()

      public override fun parse(stream: InputStream): Response = Response.ADAPTER.decode(stream)
    }
  }

  public class FooServiceImplLegacyAdapter(
    private val streamExecutor: ExecutorService,
    private val Call: () -> FooServiceCallBlockingServer,
  ) : FooServiceImplBase() {
    public override fun Call(request: Request, response: StreamObserver<Response>): Unit {
      response.onNext(Call().Call(request))
      response.onCompleted()
    }
  }

  public class FooServiceStub : AbstractStub<FooServiceStub> {
    internal constructor(channel: Channel) : super(channel)

    internal constructor(channel: Channel, callOptions: CallOptions) : super(channel, callOptions)

    public override fun build(channel: Channel, callOptions: CallOptions) = FooServiceStub(channel,
        callOptions)

    public fun Call(request: Request, response: StreamObserver<Response>): Unit {
      ClientCalls.asyncUnaryCall(channel.newCall(getCallMethod(), callOptions), request, response)
    }
  }

  public class FooServiceBlockingStub : AbstractStub<FooServiceStub> {
    internal constructor(channel: Channel) : super(channel)

    internal constructor(channel: Channel, callOptions: CallOptions) : super(channel, callOptions)

    public override fun build(channel: Channel, callOptions: CallOptions) = FooServiceStub(channel,
        callOptions)

    public fun Call(request: Request): Response = blockingUnaryCall(channel, getCallMethod(),
        callOptions, request)
  }
}
