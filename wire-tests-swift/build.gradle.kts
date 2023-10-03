subprojects {
  afterEvaluate {
    tasks.withType(SwiftCompile::class).all {
      // Include the ${DEVELOPER_DIR}/usr/lib as we also need to use libXCTestSwiftSupport.dylib as of
      // Xcode 12.5:
      // https://forums.swift.org/t/why-xcode-12-5b-cannot-find-xctassertequal-in-scope-in-playground/44411
      val developerDir = compilerArgs.get().extractDeveloperDir() ?: return@all
      compilerArgs.add("-I$developerDir/usr/lib")
    }

    tasks.withType(LinkMachOBundle::class).all {
      // Include the ${DEVELOPER_DIR}/usr/lib as we also need to use libXCTestSwiftSupport.dylib as of
      // Xcode 12.5:
      // https://forums.swift.org/t/why-xcode-12-5b-cannot-find-xctassertequal-in-scope-in-playground/44411
      val developerDir = linkerArgs.get().extractDeveloperDir() ?: return@all
      linkerArgs.add("-L$developerDir/usr/lib")
    }
  }
}

fun List<String>.extractDeveloperDir(): String? = this
  .firstOrNull {
    it.startsWith("-F") && it.endsWith("/Developer/Library/Frameworks")
  }
  ?.removePrefix("-F")
  ?.removeSuffix("/Library/Frameworks")
