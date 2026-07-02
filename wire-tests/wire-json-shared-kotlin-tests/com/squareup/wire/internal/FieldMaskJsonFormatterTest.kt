/*
 * Copyright (C) 2026 Square, Inc.
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
package com.squareup.wire.internal

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.squareup.wire.FieldMask
import com.squareup.wire.internal.FieldMaskJsonFormatter.fromString
import com.squareup.wire.internal.FieldMaskJsonFormatter.toStringOrNumber
import org.junit.Test

class FieldMaskJsonFormatterTest {
  @Test fun `field mask to string`() {
    assertThat(
      toStringOrNumber(FieldMask(listOf("user.display_name", "photo", "foo_bar.baz_qux"))),
    ).isEqualTo("user.displayName,photo,fooBar.bazQux")
  }

  @Test fun `string to field mask`() {
    assertThat(fromString("user.displayName,photo,fooBar.bazQux"))
      .isEqualTo(FieldMask(listOf("user.display_name", "photo", "foo_bar.baz_qux")))
  }

  @Test fun `leading uppercase path segment decodes with leading underscore`() {
    assertThat(fromString("foo.Bar"))
      .isEqualTo(FieldMask(listOf("foo._bar")))
  }

  @Test fun `empty paths are skipped`() {
    assertThat(toStringOrNumber(FieldMask(listOf("", "photo", ""))))
      .isEqualTo("photo")
    assertThat(fromString(",photo,"))
      .isEqualTo(FieldMask(listOf("photo")))
    assertThat(fromString(""))
      .isEqualTo(FieldMask())
  }
}
