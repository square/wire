package com.squareup.wire;

import com.squareup.moshi.JsonQualifier;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates longs that should be encoded as unsigned. For example, -1L is encoded as
 * 18446744073709551615.
 */
@Retention(RUNTIME)
@JsonQualifier
@interface Uint64 {
}
