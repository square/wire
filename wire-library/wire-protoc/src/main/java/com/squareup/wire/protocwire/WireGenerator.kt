package com.squareup.wire.protocwire

import com.google.protobuf.DescriptorProtos
import com.google.protobuf.GeneratedMessageV3
import com.google.protobuf.GeneratedMessageV3.ExtendableMessage
import com.google.protobuf.compiler.PluginProtos
import com.squareup.wire.Syntax
import com.squareup.wire.WireLogger
import com.squareup.wire.kotlin.KotlinGenerator
import com.squareup.wire.protocwire.Plugin.DefaultEnvironment
import com.squareup.wire.schema.ClaimedDefinitions
import com.squareup.wire.schema.ClaimedPaths
import com.squareup.wire.schema.CoreLoader
import com.squareup.wire.schema.EmittingRules
import com.squareup.wire.schema.ErrorCollector
import com.squareup.wire.schema.KotlinProtocTarget
import com.squareup.wire.schema.KotlinTarget
import com.squareup.wire.schema.Linker
import com.squareup.wire.schema.Location
import com.squareup.wire.schema.ProfileLoader
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.SchemaHandler
import com.squareup.wire.schema.internal.parser.MessageElement
import com.squareup.wire.schema.internal.parser.OptionElement
import com.squareup.wire.schema.internal.parser.ProtoFileElement
import java.io.File
import java.io.InputStream
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
      context.addFile("result.log", e.stackTraceToString())
    }
  }

  companion object {
    @JvmStatic
    fun main(args: Array<String>) {
//      Plugin.run(WireGenerator(), StubbedTestEnvironment())
      Plugin.run(WireGenerator())
    }
  }
}

class StubbedTestEnvironment() : DefaultEnvironment() {
  override fun getInputStream(): InputStream {
    return File("hallo").inputStream()
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
    options = parseOptions(fileDescriptor.options),
    syntax = Syntax.PROTO_3,
  )
}

fun parseMessage(message: DescriptorProtos.DescriptorProto): MessageElement {
  return MessageElement(
    location = Location.get(message.name),
    name = message.name,
    documentation = "",
    options = parseOptions(message.options),
  )
}

fun <T: ExtendableMessage<T>> parseOptions(options: T): List<OptionElement> {

  return emptyList()
}
