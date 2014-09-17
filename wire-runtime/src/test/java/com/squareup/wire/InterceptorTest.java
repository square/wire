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

import com.google.protobuf.FieldOptions;
import com.google.protobuf.MessageOptions;
import com.squareup.wire.protos.interceptors.Encrypted;
import com.squareup.wire.protos.interceptors.EncryptedField;
import com.squareup.wire.protos.interceptors.Ext_interceptors;
import com.squareup.wire.protos.interceptors.Outer;
import com.squareup.wire.protos.interceptors.Plaintext;
import com.squareup.wire.protos.interceptors.Unencrypted;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class InterceptorTest {

  private static class MessageOptionInterceptorFactory implements InterceptorFactory {

    @Override public Interceptor interceptorFor(Class<? extends Message> messageType,
        Collection<Extension<?, ?>> extensions) {
      if (shouldHandle(messageType)) {
        return new EncryptedInterceptor();
      } else {
        return null;
      }
    }

    protected boolean shouldHandle(Class<? extends Message> messageClass) {
      try {
        Field messageOptionsField = messageClass.getField("MESSAGE_OPTIONS");
        MessageOptions options = (MessageOptions) messageOptionsField.get(null);
        return options.getExtension(Ext_interceptors.encrypt_payload) == Boolean.TRUE;
      } catch (NoSuchFieldException e) {
        // This is O.K., the message has no options.
      } catch (IllegalAccessException e) {
        throw new AssertionError("Field MESSAGE_OPTIONS must be accessible");
      }
      return false;
    }
  }

  private static class FieldOptionInterceptorFactory implements InterceptorFactory {

    @Override public Interceptor interceptorFor(Class<? extends Message> messageType,
        Collection<Extension<?, ?>> extensions) {
      if (shouldHandle(messageType)) {
        return new EncryptedFieldInterceptor();
      } else {
        return null;
      }
    }

    protected boolean shouldHandle(Class<? extends Message> messageClass) {
      try {
        Field fieldOptionsField = messageClass.getField("FIELD_OPTIONS_PAYLOAD");
        FieldOptions options = (FieldOptions) fieldOptionsField.get(null);
        return options.getExtension(Ext_interceptors.encrypted) == Boolean.TRUE;
      } catch (NoSuchFieldException e) {
        // This is O.K., the message has no payload field options.
      } catch (IllegalAccessException e) {
        throw new AssertionError("Field FIELD_OPTIONS_PAYLOAD must be accessible");
      }
      return false;
    }
  }

  private static abstract class BaseInterceptor implements Interceptor {

    final int shift;

    BaseInterceptor(int shift) {
      this.shift = shift;
    }

    @Override public <M extends Message> M preSerialize(M source) {
      return handle(source, shift);
    }

    @Override public <M extends Message> M postDeserialize(M source) {
      return handle(source, -shift);
    }

    protected abstract <M extends Message> M handle(M source, int shift);

    // Reversible encrypt/decrypt.
    String encryptOrDecrypt(String payload, int shift) {
      char[] c = payload.toCharArray();
      for (int i = 0; i < c.length; i++) {
        c[i] = shift(c[i], shift);
      }
      return new String(c);
    }

    char shift(char c, int shift) {
      if (c >= 'a' && c <= 'z') {
        return (char) ('a' + ((c - 'a' + shift + 26) % 26));
      } else if (c >= 'A' && c <= 'Z') {
        return (char) ('A' + ((c - 'A' + shift + 26) % 26));
      } else {
        return c;
      }
    }
  }

  private static class EncryptedInterceptor extends BaseInterceptor {

    EncryptedInterceptor() {
      super(1);
    }

    @SuppressWarnings("unchecked")
    protected <M extends Message> M handle(M message, int shift) {
      Encrypted source = (Encrypted) message;
      return (M) new Encrypted.Builder(source).payload(encryptOrDecrypt(source.payload, shift))
          .build();
    }
  }

  private static class EncryptedFieldInterceptor extends BaseInterceptor {

    EncryptedFieldInterceptor() {
      super(2);
    }

    @SuppressWarnings("unchecked")
    protected <M extends Message> M handle(M message, int shift) {
      EncryptedField source = (EncryptedField) message;
      return (M) new EncryptedField.Builder(source).payload(encryptOrDecrypt(source.payload, shift))
          .build();
    }
  }

  private static final int[] expectedBytes = {
      // Plaintext payload is not shifted.
      10, 16, 10, 5, 'N', 'a', 'm', 'e', '1', 18, 7, 'P', 'a', 'y', 'l', 'o', 'a', 'd',
      // Unencrypted payload is not shifted.
      18, 16, 10, 5, 'N', 'a', 'm', 'e', '2', 18, 7, 'P', 'a', 'y', 'l', 'o', 'a', 'd',
      // Encrypted payload is shifted by 1.
      26, 16, 10, 5, 'N', 'a', 'm', 'e', '3', 18, 7, 'Q', 'b', 'z', 'm', 'p', 'b', 'e',
      26, 16, 10, 5, 'N', 'a', 'm', 'e', '4', 18, 7, 'Q', 'b', 'z', 'm', 'p', 'b', 'e',
      // EncryptedField payload is shifted by 2.
      34, 16, 10, 5, 'N', 'a', 'm', 'e', '5', 18, 7, 'R', 'c', 'a', 'n', 'q', 'c', 'f'
  };

  @Test public void testInterceptor() {
    List<Class<?>> extensionClasses = Collections.<Class<?>>singletonList(Ext_interceptors.class);
    List<InterceptorFactory> interceptors = new ArrayList<InterceptorFactory>();
    interceptors.add(new MessageOptionInterceptorFactory());
    interceptors.add(new FieldOptionInterceptorFactory());

    Wire wire = new Wire(extensionClasses, interceptors);

    Plaintext plaintext = new Plaintext.Builder().name("Name1").payload("Payload").build();
    Unencrypted unencrypted = new Unencrypted.Builder().name("Name2").payload("Payload").build();
    Encrypted encrypted1 = new Encrypted.Builder().name("Name3").payload("Payload").build();
    Encrypted encrypted2 = new Encrypted.Builder().name("Name4").payload("Payload").build();
    EncryptedField encryptedField =
        new EncryptedField.Builder().name("Name5").payload("Payload").build();

    Outer outer = new Outer.Builder()
        .plaintext(plaintext)
        .unencrypted(unencrypted)
        .encrypted(Arrays.asList(encrypted1, encrypted2))
        .encrypted_field(encryptedField)
        .build();

    byte[] outerBytes = wire.toByteArray(outer);

    for (int i = 0; i < expectedBytes.length; i++) {
      assertEquals(expectedBytes[i], outerBytes[i]);
    }

    try {
      Outer outerParsed = wire.parseFrom(outerBytes, Outer.class);

      assertEquals(plaintext, outerParsed.plaintext);
      assertEquals(unencrypted, outerParsed.unencrypted);
      assertEquals(2, outerParsed.encrypted.size());
      assertEquals(encrypted1, outerParsed.encrypted.get(0));
      assertEquals(encrypted2, outerParsed.encrypted.get(1));
      assertEquals(encryptedField, outerParsed.encrypted_field);
    } catch (IOException e) {
      throw new AssertionError("impossible");
    }
  }
}
