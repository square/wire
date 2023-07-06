import com.squareup.wire.schema.EventListener
import com.squareup.wire.schema.SchemaHandler
import com.squareup.wire.schema.SchemaHandler.Factory

plugins {
  id("java-library")
  kotlin("jvm")
  id("com.squareup.wire")
}

class MyEventListenerFactory : EventListener.Factory {
  override fun create(): EventListener {
    return object: EventListener() {}
  }
}

class MyCustomHandlerFactory : Factory {
  override fun create(): SchemaHandler {
    throw RuntimeException("boom")
  }

  override fun create(
    includes: List<String>,
    excludes: List<String>,
    exclusive: Boolean,
    outDirectory: String,
    options: Map<String, String>,
  ): SchemaHandler {
    throw RuntimeException(
      (("custom handler is running!! " +
        includes.joinToString(", ")).toString() + ", " + excludes.joinToString(
        ", "
      )).toString() + ", " + exclusive + ", " +
        options.entries.toTypedArray().joinToString(", ")
    )
  }
}

wire {
  sourcePath {
    srcDir("src/main/proto")
  }

  eventListenerFactory(MyEventListenerFactory())

  custom {
    schemaHandlerFactory = MyCustomHandlerFactory()
  }
}

dependencies {
  implementation(projects.wireGrpcClient)
  implementation(libs.okio.core)
  implementation(projects.wireCompiler)
  implementation(projects.wireSchema)
  implementation(projects.wireGsonSupport)
  implementation(projects.wireMoshiAdapter)
  implementation(libs.assertj)
  implementation(libs.junit)
  implementation(libs.protobuf.javaUtil)
  implementation(projects.wireTestUtils)
}
