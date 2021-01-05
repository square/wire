package com.squareup.wire.kotlin.grpcserver

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.wire.schema.RepoBuilder
import okio.buffer
import okio.source
import org.assertj.core.api.Assertions
import org.junit.Test
import java.io.File

class StubTest {
  @Test
  fun addStub() {    val repoBuilder = RepoBuilder().addLocal("src/test/proto/RouteGuideProto.proto")
    val service = repoBuilder.schema().getService("routeguide.RouteGuide")

    val code = FileSpec.builder("routeguide", "RouteGuide")
      .addType(
        TypeSpec.classBuilder("RouteGuideWireGrpc")
          .apply { Stub.addStub(this, service!!) }
          .build()
      )
      .build()
      .toString()

    println(code)
    Assertions.assertThat(code)
      .isEqualTo(File("src/test/golden/Stub.kt").source().buffer().readUtf8())
  }
}