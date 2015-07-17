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
import java.util.Collections;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class RpcElementTest {
  Location location = Location.get("file.proto");

  @Test public void locationRequired() {
    try {
      RpcElement.builder(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("location");
    }
  }

  @Test public void nameRequired() {
    try {
      RpcElement.builder(location)
          .requestType("Foo")
          .responseType("Bar")
          .build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
  }

  @Test public void requestTypeRequired() {
    try {
      RpcElement.builder(location).name("Test").responseType("Bar").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("requestType");
    }
  }

  @Test public void responseTypeRequired() {
    try {
      RpcElement.builder(location).name("Test").requestType("Foo").build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("responseType");
    }
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      RpcElement.builder(location).name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
    try {
      RpcElement.builder(location).requestType(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("requestType");
    }
    try {
      RpcElement.builder(location).responseType(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("responseType");
    }
    try {
      RpcElement.builder(location).documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation");
    }
    try {
      RpcElement.builder(location).addOption(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option");
    }
    try {
      RpcElement.builder(location).addOptions(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("options");
    }
    try {
      RpcElement.builder(location).addOptions(Collections.<OptionElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option");
    }
  }
}
