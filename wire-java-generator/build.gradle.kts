plugins {
  id("java-library")
  kotlin("jvm")
}

dependencies {
  api(projects.wireSchema)
  implementation(projects.wireRuntime)
  implementation(libs.okio.core)
  constraints {
    add("implementation", libs.guava) {
      attributes {
        attribute(
          TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE,
          objects.named(TargetJvmEnvironment::class.java, TargetJvmEnvironment.STANDARD_JVM),
        )
      }
    }
  }
  api(libs.javapoet)
  compileOnly(libs.jsr305)
  testImplementation(projects.wireTestUtils)
  testImplementation(libs.junit)
  testImplementation(libs.kotlin.test.junit)
  testImplementation(libs.truth)
  testImplementation(libs.jimfs)
}
