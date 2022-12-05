//[wire-grpc-server-generator](../../../index.md)/[com.squareup.wire.kotlin.grpcserver](../index.md)/[FileDescriptorGenerator](index.md)/[addDescriptorDataProperty](add-descriptor-data-property.md)

# addDescriptorDataProperty

[jvm]\
fun [addDescriptorDataProperty](add-descriptor-data-property.md)(builder: TypeSpec.Builder, protoFile: ProtoFile?, schema: Schema)

Adds properties to the given TypeSpec for storing com.google.protobuf.DescriptorProtos.FileDescriptorProto instances, and helper functions for getting full FileDescriptors with dependencies for any included schema file.
