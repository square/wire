/*
 * Copyright 2021 Square Inc.
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

import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.Syntax
import com.squareup.wire.WireField

interface FieldOrOneOfBinding<M : Message<M, B>, B : Message.Builder<M, B>> {
  val tag: Int

  val label: WireField.Label

  val redacted: Boolean

  val isMap: Boolean

  /**
   * The name of the field in generated code. If the declared name is a keyword like `fun`, this
   * will be a transformed name like `fun_`.
   */
  val name: String

  /**
   * The name of the field as declared in the `.proto` file.
   */
  val declaredName: String

  /**
   * The JSON name as determined at code-generation name. This is usually camelCase even if the
   * field is declared in snake_case.
   */
  val wireFieldJsonName: String

  fun keyAdapter(): ProtoAdapter<*>

  fun adapter(): ProtoAdapter<Any>

  fun value(builder: B, value: Any)

  fun set(builder: B, value: Any?)

  operator fun get(message: M): Any?

  fun getFromBuilder(builder: B): Any?

  fun singleAdapter(): ProtoAdapter<*>

  fun omitFromJson(syntax: Syntax, value: Any?): Boolean
}
