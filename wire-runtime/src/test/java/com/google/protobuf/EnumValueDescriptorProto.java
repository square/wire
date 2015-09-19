// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ../wire-runtime/src/test/proto/google/protobuf/descriptor.proto at 178:1
package com.google.protobuf;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.TagMap;
import com.squareup.wire.WireField;
import java.io.ObjectStreamException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;

/**
 * Describes a value within an enum.
 */
public final class EnumValueDescriptorProto extends Message<EnumValueDescriptorProto> {
  public static final ProtoAdapter<EnumValueDescriptorProto> ADAPTER = ProtoAdapter.newMessageAdapter(EnumValueDescriptorProto.class);

  private static final long serialVersionUID = 0L;

  public static final String DEFAULT_NAME = "";

  public static final String DEFAULT_DOC = "";

  public static final Integer DEFAULT_NUMBER = 0;

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String name;

  /**
   * Doc string for generated code.
   */
  @WireField(
      tag = 4,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String doc;

  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  public final Integer number;

  @WireField(
      tag = 3,
      adapter = "com.google.protobuf.EnumValueOptions#ADAPTER"
  )
  public final EnumValueOptions options;

  public EnumValueDescriptorProto(String name, String doc, Integer number, EnumValueOptions options) {
    this(name, doc, number, options, null);
  }

  public EnumValueDescriptorProto(String name, String doc, Integer number, EnumValueOptions options, TagMap tagMap) {
    super(tagMap);
    this.name = name;
    this.doc = doc;
    this.number = number;
    this.options = options;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof EnumValueDescriptorProto)) return false;
    EnumValueDescriptorProto o = (EnumValueDescriptorProto) other;
    return equals(tagMap(), o.tagMap())
        && equals(name, o.name)
        && equals(doc, o.doc)
        && equals(number, o.number)
        && equals(options, o.options);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    if (result == 0) {
      result = tagMap() != null ? tagMap().hashCode() : 0;
      result = result * 37 + (name != null ? name.hashCode() : 0);
      result = result * 37 + (doc != null ? doc.hashCode() : 0);
      result = result * 37 + (number != null ? number.hashCode() : 0);
      result = result * 37 + (options != null ? options.hashCode() : 0);
      hashCode = result;
    }
    return result;
  }

  private Object writeReplace() throws ObjectStreamException {
    return super.createSerializedForm();
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<EnumValueDescriptorProto, Builder> {
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
      return new EnumValueDescriptorProto(name, doc, number, options, buildTagMap());
    }
  }
}
