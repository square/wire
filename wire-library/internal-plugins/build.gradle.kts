plugins {
  `kotlin-dsl`
  `java-gradle-plugin`
}

repositories {
  jcenter()
}

dependencies {
  api("org.jetbrains.dokka:dokka-gradle-plugin:1.4.20")
  api("org.jetbrains.dokka:dokka-core:1.4.20")
}

gradlePlugin {
  plugins {
    create("internal-publishing") {
      id = "internal-publishing"
      implementationClass = "com.squareup.gradle.InternalPublishingPlugin"
    }
  }
}
