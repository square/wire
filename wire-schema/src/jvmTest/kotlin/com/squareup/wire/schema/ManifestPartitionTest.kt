/*
 * Copyright (C) 2023 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.schema

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.squareup.wire.buildSchema
import kotlin.test.Test
import okio.Path.Companion.toPath

class ManifestPartitionTest {
  @Test fun upstreamPruneIsNotGeneratedDownstream() {
    val schema = buildSchema {
      add(
        "example.proto".toPath(),
        """
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
          |
        """.trimMargin(),
      )
    }

    val modules = mapOf(
      "common" to WireRun.Module(
        pruningRules = PruningRules.Builder()
          .addRoot("B")
          .prune("C")
          .build(),
      ),
      "feature" to WireRun.Module(
        dependencies = setOf("common"),
        pruningRules = PruningRules.Builder()
          .addRoot("A")
          .build(),
      ),
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
    val schema = buildSchema {
      add(
        "example.proto".toPath(),
        """
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
          |
        """.trimMargin(),
      )
    }

    val modules = mapOf(
      "common" to WireRun.Module(
        pruningRules = PruningRules.Builder()
          .addRoot("B")
          .prune("C")
          .build(),
      ),
      "feature" to WireRun.Module(
        dependencies = setOf("common"),
        pruningRules = PruningRules.Builder()
          .addRoot("A")
          .build(),
      ),
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

  @Test fun duplicatedTypesReportedOnce() {
    val schema = buildSchema {
      add(
        "example.proto".toPath(),
        """
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
          |
        """.trimMargin(),
      )
    }

    val modules = mapOf(
      "common" to WireRun.Module(
        pruningRules = PruningRules.Builder()
          .addRoot("B")
          .prune("C")
          .build(),
      ),
      "feature1" to WireRun.Module(
        dependencies = setOf("common"),
        pruningRules = PruningRules.Builder()
          .addRoot("A")
          .build(),
      ),
      "feature2" to WireRun.Module(
        dependencies = setOf("common"),
        pruningRules = PruningRules.Builder()
          .addRoot("A")
          .build(),
      ),
    )

    val partitionedSchema = schema.partition(modules)

    assertThat(partitionedSchema.warnings).containsExactly(
      """
      |C is generated twice in peer modules feature1 and feature2.
      |  Consider moving this type into a common dependency of both modules.
      |  To suppress this warning, explicitly add the type to the roots of both modules.
      """.trimMargin(),
    )
  }
}

private fun Schema.getMessage(name: String): MessageType {
  return checkNotNull(getType(name) as? MessageType) {
    "No type '$name' in schema"
  }
}
