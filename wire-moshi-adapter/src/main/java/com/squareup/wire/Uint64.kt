package com.squareup.wire

import com.squareup.moshi.JsonQualifier

/**
 * Annotates longs that should be encoded as unsigned. For example, -1L is encoded as
 * 18446744073709551615.
 */
@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
internal annotation class Uint64
