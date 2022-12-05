//[wire-grpc-client](../../../index.md)/[com.squareup.wire](../index.md)/[GrpcStreamingCall](index.md)/[timeout](timeout.md)

# timeout

[common]\
abstract val [timeout](timeout.md): Timeout

Configures how long the call can take to complete before it is automatically canceled. The timeout applies to the full set of messages transmitted. For long-running streams you must configure a sufficiently long timeout.
