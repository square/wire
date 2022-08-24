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

private fun parseFileDescriptor(fileDescriptor: DescriptorProtos.FileDescriptorProto, descs: DescriptorSource): ProtoFileElement {
  val location = Location.get(fileDescriptor.name)
  val helper = SourceCodeHelper(fileDescriptor)

  val imports = mutableListOf<String>()
  val publicImports = mutableListOf<String>()
  val types = mutableListOf<TypeElement>()

  val path = mutableListOf<Int>()
  path.add(DescriptorProtos.FileDescriptorProto.MESSAGE_TYPE_FIELD_NUMBER)
  path.add(0) // placeholder for index
  for ((index, messageType) in fileDescriptor.messageTypeList.withIndex()) {
    path[1] = index
    types.add(parseMessage(path, helper, messageType, descs))
  }

  path.clear()
  path.add(DescriptorProtos.DescriptorProto.ENUM_TYPE_FIELD_NUMBER)
  path.add(0)
  for ((index, enumType) in fileDescriptor.enumTypeList.withIndex()) {
    path[1] = index
    types.add(parseEnum(path, helper, enumType, descs))
  }
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

private fun parseEnum(path: List<Int>, helper: SourceCodeHelper, enum: DescriptorProtos.EnumDescriptorProto, descs: DescriptorSource): EnumElement {
  val info = helper.getLocation(path)
  val constants = mutableListOf<EnumConstantElement>()
  for (enumValueDescriptorProto in enum.valueList) {
    constants.add(EnumConstantElement(
      location = info.loc, // TODO
      name = enumValueDescriptorProto.name,
      tag = enumValueDescriptorProto.number,
      documentation = info.comment,
      options = parseOptions(enumValueDescriptorProto.options, descs)
    ))
  }
  return EnumElement(
    location = info.loc,
    name = enum.name,
    documentation = info.comment,
    options = parseOptions(enum.options, descs),
    constants = constants,
    reserveds = emptyList()
  )
}

private fun parseMessage(path: List<Int>, helper: SourceCodeHelper, message: DescriptorProtos.DescriptorProto, descs: DescriptorSource): MessageElement {
  val info = helper.getLocation(path)

  val nestedTypes = mutableListOf<TypeElement>()
  val nestedPath = mutableListOf<Int>().apply { addAll(path) }
  nestedPath.add(DescriptorProtos.DescriptorProto.NESTED_TYPE_FIELD_NUMBER)
  nestedPath.add(0) // placeholder for index
  for ((index, nestedType) in message.nestedTypeList.withIndex()) {
    nestedPath[nestedPath.size-1] = index
    nestedTypes.add(parseMessage(nestedPath, helper, nestedType, descs))
  }

  val nestedEnumsPath = mutableListOf<Int>().apply { addAll(path) }
  nestedEnumsPath.add(DescriptorProtos.DescriptorProto.ENUM_TYPE_FIELD_NUMBER)
  nestedEnumsPath.add(0) // placeholder for index
  for ((index, nestedType) in message.enumTypeList.withIndex()) {
    nestedEnumsPath[nestedEnumsPath.size - 1] = index
    nestedTypes.add(parseEnum(nestedEnumsPath, helper, nestedType, descs))
  }

  return MessageElement(
    location = info.loc,
    name = message.name,
    documentation = info.comment,
    options = parseOptions(message.options, descs),
    reserveds = emptyList(),
    fields = parseFields(path, helper, message.fieldList, descs),
    nestedTypes = nestedTypes,
    oneOfs = emptyList(),
    extensions = emptyList(),
    groups = emptyList(),
  )
}

private fun parseFields(path: List<Int>, helper: SourceCodeHelper, fieldList: List<DescriptorProtos.FieldDescriptorProto>, descs: DescriptorSource): List<FieldElement> {
  val result = mutableListOf<FieldElement>()
  val fieldPath = mutableListOf<Int>().apply { addAll(path) }
  fieldPath.add(DescriptorProtos.DescriptorProto.FIELD_FIELD_NUMBER)
  fieldPath.add(0) // placeholder for index
  for ((index, field) in fieldList.withIndex()) {
    fieldPath[fieldPath.size-1] = index
    val info = helper.getLocation(fieldPath)
    result.add(FieldElement(
      location = info.loc,
      label = parseLabel(field.label),
      type = parseType(field),
      name = field.name,
//      defaultValue = field.defaultValue,
      jsonName = field.jsonName,
      tag = field.number,
      documentation = info.comment,
      options = parseOptions(field.options, descs)
    ))
  }
  return result
}

private fun parseType(field: DescriptorProtos.FieldDescriptorProto): String {

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
      field.typeName
    }
    // TODO: Figure out group types
    DescriptorProtos.FieldDescriptorProto.Type.TYPE_GROUP -> ""
    else -> TODO("else case found for ${field.type}")
  }
}

private fun parseLabel(label: DescriptorProtos.FieldDescriptorProto.Label): Field.Label? {
  return when (label) {
    DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL -> Field.Label.OPTIONAL
    DescriptorProtos.FieldDescriptorProto.Label.LABEL_REQUIRED -> Field.Label.REQUIRED
    DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED -> Field.Label.REPEATED
    else -> null
  }
}

private fun <T: ExtendableMessage<T>> parseOptions(options: T, descs: DescriptorSource): List<OptionElement> {
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

private data class SourceCodeInfo(val comment: String, val loc: Location)

private class SourceCodeHelper(
  fd: DescriptorProtos.FileDescriptorProto
) {
  val locs: Map<List<Int>, DescriptorProtos.SourceCodeInfo.Location> = makeLocationMap(fd.sourceCodeInfo.locationList)
  val baseLoc: Location = Location.get(fd.name)

  fun getLocation(path: List<Int>): SourceCodeInfo {
    val l = locs[path]
    val loc = if (l == null) baseLoc else baseLoc.at(l.getSpan(0), l.getSpan(1))
    var comment = l?.leadingComments
    if ((comment ?: "") == "") {
      comment = l?.trailingComments
    }
    return SourceCodeInfo(comment ?: "", loc)
  }

  private fun makeLocationMap(locs: List<DescriptorProtos.SourceCodeInfo.Location>): Map<List<Int>, DescriptorProtos.SourceCodeInfo.Location> {
    val m = mutableMapOf<List<Int>, DescriptorProtos.SourceCodeInfo.Location>()
    for (loc in locs) {
      val path = mutableListOf<Int>()
      for (pathElem in loc.pathList) {
        path.add(pathElem)
      }
      m[path] = loc
    }
    return m
  }
}

