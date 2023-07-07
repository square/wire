plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
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

android {
  namespace = "com.squareup.wire.whiteboard"
}
