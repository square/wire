/*
 * Copyright 2015 Square Inc.
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
package com.squareup.wire.internal;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class InternalTest {
  @Test public void countNonNull() throws Exception {
    assertThat(Internal.countNonNull(null, null)).isEqualTo(0);
    assertThat(Internal.countNonNull("xx", null)).isEqualTo(1);
    assertThat(Internal.countNonNull("xx", "xx")).isEqualTo(2);
    assertThat(Internal.countNonNull("xx", "xx", null)).isEqualTo(2);
    assertThat(Internal.countNonNull("xx", "xx", "xx")).isEqualTo(3);
    assertThat(Internal.countNonNull("xx", "xx", "xx", null)).isEqualTo(3);
    assertThat(Internal.countNonNull("xx", "xx", "xx", "xx")).isEqualTo(4);
    assertThat(Internal.countNonNull("xx", "xx", "xx", "xx", (Object) null)).isEqualTo(4);
    assertThat(Internal.countNonNull("xx", "xx", "xx", "xx", "xx")).isEqualTo(5);
  }
}
