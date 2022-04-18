package com.squareup.wire

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.SchemaLoader
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

/** Builds a schema out of written `.proto` files. */
class SchemaBuilder {
  private val sourcePath: Path = "/source".toPath()
  private val fileSystem: FileSystem = FakeFileSystem()

  init {
    fileSystem.createDirectories(sourcePath)
  }

  /**
   * Add a file to be loaded into the schema.
   * @param name The qualified name of the file. This can contain slashes.
   * @param protoFile The content of the file.
   */
  fun add(name: String, protoFile: String): SchemaBuilder {
    require(name.endsWith(".proto")) {
      "unexpected file extension for $name. Proto files should use the '.proto' extension"
    }

    val relativePath = name.toPath()
    try {
      val resolvedPath = sourcePath / relativePath
      val parent = resolvedPath.parent
      if (parent != null) {
        fileSystem.createDirectories(parent)
      }
      fileSystem.write(resolvedPath) {
        writeUtf8(protoFile)
      }
    } catch (e: IOException) {
      throw AssertionError(e)
    }
    return this
  }

  fun build(): Schema {
    val schemaLoader = SchemaLoader(fileSystem)
    schemaLoader.initRoots(
      sourcePath = listOf(Location.get(sourcePath.toString())),
      protoPath = listOf(),
    )
    return schemaLoader.loadSchema()
  }
}

/** Builds a schema out of written `.proto` files. */
inline fun buildSchema(builderAction: SchemaBuilder.() -> Unit): Schema {
  return SchemaBuilder().apply(builderAction).build()
}
