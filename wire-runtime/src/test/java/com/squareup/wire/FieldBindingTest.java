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
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(PowerMockRunner.class)
@PrepareForTest(FieldBinding.class)
public class FieldBindingTest {

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

//  @Test public void staticGetBuilderMethod() throws Exception {
//
//    // todo review
//
//    // when
//    Method method = Whitebox.invokeMethod(FieldBinding.class, "getBuilderMethod", C.Builder.class, "build");
//
//    // then
//    assertThat(method.getReturnType()).isEqualTo(C.class);
//    assertThat(method.getModifiers() | Modifier.PUBLIC).isEqualTo(1);
//  }
//
//  @Test(expected = AssertionError.class) public void staticGetBuilderMethodThrown() throws Exception {
//
//    // todo review
//
//    // when
//    Whitebox.invokeMethod(FieldBinding.class, "getBuilderMethod", C.Builder.class, "construct");
//
//    // then
//    fail("FieldBinding should throw AssertionError when tries to retrieve method that is not exist");
//  }
//
//
//  @Test public void constructor() throws Exception {
//
//  }
}
