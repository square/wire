package com.squareup.wire

import com.squareup.wire.schema.Location
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.SchemaLoader
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem

/**
 * Builds a schema out of written `.proto` and `.wire` files.
 */
class SchemaBuilder @JvmOverloads constructor(
  private val sourcePath: Path = "/source".toPath(),
  private val protoPath: Path = "/proto".toPath(),
) {
  private val fileSystem: FileSystem = FakeFileSystem()

  init {
    fileSystem.createDirectories(sourcePath)
    fileSystem.createDirectories(protoPath)
  }

  /**
   * Add a file to the `sourcePath`.
   * @param name The qualified name of the file. This can contain slashes.
   * @param protoFile The content of the file.
   */
  fun add(name: String, protoFile: String): SchemaBuilder {
    return internalToRoot(sourcePath, name, protoFile)
  }

  /**
   * Add a file to the `protoPath`.
   * @param name The qualified name of the file. This can contain slashes.
   * @param protoFile The content of the file.
   */
  fun addToProtoPath(name: String, protoFile: String): SchemaBuilder {
    return internalToRoot(protoPath, name, protoFile)
  }

  private fun internalToRoot(root: Path, name: String, protoFile: String): SchemaBuilder {
    require(name.endsWith(".proto")) {
      "unexpected file extension for $name. Proto files should use the '.proto' extension"
    }

    val relativePath = name.toPath()
    try {
      val resolvedPath = root / relativePath
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
      protoPath = listOf(Location.get(protoPath.toString())),
    )
    return schemaLoader.loadSchema()
  }
}

inline fun buildSchema(
  sourcePath: Path = "/source".toPath(),
  protoPath: Path = "/proto".toPath(),
  builderAction: SchemaBuilder.() -> Unit
): Schema {
  return SchemaBuilder(sourcePath, protoPath).apply(builderAction).build()
}
