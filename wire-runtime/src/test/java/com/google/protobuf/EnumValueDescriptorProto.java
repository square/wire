// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ../wire-runtime/src/test/proto/google/protobuf/descriptor.proto at 178:1
package com.google.protobuf;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.TypeAdapter;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;

/**
 * Describes a value within an enum.
 */
public final class EnumValueDescriptorProto extends Message<EnumValueDescriptorProto> {
  private static final long serialVersionUID = 0L;

  public static final TypeAdapter<EnumValueDescriptorProto> ADAPTER = new TypeAdapter.MessageAdapter<EnumValueDescriptorProto>() {
    @Override
    public EnumValueDescriptorProto read(ProtoReader reader) throws IOException {
      return EnumValueDescriptorProto.read(reader);
    }
  };

  public static final String DEFAULT_NAME = "";

  public static final String DEFAULT_DOC = "";

  public static final Integer DEFAULT_NUMBER = 0;

  public final String name;

  /**
   * Doc string for generated code.
   */
  public final String doc;

  public final Integer number;

  public final EnumValueOptions options;

  public EnumValueDescriptorProto(String name, String doc, Integer number, EnumValueOptions options) {
    super("EnumValueDescriptorProto");
    this.name = name;
    this.doc = doc;
    this.number = number;
    this.options = options;
  }

  private EnumValueDescriptorProto(Builder builder) {
    this(builder.name, builder.doc, builder.number, builder.options);
    setBuilder(builder);
  }

  @Override
  protected void visitFields(Message.Visitor visitor) {
    visitor.value(1, "name", name, TypeAdapter.STRING, false);
    visitor.value(4, "doc", doc, TypeAdapter.STRING, false);
    visitor.value(2, "number", number, TypeAdapter.INT32, false);
    visitor.value(3, "options", options, EnumValueOptions.ADAPTER, false);
    visitor.unknowns(this);
  }

  public static EnumValueDescriptorProto read(ProtoReader reader) throws IOException {
    Builder builder = new Builder();
    while (reader.hasNext()) {
      int tag = reader.nextTag();
      switch (tag) {
        case 1: builder.name = reader.value(TypeAdapter.STRING); break;
        case 4: builder.doc = reader.value(TypeAdapter.STRING); break;
        case 2: builder.number = reader.value(TypeAdapter.INT32); break;
        case 3: builder.options = message(reader, EnumValueOptions.ADAPTER); break;
        default: builder.readUnknown(tag, reader); break;
      }
    }
    return builder.build();
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<EnumValueDescriptorProto> {
    public String name;

    public String doc;

    public Integer number;

    public EnumValueOptions options;

    public Builder() {
    }

    public Builder(EnumValueDescriptorProto message) {
      super(message);
      if (message == null) return;
      this.name = message.name;
      this.doc = message.doc;
      this.number = message.number;
      this.options = message.options;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * Doc string for generated code.
     */
    public Builder doc(String doc) {
      this.doc = doc;
      return this;
    }

    public Builder number(Integer number) {
      this.number = number;
      return this;
    }

    public Builder options(EnumValueOptions options) {
      this.options = options;
      return this;
    }

    @Override
    public EnumValueDescriptorProto build() {
      return new EnumValueDescriptorProto(this);
    }
  }
}
