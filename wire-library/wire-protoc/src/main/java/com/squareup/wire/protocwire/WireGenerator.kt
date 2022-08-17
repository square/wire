package com.squareup.wire.protocwire

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.compiler.PluginProtos
import com.squareup.wire.Syntax
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.internal.SchemaEncoder
import com.squareup.wire.schema.internal.parser.MessageElement
import com.squareup.wire.schema.internal.parser.ProtoFileElement

class WireGenerator : CodeGenerator {
  override fun generate(descriptor: PluginProtos.CodeGeneratorRequest, parameter: String, context: CodeGenerator.Context) {
    val protoFiles = mutableListOf<ProtoFile>()
    for (fileDescriptorProto in descriptor.protoFileList) {
      val protoFileElement = parseFileDescriptor(fileDescriptorProto)
      val protoFile = ProtoFile.get(protoFileElement)
      protoFiles.add(protoFile)
    }
    val schema = Schema(protoFiles)
    val schemaEncoder = SchemaEncoder(schema)
    for (protoFile in protoFiles) {
      val encode = schemaEncoder.encode(protoFile)
      context.addFile(protoFile.name() + ".txt", encode.toString())
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) = Plugin.run(WireGenerator())
  }
}

fun parseFileDescriptor(fileDescriptor: DescriptorProtos.FileDescriptorProto): ProtoFileElement {
  val location = Location.get(fileDescriptor.name)
  val imports = mutableListOf<String>()
  val publicImports = mutableListOf<String>()
  val types = mutableListOf<MessageElement>()
  val nestedTypes = mutableListOf<MessageElement>()
  for (messageType in fileDescriptor.messageTypeList) {
    for (nestedType in messageType.nestedTypeList) {
      nestedTypes.add(parseMessage(nestedType))
    }
    types.add(parseMessage(messageType))
  }
  return ProtoFileElement(
    location = location,
    imports = imports,
    publicImports = publicImports,
    packageName = fileDescriptor.`package`,
    types = types, // TODO: this is just to see if things break
    services = emptyList(),
    options = emptyList(),
    syntax = Syntax.PROTO_3,
  )
}

fun parseMessage(message: DescriptorProtos.DescriptorProto): MessageElement {
  return MessageElement(
    location = Location.get(message.name),
    name = message.name,
    documentation = ""
  )
}
