plugins {
  id("com.android.application")
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
