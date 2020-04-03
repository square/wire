/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import com.google.common.io.Closer
import com.squareup.wire.schema.internal.parser.ProtoFileElement
import com.squareup.wire.schema.internal.parser.ProtoParser
import okio.Source
import okio.buffer
import okio.source
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.ArrayDeque
import kotlin.DeprecationLevel.ERROR

/**
 * Load proto files and their transitive dependencies, parse them, and link them together.
 *
 * To find proto files to load, a non-empty set of sources are searched. Each source is either a
 * regular directory or a ZIP file. Within ZIP files, proto files are expected to be found relative
 * to the root of the archive.
 */
class SchemaLoader {
  /** Returns a mutable list of the sources that this loader will load from. */
  @get:JvmName("sources")
  val sources = mutableListOf<Path>()

  /** Returns a mutable list of the proto paths that this loader will load. */
  @get:JvmName("protos")
  val protos = mutableListOf<String>()

  /** Add directory or zip file source from which proto files will be loaded. */
  fun addSource(file: File) = apply {
    addSource(file.toPath())
  }

  /** Add directory or zip file source from which proto files will be loaded. */
  fun addSource(path: Path) = apply {
    sources.add(path)
  }

  @Deprecated("Moved to property", ReplaceWith("this.sources"), level = ERROR)
  @JvmName("-deprecated_sources")
  fun sources(): List<Path> = sources

  /**
   * Add a proto file path to load. Dependencies will be loaded automatically from the configured
   * sources.
   * <p>
   * Examples:
   * <ul>
   * <li>{@code tea.proto}</li>
   * <li>{@code coffee/espresso.proto}</li>
   * </ul>
   */
  fun addProto(proto: String) = apply {
    protos.add(proto)
  }

  @Deprecated("Moved to property", ReplaceWith("this.protos"), level = ERROR)
  @JvmName("-deprecated_protos")
  fun protos(): List<String> = protos

  @Throws(IOException::class)
  fun load(): Schema {
    check(sources.isNotEmpty()) { "No sources added." }

    Closer.create().use { closer ->
      // Map the physical path to the file system root. For regular directories the key and the
      // value are equal. For ZIP files the key is the path to the .zip, and the value is the root
      // of the file system within it.
      val directories = mutableMapOf<Path, Path>()
      for (source in sources) {
        directories[source] = when {
          Files.isRegularFile(source) -> {
            val sourceFs = FileSystems.newFileSystem(source, javaClass.classLoader)
                .also { closer.register(it) }
            sourceFs.rootDirectories.single()
          }
          else -> {
            source
          }
        }
      }
      return loadFromDirectories(directories)
    }
  }

  @Throws(IOException::class)
  private fun loadFromDirectories(directories: Map<Path, Path>): Schema {
    val protos = ArrayDeque(this.protos)
    if (protos.isEmpty()) {
      for (value in directories.values) {
        Files.walkFileTree(value, object : SimpleFileVisitor<Path>() {
          @Throws(IOException::class)
          override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            if (file.fileName.toString().endsWith(".proto")) {
              protos.add(value.relativize(file).toString().replace(File.separator, "/"))
            }
            return FileVisitResult.CONTINUE
          }
        })
      }
    }

    val loaded = mutableMapOf<String, ProtoFile>()
    loaded[DESCRIPTOR_PROTO] = loadSpecialProto(DESCRIPTOR_PROTO)
    loaded[ANY_PROTO] = loadSpecialProto(ANY_PROTO)

    while (!protos.isEmpty()) {
      val proto = protos.removeFirst()
      if (loaded.containsKey(proto)) continue

      var element: ProtoFileElement? = null
      for ((base, value) in directories) {
        val source = source(value, proto) ?: continue

        try {
          val location = Location.get(base.toString(), proto)
          val data = source.buffer().readUtf8()
          element = ProtoParser.parse(location, data)
          break
        } catch (e: IOException) {
          throw IOException("Failed to load $proto from $base", e)
        } finally {
          source.close()
        }
      }
      if (element == null) {
        throw FileNotFoundException("Failed to locate $proto in $sources")
      }

      loaded[proto] = ProtoFile.get(element)

      // Queue dependencies to be loaded.
      for (importPath in element.imports) {
        protos.addLast(importPath)
      }
    }

    return Linker(CoreLoader).link(loaded.values)
  }

  /**
   * Loads a special proto like "google/protobuf/descriptor.proto" from the Wire resources. If the
   * user has provided their own version of this proto, that is preferred.
   */
  @Throws(IOException::class)
  private fun loadSpecialProto(path: String): ProtoFile {
    SchemaLoader::class.java.getResourceAsStream("/$path").source().buffer().use {
      val data = it.readUtf8()
      val location = Location.get("", path)
      val element = ProtoParser.parse(location, data)
      return ProtoFile.get(element)
    }
  }

  companion object {
    private const val ANY_PROTO = "google/protobuf/any.proto"
    private const val DESCRIPTOR_PROTO = "google/protobuf/descriptor.proto"

    @Throws(IOException::class)
    private fun source(base: Path, path: String): Source? {
      val resolvedPath = base.resolve(path)
      return when {
        Files.exists(resolvedPath) -> resolvedPath.source()
        else -> null
      }
    }
  }
}
