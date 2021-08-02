import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.DontIncludeResourceTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.IncludeResourceTransformer
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc

plugins {
  id("java-library")
  kotlin("jvm")
  id("com.google.protobuf")
  id("com.squareup.wire")
  id("com.github.johnrengelman.shadow")
  id("me.champeau.gradle.jmh")
}

protobuf {
  protoc {
    artifact = deps.protobuf.protoc
  }
}

wire {
  kotlin {
  }
}

jmh {
  jvmArgs = listOf("-Djmh.separateClasspathJAR=true")
  include = listOf("""com\.squareup\.wire\.benchmarks\.EncodeBenchmark.*""")
  duplicateClassesStrategy = DuplicatesStrategy.WARN
}

dependencies {
  api(deps.jmh.core)
  jmh(deps.jmh.core)
  jmh(deps.jmh.generator)

  protobuf(deps.wire.schema)
  implementation(deps.wire.runtime)
  implementation(deps.okio.jvm)
  implementation(deps.protobuf.java)
}

tasks {
  val jmhJar by getting(ShadowJar::class) {
    transform(DontIncludeResourceTransformer().apply {
      resource = "META-INF/BenchmarkList"
    })

    transform(IncludeResourceTransformer().apply {
      resource = "META-INF/BenchmarkList"
      file = file("${project.buildDir}/jmh-generated-resources/META-INF/BenchmarkList")
    })
  }

  val assemble by getting {
    dependsOn(jmhJar)
  }
}
