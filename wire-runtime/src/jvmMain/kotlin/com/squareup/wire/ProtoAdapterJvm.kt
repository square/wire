/*
 * Copyright 2015 Square Inc.
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

import com.squareup.wire.internal.RuntimeMessageAdapter
import okio.buffer
import okio.sink
import okio.source
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object ProtoAdapterJvm {
  /** Encode `value` and write it to `stream`. */
  @Throws(IOException::class)
  @JvmStatic fun <E> ProtoAdapter<E>.encode(stream: OutputStream, value: E) {
    val buffer = stream.sink().buffer()
    encode(buffer, value)
    buffer.emit()
  }

  /** Read an encoded message from `stream`. */
  @Throws(IOException::class)
  @JvmStatic fun <E> ProtoAdapter<E>.decode(
    stream: InputStream
  ): E = decode(stream.source().buffer())

  /** Creates a new proto adapter for `type`. */
  @JvmStatic fun <M : Message<M, B>, B : Message.Builder<M, B>> newMessageAdapter(
    type: Class<M>
  ): ProtoAdapter<M> {
    return RuntimeMessageAdapter.create(type)
  }

  /** Creates a new proto adapter for `type`. */
  @JvmStatic fun <E : WireEnum> newEnumAdapter(type: Class<E>): EnumAdapter<E> {
    return RuntimeEnumAdapter(type)
  }

  /** Returns the adapter for the type of `Message`. */
  @JvmStatic fun <M : Message<*, *>> get(message: M): ProtoAdapter<M> {
    return get(message.javaClass)
  }

  /** Returns the adapter for `type`. */
  @JvmStatic fun <M> get(type: Class<M>): ProtoAdapter<M> {
    try {
      return type.getField("ADAPTER").get(null) as ProtoAdapter<M>
    } catch (e: IllegalAccessException) {
      throw IllegalArgumentException("failed to access ${type.name}#ADAPTER", e)
    } catch (e: NoSuchFieldException) {
      throw IllegalArgumentException("failed to access ${type.name}#ADAPTER", e)
    }
  }

  /**
   * Returns the adapter for a given `adapterString`. `adapterString` is specified on a proto
   * message field's [WireField] annotation in the form
   * `com.squareup.wire.protos.person.Person#ADAPTER`.
   */
  @JvmStatic fun get(adapterString: String): ProtoAdapter<*> {
    try {
      val hash = adapterString.indexOf('#')
      val className = adapterString.substring(0, hash)
      val fieldName = adapterString.substring(hash + 1)
      return Class.forName(className).getField(fieldName).get(null) as ProtoAdapter<Any>
    } catch (e: IllegalAccessException) {
      throw IllegalArgumentException("failed to access $adapterString", e)
    } catch (e: NoSuchFieldException) {
      throw IllegalArgumentException("failed to access $adapterString", e)
    } catch (e: ClassNotFoundException) {
      throw IllegalArgumentException("failed to access $adapterString", e)
    }
  }
}
