package com.squareup.wire.schema

import com.squareup.wire.schema.internal.CommonSchemaLoader
import com.squareup.wire.schema.internal.toOkioFileSystem
import java.nio.file.FileSystem as NioFileSystem
import okio.FileSystem

actual class SchemaLoader : Loader, ProfileLoader {
  private val delegate: CommonSchemaLoader

  constructor(fileSystem: NioFileSystem) : this(fileSystem.toOkioFileSystem())

  constructor(fileSystem: FileSystem) {
    delegate = CommonSchemaLoader(fileSystem)
  }

  private constructor(enclosing: CommonSchemaLoader, errors: ErrorCollector) {
    delegate = CommonSchemaLoader(enclosing, errors)
  }

  override fun withErrors(errors: ErrorCollector) = SchemaLoader(delegate, errors)

  /** Strict by default. Note that golang cannot build protos with package cycles. */
  actual var permitPackageCycles: Boolean
    get() = delegate.permitPackageCycles
    set(value) {
      delegate.permitPackageCycles = value
    }

  /**
   * If true, the schema loader will load the whole graph, including files and types not used by
   * anything in the source path.
   */
  actual var loadExhaustively: Boolean
    get() = delegate.loadExhaustively
    set(value) {
      delegate.loadExhaustively = value
    }

  /** Subset of the schema that was loaded from the source path. */
  actual val sourcePathFiles: List<ProtoFile>
    get() = delegate.sourcePathFiles

  /** Initialize the [WireRun.sourcePath] and [WireRun.protoPath] from which files are loaded. */
  actual fun initRoots(
    sourcePath: List<Location>,
    protoPath: List<Location>
  ) {
    delegate.initRoots(sourcePath, protoPath)
  }

  override fun loadProfile(name: String, schema: Schema) = delegate.loadProfile(name, schema)

  override fun load(path: String) = delegate.load(path)

  actual fun loadSchema(): Schema = delegate.loadSchema()
}
