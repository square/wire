package com.squareup.wire.protos

import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Type
import com.squareup.wire.schema.internal.SchemaEncoder
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

/**
 * Generates file descriptors for the given type.
 */
class FileDescriptorGenerator(
  private val schema: Schema,
  private val fs: FileSystem,
  private val outDirectory: String
) {

  private val encoder: SchemaEncoder = SchemaEncoder(schema)
  private val generatedDescriptors = mutableMapOf<ProtoFile, Path>()

  fun generate(type: Type): Path? {
    if (shouldGenerateDescriptor(type)) {
      return generateFileDescriptor(type)
    }
    return null
  }

  private fun generateFileDescriptor(type: Type): Path? {
    val protoFile = schema.protoFile(type.type)
      ?: throw RuntimeException("Could not find proto file for type ${type.type.simpleName}")

    if (generatedDescriptors.contains(protoFile)) {
      //return null otherwise Wire's Target errors if the same path is returned
      //for two different types.
      return null
    }
    val path = writeFileDescriptor(protoFile)
    generatedDescriptors[protoFile] = path
    type.nestedTypes.forEach { generateFileDescriptor(it) }
    return path
  }

  private fun getDescriptorPath(protoFile: ProtoFile): String {
    val packageName = protoFile.packageName ?: ""
    val descriptorName = protoFile.name().plus(".desc")
    val packagePath = packageName.replace(".", "/")
    return "$packagePath/$descriptorName"
  }

  private fun writeFileDescriptor(protoFile: ProtoFile): Path {
    val content = encoder.encode(protoFile)
    val descriptorPath = getDescriptorPath(protoFile)
    val outputPath = outDirectory.toPath() / descriptorPath
    fs.createDirectories(outputPath.parent!!)
    fs.write(outputPath) {
      write(content)
    }
    return outputPath
  }

  private fun shouldGenerateDescriptor(type: Type): Boolean =
      isMessageType(type) || isEnumType(type)

  private fun isMessageType(type: Type) =
      type.javaClass == MessageType::class.java

  private fun isEnumType(type: Type) =
      type.javaClass == EnumType::class.java
}