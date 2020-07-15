// Code generated by Wire protocol buffer compiler, do not edit.
// Source: com.squareup.services.LetsDataRequest in simple_service2.proto
package com.squareup.services;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.Syntax;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import okio.ByteString;

public final class LetsDataRequest extends Message<LetsDataRequest, LetsDataRequest.Builder> {
  public static final ProtoAdapter<LetsDataRequest> ADAPTER = new ProtoAdapter_LetsDataRequest();

  private static final long serialVersionUID = 0L;

  public static final ByteString DEFAULT_DATA = ByteString.EMPTY;

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#BYTES"
  )
  public final ByteString data;

  public LetsDataRequest(ByteString data) {
    this(data, ByteString.EMPTY);
  }

  public LetsDataRequest(ByteString data, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.data = data;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.data = data;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof LetsDataRequest)) return false;
    LetsDataRequest o = (LetsDataRequest) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(data, o.data);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (data != null ? data.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (data != null) builder.append(", data=").append(data);
    return builder.replace(0, 2, "LetsDataRequest{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<LetsDataRequest, Builder> {
    public ByteString data;

    public Builder() {
    }

    public Builder data(ByteString data) {
      this.data = data;
      return this;
    }

    @Override
    public LetsDataRequest build() {
      return new LetsDataRequest(data, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_LetsDataRequest extends ProtoAdapter<LetsDataRequest> {
    public ProtoAdapter_LetsDataRequest() {
      super(FieldEncoding.LENGTH_DELIMITED, LetsDataRequest.class, "type.googleapis.com/com.squareup.services.LetsDataRequest", Syntax.PROTO_2);
    }

    @Override
    public int encodedSize(LetsDataRequest value) {
      int result = 0;
      result += ProtoAdapter.BYTES.encodedSizeWithTag(1, value.data);
      result += value.unknownFields().size();
      return result;
    }

    @Override
    public void encode(ProtoWriter writer, LetsDataRequest value) throws IOException {
      ProtoAdapter.BYTES.encodeWithTag(writer, 1, value.data);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public LetsDataRequest decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.data(ProtoAdapter.BYTES.decode(reader)); break;
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public LetsDataRequest redact(LetsDataRequest value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
