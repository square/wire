//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcStreamingCall](index.md)/[cancel](cancel.md)

# cancel

[common]\
abstract fun [cancel](cancel.md)()

Attempts to cancel the call. This function is safe to call concurrently with execution. When canceled, execution fails with an immediate IOException rather than waiting to complete normally.
