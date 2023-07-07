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

/**
 * Create and return an instance of [EventListener.Factory].
 *
 * @param eventListenerFactoryClass a fully qualified class name for a class that implements [EventListener.Factory]. The
 * class must have a no-arguments public constructor.
 */
fun newEventListenerFactory(eventListenerFactoryClass: String): EventListener.Factory {
  return ClassNameEventListenerFactory(eventListenerFactoryClass)
}

/**
 * This event listener factory is serializable (so Gradle can cache targets that use it). It works
 * even if the delegate event listener class is itself not serializable.
 */
private class ClassNameEventListenerFactory(
  private val eventListenerFactoryClass: String,
) : EventListener.Factory {
  @Transient
  private var cachedDelegate: EventListener.Factory? = null

  private val delegate: EventListener.Factory
    get() {
      val cachedResult = cachedDelegate
      if (cachedResult != null) return cachedResult

      val eventListenerType = try {
        Class.forName(eventListenerFactoryClass)
      } catch (exception: ClassNotFoundException) {
        throw IllegalArgumentException("Couldn't find EventListenerClass '$eventListenerFactoryClass'")
      }

      val constructor = try {
        eventListenerType.getConstructor()
      } catch (exception: NoSuchMethodException) {
        throw IllegalArgumentException("No public constructor on $eventListenerFactoryClass")
      }

      val result = constructor.newInstance() as? EventListener.Factory
        ?: throw IllegalArgumentException("$eventListenerFactoryClass does not implement EventListener.Factory")
      this.cachedDelegate = result
      return result
    }

  override fun create(): EventListener {
    return delegate.create()
  }
}
