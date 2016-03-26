// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: roots.proto at 65:1
package com.squareup.wire.protos.roots;

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

public final class H extends Message<H, H.Builder> {
  public static final ProtoAdapter<H> ADAPTER = new ProtoAdapter_H();

  private static final long serialVersionUID = 0L;

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.protos.roots.E$F#ADAPTER"
  )
  public final E.F ef;

  public H(E.F ef) {
    this(ef, ByteString.EMPTY);
  }

  public H(E.F ef, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.ef = ef;
  }

  /**
   * Used for deserialization.
   */
  private H() {
    this(null, ByteString.EMPTY);
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.ef = ef;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof H)) return false;
    H o = (H) other;
    return Internal.equals(unknownFields(), o.unknownFields())
        && Internal.equals(ef, o.ef);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (ef != null ? ef.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (ef != null) builder.append(", ef=").append(ef);
    return builder.replace(0, 2, "H{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<H, Builder> {
    public E.F ef;

    public Builder() {
    }

    public Builder ef(E.F ef) {
      this.ef = ef;
      return this;
    }

    @Override
    public H build() {
      return new H(ef, buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_H extends ProtoAdapter<H> {
    ProtoAdapter_H() {
      super(FieldEncoding.LENGTH_DELIMITED, H.class);
    }

    @Override
    public int encodedSize(H value) {
      return (value.ef != null ? E.F.ADAPTER.encodedSizeWithTag(1, value.ef) : 0)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, H value) throws IOException {
      if (value.ef != null) E.F.ADAPTER.encodeWithTag(writer, 1, value.ef);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public H decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.ef(E.F.ADAPTER.decode(reader)); break;
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
    public H redact(H value) {
      Builder builder = value.newBuilder();
      if (builder.ef != null) builder.ef = E.F.ADAPTER.redact(builder.ef);
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
