import com.squareup.wire.gradle.WireExtension

buildscript {
  val wireVersion = gradle.startParameter.projectProperties.getValue("wireVersion")

  dependencies {
    classpath("com.squareup.wire:wire-gradle-plugin:$wireVersion")
  }

  repositories {
    maven {
      url = uri(File(rootDir, "../../../../../build/localMaven"))
    }
    mavenCentral()
    google()
  }
}

apply(plugin = "application")
apply(plugin = "com.squareup.wire")

configure<WireExtension> {
  sourcePath("src/main/proto")

  custom {
    out.set(providers.gradleProperty("wireOut"))
    includes.set(providers.provider { listOf(providers.gradleProperty("includeType").get()) })
    excludes.set(providers.provider { listOf(providers.gradleProperty("excludeType").get()) })
    exclusive.set(providers.provider { providers.gradleProperty("exclusive").get().toBoolean() })
    options.set(
      providers.provider {
        mapOf(
          "a" to providers.gradleProperty("optionA").get(),
          "b" to providers.gradleProperty("optionB").get(),
        )
      },
    )
    schemaHandlerFactoryClass.set(providers.gradleProperty("handlerClass"))
  }
}
