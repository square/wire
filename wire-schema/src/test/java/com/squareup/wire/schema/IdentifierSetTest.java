/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.schema;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class IdentifierSetTest {
  @Test public void excludeTypeIncludeTypeThrows() throws Exception {
    try {
      new IdentifierSet.Builder()
          .exclude("a.b.Message")
          .include("a.b.Message")
          .build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("identifier set is inconsistent: \n"
          + "  include a.b.Message conflicts with exclude a.b.Message");
    }
  }

  @Test public void excludeMemberIncludeMemberThrows() throws Exception {
    try {
      new IdentifierSet.Builder()
          .exclude("a.b.Message#member")
          .include("a.b.Message#member")
          .build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("identifier set is inconsistent: \n"
          + "  include a.b.Message#member conflicts with exclude a.b.Message#member");
    }
  }

  @Test public void excludeTypeIncludeMemberThrows() throws Exception {
    try {
      new IdentifierSet.Builder()
          .exclude("a.b.Message")
          .include("a.b.Message#member")
          .build();
      fail();
    } catch (IllegalArgumentException expected) {
      assertThat(expected).hasMessage("identifier set is inconsistent: \n"
          + "  include a.b.Message#member conflicts with exclude a.b.Message");
    }
  }

  @Test public void includeTypeExcludeMemberIsOkay() throws Exception {
    IdentifierSet identifierSet = new IdentifierSet.Builder()
        .include("a.b.Message")
        .exclude("a.b.Message#member")
        .build();
    assertThat(identifierSet.includes).containsExactly("a.b.Message");
    assertThat(identifierSet.excludes).containsExactly("a.b.Message#member");
  }
}
