// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: google/protobuf/descriptor.proto
package com.google.protobuf;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.Syntax;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.io.IOException;
import java.lang.Boolean;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import okio.ByteString;

public final class EnumValueOptions extends Message<EnumValueOptions, EnumValueOptions.Builder> {
  public static final ProtoAdapter<EnumValueOptions> ADAPTER = new ProtoAdapter_EnumValueOptions();

  private static final long serialVersionUID = 0L;

  public static final Boolean DEFAULT_DEPRECATED = false;

  /**
   * Is this enum value deprecated?
   * Depending on the target platform, this can emit Deprecated annotations
   * for the enum value, or it will be completely ignored; in the very least,
   * this is a formalization for deprecating enum values.
   */
  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#BOOL"
  )
  public final Boolean deprecated;

  public EnumValueOptions(Boolean deprecated) {
    this(deprecated, ByteString.EMPTY);
  }

  public EnumValueOptions(Boolean deprecated, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.deprecated = deprecated;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.deprecated = deprecated;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof EnumValueOptions)) return false;
    EnumValueOptions o = (EnumValueOptions) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(deprecated, o.deprecated);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (deprecated != null ? deprecated.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (deprecated != null) builder.append(", deprecated=").append(deprecated);
    return builder.replace(0, 2, "EnumValueOptions{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<EnumValueOptions, Builder> {
    public Boolean deprecated;

    public Builder() {
    }

    /**
     * Is this enum value deprecated?
     * Depending on the target platform, this can emit Deprecated annotations
     * for the enum value, or it will be completely ignored; in the very least,
     * this is a formalization for deprecating enum values.
     */
    public Builder deprecated(Boolean deprecated) {
      this.deprecated = deprecated;
      return this;
    }

    @Override
    public EnumValueOptions build() {
      return new EnumValueOptions(deprecated, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_EnumValueOptions extends ProtoAdapter<EnumValueOptions> {
    public ProtoAdapter_EnumValueOptions() {
      super(FieldEncoding.LENGTH_DELIMITED, EnumValueOptions.class, "type.googleapis.com/google.protobuf.EnumValueOptions", Syntax.PROTO_2);
    }

    @Override
    public int encodedSize(EnumValueOptions value) {
      return ProtoAdapter.BOOL.encodedSizeWithTag(1, value.deprecated)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, EnumValueOptions value) throws IOException {
      ProtoAdapter.BOOL.encodeWithTag(writer, 1, value.deprecated);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public EnumValueOptions decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.deprecated(ProtoAdapter.BOOL.decode(reader)); break;
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public EnumValueOptions redact(EnumValueOptions value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
