plugins {
  id("com.vanniktech.maven.publish.base")
  id("java-platform")
}

dependencies {
  constraints {
    api(projects.wireCompiler)
    api(projects.wireGradlePlugin)
    api(projects.wireGrpcClient)
    api(projects.wireGrpcServer)
    api(projects.wireGrpcServerGenerator)
    api(projects.wireGrpcMockwebserver)
    api(projects.wireGsonSupport)
    api(projects.wireJavaGenerator)
    api(projects.wireKotlinGenerator)
    api(projects.wireMoshiAdapter)
    api(projects.wireReflector)
    api(projects.wireRuntime)
    api(projects.wireSchema)
    api(projects.wireSchemaTests)
    api(projects.wireSwiftGenerator)
  }
}

extensions.configure<PublishingExtension> {
  publications.create("maven", MavenPublication::class) {
    from(project.components.getByName("javaPlatform"))
  }
}
