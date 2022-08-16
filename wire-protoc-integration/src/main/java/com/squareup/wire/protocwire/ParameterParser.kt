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
