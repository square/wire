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
     * Parses a set of comma-delimited name/value pairs.
     *
     * Several code generators treat the parameter argument as holding a list of
     * options separated by commas: e.g., `"foo=bar,baz,qux=corge"` parses
     * to the pairs: `("foo", "bar"), ("baz", ""), ("qux", "corge")`.
     *
     *
     * When a key is present several times, only the last value is retained.
     */
    @JvmStatic
    internal fun parse(text: String): Map<String, String> {
      if (text.isEmpty()) {
        return emptyMap()
      }
      val result: MutableMap<String, String> = HashMap()
      val parts = text.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
      for (part in parts) {
        if (part.isEmpty()) {
          continue
        }
        val equalsPos = part.indexOf('=')
        var key: String
        var value: String
        if (equalsPos < 0) {
          key = part
          value = ""
        } else {
          key = part.substring(0, equalsPos)
          value = part.substring(equalsPos + 1)
        }
        result[key] = value
      }
      return result
    }
  }
}
