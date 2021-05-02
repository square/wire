plugins {
  id("com.android.application")
}

android {
  compileSdkVersion(29)
  defaultConfig {
    applicationId = "com.squareup.wire.whiteboard"
    minSdkVersion(28)
    targetSdkVersion(29)
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  lintOptions {
    isCheckReleaseBuilds = false // CI runs full lint.
  }
}

dependencies {
  implementation(project(":samples:wire-grpc-sample:protos"))
  implementation(deps.kotlin.coroutines.android)
  implementation(deps.androidx.appcompat)
  implementation(deps.androidx.ktx)
  implementation(deps.wire.grpcClient)
  implementation(deps.androidx.appcompat)
  implementation(deps.androidx.constraintLayout)
}
