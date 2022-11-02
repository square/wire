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
import io.grpc.stub.ServerCalls
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
  private var getCall1Method: MethodDescriptor<Request, Response>? = null

  @Volatile
  private var getCall2Method: MethodDescriptor<Request, Response>? = null

  public fun getServiceDescriptor(): ServiceDescriptor? {
    var result = serviceDescriptor
    if (result == null) {
      synchronized(FooServiceWireGrpc::class) {
        result = serviceDescriptor
        if (result == null) {
          result = newBuilder(SERVICE_NAME)
          .addMethod(getCall1Method())
          .addMethod(getCall2Method())
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

  public abstract class FooServiceImplBase : BindableService {
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
    private val service: () -> FooServiceBlockingServer,
  ) : FooServiceImplBase() {
    public override fun Call1(request: Request, response: StreamObserver<Response>): Unit {
      response.onNext(service().Call1(request))
      response.onCompleted()
    }

    public override fun Call2(request: Request, response: StreamObserver<Response>): Unit {
      response.onNext(service().Call2(request))
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
