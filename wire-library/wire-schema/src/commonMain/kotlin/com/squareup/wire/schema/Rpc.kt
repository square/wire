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

import com.squareup.wire.schema.internal.parser.RpcElement
import kotlin.jvm.JvmStatic

class Rpc private constructor(
  val location: Location,
  val name: String,
  val documentation: String,
  private val requestTypeElement: String,
  private val responseTypeElement: String,
  val requestStreaming: Boolean,
  val responseStreaming: Boolean,
  val options: Options
) {
  // Null until this RPC is linked.
  var requestType: ProtoType? = null
    private set

  // Null until this RPC is linked.
  var responseType: ProtoType? = null
    private set

  fun link(linker: Linker) {
    val linker = linker.withContext(this)
    requestType = linker.resolveMessageType(requestTypeElement)
    responseType = linker.resolveMessageType(responseTypeElement)
  }

  fun linkOptions(linker: Linker) {
    val linker = linker.withContext(this)
    options.link(linker)
  }

  fun validate(linker: Linker) {
    val linker = linker.withContext(this)
    linker.validateImport(location, requestType!!)
    linker.validateImport(location, responseType!!)
  }

  fun retainAll(schema: Schema, markSet: MarkSet): Rpc? {
    if (requestType!! !in markSet || responseType!! !in markSet) return null
    val result = Rpc(
        location = location,
        name = name,
        documentation = documentation,
        requestTypeElement = requestTypeElement,
        responseTypeElement = responseTypeElement,
        requestStreaming = requestStreaming,
        responseStreaming = responseStreaming,
        options = options.retainAll(schema, markSet)
    )
    result.requestType = requestType
    result.responseType = responseType
    return result
  }

  companion object {
    @JvmStatic
    fun fromElements(elements: List<RpcElement>): List<Rpc> {
      return elements.map { element ->
        Rpc(
            location = element.location,
            name = element.name,
            documentation = element.documentation,
            requestTypeElement = element.requestType,
            responseTypeElement = element.responseType,
            requestStreaming = element.requestStreaming,
            responseStreaming = element.responseStreaming,
            options = Options(Options.METHOD_OPTIONS, element.options)
        )
      }
    }

    @JvmStatic
    fun toElements(rpcs: List<Rpc>): List<RpcElement> {
      return rpcs.map { rpc ->
        RpcElement(
            location = rpc.location,
            name = rpc.name,
            documentation = rpc.documentation,
            requestType = rpc.requestTypeElement,
            responseType = rpc.responseTypeElement,
            requestStreaming = rpc.requestStreaming,
            responseStreaming = rpc.responseStreaming,
            options = rpc.options.elements
        )
      }
    }
  }
}
