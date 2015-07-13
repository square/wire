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

import com.squareup.wire.internal.protoparser.DataType.NamedType;
import java.util.Collections;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public final class RpcElementTest {
  @Test public void nameRequired() {
    try {
      RpcElement.builder()
          .requestType(NamedType.create("Foo"))
          .responseType(NamedType.create("Bar"))
          .build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
  }

  @Test public void requestTypeRequired() {
    try {
      RpcElement.builder().name("Test").responseType(NamedType.create("Bar")).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("requestType");
    }
  }

  @Test public void responseTypeRequired() {
    try {
      RpcElement.builder().name("Test").requestType(NamedType.create("Foo")).build();
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("responseType");
    }
  }

  @Test public void nullBuilderValuesThrow() {
    try {
      RpcElement.builder().name(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("name");
    }
    try {
      RpcElement.builder().requestType(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("requestType");
    }
    try {
      RpcElement.builder().responseType(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("responseType");
    }
    try {
      RpcElement.builder().documentation(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("documentation");
    }
    try {
      RpcElement.builder().addOption(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option");
    }
    try {
      RpcElement.builder().addOptions(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("options");
    }
    try {
      RpcElement.builder().addOptions(Collections.<OptionElement>singleton(null));
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("option");
    }
  }
}
