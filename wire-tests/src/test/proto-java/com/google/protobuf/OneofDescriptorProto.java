// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: google/protobuf/descriptor.proto
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
import kotlin.jvm.JvmClassMappingKt;
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

  @WireField(
      tag = 2,
      adapter = "com.google.protobuf.OneofOptions#ADAPTER"
  )
  public final OneofOptions options;

  public OneofDescriptorProto(String name, OneofOptions options) {
    this(name, options, ByteString.EMPTY);
  }

  public OneofDescriptorProto(String name, OneofOptions options, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.name = name;
    this.options = options;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.name = name;
    builder.options = options;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof OneofDescriptorProto)) return false;
    OneofDescriptorProto o = (OneofDescriptorProto) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(name, o.name)
        && Internal.equals(options, o.options);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (name != null ? name.hashCode() : 0);
      result = result * 37 + (options != null ? options.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (name != null) builder.append(", name=").append(name);
    if (options != null) builder.append(", options=").append(options);
    return builder.replace(0, 2, "OneofDescriptorProto{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<OneofDescriptorProto, Builder> {
    public String name;

    public OneofOptions options;

    public Builder() {
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder options(OneofOptions options) {
      this.options = options;
      return this;
    }

    @Override
    public OneofDescriptorProto build() {
      return new OneofDescriptorProto(name, options, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_OneofDescriptorProto extends ProtoAdapter<OneofDescriptorProto> {
    public ProtoAdapter_OneofDescriptorProto() {
      super(FieldEncoding.LENGTH_DELIMITED, JvmClassMappingKt.getKotlinClass(OneofDescriptorProto.class));
    }

    @Override
    public int encodedSize(OneofDescriptorProto value) {
      return ProtoAdapter.STRING.encodedSizeWithTag(1, value.name)
          + OneofOptions.ADAPTER.encodedSizeWithTag(2, value.options)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, OneofDescriptorProto value) throws IOException {
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name);
      OneofOptions.ADAPTER.encodeWithTag(writer, 2, value.options);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public OneofDescriptorProto decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.name(ProtoAdapter.STRING.decode(reader)); break;
          case 2: builder.options(OneofOptions.ADAPTER.decode(reader)); break;
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public OneofDescriptorProto redact(OneofDescriptorProto value) {
      Builder builder = value.newBuilder();
      if (builder.options != null) builder.options = OneofOptions.ADAPTER.redact(builder.options);
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
