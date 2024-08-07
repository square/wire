// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.custom_options.enum_option in custom_options.proto
@file:Suppress(
  "DEPRECATION",
  "RUNTIME_ANNOTATION_NOT_SUPPORTED",
)

package com.squareup.wire.protos.custom_options

import kotlin.Boolean
import kotlin.Suppress
import kotlin.`annotation`.AnnotationRetention
import kotlin.`annotation`.AnnotationTarget
import kotlin.`annotation`.Retention
import kotlin.`annotation`.Target

/**
 * This is a reasonable option! Apply it to your available enum types.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
public annotation class EnumOptionOption(
  public val `value`: Boolean,
)
