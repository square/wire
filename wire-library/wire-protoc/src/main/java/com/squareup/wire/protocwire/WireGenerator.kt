package com.squareup.wire.protocwire

import com.google.protobuf.Descriptors.Descriptor
import com.google.protobuf.Descriptors.FileDescriptor
import com.squareup.wire.Syntax
import com.squareup.wire.schema.*
import com.squareup.wire.schema.internal.parser.MessageElement
import com.squareup.wire.schema.internal.parser.ProtoFileElement

class WireGenerator : CodeGenerator {
    override fun generate(fileToGenerate: FileDescriptor, parameter: String, context: CodeGenerator.Context) {
      val protoFileElement = parseFileDescriptor(fileToGenerate)
      context.addFile(fileToGenerate.name +".txt", protoFileElement.toSchema())
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Plugin.run(WireGenerator())
        }
    }
}

fun parseFileDescriptor(fileDescriptor: FileDescriptor): ProtoFileElement {
  val location = Location.get(fileDescriptor.name)
  val imports = mutableListOf<String>()
  val publicImports = mutableListOf<String>()
  val types = mutableListOf<MessageElement>()
  val nestedTypes = mutableListOf<MessageElement>()

  for (dependency in fileDescriptor.dependencies) {
    imports.add(dependency.fullName)
  }

  for (publicDependency in fileDescriptor.publicDependencies) {
    publicImports.add(publicDependency.fullName)
  }

  for (messageType in fileDescriptor.messageTypes) {
    for (nestedType in messageType.nestedTypes) {
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

fun parseMessage(message: Descriptor): MessageElement {
  return MessageElement(
    location = Location.get(message.name),
    name = message.name,
    documentation = ""
  )
}
