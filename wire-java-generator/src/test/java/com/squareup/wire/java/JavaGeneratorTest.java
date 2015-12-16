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
package com.squareup.wire.java;

import com.google.common.collect.ImmutableMap;
import com.squareup.javapoet.*;
import com.squareup.wire.WireEnum;
import com.squareup.wire.schema.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import javax.lang.model.element.Modifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.*;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
  JavaGenerator.class,
  Schema.class, ProtoFile.class, MessageType.class,
  EnumType.class, Service.class, ProtoType.class,
  ParameterizedTypeName.class, ImmutableMap.class,
  TypeSpec.class, TypeSpec.Builder.class, MethodSpec.class,
  FieldSpec.class
})
public final class JavaGeneratorTest extends JavaGeneratorTestSupport {

  @Rule
  public final TemporaryFolder tempFolder = new TemporaryFolder();

  @Before
  public void setup() {
    mockEmptySchema();
    mockMessageType();
  }

  @Test
  public void sanitizeJavadocStripsTrailingWhitespace() {
    String input = "The quick brown fox  \nJumps over  \n\t \t\nThe lazy dog  ";
    String expected = "The quick brown fox\nJumps over\n\nThe lazy dog";
    assertThat(JavaGenerator.sanitizeJavadoc(input)).isEqualTo(expected);
  }

  @Test
  public void sanitizeJavadocWrapsSeeLinks() {
    String input = "Google query.\n\n@see http://google.com";
    String expected = "Google query.\n\n@see <a href=\"http://google.com\">http://google.com</a>";
    assertThat(JavaGenerator.sanitizeJavadoc(input)).isEqualTo(expected);
  }

  @Test
  public void sanitizeJavadocStarSlash() {
    String input = "/* comment inside comment. */";
    String expected = "/* comment inside comment. &#42;/";
    assertThat(JavaGenerator.sanitizeJavadoc(input)).isEqualTo(expected);
  }

  @Test
  public void testStaticGet() throws Exception {
    // given
    Schema schema = JavaGeneratorTestUtils.buildSchemaFromResource(tempFolder, "message.proto");

    // when
    JavaGenerator javaGenerator = JavaGenerator.get(schema);

    // then
    assertEquals(Whitebox.getInternalState(javaGenerator, "schema"), schema);
    assertEquals(Whitebox.getInternalState(javaGenerator, "emitAndroid"), false);
    assertEquals(Whitebox.getInternalState(javaGenerator, "emitCompact"), false);

    checkBuiltInTypes(javaGenerator);
    assertNotNull(javaGenerator.typeName(ProtoType.get("javagenerator.test.TestMessage")));
    assertNotNull(javaGenerator.typeName(ProtoType.get("javagenerator.test.TestMessage.InnerMessage")));
    assertNotNull(javaGenerator.typeName(ProtoType.get("javagenerator.test.TestMessage.InnerEnum")));
    assertNotNull(javaGenerator.typeName(ProtoType.get("javagenerator.test.TestService")));
  }

  @Test
  public void testStaticAdapterOf() {
    // given
    spy(ParameterizedTypeName.class);

    // when
    JavaGenerator.adapterOf(TypeName.OBJECT);

    // then
    verifyStatic(Mockito.times(1));
    ParameterizedTypeName.get(JavaGenerator.ADAPTER, TypeName.OBJECT);
  }

  @Test
  public void testStaticBuilderOf() {
    // given
    spy(ParameterizedTypeName.class);

    // when
    JavaGenerator.builderOf(TypeName.OBJECT, ClassName.OBJECT);

    // then
    verifyStatic(Mockito.times(1));
    ParameterizedTypeName.get(JavaGenerator.BUILDER, TypeName.OBJECT, ClassName.OBJECT);
  }


  @Test
  public void testStaticCreatorOf() {
    // given
    spy(ParameterizedTypeName.class);

    // when
    JavaGenerator.creatorOf(TypeName.OBJECT);

    // then
    verifyStatic(Mockito.times(1));
    ParameterizedTypeName.get(JavaGenerator.CREATOR, TypeName.OBJECT);
  }

  @Test
  public void testStaticListOf() {
    // given
    spy(ParameterizedTypeName.class);

    // when
    JavaGenerator.listOf(TypeName.OBJECT);

    // then
    verifyStatic(Mockito.times(1));
    ParameterizedTypeName.get(JavaGenerator.LIST, TypeName.OBJECT);
  }

  @Test
  public void testStaticMessageOf() {
    // given
    spy(ParameterizedTypeName.class);

    // when
    JavaGenerator.messageOf(TypeName.OBJECT, ClassName.OBJECT);

    // then
    verifyStatic(Mockito.times(1));
    ParameterizedTypeName.get(JavaGenerator.MESSAGE, TypeName.OBJECT, ClassName.OBJECT);
  }

  @Test
  public void testWithAndroid() throws Exception {
    basicAndroidTest(true);
  }

  @Test
  public void testWithoutAndroid() throws Exception {
    basicAndroidTest(false);
  }

  @Test
  public void testWithCompact() throws Exception {
    basicCompactTest(true);
  }

  @Test
  public void testWithoutCompact() throws Exception {
    basicCompactTest(false);
  }

  @Test
  public void testSchema() throws Exception {
    //given
    Schema schema = JavaGeneratorTestUtils.buildSchemaFromResource(tempFolder, "enum.proto");
    JavaGenerator javaGenerator = JavaGenerator.get(schema);

    // when
    Schema javaGeneratorSchema = javaGenerator.schema();

    // then
    assertEquals(schema, javaGeneratorSchema);
  }

  @Test
  public void testIsEnum() throws Exception {
    // given
    Schema schema = JavaGeneratorTestUtils.buildSchemaFromResource(tempFolder, "enum.proto");
    ProtoType protoType = ProtoType.get("javagenerator.test.TestEnum");
    JavaGenerator javaGenerator = JavaGenerator.get(schema);

    // when
    boolean isEnum = javaGenerator.isEnum(protoType);

    // then
    assertTrue(isEnum);
  }

  @Test
  public void testIsNotEnum() throws Exception {
    // given
    Schema schema = JavaGeneratorTestUtils.buildSchemaFromResource(tempFolder, "message.proto");
    ProtoType protoType = ProtoType.get("javagenerator.test.TestMessage");
    JavaGenerator javaGenerator = JavaGenerator.get(schema);

    // when
    boolean isNotEnum = !javaGenerator.isEnum(protoType);

    // then
    assertTrue(isNotEnum);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testTypeName() throws Exception {
    // given
    ImmutableMap<ProtoType, TypeName> nameToJavaNameMock = mock(ImmutableMap.class);
    when(nameToJavaNameMock, "get", messageProtoType).thenReturn(TypeName.OBJECT);

    mockStatic(ImmutableMap.class);
    when(ImmutableMap.class, "copyOf", Mockito.anyMapOf(ProtoType.class, TypeName.class)).thenReturn(nameToJavaNameMock);

    JavaGenerator javaGenerator = JavaGenerator.get(emptySchema);

    // when
    TypeName typeName = javaGenerator.typeName(messageProtoType);

    // then
    Mockito.verify(nameToJavaNameMock, Mockito.times(1)).get(messageProtoType);
    assertEquals(typeName, TypeName.OBJECT);
  }

  @Test(expected = IllegalArgumentException.class)
  @SuppressWarnings("unchecked")
  public void testNonExistTypeName() throws Exception {
    // given
    ImmutableMap<ProtoType, TypeName> nameToJavaNameMock = mock(ImmutableMap.class);
    when(nameToJavaNameMock, "get", messageProtoType).thenReturn(null);

    mockStatic(ImmutableMap.class);
    when(ImmutableMap.class, "copyOf", Mockito.anyMapOf(ProtoType.class, TypeName.class)).thenReturn(nameToJavaNameMock);

    JavaGenerator javaGenerator = JavaGenerator.get(emptySchema);

    // when
    javaGenerator.typeName(messageProtoType);

    // then
    fail("An IllegalArgumentException must be thrown as there is no type name for messageProtoType here");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEmptyEnum() throws Exception {
    // given
    Schema schema = JavaGeneratorTestUtils.buildSchema(tempFolder, "emptyEnum.proto",
      "syntax = \"proto2\";\n" +
        "\n" +
        "enum EmptyEnum {\n" +
        "}"
    );

    JavaGenerator javaGenerator = JavaGenerator.get(schema);
    ProtoType protoType = ProtoType.get("EmptyEnum");
    EnumType enumType = (EnumType)schema.getType(protoType);

    // when
    javaGenerator.generateEnum(enumType);

    // then
    fail("An IllegalArgumentException must be thrown as there is no items within enum type");
  }

  @Test
  public void testNotDocumentedEnum() throws Exception {
    // given
    Schema schema = JavaGeneratorTestUtils.buildSchema(tempFolder, "notDocumentedEnum.proto",
      "syntax = \"proto2\";\n" +
        "\n" +
        "enum NotDocumentedEnum {\n" +
        "  OPTION = 1;\n" +
        "}"
    );

    JavaGenerator javaGenerator = JavaGenerator.get(schema);
    ProtoType protoType = ProtoType.get("NotDocumentedEnum");
    EnumType enumType = (EnumType)schema.getType(protoType);


    TypeSpec.Builder builderSpy = spy(TypeSpec.enumBuilder("NotDocumentedEnum"));
    spy(TypeSpec.class);
    when(TypeSpec.enumBuilder("NotDocumentedEnum")).thenReturn(builderSpy);

    // when
    TypeSpec generatedEnum = javaGenerator.generateEnum(enumType);

    // then
    assertNotNull(generatedEnum);
    assertEquals(generatedEnum.name, "NotDocumentedEnum");
    assertTrue(generatedEnum.javadoc.isEmpty());
    assertEquals(generatedEnum.enumConstants.size(), 1);
    assertTrue(generatedEnum.enumConstants.containsKey("OPTION"));

    verifyStatic(Mockito.times(1));
    TypeSpec.enumBuilder("NotDocumentedEnum");

    Mockito.verify(builderSpy, Mockito.times(1)).addModifiers(Modifier.PUBLIC);
    Mockito.verify(builderSpy, Mockito.times(1)).addSuperinterface(WireEnum.class);
    Mockito.verify(builderSpy, Mockito.never()).addJavadoc(Mockito.eq("$L\n"), Mockito.any(Object[].class));
    Mockito.verify(builderSpy, Mockito.times(1)).addEnumConstant(Mockito.eq("OPTION"), Mockito.any(TypeSpec.class));
  }

    @Test
    public void testGenerateEnum() throws Exception {
      // given
      Schema schema = JavaGeneratorTestUtils.buildSchemaFromResource(tempFolder, "enum.proto");

      JavaGenerator javaGenerator = JavaGenerator.get(schema);
      ProtoType protoType = ProtoType.get("javagenerator.test.TestEnum");
      EnumType enumType = (EnumType)schema.getType(protoType);

      TypeSpec.Builder builderSpy = spy(TypeSpec.enumBuilder("TestEnum"));
      spy(TypeSpec.class);
      when(TypeSpec.enumBuilder("TestEnum")).thenReturn(builderSpy);

      // when
      TypeSpec generatedEnum = javaGenerator.generateEnum(enumType);

      // then
      assertNotNull(generatedEnum);
      assertEquals(generatedEnum.name, "TestEnum");
      assertEquals(generatedEnum.javadoc.toString(), "Enum example\n");
      assertEquals(generatedEnum.enumConstants.size(), ENUM_FIELDS.length);

      for (String enumField : ENUM_FIELDS) {
        assertTrue(generatedEnum.enumConstants.containsKey(enumField));
      }

      verifyStatic(Mockito.times(1));
      TypeSpec.enumBuilder("TestEnum");

      Mockito.verify(builderSpy, Mockito.times(1)).addModifiers(Modifier.PUBLIC);
      Mockito.verify(builderSpy, Mockito.times(1)).addSuperinterface(WireEnum.class);
      Mockito.verify(builderSpy, Mockito.times(1)).addJavadoc(Mockito.eq("$L\n"), Mockito.eq("Enum example"));

      for (String enumField : ENUM_FIELDS) {
        Mockito.verify(builderSpy, Mockito.times(1)).addEnumConstant(Mockito.eq(enumField), Mockito.any(TypeSpec.class));
      }
    }

  @Test
  @SuppressWarnings("unchecked")
  public void testGenerateEmptyMessage() throws Exception {
    // given
    Schema schema = JavaGeneratorTestUtils.buildSchema(tempFolder, "emptyMessage.proto", "message EmptyMessage {}");

    JavaGenerator javaGenerator = JavaGenerator.get(schema);
    ProtoType protoType = ProtoType.get("EmptyMessage");
    MessageType messageType = (MessageType)schema.getType(protoType);

    TypeSpec.Builder builderSpy = spy(TypeSpec.classBuilder("EmptyMessage"));
    spy(TypeSpec.class);
    when(TypeSpec.classBuilder("EmptyMessage")).thenReturn(builderSpy);

    // when
    TypeSpec generatedMessage = javaGenerator.generateMessage(messageType);

    // then
    assertNotNull(generatedMessage);
    assertEquals(generatedMessage.name, "EmptyMessage");
    assertTrue(generatedMessage.javadoc.isEmpty());
    assertTrue(generatedMessage.typeVariables.isEmpty());
    assertTrue(generatedMessage.enumConstants.isEmpty());

    Mockito.verify(builderSpy, Mockito.times(1)).addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    Mockito.verify(builderSpy, Mockito.never()).addSuperinterface(Mockito.any(TypeName.class));
    Mockito.verify(builderSpy, Mockito.never()).addJavadoc(Mockito.eq("$L\n"), Mockito.any(Object[].class));
  }

  @Test
  public void testGenerateMessage() throws Exception {
    // given
    Schema schema = JavaGeneratorTestUtils.buildSchemaFromResource(tempFolder, "message.proto");
    JavaGenerator javaGenerator = spy(JavaGenerator.get(schema));

    ProtoType protoType = ProtoType.get("javagenerator.test.TestMessage");
    MessageType messageType = (MessageType)schema.getType(protoType);

    TypeSpec.Builder builderSpy = spy(TypeSpec.classBuilder("TestMessage"));
    spy(TypeSpec.class);
    when(TypeSpec.classBuilder("TestMessage")).thenReturn(builderSpy);

    spy(FieldSpec.class);

    // when
    TypeSpec generatedMessage = javaGenerator.generateMessage(messageType);

    // then
    assertNotNull(generatedMessage);
    assertEquals(generatedMessage.name, "TestMessage");
    assertTrue(generatedMessage.javadoc.isEmpty());
    assertTrue(generatedMessage.typeVariables.isEmpty());
    assertTrue(generatedMessage.enumConstants.isEmpty());

    Mockito.verify(builderSpy, Mockito.times(1)).addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    Mockito.verify(builderSpy, Mockito.never()).addJavadoc(Mockito.anyString(), Mockito.anyString());

    // fields verification
    verifyStatic(Mockito.times(2));
    FieldSpec.builder(Mockito.eq(TypeName.LONG), Mockito.eq("serialVersionUID"));

    verifyStatic(Mockito.times(3));
    FieldSpec.builder(Mockito.any(TypeName.class), Mockito.eq("ADAPTER"));

    verifyStatic(Mockito.times(MESSAGE_FIELDS.length + 11));
    FieldSpec.builder(Mockito.any(TypeName.class), Mockito.anyString(), Mockito.eq(Modifier.PUBLIC), Mockito.eq(Modifier.FINAL));
  }

  @Test
  public void testGenerateMessageWithAndroidAndCompact() throws Exception {
    // given
    Schema schema = JavaGeneratorTestUtils.buildSchemaFromResource(tempFolder, "messageAndroidCompact.proto");
    JavaGenerator javaGenerator = spy(JavaGenerator.get(schema).withAndroid(true).withCompact(true));

    ProtoType protoType = ProtoType.get("javagenerator.test.ShortMessage");
    MessageType messageType = (MessageType)schema.getType(protoType);

    TypeSpec.Builder builderSpy = spy(TypeSpec.classBuilder("ShortMessage"));
    spy(TypeSpec.class);
    when(TypeSpec.classBuilder("ShortMessage")).thenReturn(builderSpy);

    MethodSpec writeToParcelMethodSpy = spy(MethodSpec.methodBuilder("writeToParcel")).build();
    MethodSpec describeContentsMethodSpy = spy(MethodSpec.methodBuilder("describeContents")).build();

    MethodSpec.Builder writeToParcelBuilderSpy = spy(MethodSpec.methodBuilder("writeToParcel"));
    MethodSpec.Builder describeContentsBuilderSpy = spy(MethodSpec.methodBuilder("describeContents"));

    spy(MethodSpec.class);
    when(writeToParcelBuilderSpy.build()).thenReturn(writeToParcelMethodSpy);
    when(describeContentsBuilderSpy.build()).thenReturn(describeContentsMethodSpy);

    when(MethodSpec.methodBuilder("writeToParcel")).thenReturn(writeToParcelBuilderSpy);
    when(MethodSpec.methodBuilder("describeContents")).thenReturn(describeContentsBuilderSpy);

    spy(FieldSpec.class);

    // when
    TypeSpec generatedMessage = javaGenerator.generateMessage(messageType);

    // then
    assertNotNull(generatedMessage);
    assertEquals(generatedMessage.name, "ShortMessage");
    assertEquals(generatedMessage.javadoc.toString(), "Short message\n");
    assertTrue(generatedMessage.typeVariables.isEmpty());
    assertTrue(generatedMessage.enumConstants.isEmpty());

    Mockito.verify(builderSpy, Mockito.times(1)).addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    Mockito.verify(builderSpy, Mockito.times(1)).addJavadoc(Mockito.eq("$L\n"), Mockito.eq("Short message"));

    // fields verification
    for (String field : ANDROID_COMPACT_MESSAGE_FIELDS) {
      verifyStatic(Mockito.times(1));
      FieldSpec.builder(Mockito.any(TypeName.class), Mockito.eq(field), Mockito.eq(Modifier.PUBLIC), Mockito.eq(Modifier.FINAL));
    }

    // compact verifications
    PowerMockito.verifyPrivate(javaGenerator, Mockito.times(ANDROID_COMPACT_MESSAGE_FIELDS.length)).invoke("wireFieldAnnotation", Mockito.any(Field.class));

      // android verifications
    Mockito.verify(builderSpy, Mockito.times(1)).addSuperinterface(Mockito.eq(JavaGenerator.PARCELABLE));
    Mockito.verify(builderSpy, Mockito.times(1)).addMethod(Mockito.eq(writeToParcelMethodSpy));
    Mockito.verify(builderSpy, Mockito.times(1)).addMethod(Mockito.eq(describeContentsMethodSpy));
  }

  private void basicAndroidTest(boolean withAndroid) throws Exception {
    //given
    JavaGenerator javaGenerator = JavaGenerator.get(emptySchema);

    // when
    JavaGenerator newJavaGenerator = javaGenerator.withAndroid(withAndroid);

    // then
    assertEquals(Whitebox.getInternalState(javaGenerator, "schema"),
      Whitebox.getInternalState(newJavaGenerator, "schema"));
    assertEquals(Whitebox.getInternalState(newJavaGenerator, "emitAndroid"), withAndroid);
    assertEquals(Whitebox.getInternalState(newJavaGenerator, "emitCompact"), false);
    assertEquals(Whitebox.<ImmutableMap<ProtoType, TypeName>>getInternalState(javaGenerator, "nameToJavaName").size(),
      Whitebox.<ImmutableMap<ProtoType, TypeName>>getInternalState(newJavaGenerator, "nameToJavaName").size());
  }

  private void basicCompactTest(boolean withCompact) throws Exception {
    //given
    JavaGenerator javaGenerator = JavaGenerator.get(emptySchema);

    // when
    JavaGenerator newJavaGenerator = javaGenerator.withCompact(withCompact);

    // then
    assertEquals(Whitebox.getInternalState(javaGenerator, "schema"),
      Whitebox.getInternalState(newJavaGenerator, "schema"));
    assertEquals(Whitebox.getInternalState(newJavaGenerator, "emitAndroid"), false);
    assertEquals(Whitebox.getInternalState(newJavaGenerator, "emitCompact"), withCompact);
    assertEquals(Whitebox.<ImmutableMap<ProtoType, TypeName>>getInternalState(javaGenerator, "nameToJavaName").size(),
      Whitebox.<ImmutableMap<ProtoType, TypeName>>getInternalState(newJavaGenerator, "nameToJavaName").size());
  }

}
