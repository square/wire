package com.squareup.wire.kotlin.grpcserver

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.wire.schema.ProtoType

internal class ClassNameGenerator(private val typeToKotlinName: Map<ProtoType, TypeName>) {
  fun classNameFor(type: ProtoType, suffix: String = ""): ClassName {
    val originalClassName = typeToKotlinName[type]!! as ClassName
    return ClassName(
      originalClassName.packageName,
      "${originalClassName.simpleName}$suffix"
    )
  }
}
