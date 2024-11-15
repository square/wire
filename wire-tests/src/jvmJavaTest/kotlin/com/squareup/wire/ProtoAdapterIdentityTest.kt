/*
 * Copyright (C) 2020 Square, Inc.
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
package com.squareup.wire

import assertk.assertThat
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import com.squareup.wire.FieldEncoding.LENGTH_DELIMITED
import com.squareup.wire.protos.person.Person
import com.squareup.wire.protos.person.Person.PhoneType
import com.squareup.wire.protos.simple.SimpleMessage.NestedEnum
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.test.Test
import okio.ByteString

class ProtoAdapterIdentityTest {
  @Test fun generatedAdaptersHaveNullIdentities() {
    for (protoAdapter in builtInProtoAdapters) {
      when {
        protoAdapter.type == Boolean::class && protoAdapter.syntax === Syntax.PROTO_2 -> {
          assertThat(protoAdapter.identity).isEqualTo(false)
        }
        protoAdapter.type == Nothing::class -> {
          // StructNull's <Nothing> in Kotlin is <Void> in Java.
          assertThat(protoAdapter.identity).isNull()
        }
        protoAdapter.type == DoubleArray::class -> {
          assertThat(protoAdapter.identity!!).isInstanceOf<DoubleArray>().isEmpty()
        }
        protoAdapter.type == FloatArray::class -> {
          assertThat(protoAdapter.identity!!).isInstanceOf<FloatArray>().isEmpty()
        }
        protoAdapter.type == LongArray::class -> {
          assertThat(protoAdapter.identity!!).isInstanceOf<LongArray>().isEmpty()
        }
        protoAdapter.type == IntArray::class -> {
          assertThat(protoAdapter.identity!!).isInstanceOf<IntArray>().isEmpty()
        }
        protoAdapter.type.isPrimitive && protoAdapter.syntax === Syntax.PROTO_2 -> {
          // All other primitive types are numbers and must have 0 as their identity value.
          assertThat((protoAdapter.identity as Number).toDouble()).isEqualTo(0.0)
        }
        protoAdapter.type == ByteString::class && protoAdapter.syntax === Syntax.PROTO_2 -> {
          assertThat(protoAdapter.identity).isEqualTo(ByteString.EMPTY)
        }
        protoAdapter.type == String::class && protoAdapter.syntax === Syntax.PROTO_2 -> {
          assertThat(protoAdapter.identity).isEqualTo("")
        }
        protoAdapter.typeUrl == "type.googleapis.com/google.protobuf.DoubleValue" -> {
          assertThat(protoAdapter.identity).isEqualTo(ProtoAdapter.DOUBLE.identity)
        }
        protoAdapter.typeUrl == "type.googleapis.com/google.protobuf.FloatValue" -> {
          assertThat(protoAdapter.identity).isEqualTo(ProtoAdapter.FLOAT.identity)
        }
        protoAdapter.typeUrl == "type.googleapis.com/google.protobuf.Int64Value" -> {
          assertThat(protoAdapter.identity).isEqualTo(ProtoAdapter.INT64.identity)
        }
        protoAdapter.typeUrl == "type.googleapis.com/google.protobuf.UInt64Value" -> {
          assertThat(protoAdapter.identity).isEqualTo(ProtoAdapter.UINT64.identity)
        }
        protoAdapter.typeUrl == "type.googleapis.com/google.protobuf.Int32Value" -> {
          assertThat(protoAdapter.identity).isEqualTo(ProtoAdapter.INT32.identity)
        }
        protoAdapter.typeUrl == "type.googleapis.com/google.protobuf.UInt32Value" -> {
          assertThat(protoAdapter.identity).isEqualTo(ProtoAdapter.UINT32.identity)
        }
        protoAdapter.typeUrl == "type.googleapis.com/google.protobuf.BoolValue" -> {
          assertThat(protoAdapter.identity).isEqualTo(ProtoAdapter.BOOL.identity)
        }
        protoAdapter.typeUrl == "type.googleapis.com/google.protobuf.StringValue" -> {
          assertThat(protoAdapter.identity).isEqualTo(ProtoAdapter.STRING.identity)
        }
        protoAdapter.typeUrl == "type.googleapis.com/google.protobuf.BytesValue" -> {
          assertThat(protoAdapter.identity).isEqualTo(ProtoAdapter.BYTES.identity)
        }
        else -> {
          // All other types are messages and must have null as their identity value.
          assertThat(protoAdapter.identity).isNull()
        }
      }
    }
  }

  @Test fun generatedMessageAdaptersHaveNullIdentities() {
    assertThat(Person.ADAPTER.identity).isNull()
  }

  @Test fun generatedEnumAdaptersHaveZeroIdentities() {
    assertThat(PhoneType.ADAPTER.identity).isEqualTo(PhoneType.MOBILE) // value = 0.
  }

  @Test fun generatedEnumAdaptersHaveNullIdentitiesWhenThereIsNoZero() {
    assertThat(NestedEnum.ADAPTER.identity).isNull()
  }

  @Test fun runtimeMessageAdaptersHaveNullIdentities() {
    val protoAdapter =
      ProtoAdapter.newMessageAdapter(
        Person::class.java,
      )
    assertThat(protoAdapter.identity).isNull()
  }

  @Test fun runtimeEnumAdaptersHaveZeroIdentities() {
    val protoAdapter = ProtoAdapter.newEnumAdapter(
      PhoneType::class.java,
    )
    assertThat(protoAdapter.identity).isEqualTo(PhoneType.MOBILE) // value = 0.
  }

  @Test fun runtimeEnumAdaptersHaveNullIdentitiesWhenThereIsNoZero() {
    val protoAdapter = ProtoAdapter.newEnumAdapter(
      NestedEnum::class.java,
    )
    assertThat(protoAdapter.identity).isNull()
  }

  @Test fun oldGeneratedMessageAdaptersHaveNullIdentities() {
    // Simulate old generated code that uses the old 2-argument ProtoAdapter constructor.
    val protoAdapter = object : ProtoAdapter<Person>(LENGTH_DELIMITED, Person::class.java) {
      override fun redact(value: Person) = error("not implemented")
      override fun encodedSize(value: Person) = error("not implemented")
      override fun encode(writer: ProtoWriter, value: Person) = error("not implemented")
      override fun encode(writer: ReverseProtoWriter, value: Person) = error("not implemented")
      override fun decode(reader: ProtoReader) = error("not implemented")
    }
    assertThat(protoAdapter.identity).isNull()
  }

  @Test fun oldGeneratedEnumAdaptersHaveZeroIdentities() {
    // Simulate old generated code that uses the old 2-argument ProtoAdapter constructor.
    val protoAdapter = object : EnumAdapter<PhoneType>(PhoneType::class.java) {
      override fun fromValue(value: Int) = error("not implemented")
    }
    assertThat(protoAdapter.identity).isEqualTo(PhoneType.MOBILE) // value = 0.
  }

  @Test fun listIdentityIsEmptyList() {
    assertThat(Person.ADAPTER.asRepeated().identity).isEqualTo(listOf<Person>())
    assertThat(ProtoAdapter.INT32.asRepeated().identity).isEqualTo(listOf<Int>())
  }

  @Test fun packedIdentityIsEmptyList() {
    assertThat(ProtoAdapter.INT32.asPacked().identity).isEqualTo(listOf<Int>())
    assertThat(ProtoAdapter.FIXED64.asPacked().identity).isEqualTo(listOf<Long>())
  }

  @Test fun mapIdentityIsEmptyMap() {
    assertThat(
      ProtoAdapter.newMapAdapter(
        ProtoAdapter.STRING,
        ProtoAdapter.STRING,
      ).identity,
    )
      .isEqualTo(mapOf<String, String>())
  }

  private val KClass<*>?.isPrimitive
    get() = this!!.javaPrimitiveType != null

  private val builtInProtoAdapters: List<ProtoAdapter<*>>
    get() {
      return ProtoAdapter::class.java.declaredFields.mapNotNull {
        when {
          it.type !== ProtoAdapter::class.java -> null
          !Modifier.isStatic(it.modifiers) -> null
          else -> it.get(null) as ProtoAdapter<*>
        }
      }
    }
}
