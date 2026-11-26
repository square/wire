/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.squareup.wire.protos.redacted.NotRedacted;
import com.squareup.wire.protos.redacted.RedactedChild;
import com.squareup.wire.protos.redacted.RedactedCycleA;
import com.squareup.wire.protos.redacted.RedactedExtension;
import com.squareup.wire.protos.redacted.RedactedFields;
import com.squareup.wire.protos.redacted.RedactedMap;
import com.squareup.wire.protos.redacted.RedactedOneOf;
import com.squareup.wire.protos.redacted.RedactedRepeated;
import com.squareup.wire.protos.redacted.RedactedRequired;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;

public class RuntimeMessageAdapterRedactTest {
  @Test
  public void string() throws IOException {
    RedactedFields redacted = new RedactedFields.Builder().a("a").b("b").c("c").build();
    assertThat(redacted.toString()).isEqualTo("RedactedFields{a=██, b=b, c=c}");

    RedactedRepeated redactedRepeated =
        new RedactedRepeated.Builder()
            .a(Arrays.asList("a", "b"))
            .b(
                Arrays.asList(
                    new RedactedFields("a", "b", "c", null),
                    new RedactedFields("d", "e", "f", null)))
            .build();
    assertThat(redactedRepeated.toString())
        .isEqualTo(
            "RedactedRepeated{a=██, b=[RedactedFields{a=██, b=b, c=c},"
                + " RedactedFields{a=██, b=e, c=f}]}");
  }

  @Test
  public void message() {
    RedactedFields message = new RedactedFields.Builder().a("a").b("b").c("c").build();
    RedactedFields expected = message.newBuilder().a(null).build();
    assertThat(RedactedFields.ADAPTER.redact(message)).isEqualTo(expected);
  }

  @Test
  public void messageWithNoRedactions() {
    NotRedacted message = new NotRedacted.Builder().a("a").b("b").build();
    assertThat(NotRedacted.ADAPTER.redact(message)).isEqualTo(message);
  }

  @Test
  public void nestedRedactions() {
    RedactedChild message =
        new RedactedChild.Builder()
            .a("a")
            .b(new RedactedFields.Builder().a("a").b("b").c("c").build())
            .c(new NotRedacted.Builder().a("a").b("b").build())
            .build();
    RedactedChild expected = message.newBuilder().b(message.b.newBuilder().a(null).build()).build();
    assertThat(RedactedChild.ADAPTER.redact(message)).isEqualTo(expected);
  }

  @Test
  public void redactedExtensions() {
    RedactedFields message =
        new RedactedFields.Builder()
            .extension(new RedactedExtension.Builder().d("d").e("e").build())
            .build();
    RedactedFields expected =
        new RedactedFields.Builder()
            .extension(new RedactedExtension.Builder().e("e").build())
            .build();
    assertThat(RedactedFields.ADAPTER.redact(message)).isEqualTo(expected);
  }

  @Test
  public void messageCycle() {
    RedactedCycleA message = new RedactedCycleA.Builder().build();
    assertThat(RedactedCycleA.ADAPTER.redact(message)).isEqualTo(message);
  }

  @Test
  public void repeatedField() {
    RedactedRepeated message =
        new RedactedRepeated.Builder()
            .a(Arrays.asList("a", "b"))
            .b(
                Arrays.asList(
                    new RedactedFields("a", "b", "c", null),
                    new RedactedFields("d", "e", "f", null)))
            .build();
    RedactedRepeated expected =
        new RedactedRepeated.Builder()
            .b(
                Arrays.asList(
                    new RedactedFields(null, "b", "c", null),
                    new RedactedFields(null, "e", "f", null)))
            .build();
    RedactedRepeated actual = RedactedRepeated.ADAPTER.redact(message);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void requiredRedactedFieldThrowsRedacting() {
    ProtoAdapter<RedactedRequired> adapter = RedactedRequired.ADAPTER;
    try {
      adapter.redact(new RedactedRequired("a"));
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessageThat().contains("Field 'a' is required and cannot be redacted.");
    }
  }

  @Test
  public void requiredRedactedFieldToString() {
    ProtoAdapter<RedactedRequired> adapter = RedactedRequired.ADAPTER;
    assertThat(adapter.toString(new RedactedRequired("a"))).isEqualTo("RedactedRequired{a=██}");
  }

  @Test
  public void reflectiveMessage() {
    ProtoAdapter<RedactedFields> adapter = ProtoAdapter.newMessageAdapter(RedactedFields.class);
    RedactedFields message = new RedactedFields.Builder().a("a").b("b").c("c").build();
    assertThat(adapter.redact(message)).isEqualTo(message.newBuilder().a(null).build());
  }

  @Test
  public void reflectiveMessageWithNoRedactions() {
    ProtoAdapter<NotRedacted> adapter = ProtoAdapter.newMessageAdapter(NotRedacted.class);
    NotRedacted message = new NotRedacted.Builder().a("a").b("b").build();
    assertThat(adapter.redact(message)).isEqualTo(message);
  }

  @Test
  public void reflectiveDropsUnknownFields() {
    ProtoAdapter<NotRedacted> adapter = ProtoAdapter.newMessageAdapter(NotRedacted.class);
    NotRedacted message =
        new NotRedacted.Builder().a("a").addUnknownField(9, FieldEncoding.VARINT, 1L).build();
    assertThat(adapter.redact(message)).isEqualTo(new NotRedacted.Builder().a("a").build());
  }

  @Test
  public void reflectiveNestedRedactions() {
    ProtoAdapter<RedactedChild> adapter = ProtoAdapter.newMessageAdapter(RedactedChild.class);
    RedactedChild message =
        new RedactedChild.Builder()
            .a("a")
            .b(new RedactedFields.Builder().a("a").b("b").c("c").build())
            .c(new NotRedacted.Builder().a("a").b("b").build())
            .build();
    RedactedChild expected = message.newBuilder().b(message.b.newBuilder().a(null).build()).build();
    assertThat(adapter.redact(message)).isEqualTo(expected);
  }

  @Test
  public void reflectiveRedactedExtensions() {
    ProtoAdapter<RedactedFields> adapter = ProtoAdapter.newMessageAdapter(RedactedFields.class);
    RedactedFields message =
        new RedactedFields.Builder()
            .extension(new RedactedExtension.Builder().d("d").e("e").build())
            .build();
    RedactedFields expected =
        new RedactedFields.Builder()
            .extension(new RedactedExtension.Builder().e("e").build())
            .build();
    assertThat(adapter.redact(message)).isEqualTo(expected);
  }

  @Test
  public void reflectiveRepeatedField() {
    ProtoAdapter<RedactedRepeated> adapter = ProtoAdapter.newMessageAdapter(RedactedRepeated.class);
    RedactedRepeated message =
        new RedactedRepeated.Builder()
            .a(Arrays.asList("a", "b"))
            .b(
                Arrays.asList(
                    new RedactedFields("a", "b", "c", null),
                    new RedactedFields("d", "e", "f", null)))
            .build();
    RedactedRepeated expected =
        new RedactedRepeated.Builder()
            .b(
                Arrays.asList(
                    new RedactedFields(null, "b", "c", null),
                    new RedactedFields(null, "e", "f", null)))
            .build();
    assertThat(adapter.redact(message)).isEqualTo(expected);
    // The original message is unchanged.
    assertThat(message.b.get(0).a).isEqualTo("a");
  }

  @Test
  public void reflectiveMapField() {
    ProtoAdapter<RedactedMap> adapter = ProtoAdapter.newMessageAdapter(RedactedMap.class);
    RedactedMap message =
        new RedactedMap.Builder()
            .a(Collections.singletonMap("k", new RedactedFields("a", "b", "c", null)))
            .build();
    RedactedMap expected =
        new RedactedMap.Builder()
            .a(Collections.singletonMap("k", new RedactedFields(null, "b", "c", null)))
            .build();
    assertThat(adapter.redact(message)).isEqualTo(expected);
    assertThat(message.a.get("k").a).isEqualTo("a");
  }

  @Test
  public void reflectiveOneOf() {
    ProtoAdapter<RedactedOneOf> adapter = ProtoAdapter.newMessageAdapter(RedactedOneOf.class);
    assertThat(adapter.redact(new RedactedOneOf.Builder().a("a").build()))
        .isEqualTo(new RedactedOneOf.Builder().build());
    assertThat(adapter.redact(new RedactedOneOf.Builder().c("c").build()))
        .isEqualTo(new RedactedOneOf.Builder().c("c").build());
    assertThat(
            adapter.redact(
                new RedactedOneOf.Builder().b(new RedactedFields("a", "b", "c", null)).build()))
        .isEqualTo(new RedactedOneOf.Builder().b(new RedactedFields(null, "b", "c", null)).build());
  }

  @Test
  public void generatedOneOfChangesOnlyTheMemberThatIsSet() {
    ProtoAdapter<RedactedOneOf> reflective = ProtoAdapter.newMessageAdapter(RedactedOneOf.class);
    RedactedOneOf[] messages = {
      new RedactedOneOf.Builder().a("a").build(),
      new RedactedOneOf.Builder().b(new RedactedFields("a", "b", "c", null)).build(),
      new RedactedOneOf.Builder().c("c").build(),
      new RedactedOneOf.Builder().build(),
    };
    RedactedOneOf[] expected = {
      new RedactedOneOf.Builder().build(),
      new RedactedOneOf.Builder().b(new RedactedFields(null, "b", "c", null)).build(),
      new RedactedOneOf.Builder().c("c").build(),
      new RedactedOneOf.Builder().build(),
    };
    for (int i = 0; i < messages.length; i++) {
      assertThat(RedactedOneOf.ADAPTER.redact(messages[i])).isEqualTo(expected[i]);
      assertThat(reflective.redact(messages[i])).isEqualTo(expected[i]);
    }
  }

  @Test
  public void reflectiveRequiredRedactedFieldThrowsRedacting() {
    ProtoAdapter<RedactedRequired> adapter = ProtoAdapter.newMessageAdapter(RedactedRequired.class);
    try {
      adapter.redact(new RedactedRequired("a"));
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessageThat().contains("Field 'a' in ");
      assertThat(e).hasMessageThat().contains("is required and cannot be redacted.");
    }
  }
}
