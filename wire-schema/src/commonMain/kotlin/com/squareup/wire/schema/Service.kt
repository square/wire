/*
 * Copyright (C) 2015 Square, Inc.
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
@file:Suppress("NAME_SHADOWING")

package com.squareup.wire.schema

import com.squareup.wire.schema.ProtoMember.Companion.get
import com.squareup.wire.schema.ProtoType.Companion.get
import com.squareup.wire.schema.Rpc.Companion.fromElements
import com.squareup.wire.schema.internal.parser.ServiceElement
import kotlin.jvm.JvmName
import kotlin.jvm.JvmStatic

data class Service(
  @get:JvmName("type") // For binary compatibility.
  val type: ProtoType,
  @get:JvmName("location") // For binary compatibility.
  val location: Location,
  @get:JvmName("documentation") // For binary compatibility.
  val documentation: String,
  @get:JvmName("name") // For binary compatibility.
  val name: String,
  @get:JvmName("rpcs") // For binary compatibility.
  val rpcs: List<Rpc>,
  @get:JvmName("options") // For binary compatibility.
  val options: Options,
) {
  /** Returns the RPC named `name`, or null if this service has no such method.  */
  fun rpc(name: String): Rpc? {
    return rpcs.find { it.name == name }
  }

  fun link(linker: Linker) {
    var linker = linker
    linker = linker.withContext(this)
    for (rpc in rpcs) {
      rpc.link(linker)
    }
  }

  fun linkOptions(linker: Linker, validate: Boolean) {
    val linker = linker.withContext(this)
    for (rpc in rpcs) {
      rpc.linkOptions(linker, validate)
    }
    options.link(linker, location, validate)
  }

  fun validate(linker: Linker) {
    var linker = linker
    linker = linker.withContext(this)
    validateRpcUniqueness(linker, rpcs)
    for (rpc in rpcs) {
      rpc.validate(linker)
    }
  }

  private fun validateRpcUniqueness(
    linker: Linker,
    rpcs: List<Rpc>,
  ) {
    val nameToRpc = linkedMapOf<String, MutableSet<Rpc>>()
    for (rpc in rpcs) {
      nameToRpc.getOrPut(rpc.name, { mutableSetOf() }).add(rpc)
    }
    for ((key, values) in nameToRpc) {
      if (values.size > 1) {
        val error = buildString {
          append("mutable rpcs share name $key:")
          values.forEachIndexed { index, rpc ->
            append("\n  ${index + 1}. ${rpc.name} (${rpc.location})")
          }
        }
        linker.errors += error
      }
    }
  }

  fun retainAll(
    schema: Schema,
    markSet: MarkSet,
  ): Service? {
    // If this service is not retained, prune it.
    if (!markSet.contains(type)) {
      return null
    }

    val retainedRpcs = mutableListOf<Rpc>()
    for (rpc in rpcs) {
      val retainedRpc = rpc.retainAll(schema, markSet)
      if (retainedRpc != null && markSet.contains(get(type, rpc.name))) {
        retainedRpcs.add(retainedRpc)
      }
    }

    return Service(
      type,
      location,
      documentation,
      name,
      retainedRpcs,
      options.retainAll(schema, markSet),
    )
  }

  companion object {
    internal fun fromElement(
      protoType: ProtoType,
      element: ServiceElement,
    ): Service {
      val rpcs = fromElements(element.rpcs)
      val options = Options(Options.SERVICE_OPTIONS, element.options)

      return Service(
        protoType,
        element.location,
        element.documentation,
        element.name,
        rpcs,
        options,
      )
    }

    @JvmStatic internal fun fromElements(
      packageName: String?,
      elements: List<ServiceElement>,
    ): List<Service> {
      return elements.map { service ->
        val protoType = get(packageName, service.name)
        fromElement(protoType, service)
      }
    }

    @JvmStatic internal fun toElements(services: List<Service>): List<ServiceElement> {
      return services.map { service ->
        ServiceElement(
          service.location,
          service.name,
          service.documentation,
          Rpc.toElements(service.rpcs),
          service.options.elements,
        )
      }
    }
  }
}
