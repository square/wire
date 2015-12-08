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

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;
import com.squareup.wire.schema.MessageType;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.ProtoType;
import com.squareup.wire.schema.Schema;
import okio.ByteString;

import static com.squareup.wire.schema.Options.*;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Basic java generator mocking and validation toolkit
 */
public class JavaGeneratorTestSupport
{
  protected static final String[] ENUM_FIELDS = new String[]{"ONE", "FIRST", "TWO","THREE"};

  protected static final String[] MESSAGE_FIELDS = new String[]{"name", "oldName", "hashedName",
                                                                "firstName", "secondName", "options",
                                                                "tags", "voteResults", "testPacked", "repeatedEnum",
                                                                "innerMessage"};

  protected static final String[] ANDROID_COMPACT_MESSAGE_FIELDS = new String[]{"uid", "label", "hash", "firstName", "secondName", "tags", "innerEnum"};

    protected Schema emptySchema;
    protected MessageType messageType;
    protected ProtoType messageProtoType;

    protected void mockEmptySchema()
    {
        emptySchema = mock(Schema.class);
        when(emptySchema.protoFiles()).thenReturn(ImmutableList.<ProtoFile>of());
    }

    protected void mockMessageType()
    {
        messageType = mock(MessageType.class);

        messageProtoType = mock(ProtoType.class);
        when(messageProtoType.simpleName()).thenReturn(MessageType.class.getSimpleName());
        when(messageType.type()).thenReturn(messageProtoType);
    }

    /**
     * Validate that java generator contains all all built in types
     *
     * @param javaGenerator java generator
     */
    protected void checkBuiltInTypes(JavaGenerator javaGenerator)
    {
        assertEquals(javaGenerator.typeName(ProtoType.BOOL), TypeName.BOOLEAN.box());
        assertEquals(javaGenerator.typeName(ProtoType.BYTES), ClassName.get(ByteString.class));
        assertEquals(javaGenerator.typeName(ProtoType.DOUBLE), TypeName.DOUBLE.box());
        assertEquals(javaGenerator.typeName(ProtoType.FLOAT), TypeName.FLOAT.box());
        assertEquals(javaGenerator.typeName(ProtoType.FIXED32), TypeName.INT.box());
        assertEquals(javaGenerator.typeName(ProtoType.FIXED64), TypeName.LONG.box());
        assertEquals(javaGenerator.typeName(ProtoType.INT32), TypeName.INT.box());
        assertEquals(javaGenerator.typeName(ProtoType.INT64), TypeName.LONG.box());
        assertEquals(javaGenerator.typeName(ProtoType.SFIXED32), TypeName.INT.box());
        assertEquals(javaGenerator.typeName(ProtoType.SFIXED64), TypeName.LONG.box());
        assertEquals(javaGenerator.typeName(ProtoType.SINT32), TypeName.INT.box());
        assertEquals(javaGenerator.typeName(ProtoType.SINT64), TypeName.LONG.box());
        assertEquals(javaGenerator.typeName(ProtoType.STRING), ClassName.get(String.class));
        assertEquals(javaGenerator.typeName(ProtoType.UINT32), TypeName.INT.box());
        assertEquals(javaGenerator.typeName(ProtoType.UINT64), TypeName.LONG.box());
        assertEquals(javaGenerator.typeName(MESSAGE_OPTIONS), ClassName.get("com.google.protobuf", "MessageOptions"));
        assertEquals(javaGenerator.typeName(FIELD_OPTIONS), ClassName.get("com.google.protobuf", "FieldOptions"));
        assertEquals(javaGenerator.typeName(ENUM_OPTIONS), ClassName.get("com.google.protobuf", "EnumOptions"));
    }
}
