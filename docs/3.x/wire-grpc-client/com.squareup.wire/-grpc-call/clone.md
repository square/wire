//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcCall](index.md)/[clone](clone.md)

# clone

[common]\
abstract fun [clone](clone.md)(): [GrpcCall](index.md)&lt;[S](index.md), [R](index.md)&gt;

Create a new, identical gRPC call to this one which can be enqueued or executed even if this call has already been.
