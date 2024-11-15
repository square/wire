buildCache {
  local {
    directory = File(rootDir, "../.relocation-build-cache")
  }
}

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
    }
  }
}
