/*
 * Copyright 2020 Square Inc.
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
package com.squareup.wire

import com.squareup.wire.protos.person.Person
import com.squareup.wire.protos.person.Person.PhoneType
import okio.ByteString
import org.assertj.core.api.Assertions.assertThat
import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.test.Test

class ProtoAdapterTypeUrlTest {
  @Test fun allBuiltInAdaptersHaveReasonableTypeUrls() {
    for (protoAdapter in builtInProtoAdapters) {
      when {
        protoAdapter.type == Nothing::class -> {
          // StructNull's <Nothing> in Kotlin is <Void> in Java.
          assertThat(protoAdapter.typeUrl)
              .isEqualTo("type.googleapis.com/google.protobuf.NullValue")
        }
        protoAdapter.syntax === Syntax.PROTO_2 &&
            (protoAdapter.type.isPrimitive ||
                protoAdapter.type == String::class ||
                protoAdapter.type == ByteString::class) -> {
          // Scalar types don't have a type URL.
          assertThat(protoAdapter.typeUrl).isNull()
        }
        else -> {
          assertThat(protoAdapter.typeUrl).startsWith("type.googleapis.com/")
        }
      }
    }
  }

  @Test fun generatedMessageAdaptersHaveTypeUrls() {
    assertThat(Person.ADAPTER.typeUrl)
        .isEqualTo("type.googleapis.com/squareup.protos.person.Person")
  }

  @Test fun generatedEnumAdaptersDoNotHaveTypeUrls() {
    assertThat(PhoneType.ADAPTER.typeUrl).isNull()
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
