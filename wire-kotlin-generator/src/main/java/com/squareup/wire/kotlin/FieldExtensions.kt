/*
 * Copyright (C) 2019 Square, Inc.
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
package com.squareup.wire.kotlin

import com.squareup.wire.schema.Field
import com.squareup.wire.schema.ProtoType

internal val Field.isMap: Boolean
  get() = type!!.isMap

internal val Field.keyType: ProtoType
  get() = type!!.keyType!!

internal val Field.valueType: ProtoType
  get() = type!!.valueType!!

internal val Field.isScalar: Boolean
  get() = type!!.isScalar
