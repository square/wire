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
import com.squareup.wire.protos.roots.WiredFieldsMessage;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.powermock.api.mockito.PowerMockito.spy;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FieldBinding.class, WiredFieldsMessage.Builder.class})
public class FieldBindingTest {

  private WiredFieldsMessage message;
  private Field valField;
  private Field listField;
  private Field cField;

  @Before
  public void init() throws Exception {
    message = new WiredFieldsMessage(12, new ArrayList<Integer>(), new C(10));
    valField = message.getClass().getField("val");
    listField = message.getClass().getField("list");
    cField = message.getClass().getField("c");
  }

  @Test public void staticGetBuilderField() throws Exception {
    // when
    Field field = Whitebox.invokeMethod(FieldBinding.class, "getBuilderField", C.Builder.class, "i");

    // then
    assertThat(field.getType()).isEqualTo(Integer.class);
  }

  @Test(expected = AssertionError.class) public void staticGetBuilderFieldThrown() throws Exception {
    // when
    Whitebox.invokeMethod(FieldBinding.class, "getBuilderField", C.Builder.class, "j");

    // then
    fail("FieldBinding should throw AssertionError when tries to retrieve field that is not exist");
  }

  @Test public void staticGetBuilderMethod() throws Exception {
    // when
    Method method = Whitebox.invokeMethod(FieldBinding.class, "getBuilderMethod", C.Builder.class, "i", Integer.class);

    // then
    assertThat(method).isEqualTo(C.Builder.class.getMethod("i", Integer.class));
  }

  @Test public void staticGetBuilderMethodThrown() throws Exception {
    // when
    try {
      Whitebox.invokeMethod(FieldBinding.class, "getBuilderMethod", C.Builder.class, "j", Integer.class);
    }
    catch (AssertionError e) {
      return;
    }

    // then
    fail("FieldBinding should throw AssertionError when tries to retrieve method that is not exist");
  }


  @Test public void constructor() throws Exception {
    // given
    WireField wireField = valField.getAnnotation(WireField.class);

    // when
    FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder> fieldBinding =
      new FieldBinding<>(wireField, valField, WiredFieldsMessage.Builder.class);

    // then
    assertThat(fieldBinding.label).isEqualTo(wireField.label());
    assertThat(fieldBinding.name).isEqualTo("val");
    assertThat(fieldBinding.tag).isEqualTo(wireField.tag());
    assertThat(fieldBinding.adapterString).isEqualTo(wireField.adapter());
    assertThat(fieldBinding.redacted).isEqualTo(wireField.redacted());
    assertThat(Whitebox.getInternalState(fieldBinding, "messageField")).isEqualTo(valField);
    assertThat(Whitebox.getInternalState(fieldBinding, "builderField")).isEqualTo(WiredFieldsMessage.Builder.class.getField("val"));
    assertThat(Whitebox.getInternalState(fieldBinding, "builderMethod")).isEqualTo(WiredFieldsMessage.Builder.class.getMethod("val", Integer.class));
  }

  @Test public void valueList() throws Exception {
    // given
    WireField wireField = listField.getAnnotation(WireField.class);
    FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder> fieldBinding =
      new FieldBinding<>(wireField, listField, WiredFieldsMessage.Builder.class);
    WiredFieldsMessage.Builder builder = new WiredFieldsMessage.Builder();

    // when
    fieldBinding.value(builder, 15);

    // then
    assertThat(builder.val).isNull();
    assertThat(builder.list.get(0)).isEqualTo(15);
    assertThat(message.val).isEqualTo(12);
    assertThat(message.list).isEmpty();
  }

  @Test public void valueVal() throws Exception {
    // given
    WireField wireField = valField.getAnnotation(WireField.class);
    FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder> fieldBinding =
      new FieldBinding<>(wireField, valField, WiredFieldsMessage.Builder.class);
    WiredFieldsMessage.Builder builder = new WiredFieldsMessage.Builder();

    // when
    fieldBinding.value(builder, 20);

    // then
    assertThat(builder.val).isEqualTo(20);
    assertThat(builder.list).isEmpty();
    assertThat(message.val).isEqualTo(12);
    assertThat(message.list).isEmpty();
  }

  @Test public void singleAdapter() throws Exception {
    // given
    WireField wireField = valField.getAnnotation(WireField.class);
    FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder> fieldBinding =
      new FieldBinding<>(wireField, valField, WiredFieldsMessage.Builder.class);

    // when
    ProtoAdapter adapter1 = fieldBinding.singleAdapter();
    ProtoAdapter adapter2 = fieldBinding.singleAdapter();

    // then
    assertThat(adapter1).isEqualTo(ProtoAdapter.INT32);
    assertThat(adapter1).isEqualTo(adapter2);
  }

  @Test public void adapter() throws Exception {
    // given
    WireField wireField = listField.getAnnotation(WireField.class);
    FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder> fieldBinding =
      new FieldBinding<>(wireField, listField, WiredFieldsMessage.Builder.class);

    // when
    ProtoAdapter adapter1 = fieldBinding.adapter();
    ProtoAdapter adapter2 = fieldBinding.adapter();

    // then
    assertThat(adapter1.javaType).isEqualTo(List.class);
    assertThat(Whitebox.getInternalState(adapter1, "fieldEncoding")).isEqualTo(FieldEncoding.VARINT);
    assertThat(adapter1).isEqualTo(adapter2);
  }

  @Test public void get() throws Exception {
    // given
    WireField wireField = valField.getAnnotation(WireField.class);
    FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder> fieldBinding =
      new FieldBinding<>(wireField, valField, WiredFieldsMessage.Builder.class);
    WiredFieldsMessage anotherMessage = new WiredFieldsMessage(57, new ArrayList<Integer>(), new C(20));

    // when
    Object result = fieldBinding.get(anotherMessage);

    // then
    assertThat(result).isEqualTo(57);
    assertThat(message.val).isEqualTo(12);
  }

  @Test public void setOneOf() throws Exception {
    // given
    WireField wireField = cField.getAnnotation(WireField.class);
    FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder> fieldBinding =
      new FieldBinding<>(wireField, cField, WiredFieldsMessage.Builder.class);
    WiredFieldsMessage.Builder builder = spy(new WiredFieldsMessage.Builder().c(new C(55)));
    C newC = new C(15);

    // when
    fieldBinding.set(builder, newC);

    // then
    Mockito.verify(builder).c(newC);
    assertThat(builder.c.i).isEqualTo(15);
    assertThat(message.c.i).isEqualTo(10);
  }

  @Test public void setVal() throws Exception {
    // given
    WireField wireField = valField.getAnnotation(WireField.class);
    FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder> fieldBinding =
      new FieldBinding<>(wireField, valField, WiredFieldsMessage.Builder.class);
    WiredFieldsMessage.Builder builder = spy(new WiredFieldsMessage.Builder().val(88));

    // when
    fieldBinding.set(builder, 33);

    // then
    Mockito.verify(builder, Mockito.never()).val(33);   // since we need to check that OneOf and other labels are processed in different ways
    assertThat(builder.val).isEqualTo(33);
    assertThat(message.val).isEqualTo(12);
  }

  @Test public void getFromBuilder() throws Exception {
    // given
    WireField wireField = valField.getAnnotation(WireField.class);
    FieldBinding<WiredFieldsMessage, WiredFieldsMessage.Builder> fieldBinding =
      new FieldBinding<>(wireField, valField, WiredFieldsMessage.Builder.class);
    WiredFieldsMessage.Builder builder = new WiredFieldsMessage.Builder().val(41);

    // when
    Object result = fieldBinding.getFromBuilder(builder);

    // then
    assertThat(result).isEqualTo(41);
    assertThat(message.val).isEqualTo(12);
  }

}
