package com.squareup.wire

@OptionalExpectation
@UseExperimental(ExperimentalMultiplatform::class)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
expect annotation class IgnoreJs()
