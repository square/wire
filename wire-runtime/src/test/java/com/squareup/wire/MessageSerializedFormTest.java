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

import com.squareup.wire.protos.roots.C;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MessageSerializedFormTest {

  @Test public void readResolve() throws Exception {
    // when
    C message = new C(5);
    MessageSerializedForm messageSerializedForm = new MessageSerializedForm(message, message.getClass());

    // then
    assertThat(messageSerializedForm.readResolve()).isEqualTo(message);
  }
}
