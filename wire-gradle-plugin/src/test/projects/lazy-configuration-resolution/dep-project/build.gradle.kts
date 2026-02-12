buildscript {
  dependencies {
    classpath(libs.pluginz.kotlin)
  }

  repositories {
    maven {
      setUrl(File(rootDir, "../../../../../build/localMaven").toURI().toString())
    }
    mavenCentral()
    google()
  }
}

plugins {
  id("org.jetbrains.kotlin.jvm") version "1.9.22"
}

// The below is redundant since the kotlin plugin above does it too
// but some future version of the kotlin plugin may not, and we still
// want to capture this explicitly. The projectsEvaluated call fails
// if a gradle mutation guard is in place, which means it fails if
// wire-project forces this project to be configured while in the middle
// of applying the wire plugin (i.e. because the wire plugin evaluates
// the project dependency). Wire should not evaluate the project
// dependency eagerly.
gradle.projectsEvaluated {
}
