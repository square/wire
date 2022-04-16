package com.squareup.wire

import com.squareup.wire.schema.ProtoType
import okio.Path

class WireTestLogger : WireLogger {

  val artifactHandled = ArrayDeque<Triple<Path, String, String>>()
  override fun artifactHandled(outputPath: Path, qualifiedName: String, targetName: String) {
    this.artifactHandled.add(Triple(outputPath, qualifiedName, targetName))
  }

  val artifactSkipped = ArrayDeque<Pair<ProtoType, String>>()
  override fun artifactSkipped(type: ProtoType, targetName: String) {
    this.artifactSkipped.add(type to targetName)
  }

  val unusedRoots = ArrayDeque<Set<String>>()
  override fun unusedRoots(unusedRoots: Set<String>) {
    this.unusedRoots.add(unusedRoots)
  }

  val unusedPrunes = ArrayDeque<Set<String>>()
  override fun unusedPrunes(unusedPrunes: Set<String>) {
    this.unusedPrunes.add(unusedPrunes)
  }

  val unusedIncludesInTarget = ArrayDeque<Set<String>>()
  override fun unusedIncludesInTarget(unusedIncludes: Set<String>) {
    this.unusedIncludesInTarget.add(unusedIncludes)
  }

  val unusedExcludesInTarget = ArrayDeque<Set<String>>()
  override fun unusedExcludesInTarget(unusedExcludes: Set<String>) {
    this.unusedExcludesInTarget.add(unusedExcludes)
  }
}
