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

import com.squareup.wire.protos.redacted.*;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class RedactedTest {

  @Test
  public void testRedacted() throws IOException {
    assertEquals("Redacted{a=██, b=b, c=c}",
        new Redacted.Builder().a("a").b("b").c("c").build().toString());
  }

  @Test
  public void testRedactor() {
    Redacted message = new Redacted.Builder().a("a").b("b").c("c").build();
    Redacted expected = new Redacted.Builder(message).a(null).build();
    assertEquals(expected, Redactor.get(Redacted.class).redact(message));
  }

  @Test
  public void testRedactorNoRedactions() {
    NotRedacted message = new NotRedacted.Builder().a("a").b("b").build();
    assertEquals(message, Redactor.get(NotRedacted.class).redact(message));
  }

  @Test
  public void testRedactorRecursive() {
    RedactedChild message = new RedactedChild.Builder()
        .a("a")
        .b(new Redacted.Builder().a("a").b("b").c("c").build())
        .c(new NotRedacted.Builder().a("a").b("b").build())
        .build();
    RedactedChild expected = new RedactedChild.Builder(message)
        .b(new Redacted.Builder(message.b).a(null).build())
        .build();
    assertEquals(expected, Redactor.get(RedactedChild.class).redact(message));
  }

  @Test
  public void testRedactorCycle() {
    RedactedCycleA message = new RedactedCycleA.Builder().build();
    assertEquals(message, Redactor.get(RedactedCycleA.class).redact(message));
  }

  @Test
  public void testRedactorRepeated() {
    RedactedRepeated message = new RedactedRepeated.Builder().a(Arrays.asList("a", "b")).build();
    RedactedRepeated expected =
        new RedactedRepeated.Builder(message).a(Collections.<String>emptyList()).build();
    assertEquals(expected, Redactor.get(RedactedRepeated.class).redact(message));
  }

  @Test
  public void testRedactorRequired() {
    try {
      Redactor.get(RedactedRequired.class);
      fail("Expected an exception to be thrown.");
    } catch (IllegalArgumentException expected) {
    }
  }
}
