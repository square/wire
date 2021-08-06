package com.squareup.wire.protos

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors
import com.google.protobuf.Descriptors.Descriptor
import com.squareup.wire.Message

/**
 * Utility class to retrieve type descriptor from encoded Wire file descriptors.
 */
object ProtoTypeDescriptor {
  fun <T: Message<T, *>> getMessageDescriptor(record: T): Descriptor? {
    val sourceFile = record.adapter.sourceFile ?: return null
    val descriptor = getFileDescriptor(sourceFile)

    val recordClass = record::class.java
    val className = recordClass.canonicalName.removePrefix("${recordClass.packageName}.")
    val types = className.split(".").iterator()
    val typeName = types.next()
    var messageDescriptor = descriptor.messageTypes.find { it.name == typeName }
      ?: throw RuntimeException("could not find type $typeName in descriptor ${descriptor.fullName}")
    types.forEachRemaining { type ->
      messageDescriptor = messageDescriptor.nestedTypes.find { it.name == type }
        ?: throw RuntimeException("could not find type $type in descriptor ${descriptor.fullName}")
      }
    return messageDescriptor
  }

  private fun getFileDescriptor(sourceFile: String): Descriptors.FileDescriptor {
    val stream = ClassLoader.getSystemResourceAsStream(sourceFile)
    // TODO(kalfonso): need to pass extension registry?
    val descriptorProto = DescriptorProtos.FileDescriptorProto.parseFrom(stream)
    return Descriptors.FileDescriptor.buildFrom(
      descriptorProto,
      dependencies(descriptorProto).toTypedArray()
    )
  }

  private fun dependencies(descriptorProto: DescriptorProtos.FileDescriptorProto): List<Descriptors.FileDescriptor> {
    return descriptorProto.dependencyList.map {
      val sourceFile = it.removeSuffix(".proto") + ".desc"
      getFileDescriptor(sourceFile)
    }
  }
}