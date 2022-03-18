/*
 * Copyright 2013 Square Inc.
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
@file:JvmName("Wire")

package com.squareup.wire

/**
 * Returns `value` if it is not null; `defaultValue` otherwise. This is used to conveniently return
 * a default value when a value is null. For example,
 *
 * ```
 * MyProto myProto = ...
 * MyField field = Wire.get(myProto.f, MyProto.f_default);
 * ```
 *
 * will attempt to retrieve the value of the field 'f' defined by MyProto. If the field is null
 * (i.e., unset), `get` will return its second argument, which in this case is the default value for
 * the field 'f'.
 */
fun <T> get(value: T?, defaultValue: T): T = value ?: defaultValue
