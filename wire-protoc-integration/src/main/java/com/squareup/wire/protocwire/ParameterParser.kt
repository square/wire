/*
 * Copyright 2023 Block Inc.
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
package com.squareup.wire.protocwire

internal class ParameterParser {
  companion object {
    /**
     * Parses protoc parameters.
     * Protoc parameters are the `opt` passed as a single string.
     *
     * Wire targets have configuration which relies on multiple values.
     * In order to preserve that behavior, the opts which are passed through
     * will be assumed to be key value pairs separated by `=` and
     * different pairs are delimited by `,`.
     */
    internal fun parse(parameter: String): Map<String, String> {
      val parsedParameters = mutableMapOf<String, String>()
      val split = parameter.split(',')
      split.forEach { str -> str.trim() }
      for (elm in split) {
        if (!elm.contains('=')) {
          continue
        }
        val pair = elm.split('=')
        if (pair.size != 2) {
          continue
        }
        parsedParameters[pair.first()] = pair[1]
      }
      return parsedParameters
    }
  }
}
