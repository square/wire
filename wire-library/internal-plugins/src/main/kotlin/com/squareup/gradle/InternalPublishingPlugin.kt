/*
 * Copyright 2021 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.dokka.gradle.DokkaTask

/** Plugin used internally to this project to publish its artifacts. */
class InternalPublishingPlugin : Plugin<Project> {
  override fun apply(project: Project) {
    project.apply(plugin = "maven-publish")
    project.apply(plugin = "signing")
    project.apply(plugin = "org.jetbrains.dokka")

    project.tasks.withType<DokkaTask>().configureEach {
      dokkaSourceSets.configureEach {
        reportUndocumented.set(false)
        skipDeprecated.set(true)
        jdkVersion.set(8)
        perPackageOption {
          matchingRegex.set("com\\.squareup\\.wire\\.internal.*")
          suppress.set(true)
        }
      }
      if (name == "dokkaGfm") {
        outputDirectory.set(project.file("${project.rootDir}/docs/3.x"))
      }
    }

    val versionName = project.findProperty("VERSION_NAME") as String?
    val isReleaseBuild = versionName?.contains("SNAPSHOT") == false
    val pomArtifactId = project.findProperty("POM_ARTIFACT_ID") as String?
    val pomName = project.findProperty("POM_NAME") as String?
    val releaseRepositoryUrl = project.findProperty("RELEASE_REPOSITORY_URL") as String?
      ?: "https://oss.sonatype.org/service/local/staging/deploy/maven2/"
    val snapshotRepositoryUrl = project.findProperty("SNAPSHOT_REPOSITORY_URL") as String?
      ?: "https://oss.sonatype.org/content/repositories/snapshots/"

    val javadocsJar = project.tasks.create("javadocsJar", Jar::class.java) {
      project.tasks.findByName("dokkaGfm")?.let { dokkaGfm ->
        dependsOn(dokkaGfm)
      }
      classifier = "javadoc"
      from("${project.buildDir}/dokka/gfm")
    }

    project.extensions.getByType<PublishingExtension>().apply {
      publications {
        all {
          if (this !is MavenPublication) return@all

          artifact(javadocsJar)
          pom {
            this.description.set("gRPC and protocol buffers for Android, Kotlin, and Java.")
            this.name.set(pomName)
            this.url.set("https://github.com/square/wire/")
            licenses {
              license {
                this.name.set("The Apache Software License, Version 2.0")
                this.url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                this.distribution.set("repo")
              }
            }
            scm {
              this.url.set("https://github.com/square/wire/")
              this.connection.set("scm:git:https://github.com/square/wire.git")
              this.developerConnection.set("scm:git:ssh://git@github.com/square/wire.git")
            }
            developers {
              developer {
                this.id.set("square")
                this.name.set("Square, Inc.")
              }
            }
          }
          artifactId = pomArtifactId
        }
      }

      repositories {
        maven {
          setUrl(if (isReleaseBuild) releaseRepositoryUrl else snapshotRepositoryUrl)
          credentials {
            username = project.findProperty("mavenCentralRepositoryUsername") as String? ?: ""
            password = project.findProperty("mavenCentralRepositoryPassword") as String? ?: ""
          }
        }
        maven {
          name = "test"
          setUrl("file://${project.rootProject.buildDir}/localMaven")
        }
      }
    }

    project.extensions.getByType<SigningExtension>().apply {
      val signingKey = project.findProperty("signingKey") as String? ?: ""
      if (signingKey.isNotEmpty()) {
        useInMemoryPgpKeys(signingKey, "")
      }
      setRequired { isReleaseBuild }
      sign(project.extensions.getByType<PublishingExtension>().publications)
    }
  }
}
