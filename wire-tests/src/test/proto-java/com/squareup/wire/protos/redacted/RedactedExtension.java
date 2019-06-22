// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: redacted_test.proto
package com.squareup.wire.protos.redacted;

import com.google.protobuf.FieldOptions;
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

public final class RedactedExtension extends Message<RedactedExtension, RedactedExtension.Builder> {
  public static final ProtoAdapter<RedactedExtension> ADAPTER = new ProtoAdapter_RedactedExtension();

  private static final long serialVersionUID = 0L;

  public static final FieldOptions FIELD_OPTIONS_D = new FieldOptions.Builder()
      .redacted(true)
      .build();

  public static final String DEFAULT_D = "";

  public static final String DEFAULT_E = "";

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING",
      redacted = true
  )
  public final String d;

  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String e;

  public RedactedExtension(String d, String e) {
    this(d, e, ByteString.EMPTY);
  }

  public RedactedExtension(String d, String e, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.d = d;
    this.e = e;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.d = d;
    builder.e = e;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof RedactedExtension)) return false;
    RedactedExtension o = (RedactedExtension) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(d, o.d)
        && Internal.equals(e, o.e);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (d != null ? d.hashCode() : 0);
      result = result * 37 + (e != null ? e.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (d != null) builder.append(", d=██");
    if (e != null) builder.append(", e=").append(e);
    return builder.replace(0, 2, "RedactedExtension{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<RedactedExtension, Builder> {
    public String d;

    public String e;

    public Builder() {
    }

    public Builder d(String d) {
      this.d = d;
      return this;
    }

    public Builder e(String e) {
      this.e = e;
      return this;
    }

    @Override
    public RedactedExtension build() {
      return new RedactedExtension(d, e, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_RedactedExtension extends ProtoAdapter<RedactedExtension> {
    public ProtoAdapter_RedactedExtension() {
      super(FieldEncoding.LENGTH_DELIMITED, JvmClassMappingKt.getKotlinClass(RedactedExtension.class));
    }

    @Override
    public int encodedSize(RedactedExtension value) {
      return ProtoAdapter.STRING.encodedSizeWithTag(1, value.d)
          + ProtoAdapter.STRING.encodedSizeWithTag(2, value.e)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, RedactedExtension value) throws IOException {
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.d);
      ProtoAdapter.STRING.encodeWithTag(writer, 2, value.e);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public RedactedExtension decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.d(ProtoAdapter.STRING.decode(reader)); break;
          case 2: builder.e(ProtoAdapter.STRING.decode(reader)); break;
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public RedactedExtension redact(RedactedExtension value) {
      Builder builder = value.newBuilder();
      builder.d = null;
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
