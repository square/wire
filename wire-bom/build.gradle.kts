plugins {
  id("java-platform")
}

// TODO(Benoit) Find why collectBomConstraints() is causing troubles for consumers using the BOM.
//  I'm seeing many like `:<project>: Could not find com.squareup.wire:wire-compiler:.` etc.
dependencies {
  constraints {
    api(projects.wireCompiler)
    api(projects.wireGradlePlugin)
    api(projects.wireGrpcClient)
    api(projects.wireGrpcClient.group + ":wire-grpc-client-jvm:" + projects.wireGrpcClient.version)
    api(projects.wireGrpcMockwebserver)
    api(projects.wireGsonSupport)
    api(projects.wireJavaGenerator)
    api(projects.wireKotlinGenerator)
    api(projects.wireMoshiAdapter)
    api(projects.wireReflector)
    api(projects.wireRuntime)
    api(projects.wireRuntime.group + ":wire-runtime-jvm:" + projects.wireRuntime.version)
    api(projects.wireSchema)
    api(projects.wireSchema.group + ":wire-schema-jvm:" + projects.wireSchema.version)
    api(projects.wireSchemaTests)
    api(projects.wireSchemaTests.group + ":wire-schema-tests-jvm:" + projects.wireSchemaTests.version)
    api(projects.wireSwiftGenerator)
  }
}

extensions.configure<PublishingExtension> {
  publications.create("maven-java-platform", MavenPublication::class) {
    from(project.components.getByName("javaPlatform"))
  }
}
