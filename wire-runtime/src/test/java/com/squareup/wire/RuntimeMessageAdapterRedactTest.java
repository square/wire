/*
 * Copyright 2014 Square Inc.
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

import com.squareup.wire.protos.redacted.NotRedacted;
import com.squareup.wire.protos.redacted.Redacted;
import com.squareup.wire.protos.redacted.RedactedChild;
import com.squareup.wire.protos.redacted.RedactedCycleA;
import com.squareup.wire.protos.redacted.RedactedRepeated;
import com.squareup.wire.protos.redacted.RedactedRequired;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RuntimeMessageAdapterRedactTest {
  private final Wire wire = new Wire();

  @Test public void string() throws IOException {
    assertThat(new Redacted.Builder().a("a").b("b").c("c").build().toString())
        .isEqualTo("Redacted{a=██, b=b, c=c}");
  }

  @Test public void message() {
    Redacted message = new Redacted.Builder().a("a").b("b").c("c").build();
    Redacted expected = new Redacted.Builder(message).a(null).build();
    assertThat(wire.adapter(Redacted.class).redact(message)).isEqualTo(expected);
  }

  @Test public void messageWithNoRedactions() {
    NotRedacted message = new NotRedacted.Builder().a("a").b("b").build();
    assertThat(wire.adapter(NotRedacted.class).redact(message)).isEqualTo(message);
  }

  @Test public void nestedRedactions() {
    RedactedChild message = new RedactedChild.Builder()
        .a("a")
        .b(new Redacted.Builder().a("a").b("b").c("c").build())
        .c(new NotRedacted.Builder().a("a").b("b").build())
        .build();
    RedactedChild expected = new RedactedChild.Builder(message)
        .b(new Redacted.Builder(message.b).a(null).build())
        .build();
    assertThat(wire.adapter(RedactedChild.class).redact(message)).isEqualTo(expected);
  }

  @Test public void messageCycle() {
    RedactedCycleA message = new RedactedCycleA.Builder().build();
    assertThat(wire.adapter(RedactedCycleA.class).redact(message)).isEqualTo(message);
  }

  @Test public void repeatedField() {
    RedactedRepeated message = new RedactedRepeated(Arrays.asList("a", "b"));
    RedactedRepeated expected = new RedactedRepeated(Collections.<String>emptyList());
    assertThat(wire.adapter(RedactedRepeated.class).redact(message)).isEqualTo(expected);
  }

  @Test public void requiredRedactedFieldThrowsRedacting() {
    MessageAdapter<RedactedRequired> adapter = wire.adapter(RedactedRequired.class);
    try {
      adapter.redact(new RedactedRequired("a"));
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage(
          "Field com.squareup.wire.protos.redacted.RedactedRequired.a is REQUIRED and cannot be redacted.");
    }
  }

  @Test public void requiredRedactedFieldToString() {
    MessageAdapter<RedactedRequired> adapter = wire.adapter(RedactedRequired.class);
    assertThat(adapter.toString(new RedactedRequired("a"))).isEqualTo("RedactedRequired{a=██}");
  }
}
