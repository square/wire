package com.squareup.wire.kotlin.grpcserver

import com.squareup.kotlinpoet.FileSpec
import com.squareup.wire.schema.RepoBuilder
import okio.buffer
import okio.source
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

internal class KotlinGrpcGeneratorTest {
  @Test
  fun fullFile() {
    val repoBuilder = RepoBuilder().addLocal("src/test/proto/RouteGuideProto.proto")
    val service = repoBuilder.schema().getService("routeguide.RouteGuide")

    val (_, typeSpec) = KotlinGrpcGenerator().generateGrpcServer(service!!)
    val output = FileSpec.get("routeguide.kotlin", typeSpec)

    assertThat(output.toString())
      .isEqualTo(File("src/test/golden/RouteGuideWireGrpc.kt").source().buffer().readUtf8())
  }
}
