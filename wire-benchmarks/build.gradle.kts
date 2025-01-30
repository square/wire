import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.DontIncludeResourceTransformer
import com.github.jengelman.gradle.plugins.shadow.transformers.IncludeResourceTransformer
import com.google.protobuf.gradle.protobuf

plugins {
  id("java-library")
  kotlin("jvm")
  id("com.google.protobuf")
  id("com.squareup.wire")
  id("com.github.johnrengelman.shadow")
  id("me.champeau.jmh").version("0.7.3")
}

sourceSets {
  main {
    // Adds protobuf generated classes to our source sets.
    java.srcDir("$buildDir/generated/source/proto/main/java")
  }
}

protobuf {
  protoc {
    artifact = libs.protobuf.protoc.get().toString()
  }
}

wire {
  kotlin {
  }
}

jmh {
  jvmArgs.addAll(listOf("-Djmh.separateClasspathJAR=true"))
  includes.addAll(listOf("""com\.squareup\.wire\.benchmarks\..*Benchmark.*"""))
  duplicateClassesStrategy.set(DuplicatesStrategy.WARN)
  verbosity.set("EXTRA")
}

dependencies {
  api(libs.jmh.core)
  jmh(libs.jmh.core)
  jmh(libs.jmh.generator)

  protobuf(projects.wireSchema)
  implementation(projects.wireMoshiAdapter)
  implementation(projects.wireRuntime)
  implementation(libs.okio.core)
  implementation(libs.protobuf.java)
}

tasks {
  val jmhJar by getting(ShadowJar::class) {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

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
