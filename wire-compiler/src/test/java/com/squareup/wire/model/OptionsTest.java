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
package com.squareup.wire.model;

import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.Test;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public final class OptionsTest {
  @Test public void resolveFieldPathMatchesFirstSegment() throws Exception {
    assertEquals(
        asList("a", "b", "c", "d"),
        asList(Options.resolveFieldPath("a.b.c.d", set("a", "z", "y"))));
  }

  @Test public void resolveFieldPathMatchesMultipleSegments() throws Exception {
    assertEquals(
        asList("a.b", "c", "d"),
        asList(Options.resolveFieldPath("a.b.c.d", set("a.b", "z.b", "y.b"))));
  }

  @Test public void resolveFieldPathMatchesAllSegments() throws Exception {
    assertEquals(
        asList("a.b.c.d"),
        asList(Options.resolveFieldPath("a.b.c.d", set("a.b.c.d", "z.b.c.d"))));
  }

  @Test public void resolveFieldPathMatchesOnlySegment() throws Exception {
    assertEquals(
        asList("a"),
        asList(Options.resolveFieldPath("a", set("a", "b"))));
  }

  @Test public void resolveFieldPathDoesntMatch() throws Exception {
    assertNull(Options.resolveFieldPath("a.b", set("c", "d")));
  }

  private Set<String> set(String... elements) {
    return new LinkedHashSet<String>(asList(elements));
  }
}
