import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  application

  java
  kotlin("jvm")
}

tasks {
  withType<KotlinCompile>().configureEach {
    kotlinOptions {
      freeCompilerArgs += "-Xjvm-default=all"
    }
  }

  // binary: https://www.baeldung.com/kotlin/gradle-executable-jar
  val javaGeneratorBinary = register<Jar>("javaGeneratorBinary") {
    archiveBaseName.set("protoc-java")
    dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources")) // We need this for Gradle optimization to work
    archiveVersion.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes(mapOf("Main-Class" to "com.squareup.wire.protocwire.cmd.JavaGenerator")) } // Provided we set it up in the application plugin configuration
    // zip64 is needed due to the dependency on the gradle plugin.
    // The exclusions are there due to gradle including some rogue files:
    // https://stackoverflow.com/questions/43990707/could-not-find-or-load-main-class-error-for-gradle-generated-scala-jar.
    isZip64 = true
    exclude ("META-INF/*.RSA", "META-INF/*.SF","META-INF/*.DSA")
    val sourcesMain = sourceSets.main.get()
    val contents = configurations.runtimeClasspath.get()
      .map { if (it.isDirectory) it else zipTree(it) } +
      sourcesMain.output
    from(contents)
  }
  val kotlinGeneratorBinary = register<Jar>("kotlinGeneratorBinary") {
    archiveBaseName.set("protoc-kotlin")
    dependsOn.addAll(listOf("compileJava", "compileKotlin", "processResources")) // We need this for Gradle optimization to work
    archiveVersion.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest { attributes(mapOf("Main-Class" to "com.squareup.wire.protocwire.cmd.KotlinGenerator")) } // Provided we set it up in the application plugin configuration
    // zip64 is needed due to the dependency on the gradle plugin.
    // The exclusions are there due to gradle including some rogue files:
    // https://stackoverflow.com/questions/43990707/could-not-find-or-load-main-class-error-for-gradle-generated-scala-jar.
    isZip64 = true
    exclude ("META-INF/*.RSA", "META-INF/*.SF","META-INF/*.DSA")
    val sourcesMain = sourceSets.main.get()
    val contents = configurations.runtimeClasspath.get()
      .map { if (it.isDirectory) it else zipTree(it) } +
      sourcesMain.output
    from(contents)
  }
  build {
    dependsOn(kotlinGeneratorBinary, javaGeneratorBinary)
  }
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  implementation(libs.protobuf.java)
  implementation(projects.wireSchema)
  implementation(projects.wireCompiler)
  implementation(projects.wireKotlinGenerator)
  implementation(projects.wireJavaGenerator)
  implementation(projects.wireGradlePlugin)
}
