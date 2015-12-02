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
package com.squareup.wire.schema;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.Message;
import com.squareup.wire.schema.internal.parser.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ TypeElement.class, MessageElement.class, EnumElement.class })
public final class TypeTest {

  protected ImmutableList<EnumConstantElement> enumConstants;
  protected ImmutableList<OptionElement> enumOptions;

  protected void mockEnumConstants() {
    enumConstants = ImmutableList.<EnumConstantElement>of(
      mockEnumConstant("FIRST"),
      mockEnumConstant("SECOND"),
      mockEnumConstant("THIRD")
    );
  }

  protected void mockEnumOptions() {
    enumOptions = ImmutableList.<OptionElement>of(
      OptionElement.create("timeout", OptionElement.Kind.NUMBER, "30", true),
      OptionElement.create("message", OptionElement.Kind.STRING, "test string"),
      OptionElement.create("processed", OptionElement.Kind.BOOLEAN, "true", true)
    );
  }

  protected EnumConstantElement mockEnumConstant(final String name) {
    EnumConstantElement mock = mock(EnumConstantElement.class);

    when(mock.name()).thenReturn(name);
    when(mock.options()).thenReturn(ImmutableList.<OptionElement>of());

    return mock;
  }

  @Before public void init() {
    mockEnumConstants();
    mockEnumOptions();
  }

  @Test public void getEnumType() throws Exception {

    EnumElement enumElement = mock(EnumElement.class);
    when(enumElement.constants()).thenReturn(enumConstants);
    when(enumElement.options()).thenReturn(enumOptions);

    Type enumType = Type.get("", ProtoType.get("Person"), enumElement);

    assertTrue(enumType instanceof EnumType);
    assertEquals(((EnumType)enumType).constants().size(), enumElement.constants().size());
    assertEquals(((EnumType)enumType).constants().get(0).name(), enumElement.constants().get(0).name());
    assertEquals(((EnumType) enumType).constants().get(0).tag(), enumElement.constants().get(0).tag());
    assertEquals(((EnumType)enumType).constants().get(1).name(), enumElement.constants().get(1).name());
    assertEquals(((EnumType)enumType).constants().get(1).tag(), enumElement.constants().get(1).tag());
    assertEquals(((EnumType)enumType).constants().get(2).name(), enumElement.constants().get(2).name());
    assertEquals(((EnumType)enumType).constants().get(2).tag(), enumElement.constants().get(2).tag());
    assertTrue(enumType.options().optionMatches("timeout", "30"));
    assertTrue(enumType.options().optionMatches("message", "test string"));
    assertTrue(enumType.options().optionMatches("processed", "true"));
  }

  @Test(expected = IllegalArgumentException.class) public void getIllegalType() throws Exception {
    TypeElement typeElement = mock(TypeElement.class);
    Type.get("", ProtoType.get("Person"), typeElement);

    fail("Type should throw IllegalArgumentException when TypeElement argument is not instance of EnumElement or MessageElement");
  }

}
