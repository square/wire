package com.squareup.wire.gradle.internal

import org.gradle.api.Project

internal fun Project.targetDefaultOutputPath(): String {
  return "${buildDir}/generated/source/wire"
}

internal fun Project.libraryProtoOutputPath(): String {
  return "${buildDir}/wire/proto-sources"
}
