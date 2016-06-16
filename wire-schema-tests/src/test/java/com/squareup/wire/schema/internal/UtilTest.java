/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire.schema.internal;

import org.junit.Test;

import static com.squareup.wire.schema.internal.Util.appendDocumentation;
import static com.squareup.wire.schema.internal.Util.appendIndented;
import static org.assertj.core.api.Assertions.assertThat;

public final class UtilTest {
  @Test public void indentationTest() {
    String input = "Foo\nBar\nBaz";
    String expected = "  Foo\n  Bar\n  Baz\n";
    StringBuilder builder = new StringBuilder();
    appendIndented(builder, input);
    assertThat(builder.toString()).isEqualTo(expected);
  }

  @Test public void documentationTest() {
    String input = "Foo\nBar\nBaz";
    String expected = ""
        + "// Foo\n"
        + "// Bar\n"
        + "// Baz\n";
    StringBuilder builder = new StringBuilder();
    appendDocumentation(builder, input);
    assertThat(builder.toString()).isEqualTo(expected);
  }
}
