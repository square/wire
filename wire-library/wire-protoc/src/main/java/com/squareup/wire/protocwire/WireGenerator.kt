package com.squareup.wire.protocwire

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.GeneratedMessageV3.ExtendableMessage
import com.google.protobuf.compiler.PluginProtos
import com.squareup.wire.Syntax
import com.squareup.wire.WireLogger
import com.squareup.wire.protocwire.StubbedRequestDebugging.Companion.debug
import com.squareup.wire.schema.ClaimedDefinitions
import com.squareup.wire.schema.ClaimedPaths
import com.squareup.wire.schema.CoreLoader
import com.squareup.wire.schema.EmittingRules
import com.squareup.wire.schema.ErrorCollector
import com.squareup.wire.schema.Field
import com.squareup.wire.schema.KotlinProtocTarget
import com.squareup.wire.schema.Linker
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.ProfileLoader
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.SchemaHandler
import com.squareup.wire.schema.internal.parser.FieldElement
import com.squareup.wire.schema.internal.parser.MessageElement
import com.squareup.wire.schema.internal.parser.OptionElement
import com.squareup.wire.schema.internal.parser.ProtoFileElement
import okio.Buffer
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

fun <T> TODO(message: String): T {
  throw RuntimeException(message)
}

data class CodeGeneratorResponseContext(
  private val context: CodeGenerator.Context,
  override val sourcePathPaths: Set<String> = emptySet()
) : SchemaHandler.Context {
  override val fileSystem: FileSystem
    get() = TODO("FileSystem")
  override val outDirectory: Path
    get() = "".toPath()
  override val logger: WireLogger
    get() = TODO("WireLogger")
  override val errorCollector: ErrorCollector
    get() = TODO("ErrorCollector")
  override val emittingRules: EmittingRules
    get() = EmittingRules()
  override val claimedDefinitions: ClaimedDefinitions?
    get() = null
  override val claimedPaths: ClaimedPaths = ClaimedPaths()
  override val module: SchemaHandler.Module?
    get() = null

  override val profileLoader: ProfileLoader?
    get() = null

  override fun inSourcePath(protoFile: ProtoFile): Boolean {
    return inSourcePath(protoFile.location)
  }

  override fun inSourcePath(location: Location): Boolean {
    return location.path in sourcePathPaths
  }

  override fun createDirectories(dir: Path, mustCreate: Boolean) {
  }

  override fun <T> write(file: Path, mustCreate: Boolean, writerAction: BufferedSink.() -> T): T {
    return Buffer().writerAction()
  }

  override fun write(file: Path, str: String) {
    context.addFile(file.name, str)
  }
}

class WireGenerator(

) : CodeGenerator {
  override fun generate(request: PluginProtos.CodeGeneratorRequest, parameter: String, context: CodeGenerator.Context) {
    debug(request, context)
    val loader = CoreLoader
    val errorCollector = ErrorCollector()
    val linker = Linker(loader, errorCollector, permitPackageCycles = true, loadExhaustively = true)

    val protoFiles = mutableListOf<ProtoFile>()
    val sourcePaths = mutableSetOf<String>()
    for (fileDescriptorProto in request.protoFileList) {
      sourcePaths.add(fileDescriptorProto.name)
      val protoFileElement = parseFileDescriptor(fileDescriptorProto)
      val protoFile = ProtoFile.get(protoFileElement)
      protoFiles.add(protoFile)
    }

    try {
      val schema = linker.link(protoFiles)
      // Create a specific target and just run.
      KotlinProtocTarget().newHandler().handle(schema, CodeGeneratorResponseContext(context, sourcePaths))
    } catch (e: Throwable) {
      context.addFile("error.log", e.stackTraceToString())
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      Plugin.run(WireGenerator())
    }
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
      nestedTypes.add(parseMessage(fileDescriptor.name, nestedType))
    }
    types.add(parseMessage(fileDescriptor.name, messageType))
  }

  return ProtoFileElement(
    location = location,
    imports = imports,
    publicImports = publicImports,
    packageName = fileDescriptor.`package`,
    types = types,
    services = emptyList(),
    options = parseOptions(fileDescriptor.options),
    syntax = Syntax.PROTO_3,
  )
}

fun parseMessage(protoLocation: String, message: DescriptorProtos.DescriptorProto): MessageElement {
  val nestedTypes = mutableListOf<TypeElement>()
  for (descriptorProto in message.nestedTypeList) {
    nestedTypes.add(parseMessage(protoLocation, descriptorProto))
  }
  return MessageElement(
    location = Location.get(protoLocation),
    name = message.name,
    documentation = "",
    nestedTypes = nestedTypes,
    options = parseOptions(message.options),
    reserveds = emptyList(),
    fields = parseFields(protoLocation, message.fieldList),
    oneOfs = emptyList(),
    extensions = emptyList(),
    groups = emptyList()
  )
}

fun parseFields(protoLocation: String, fieldList: List<DescriptorProtos.FieldDescriptorProto>): List<FieldElement> {
  val result = mutableListOf<FieldElement>()

  for (field in fieldList) {
    result.add(FieldElement(
      location = Location.get(protoLocation),
      label = parseLabel(field.label),
      type = parseType(field),
      name = field.name,
//      defaultValue = field.defaultValue,
      jsonName = field.jsonName,
      tag = field.number,
      documentation = "",
      options = parseOptions(field.options)
    ))
  }
  return result
}

fun parseType(field: DescriptorProtos.FieldDescriptorProto): String {
  return when (field.type) {
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE -> "double"
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT -> "float"
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64 -> "int64"
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT64 -> "uint64"
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32 -> "int32"
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED64 -> "fixed64"
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_FIXED32 -> "fixed32"
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL -> "bool"
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING -> "string"
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES -> "bytes"
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_UINT32 -> "uint32"
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED32 -> "sfixed32"
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_SFIXED64 -> "sfixed64"
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT32 -> "sint32"
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_SINT64 -> "sint64"
    // Collapsing enums to messages for now
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM,
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE -> {
      // Get the last section of the type name.
      // E.g. .bufbuild.testing.NestedMsg.InnerMsg will be InnerMsg
      field.typeName.split(".").last()
    }
    // TODO: Figure out group types
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_GROUP -> ""
    else -> TODO("else case found for ${field.type}")
  }
}

fun parseLabel(label: DescriptorProtos.FieldDescriptorProto.Label): Field.Label? {
  return when (label) {
    DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL -> Field.Label.OPTIONAL
    DescriptorProtos.FieldDescriptorProto.Label.LABEL_REQUIRED -> Field.Label.REQUIRED
    DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED -> Field.Label.REPEATED
    else -> null
  }
}

fun <T : ExtendableMessage<T>> parseOptions(options: T): List<OptionElement> {
  return emptyList()
}
