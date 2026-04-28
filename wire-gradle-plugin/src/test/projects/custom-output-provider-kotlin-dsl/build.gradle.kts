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
    outProperty.set(providers.gradleProperty("wireOut"))
    includesProperty.set(providers.provider { listOf(providers.gradleProperty("includeType").get()) })
    excludesProperty.set(providers.provider { listOf(providers.gradleProperty("excludeType").get()) })
    exclusiveProperty.set(providers.provider { providers.gradleProperty("exclusive").get().toBoolean() })
    optionsProperty.set(
      providers.provider {
        mapOf(
          "a" to providers.gradleProperty("optionA").get(),
          "b" to providers.gradleProperty("optionB").get(),
        )
      },
    )
    schemaHandlerFactoryClassProperty.set(providers.gradleProperty("handlerClass"))
  }
}
