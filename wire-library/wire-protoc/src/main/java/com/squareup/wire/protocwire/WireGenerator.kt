package com.squareup.wire.protocwire

import com.google.protobuf.AbstractMessage
import com.google.protobuf.DescriptorProtos
import com.google.protobuf.Descriptors.EnumValueDescriptor
import com.google.protobuf.DynamicMessage
import com.google.protobuf.GeneratedMessageV3.ExtendableMessage
import com.google.protobuf.compiler.PluginProtos
import com.squareup.wire.Syntax
import com.squareup.wire.WireLogger
import com.squareup.wire.protocwire.Plugin.DescriptorSource
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
import com.squareup.wire.schema.internal.parser.EnumConstantElement
import com.squareup.wire.schema.internal.parser.EnumElement
import com.squareup.wire.schema.internal.parser.FieldElement
import com.squareup.wire.schema.internal.parser.MessageElement
import com.squareup.wire.schema.internal.parser.OptionElement
import com.squareup.wire.schema.internal.parser.ProtoFileElement
import com.squareup.wire.schema.internal.parser.TypeElement
import okio.Buffer
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

fun <T> TODO(message: String): T {
  throw RuntimeException(message)
}

data class CodeGeneratorResponseContext(
  private val response: Plugin.Response,
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
    response.addFile(file.name, str)
  }
}

class WireGenerator(

) : CodeGenerator {
  override fun generate(request: PluginProtos.CodeGeneratorRequest, descs: DescriptorSource, response: Plugin.Response) {
    debug(request)
    val loader = CoreLoader
    val errorCollector = ErrorCollector()
    val linker = Linker(loader, errorCollector, permitPackageCycles = true, loadExhaustively = true)

    val protoFiles = mutableListOf<ProtoFile>()
    val sourcePaths = mutableSetOf<String>()
    for (fileDescriptorProto in request.protoFileList) {
      sourcePaths.add(fileDescriptorProto.name)
      val protoFileElement = parseFileDescriptor(fileDescriptorProto, descs)
      val protoFile = ProtoFile.get(protoFileElement)
      protoFiles.add(protoFile)
    }

    try {
      val schema = linker.link(protoFiles)
      // Create a specific target and just run.
      KotlinProtocTarget().newHandler().handle(schema, CodeGeneratorResponseContext(response, sourcePaths))
    } catch (e: Throwable) {
      response.addFile("error.log", e.stackTraceToString())
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
      Plugin.run(WireGenerator())
    }
  }
}

fun parseFileDescriptor(fileDescriptor: DescriptorProtos.FileDescriptorProto, descs: DescriptorSource): ProtoFileElement {
  val location = Location.get(fileDescriptor.name)
  val imports = mutableListOf<String>()
  val publicImports = mutableListOf<String>()
  val types = mutableListOf<TypeElement>()
  val nestedTypes = mutableListOf<TypeElement>()
  for (enumType in fileDescriptor.enumTypeList) {
    types.add(parseEnum(fileDescriptor.name, enumType, descs))
  }
  for (messageType in fileDescriptor.messageTypeList) {
    types.add(parseMessage(fileDescriptor.name, messageType, descs))
  }
  // TODO: enums
//  for (nestedType in fileDescriptor.enumTypeList) {
//    types.add(parseEnum(nestedType, descs))
//  }

  return ProtoFileElement(
    location = location,
    imports = imports,
    publicImports = publicImports,
    packageName = fileDescriptor.`package`,
    types = types,
    services = emptyList(),
    options = parseOptions(fileDescriptor.options, descs),
    syntax = Syntax.PROTO_3,
  )
}

fun parseEnum(protoLocation: String, enum: DescriptorProtos.EnumDescriptorProto, descs: DescriptorSource): EnumElement {
  val constants = mutableListOf<EnumConstantElement>()
  for (enumValueDescriptorProto in enum.valueList) {
    constants.add(EnumConstantElement(
      location = Location.get(protoLocation),
      name = enumValueDescriptorProto.name,
      tag = enumValueDescriptorProto.number,
      documentation = "",
      options = parseOptions(enumValueDescriptorProto.options, descs)
    ))
  }
  return EnumElement(
    location = Location.get(protoLocation),
    name = enum.name,
    documentation = "",
    options = parseOptions(enum.options, descs),
    constants = constants,
    reserveds = emptyList()
  )
}

fun parseMessage(protoLocation: String, message: DescriptorProtos.DescriptorProto, descs: DescriptorSource): MessageElement {
  val nestedTypes = mutableListOf<TypeElement>()
  for (nestedType in message.nestedTypeList) {
    nestedTypes.add(parseMessage(protoLocation, nestedType, descs))
  }
  // TODO: enums
//  for (nestedType in message.enumTypeList) {
//    nestedTypes.add(parseEnum(nestedType, descs))
//  }
  return MessageElement(
    location = Location.get(protoLocation),
    name = message.name,
    documentation = "",
    options = parseOptions(message.options, descs),
    reserveds = emptyList(),
    fields = parseFields(protoLocation, message.fieldList, descs),
    nestedTypes = nestedTypes,
    oneOfs = emptyList(),
    extensions = emptyList(),
    groups = emptyList(),
  )
}

fun parseFields(protoLocation: String, fieldList: List<DescriptorProtos.FieldDescriptorProto>, descs: DescriptorSource): List<FieldElement> {
  val result = mutableListOf<FieldElement>()
  for (field in fieldList) {
    result.add(FieldElement(
      location = Location.get(protoLocation),
      label = parseLabel(field.label),
      type = parseType(field),
      name = field.name,
//      defaultValue = field.defaultValue,
//      jsonName = field.jsonName,
      tag = field.number,
      documentation = "",
      options = parseOptions(field.options, descs)
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
    // Collapsing enums and messages are the same.
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM,
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE -> {
      if (field.typeName.startsWith('.')) {
        return field.typeName.substring(1)
      }
      field.typeName
    }
    // TODO: Figure out group types
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_GROUP -> ""
    else -> TODO("else case found for $ {field.type}")
  }
}

fun parseLabel(label: DescriptorProtos.FieldDescriptorProto.Label): Field.Label {
  return when (label) {
    DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL -> Field.Label.OPTIONAL
    DescriptorProtos.FieldDescriptorProto.Label.LABEL_REQUIRED -> Field.Label.REQUIRED
    DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED -> Field.Label.REPEATED
    else -> Field.Label.OPTIONAL
  }
}

fun <T: ExtendableMessage<T>> parseOptions(options: T, descs: DescriptorSource): List<OptionElement> {
  val optDesc = options.descriptorForType
  val overrideDesc = descs.findMessageTypeByName(optDesc.fullName)
  if (overrideDesc != null) {
    val optsDm = DynamicMessage.newBuilder(overrideDesc)
        .mergeFrom(options)
        .build()
    return createOptionElements(optsDm)
  }
  return createOptionElements(options)
}

private fun createOptionElements(options: AbstractMessage): List<OptionElement> {
  val elements = mutableListOf<OptionElement>()
  for (entry in options.allFields.entries) {
    val fld = entry.key
    val name = if (fld.isExtension) fld.fullName else fld.name
    val (value, kind) = valueOf(entry.value)
    elements.add(OptionElement(name, kind, value, fld.isExtension))
  }
  return elements
}

private fun valueOf(value: Any): OptionValueAndKind {
  return when (value) {
    is Number -> OptionValueAndKind(value.toString(), OptionElement.Kind.NUMBER)
    is Boolean -> OptionValueAndKind(value.toString(), OptionElement.Kind.BOOLEAN)
    is String -> OptionValueAndKind(value, OptionElement.Kind.STRING)
    is ByteArray -> OptionValueAndKind(String(toCharArray(value)), OptionElement.Kind.STRING)
    is EnumValueDescriptor -> OptionValueAndKind(value.name, OptionElement.Kind.ENUM)
    is List<*> -> OptionValueAndKind(valueOfList(value), OptionElement.Kind.LIST)
    is AbstractMessage -> OptionValueAndKind(valueOfMessage(value), OptionElement.Kind.MAP)
    else -> throw IllegalStateException("Unexpected field value type: ${value::class.qualifiedName}")
  }
}

private fun toCharArray(bytes: ByteArray): CharArray {
  val ch = CharArray(bytes.size)
  bytes.forEachIndexed{ index, element -> ch[index] = element.toInt().toChar() }
  return ch
}

private fun simpleValue(optVal: OptionValueAndKind): Any {
  return if (optVal.kind == OptionElement.Kind.BOOLEAN ||
      optVal.kind == OptionElement.Kind.ENUM ||
      optVal.kind == OptionElement.Kind.NUMBER) {
    OptionElement.OptionPrimitive(optVal.kind, optVal.value)
  } else {
    optVal.value
  }
}

private fun valueOfList(list: List<*>): List<Any> {
  val ret = mutableListOf<Any>()
  for (element in list) {
    if (element == null) {
      throw NullPointerException("list value should not contain null")
    }
    ret.add(simpleValue(valueOf(element)))
  }
  return ret
}

private fun valueOfMessage(msg: AbstractMessage): Map<String, Any> {
  val ret = mutableMapOf<String, Any>()
  for (entry in msg.allFields.entries) {
    val fld = entry.key
    val name = if (fld.isExtension) "[${fld.fullName}]" else fld.name
    ret[name] = simpleValue(valueOf(entry.value))
  }
  return ret
}

private data class OptionValueAndKind(val value: Any, val kind: OptionElement.Kind)
