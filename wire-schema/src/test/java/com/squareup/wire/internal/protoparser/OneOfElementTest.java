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
package com.squareup.wire.internal.protoparser;

import java.util.Collections;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class OneOfElementTest {
  @Test public void nameRequired() {
    try {
      OneOfElement.builder().build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      OneOfElement.builder().name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
    try {
      OneOfElement.builder().documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation");
    }
    try {
      OneOfElement.builder().addField(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("field");
    }
    try {
      OneOfElement.builder().addFields(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("fields");
    }
    try {
      OneOfElement.builder().addFields(Collections.<FieldElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("field");
    }
  }
}
