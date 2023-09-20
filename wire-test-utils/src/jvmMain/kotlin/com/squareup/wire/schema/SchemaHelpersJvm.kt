/*
 * Copyright (C) 2022 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import com.squareup.wire.SchemaBuilder
import java.io.File
import okio.Path
import okio.buffer
import okio.source

/** This will read the content of [path] and add it to the [SchemaBuilder]. */
fun SchemaBuilder.addLocal(path: Path): SchemaBuilder {
  val file = File(path.toString())
  file.source().use { source ->
    val protoFile = source.buffer().readUtf8()
    return add(path, protoFile)
  }
}

/**
 * This will read the content of [path] in the context of `../wire-tests/src/commonTest/proto/java/`
 * and add it to the [SchemaBuilder].
 */
fun SchemaBuilder.addFromTest(path: Path): SchemaBuilder {
  val file = File("../wire-tests/src/commonTest/proto/java/$path")
  file.source().use { source ->
    val protoFile = source.buffer().readUtf8()
    return add(path, protoFile)
  }
}
