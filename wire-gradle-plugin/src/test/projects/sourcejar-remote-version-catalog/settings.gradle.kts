pluginManagement {
  listOf(repositories, dependencyResolutionManagement.repositories).forEach {
    it.apply {
      mavenCentral()
      maven {
        url = uri(rootDir.resolve("../../../../../build/localMaven").absolutePath)
      }
    }
  }
}

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      from(files(rootDir.resolve("../../../../../gradle/libs.versions.toml").absolutePath))
      library("allprotos", "com.squareup.wire.dinosaur:all-protos:1.0")
    }
  }
}
