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
package com.squareup.wire.internal

import kotlin.jvm.JvmName

/** A wrapper around an empty/immutable list which only switches to mutable on first mutation. */
internal class MutableOnWriteList<T>(
  private val immutableList: List<T>
) : AbstractMutableList<T>(), RandomAccess, Serializable {
  internal var mutableList: List<T> = immutableList

  override fun get(index: Int): T = mutableList[index]

  override val size: Int
    @get:JvmName("size") get() = mutableList.size

  override fun set(index: Int, element: T): T {
    if (mutableList === immutableList) {
      mutableList = ArrayList(immutableList)
    }
    return (mutableList as ArrayList).set(index, element)
  }

  override fun add(index: Int, element: T) {
    if (mutableList === immutableList) {
      mutableList = ArrayList(immutableList)
    }
    (mutableList as ArrayList).add(index, element)
  }

  override fun removeAt(index: Int): T {
    if (mutableList === immutableList) {
      mutableList = ArrayList(immutableList)
    }
    return (mutableList as ArrayList).removeAt(index)
  }

  @Throws(ObjectStreamException::class)
  private fun writeReplace(): Any = ArrayList(mutableList)
}
