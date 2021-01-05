package com.squareup.wire.kotlin.grpcserver

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.schema.RepoBuilder
import okio.buffer
import okio.source
import org.assertj.core.api.Assertions
import org.junit.Test
import java.io.File

class ImplBaseTest {
  @Test
  fun addImplBase() {
    val repoBuilder = RepoBuilder().addLocal("src/test/proto/RouteGuideProto.proto")
    val service = repoBuilder.schema().getService("routeguide.RouteGuide")

    val code = FileSpec.builder("routeguide", "RouteGuide")
      .addType(
        TypeSpec.classBuilder("RouteGuideWireGrpc")
          .apply { ImplBase.addImplBase(this, service!!) }
          .build()
      )
      .build()
      .toString()

    println(code)
    Assertions.assertThat(code)
      .isEqualTo(File("src/test/golden/ImplBase.kt").source().buffer().readUtf8())
  }
}
