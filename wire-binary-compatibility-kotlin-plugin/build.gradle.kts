import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.MavenPublishBaseExtension

plugins {
  kotlin("jvm")
  id("com.github.gmazzo.buildconfig")
  id("com.vanniktech.maven.publish.base")
}

dependencies {
  compileOnly(kotlin("compiler-embeddable"))
  compileOnly(kotlin("stdlib"))
}

buildConfig {
  useKotlinOutput {
    internalVisibility = true
  }

  packageName("com.squareup.wire.binarycompatibility.kotlin")
  buildConfigField("String", "KOTLIN_PLUGIN_ID", "\"com.squareup.wire.binarycompatibility.kotlin\"")
}

configure<MavenPublishBaseExtension> {
  configure(
    KotlinJvm(
      javadocJar = JavadocJar.Empty()
    )
  )
}
