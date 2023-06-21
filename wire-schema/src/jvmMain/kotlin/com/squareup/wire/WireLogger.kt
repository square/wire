package com.squareup.wire

/**
 * Create and return an instance of [WireLogger.Factory].
 *
 * @param loggerFactoryClass a fully qualified class name for a class that implements [WireLogger.Factory]. The
 * class must have a no-arguments public constructor.
 */
fun newLoggerFactory(loggerFactoryClass: String): WireLogger.Factory {
  return ClassNameLoggerFactory(loggerFactoryClass)
}

/**
 * This logger factory is serializable (so Gradle can cache targets that use it). It works even if the delegate logger
 * class is itself not serializable.
 */
private class ClassNameLoggerFactory(
  private val loggerFactoryClass: String
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
