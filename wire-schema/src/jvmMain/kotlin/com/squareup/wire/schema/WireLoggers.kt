/*
 * Copyright (C) 2023 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import com.squareup.wire.WireLogger
import okio.Path

/**
 * Create and return an instance of [WireLogger.Factory].
 *
 * @param loggerFactoryClass a fully qualified class name for a class that implements [WireLogger.Factory]. The
 * class must have a no-arguments public constructor.
 */
fun newLoggerFactory(loggerFactoryClass: String): WireLogger.Factory {
  return ClassNameLoggerFactory(loggerFactoryClass)
}

class EmptyWireLoggerFactory : WireLogger.Factory {
  override fun create(): WireLogger {
    return EmptyWireLogger()
  }
}

class EmptyWireLogger : WireLogger {
  override fun artifactHandled(
    outputPath: Path,
    qualifiedName: String,
    targetName: String,
  ) = Unit

  override fun artifactSkipped(type: ProtoType, targetName: String) = Unit
  override fun unusedRoots(unusedRoots: Set<String>) = Unit
  override fun unusedPrunes(unusedPrunes: Set<String>) = Unit
  override fun unusedIncludesInTarget(unusedIncludes: Set<String>) = Unit
  override fun unusedExcludesInTarget(unusedExcludes: Set<String>) = Unit
}

/**
 * This logger factory is serializable (so Gradle can cache targets that use it). It works even if the delegate logger
 * class is itself not serializable.
 */
private class ClassNameLoggerFactory(
  private val loggerFactoryClass: String,
) : WireLogger.Factory {
  @Transient
  private var cachedDelegate: WireLogger.Factory? = null

  private val delegate: WireLogger.Factory
    get() {
      val cachedResult = cachedDelegate
      if (cachedResult != null) return cachedResult

      val wireLoggerType = try {
        Class.forName(loggerFactoryClass)
      } catch (exception: ClassNotFoundException) {
        throw IllegalArgumentException("Couldn't find LoggerClass '$loggerFactoryClass'")
      }

      val constructor = try {
        wireLoggerType.getConstructor()
      } catch (exception: NoSuchMethodException) {
        throw IllegalArgumentException("No public constructor on $loggerFactoryClass")
      }

      val result = constructor.newInstance() as? WireLogger.Factory
        ?: throw IllegalArgumentException("$loggerFactoryClass does not implement WireLogger.Factory")
      this.cachedDelegate = result
      return result
    }

  override fun create(): WireLogger {
    return delegate.create()
  }
}
