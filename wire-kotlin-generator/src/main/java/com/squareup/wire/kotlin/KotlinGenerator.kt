/*
 * Copyright 2019 Square Inc.
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
package com.squareup.wire.kotlin

import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.FLOAT
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LONG
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.wire.schema.EnclosingType
import com.squareup.wire.schema.EnumType
import com.squareup.wire.schema.MessageType
import com.squareup.wire.schema.ProtoFile
import com.squareup.wire.schema.ProtoType
import com.squareup.wire.schema.Schema
import com.squareup.wire.schema.Type
import okio.ByteString

class KotlinGenerator private constructor(
  schema: Schema,
  nameToKotlinName: Map<ProtoType, ClassName>,
  emitAndroid: Boolean,
  javaInterOp: Boolean
) : KotlinGeneratorBase(schema, nameToKotlinName, emitAndroid, javaInterOp) {
  private val enumGenerator =
      EnumGenerator(schema, nameToKotlinName, emitAndroid, javaInterOp, nameAllocator)
  private val messageGenerator =
      MessageGenerator(schema, nameToKotlinName, emitAndroid, javaInterOp, nameAllocator, this)

  fun generateType(type: Type): TypeSpec = when (type) {
    is MessageType -> messageGenerator.generate(type)
    is EnumType -> enumGenerator.generate(type)
    is EnclosingType -> generateEnclosing(type)
    else -> error("Unknown type $type")
  }

  private fun generateEnclosing(type: EnclosingType): TypeSpec {
    val classBuilder = TypeSpec.objectBuilder(type.typeName)

    type.nestedTypes()
        .forEach { classBuilder.addType(generateType(it)) }

    return classBuilder.build()
  }

  companion object {
    private val BUILT_IN_TYPES = mapOf(
        ProtoType.BOOL to BOOLEAN,
        ProtoType.BYTES to ByteString::class.asClassName(),
        ProtoType.DOUBLE to DOUBLE,
        ProtoType.FLOAT to FLOAT,
        ProtoType.FIXED32 to INT,
        ProtoType.FIXED64 to LONG,
        ProtoType.INT32 to INT,
        ProtoType.INT64 to LONG,
        ProtoType.SFIXED32 to INT,
        ProtoType.SFIXED64 to LONG,
        ProtoType.SINT32 to INT,
        ProtoType.SINT64 to LONG,
        ProtoType.STRING to String::class.asClassName(),
        ProtoType.UINT32 to INT,
        ProtoType.UINT64 to LONG
    )

    @JvmStatic @JvmName("get")
    operator fun invoke(
      schema: Schema,
      emitAndroid: Boolean = false,
      javaInterop: Boolean = false
    ): KotlinGenerator {
      val map = BUILT_IN_TYPES.toMutableMap()

      fun putAll(kotlinPackage: String, enclosingClassName: ClassName?, types: List<Type>) {
        for (type in types) {
          val className = enclosingClassName?.nestedClass(type.type().simpleName())
              ?: ClassName(kotlinPackage, type.type().simpleName())
          map[type.type()] = className
          putAll(kotlinPackage, className, type.nestedTypes())
        }
      }

      for (protoFile in schema.protoFiles()) {
        val kotlinPackage = protoFile.kotlinPackage()
        putAll(kotlinPackage, null, protoFile.types())

        for (service in protoFile.services()) {
          val className = ClassName(kotlinPackage, service.type().simpleName())
          map[service.type()] = className
        }
      }

      return KotlinGenerator(schema, map, emitAndroid, javaInterop)
    }
  }
}

private fun ProtoFile.kotlinPackage() = javaPackage() ?: packageName() ?: ""