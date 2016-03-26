// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: redacted_test.proto at 22:1
package com.squareup.wire.protos.redacted;

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

public final class NotRedacted extends Message<NotRedacted, NotRedacted.Builder> {
  public static final ProtoAdapter<NotRedacted> ADAPTER = new ProtoAdapter_NotRedacted();

  private static final long serialVersionUID = 0L;

  public static final String DEFAULT_A = "";

  public static final String DEFAULT_B = "";

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String a;

  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String b;

  public NotRedacted(String a, String b) {
    this(a, b, ByteString.EMPTY);
  }

  public NotRedacted(String a, String b, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.a = a;
    this.b = b;
  }

  /**
   * Used for deserialization.
   */
  private NotRedacted() {
    this(null, null, ByteString.EMPTY);
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.a = a;
    builder.b = b;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof NotRedacted)) return false;
    NotRedacted o = (NotRedacted) other;
    return Internal.equals(unknownFields(), o.unknownFields())
        && Internal.equals(a, o.a)
        && Internal.equals(b, o.b);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (a != null ? a.hashCode() : 0);
      result = result * 37 + (b != null ? b.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (a != null) builder.append(", a=").append(a);
    if (b != null) builder.append(", b=").append(b);
    return builder.replace(0, 2, "NotRedacted{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<NotRedacted, Builder> {
    public String a;

    public String b;

    public Builder() {
    }

    public Builder a(String a) {
      this.a = a;
      return this;
    }

    public Builder b(String b) {
      this.b = b;
      return this;
    }

    @Override
    public NotRedacted build() {
      return new NotRedacted(a, b, buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_NotRedacted extends ProtoAdapter<NotRedacted> {
    ProtoAdapter_NotRedacted() {
      super(FieldEncoding.LENGTH_DELIMITED, NotRedacted.class);
    }

    @Override
    public int encodedSize(NotRedacted value) {
      return (value.a != null ? ProtoAdapter.STRING.encodedSizeWithTag(1, value.a) : 0)
          + (value.b != null ? ProtoAdapter.STRING.encodedSizeWithTag(2, value.b) : 0)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, NotRedacted value) throws IOException {
      if (value.a != null) ProtoAdapter.STRING.encodeWithTag(writer, 1, value.a);
      if (value.b != null) ProtoAdapter.STRING.encodeWithTag(writer, 2, value.b);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public NotRedacted decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.a(ProtoAdapter.STRING.decode(reader)); break;
          case 2: builder.b(ProtoAdapter.STRING.decode(reader)); break;
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
    public NotRedacted redact(NotRedacted value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
