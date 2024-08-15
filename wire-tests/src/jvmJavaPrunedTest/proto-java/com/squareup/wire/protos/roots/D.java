// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.roots.D in roots.proto
package com.squareup.wire.protos.roots;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.ReverseProtoWriter;
import com.squareup.wire.Syntax;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import okio.ByteString;

public final class D extends Message<D, D.Builder> {
  public static final ProtoAdapter<D> ADAPTER = new ProtoAdapter_D();

  private static final long serialVersionUID = 0L;

  public static final Integer DEFAULT_I = 0;

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  public final Integer i;

  public D(Integer i) {
    this(i, ByteString.EMPTY);
  }

  public D(Integer i, ByteString unknownFields) {
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
    if (!(other instanceof D)) return false;
    D o = (D) other;
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
    return builder.replace(0, 2, "D{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<D, Builder> {
    public Integer i;

    public Builder() {
    }

    public Builder i(Integer i) {
      this.i = i;
      return this;
    }

    @Override
    public D build() {
      return new D(i, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_D extends ProtoAdapter<D> {
    public ProtoAdapter_D() {
      super(FieldEncoding.LENGTH_DELIMITED, D.class, "type.googleapis.com/squareup.protos.roots.D", Syntax.PROTO_2.INSTANCE, null, "roots.proto");
    }

    @Override
    public int encodedSize(D value) {
      int result = 0;
      result += ProtoAdapter.INT32.encodedSizeWithTag(1, value.i);
      result += value.unknownFields().size();
      return result;
    }

    @Override
    public void encode(ProtoWriter writer, D value) throws IOException {
      ProtoAdapter.INT32.encodeWithTag(writer, 1, value.i);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public void encode(ReverseProtoWriter writer, D value) throws IOException {
      writer.writeBytes(value.unknownFields());
      ProtoAdapter.INT32.encodeWithTag(writer, 1, value.i);
    }

    @Override
    public D decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.i(ProtoAdapter.INT32.decode(reader)); break;
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public D redact(D value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
