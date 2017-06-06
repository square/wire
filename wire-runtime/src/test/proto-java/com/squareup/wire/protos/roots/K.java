// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: roots.proto
package com.squareup.wire.protos.roots;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import okio.ByteString;

public final class K extends Message<K, K.Builder> {
  public static final ProtoAdapter<K> ADAPTER = new ProtoAdapter_K();

  private static final long serialVersionUID = 0L;

  public static final Integer DEFAULT_I = 0;

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  public final Integer i;

  public K(Integer i) {
    this(i, ByteString.EMPTY);
  }

  public K(Integer i, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.i = i;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.i = i;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof K)) return false;
    K o = (K) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(i, o.i);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (i != null ? i.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (i != null) builder.append(", i=").append(i);
    return builder.replace(0, 2, "K{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<K, Builder> {
    public Integer i;

    public Builder() {
    }

    public Builder(boolean useDefaults) {
      this();
      if (useDefaults) {
        i = DEFAULT_I;
      }
    }

    public Builder i(Integer i) {
      this.i = i;
      return this;
    }

    @Override
    public K build() {
      return new K(i, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_K extends ProtoAdapter<K> {
    public ProtoAdapter_K() {
      super(FieldEncoding.LENGTH_DELIMITED, K.class);
    }

    @Override
    public int encodedSize(K value) {
      return ProtoAdapter.INT32.encodedSizeWithTag(1, value.i)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, K value) throws IOException {
      ProtoAdapter.INT32.encodeWithTag(writer, 1, value.i);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public K decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.i(ProtoAdapter.INT32.decode(reader)); break;
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
    public K redact(K value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
