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

import com.squareup.wire.schema.Location;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class EnumConstantElementTest {
  Location location = Location.get("file.proto");

  @Test public void locationRequired() {
    try {
      EnumConstantElement.builder(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("location");
    }
  }

  @Test public void nameRequired() {
    try {
      EnumConstantElement.builder(location).tag(1).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
  }

  @Test public void tagRequired() {
    try {
      EnumConstantElement.builder(location).name("Test").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("tag");
    }
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      EnumConstantElement.builder(location).name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
    try {
      EnumConstantElement.builder(location).documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation");
    }
    try {
      EnumConstantElement.builder(location).addOption(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option");
    }
  }
}
