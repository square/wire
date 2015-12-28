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
package com.squareup.wire;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class WireFieldTest {

  @Test public void isOneOf() {
    assertThat(WireField.Label.ONE_OF.isOneOf()).isTrue();
    assertThat(WireField.Label.OPTIONAL.isOneOf()).isFalse();
    assertThat(WireField.Label.PACKED.isOneOf()).isFalse();
    assertThat(WireField.Label.REPEATED.isOneOf()).isFalse();
    assertThat(WireField.Label.REQUIRED.isOneOf()).isFalse();
  }

  @Test public void isPacked() {
    assertThat(WireField.Label.ONE_OF.isPacked()).isFalse();
    assertThat(WireField.Label.OPTIONAL.isPacked()).isFalse();
    assertThat(WireField.Label.PACKED.isPacked()).isTrue();
    assertThat(WireField.Label.REPEATED.isPacked()).isFalse();
    assertThat(WireField.Label.REQUIRED.isPacked()).isFalse();
  }

  @Test public void isRepeated() {
    assertThat(WireField.Label.ONE_OF.isRepeated()).isFalse();
    assertThat(WireField.Label.OPTIONAL.isRepeated()).isFalse();
    assertThat(WireField.Label.PACKED.isRepeated()).isTrue();
    assertThat(WireField.Label.REPEATED.isRepeated()).isTrue();
    assertThat(WireField.Label.REQUIRED.isRepeated()).isFalse();
  }
}
