plugins {
  id("com.android.application")
}

android {
  compileSdkVersion(30)
  defaultConfig {
    applicationId = "com.squareup.wire.whiteboard"
    minSdkVersion(28)
    targetSdkVersion(30)
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
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.ktx)
  implementation(libs.wire.grpcClient)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.constraintLayout)
}
