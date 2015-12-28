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
import com.squareup.wire.protos.redacted.RedactedExtension;
import com.squareup.wire.protos.redacted.RedactedRepeated;
import com.squareup.wire.protos.redacted.RedactedRequired;
import com.squareup.wire.protos.roots.C;
import com.squareup.wire.protos.roots.RedactFieldsMessage;
import okio.ByteString;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class RuntimeMessageAdapterRedactTest {
  @Test public void string() throws IOException {
    Redacted redacted = new Redacted.Builder().a("a").b("b").c("c").build();
    assertThat(redacted.toString()).isEqualTo("Redacted{a=██, b=b, c=c}");

    RedactedRepeated redactedRepeated = new RedactedRepeated.Builder()
        .a(Arrays.asList("a", "b"))
        .b(Arrays.asList(new Redacted("a", "b", "c", null), new Redacted("d", "e", "f", null)))
        .build();
    assertThat(redactedRepeated.toString()).isEqualTo(
        "RedactedRepeated{a=██, b=[Redacted{a=██, b=b, c=c}, Redacted{a=██, b=e, c=f}]}");
  }

  @Test public void message() {
    Redacted message = new Redacted.Builder().a("a").b("b").c("c").build();
    Redacted expected = message.newBuilder().a(null).build();
    assertThat(Redacted.ADAPTER.redact(message)).isEqualTo(expected);
  }

  @Test public void messageWithNoRedactions() {
    NotRedacted message = new NotRedacted.Builder().a("a").b("b").build();
    assertThat(NotRedacted.ADAPTER.redact(message)).isEqualTo(message);
  }

  @Test public void nestedRedactions() {
    RedactedChild message = new RedactedChild.Builder()
        .a("a")
        .b(new Redacted.Builder().a("a").b("b").c("c").build())
        .c(new NotRedacted.Builder().a("a").b("b").build())
        .build();
    RedactedChild expected = message.newBuilder()
        .b(message.b.newBuilder().a(null).build())
        .build();
    assertThat(RedactedChild.ADAPTER.redact(message)).isEqualTo(expected);
  }

  @Test public void redactedExtensions() {
    Redacted message = new Redacted.Builder()
        .extension(new RedactedExtension.Builder()
            .d("d")
            .e("e")
            .build())
        .build();
    Redacted expected = new Redacted.Builder()
        .extension(new RedactedExtension.Builder()
            .e("e")
            .build())
        .build();
    assertThat(Redacted.ADAPTER.redact(message)).isEqualTo(expected);
  }

  @Test public void messageCycle() {
    RedactedCycleA message = new RedactedCycleA.Builder().build();
    assertThat(RedactedCycleA.ADAPTER.redact(message)).isEqualTo(message);
  }

  @Test public void repeatedField() {
    RedactedRepeated message = new RedactedRepeated.Builder()
        .a(Arrays.asList("a", "b"))
        .b(Arrays.asList(new Redacted("a", "b", "c", null), new Redacted("d", "e", "f", null)))
        .build();
    RedactedRepeated expected = new RedactedRepeated.Builder()
        .b(Arrays.asList(new Redacted(null, "b", "c", null), new Redacted(null, "e", "f", null)))
        .build();
    RedactedRepeated actual = RedactedRepeated.ADAPTER.redact(message);
    assertThat(actual).isEqualTo(expected);
  }

  @Test public void requiredRedactedFieldThrowsRedacting() {
    ProtoAdapter<RedactedRequired> adapter = RedactedRequired.ADAPTER;
    try {
      adapter.redact(new RedactedRequired("a"));
      fail();
    } catch (UnsupportedOperationException e) {
      assertThat(e).hasMessage("Field 'a' is required and cannot be redacted.");
    }
  }

  @Test public void requiredRedactedFieldToString() {
    ProtoAdapter<RedactedRequired> adapter = RedactedRequired.ADAPTER;
    assertThat(adapter.toString(new RedactedRequired("a"))).isEqualTo("RedactedRequired{a=██}");
  }

  @Test(expected = UnsupportedOperationException.class) public void redactRequiredField() throws NoSuchFieldException {
    // given
    RuntimeMessageAdapter adapter = createAdapter("requiredRedacted");

    // when
    adapter.redact(new RedactFieldsMessage(10, 20L, 35, 45, new C(51), new C(61),
      Arrays.asList(new C(3), new C(7), new C(5)), Arrays.asList(new C(2), new C(4))));

    // then
    fail("RuntimeMessageAdapter should throw UnsupportedOperationException when tries to redact required field");
  }

  @Test public void notRedactRequiredField() throws NoSuchFieldException {
    // given
    RuntimeMessageAdapter adapter = createAdapter("requiredNotRedacted");

    // when
    RedactFieldsMessage message = (RedactFieldsMessage)adapter.redact(new RedactFieldsMessage(10, 20L, 35, 45,
      new C(51), new C(61), Arrays.asList(new C(3), new C(7), new C(5)), Arrays.asList(new C(2), new C(4))));

    // then
    assertThat(message.requiredRedacted).isEqualTo(10);
    assertThat(message.requiredNotRedacted).isEqualTo(20L);
    assertThat(message.optionalRedacted).isEqualTo(35);
    assertThat(message.optionalNotRedacted).isEqualTo(45);
    assertThat(message.cRedacted.i).isEqualTo(51);
    assertThat(message.cNotRedacted.i).isEqualTo(61);
    assertThat(message.unknownFields()).isEqualTo(ByteString.EMPTY);
  }

  @Test public void redactOptionalField() throws NoSuchFieldException {
    // given
    RuntimeMessageAdapter adapter = createAdapter("optionalRedacted");

    // when
    RedactFieldsMessage nullValueMessage = (RedactFieldsMessage)adapter.redact(new RedactFieldsMessage(10, 20L, null, 45,
      new C(51), new C(61), Arrays.asList(new C(3), new C(7), new C(5)), Arrays.asList(new C(2), new C(4))));
    RedactFieldsMessage message = (RedactFieldsMessage)adapter.redact(new RedactFieldsMessage(10, 20L, 35, 45,
      new C(51), new C(61), Arrays.asList(new C(3), new C(7), new C(5)), Arrays.asList(new C(2), new C(4))));

    // then
    assertThat(message.requiredRedacted).isEqualTo(10);
    assertThat(message.requiredNotRedacted).isEqualTo(20L);
    assertThat(message.optionalRedacted).isNull();
    assertThat(message.optionalNotRedacted).isEqualTo(45);
    assertThat(message.cRedacted.i).isEqualTo(51);
    assertThat(message.cNotRedacted.i).isEqualTo(61);
    assertThat(message.unknownFields()).isEqualTo(ByteString.EMPTY);

    assertThat(nullValueMessage.requiredRedacted).isEqualTo(10);
    assertThat(nullValueMessage.requiredNotRedacted).isEqualTo(20L);
    assertThat(nullValueMessage.optionalRedacted).isNull();
    assertThat(nullValueMessage.optionalNotRedacted).isEqualTo(45);
    assertThat(nullValueMessage.cRedacted.i).isEqualTo(51);
    assertThat(nullValueMessage.cNotRedacted.i).isEqualTo(61);
    assertThat(nullValueMessage.unknownFields()).isEqualTo(ByteString.EMPTY);
  }

  @Test public void notRedactOptionalField() throws NoSuchFieldException {
    // given
    RuntimeMessageAdapter adapter = createAdapter("optionalNotRedacted");

    // when
    RedactFieldsMessage message = (RedactFieldsMessage)adapter.redact(new RedactFieldsMessage(10, 20L, 35, 45,
      new C(51), new C(61), Arrays.asList(new C(3), new C(7), new C(5)), Arrays.asList(new C(2), new C(4))));

    // then
    assertThat(message.requiredRedacted).isEqualTo(10);
    assertThat(message.requiredNotRedacted).isEqualTo(20L);
    assertThat(message.optionalRedacted).isEqualTo(35);
    assertThat(message.optionalNotRedacted).isEqualTo(45);
    assertThat(message.cRedacted.i).isEqualTo(51);
    assertThat(message.cNotRedacted.i).isEqualTo(61);
    assertThat(message.unknownFields()).isEqualTo(ByteString.EMPTY);
  }

  @Test public void redactMessageField() throws NoSuchFieldException {
    // given
    RuntimeMessageAdapter adapter = createAdapter("cRedacted");

    // when
    RedactFieldsMessage nullValueMessage = (RedactFieldsMessage)adapter.redact(new RedactFieldsMessage(10, 20L, 35, 45,
      null, new C(61), Arrays.asList(new C(3), new C(7), new C(5)), Arrays.asList(new C(2), new C(4))));
    RedactFieldsMessage message = (RedactFieldsMessage)adapter.redact(new RedactFieldsMessage(10, 20L, 35, 45,
      new C(51), new C(61), Arrays.asList(new C(3), new C(7), new C(5)), Arrays.asList(new C(2), new C(4))));

    // then
    assertThat(message.requiredRedacted).isEqualTo(10);
    assertThat(message.requiredNotRedacted).isEqualTo(20L);
    assertThat(message.optionalRedacted).isEqualTo(35);
    assertThat(message.optionalNotRedacted).isEqualTo(45);
    assertThat(message.cRedacted.i).isEqualTo(51);
    assertThat(message.cNotRedacted.i).isEqualTo(61);
    assertThat(message.unknownFields()).isEqualTo(ByteString.EMPTY);

    assertThat(nullValueMessage.requiredRedacted).isEqualTo(10);
    assertThat(nullValueMessage.requiredNotRedacted).isEqualTo(20L);
    assertThat(nullValueMessage.optionalRedacted).isEqualTo(35);
    assertThat(nullValueMessage.optionalNotRedacted).isEqualTo(45);
    assertThat(nullValueMessage.cRedacted).isNull();
    assertThat(nullValueMessage.cNotRedacted.i).isEqualTo(61);
    assertThat(nullValueMessage.unknownFields()).isEqualTo(ByteString.EMPTY);
  }

  @Test public void notRedactMessageField() throws NoSuchFieldException {
    // given
    RuntimeMessageAdapter adapter = createAdapter("cNotRedacted");

    // when
    RedactFieldsMessage message = (RedactFieldsMessage)adapter.redact(new RedactFieldsMessage(10, 20L, 35, 45,
      new C(51), new C(61), Arrays.asList(new C(3), new C(7), new C(5)), Arrays.asList(new C(2), new C(4))));

    // then
    assertThat(message.requiredRedacted).isEqualTo(10);
    assertThat(message.requiredNotRedacted).isEqualTo(20L);
    assertThat(message.optionalRedacted).isEqualTo(35);
    assertThat(message.optionalNotRedacted).isEqualTo(45);
    assertThat(message.cRedacted.i).isEqualTo(51);
    assertThat(message.cNotRedacted.i).isEqualTo(61);
    assertThat(message.unknownFields()).isEqualTo(ByteString.EMPTY);
  }

  @Test public void redactRepeatedMessageField() throws NoSuchFieldException {
    // given
    RuntimeMessageAdapter adapter = createAdapter("cRepeatedRedacted");

    // when
    RedactFieldsMessage message = (RedactFieldsMessage)adapter.redact(new RedactFieldsMessage(10, 20L, 35, 45,
      new C(51), new C(61), Arrays.asList(new C(3), new C(7), new C(5)), Arrays.asList(new C(2), new C(4))));

    // then
    assertThat(message.requiredRedacted).isEqualTo(10);
    assertThat(message.requiredNotRedacted).isEqualTo(20L);
    assertThat(message.optionalRedacted).isEqualTo(35);
    assertThat(message.optionalNotRedacted).isEqualTo(45);
    assertThat(message.cRedacted.i).isEqualTo(51);
    assertThat(message.cNotRedacted.i).isEqualTo(61);
    assertThat(message.unknownFields()).isEqualTo(ByteString.EMPTY);
  }

  @Test public void notRedactRepeatedMessageField() throws NoSuchFieldException {
    // given
    RuntimeMessageAdapter adapter = createAdapter("cRepeatedNotRedacted");

    // when
    RedactFieldsMessage message = (RedactFieldsMessage)adapter.redact(new RedactFieldsMessage(10, 20L, 35, 45,
      new C(51), new C(61), Arrays.asList(new C(3), new C(7), new C(5)), Arrays.asList(new C(2), new C(4))));

    // then
    assertThat(message.requiredRedacted).isEqualTo(10);
    assertThat(message.requiredNotRedacted).isEqualTo(20L);
    assertThat(message.optionalRedacted).isEqualTo(35);
    assertThat(message.optionalNotRedacted).isEqualTo(45);
    assertThat(message.cRedacted.i).isEqualTo(51);
    assertThat(message.cNotRedacted.i).isEqualTo(61);
    assertThat(message.unknownFields()).isEqualTo(ByteString.EMPTY);
  }

  private RuntimeMessageAdapter<RedactFieldsMessage, RedactFieldsMessage.Builder>
  createAdapter(String... fieldNames) throws NoSuchFieldException {

    Map<Integer, FieldBinding<RedactFieldsMessage, RedactFieldsMessage.Builder>> fieldBindings = new HashMap<>();

    for (String fieldName : fieldNames)
    {
      Field valField = RedactFieldsMessage.class.getField(fieldName);
      WireField wireField = valField.getAnnotation(WireField.class);

      fieldBindings.put(wireField.tag(), new FieldBinding<>(wireField, valField, RedactFieldsMessage.Builder.class));
    }

    return new RuntimeMessageAdapter<>(RedactFieldsMessage.class, RedactFieldsMessage.Builder.class, fieldBindings);
  }
}
