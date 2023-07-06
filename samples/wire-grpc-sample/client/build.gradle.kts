plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

android {
  compileSdkVersion(32)

  defaultConfig {
    applicationId = "com.squareup.wire.whiteboard"
    minSdkVersion(30)
    targetSdkVersion(32)
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
  kotlinOptions {
    jvmTarget = "1.8"
  }
  packagingOptions {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }

  namespace = "com.squareup.wire.whiteboard"
}

dependencies {
  implementation(libs.androidx.compose)
  implementation(libs.androidx.ktx)
  implementation(libs.contour)
  implementation(libs.contour)
  implementation(libs.kotlin.coroutines.android)
  implementation(projects.wireGrpcClient)
  implementation(projects.samples.wireGrpcSample.protos)
}
