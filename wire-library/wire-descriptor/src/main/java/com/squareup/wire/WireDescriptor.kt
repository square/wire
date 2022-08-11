@file:JvmName("WireDescriptor")

package com.squareup.wire

import com.google.protobuf.DescriptorProtos.FileDescriptorProto
import com.google.protobuf.DescriptorProtos.FileDescriptorSet
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.internal.SchemaEncoder

/**
 * This class converts a Wire schema into the JVM types defined in protobuf's `descriptor.proto`
 * starting from [FileDescriptorSet].
 */
class WireDescriptor(
  private val schema: Schema,
) {
  // TODO(Benoit) keep lazy execution but cache it to add `getDescriptorForType`, etc?
  fun fileDescriptorSet(): FileDescriptorSet {
    val schemaEncoder = SchemaEncoder(schema)

    return FileDescriptorSet.newBuilder()
      .addAllFile(schema.protoFiles.map {
        FileDescriptorProto.parseFrom(
          schemaEncoder.encode(it).toByteArray()
        )
      })
      .build()
  }
}
