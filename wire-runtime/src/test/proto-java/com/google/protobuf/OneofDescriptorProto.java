// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: google/protobuf/descriptor.proto at 197:1
package com.google.protobuf;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import okio.ByteString;

/**
 * Describes a oneof.
 */
public final class OneofDescriptorProto extends Message<OneofDescriptorProto, OneofDescriptorProto.Builder> {
  public static final ProtoAdapter<OneofDescriptorProto> ADAPTER = new ProtoAdapter_OneofDescriptorProto();

  private static final long serialVersionUID = 0L;

  public static final String DEFAULT_NAME = "";

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String name;

  public OneofDescriptorProto(String name) {
    this(name, ByteString.EMPTY);
  }

  public OneofDescriptorProto(String name, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.name = name;
  }

  /**
   * Used for deserialization.
   */
  private OneofDescriptorProto() {
    this(null, ByteString.EMPTY);
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.name = name;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof OneofDescriptorProto)) return false;
    OneofDescriptorProto o = (OneofDescriptorProto) other;
    return Internal.equals(unknownFields(), o.unknownFields())
        && Internal.equals(name, o.name);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (name != null ? name.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (name != null) builder.append(", name=").append(name);
    return builder.replace(0, 2, "OneofDescriptorProto{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<OneofDescriptorProto, Builder> {
    public String name;

    public Builder() {
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    @Override
    public OneofDescriptorProto build() {
      return new OneofDescriptorProto(name, buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_OneofDescriptorProto extends ProtoAdapter<OneofDescriptorProto> {
    ProtoAdapter_OneofDescriptorProto() {
      super(FieldEncoding.LENGTH_DELIMITED, OneofDescriptorProto.class);
    }

    @Override
    public int encodedSize(OneofDescriptorProto value) {
      return (value.name != null ? ProtoAdapter.STRING.encodedSizeWithTag(1, value.name) : 0)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, OneofDescriptorProto value) throws IOException {
      if (value.name != null) ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public OneofDescriptorProto decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.name(ProtoAdapter.STRING.decode(reader)); break;
          default: {
            FieldEncoding fieldEncoding = reader.peekFieldEncoding();
            Object value = fieldEncoding.rawProtoAdapter().decode(reader);
            builder.addUnknownField(tag, fieldEncoding, value);
          }
        }
      }
      reader.endMessage(token);
      return builder.build();
    }

    @Override
    public OneofDescriptorProto redact(OneofDescriptorProto value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
