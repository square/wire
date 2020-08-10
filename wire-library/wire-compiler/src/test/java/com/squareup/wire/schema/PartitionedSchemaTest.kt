/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import com.squareup.wire.schema.WireRun.Module

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ManifestPartitionTest {
  @Test fun upstreamPruneIsNotGeneratedDownstream() {
    val schema = RepoBuilder()
        .add("example.proto", """
          |syntax = "proto2";
          |
          |message A {
          |  optional B b = 1;
          |}
          |message B {
          |  optional C c = 1;
          |}
          |message C {
          |}
          |""".trimMargin())
        .schema()

    val modules = mapOf(
        "common" to Module(
            pruningRules = PruningRules.Builder()
                .addRoot("B")
                .prune("C")
                .build()
        ),
        "feature" to Module(
            dependencies = setOf("common"),
            pruningRules = PruningRules.Builder()
                .addRoot("A")
                .build()
        )
    )

    val partitionedSchema = schema.partition(modules)

    val commonPartition = partitionedSchema.partitions.getValue("common")
    // B has no field of type C because of its inclusion in common's prune list.
    assertThat(commonPartition.schema.getMessage("B").field("c")).isNull()
    // C is not generated in common because of its inclusion in the prune list.
    assertThat(commonPartition.types).doesNotContain(ProtoType.get("C"))

    // C is not in feature because its only dependant is B which is from common.
    val featurePartition = partitionedSchema.partitions.getValue("feature")
    assertThat(featurePartition.types).doesNotContain(ProtoType.get("C"))
  }

  @Test fun upstreamPruneIsNotPrunedDownstream() {
    val schema = RepoBuilder()
        .add("example.proto", """
          |syntax = "proto2";
          |
          |message A {
          |  optional B b = 1;
          |  optional C c = 2;
          |}
          |message B {
          |  optional C c = 1;
          |}
          |message C {
          |}
          |""".trimMargin())
        .schema()

    val modules = mapOf(
        "common" to Module(
            pruningRules = PruningRules.Builder()
                .addRoot("B")
                .prune("C")
                .build()
        ),
        "feature" to Module(
            dependencies = setOf("common"),
            pruningRules = PruningRules.Builder()
                .addRoot("A")
                .build()
        )
    )

    val partitionedSchema = schema.partition(modules)

    val commonPartition = partitionedSchema.partitions.getValue("common")
    // B has no field of type C because of its inclusion in common's prune list.
    assertThat(commonPartition.schema.getMessage("B").field("c")).isNull()
    // C is not generated in common because of its inclusion in the prune list.
    assertThat(commonPartition.types).doesNotContain(ProtoType.get("C"))

    val featurePartition = partitionedSchema.partitions.getValue("feature")
    // A has a field of type C because common's prunes do not apply to feature.
    assertThat(featurePartition.schema.getMessage("A").field("c")).isNotNull()
    // C is generated in feature because common's prunes do not apply and A depends on it.
    assertThat(featurePartition.types).contains(ProtoType.get("C"))
  }
}

private fun Schema.getMessage(name: String): MessageType {
  return checkNotNull(getType(name) as? MessageType) {
    "No type '$name' in schema"
  }
}
