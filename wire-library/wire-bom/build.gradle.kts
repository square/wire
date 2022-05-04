plugins {
  id("com.vanniktech.maven.publish.base")
  id("java-platform")
}

dependencies {
  constraints {
    api(project(":wire-compiler"))
    api(project(":wire-gradle-plugin"))
    api(project(":wire-grpc-client"))
    api(project(":wire-grpc-server"))
    api(project(":wire-grpc-server-generator"))
    api(project(":wire-grpc-mockwebserver"))
    api(project(":wire-gson-support"))
    api(project(":wire-java-generator"))
    api(project(":wire-kotlin-generator"))
    api(project(":wire-moshi-adapter"))
    api(project(":wire-reflector"))
    api(project(":wire-runtime"))
    api(project(":wire-schema"))
    api(project(":wire-schema-tests"))
    api(project(":wire-swift-generator"))
  }
}

extensions.configure<PublishingExtension> {
  publications.create("maven", MavenPublication::class) {
    from(project.components.getByName("javaPlatform"))
  }
}
