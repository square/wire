// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.wire.protos.roots.TheResponse in service_root.proto
package com.squareup.wire.protos.roots;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.ReverseProtoWriter;
import com.squareup.wire.Syntax;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import okio.ByteString;

public final class TheResponse extends Message<TheResponse, TheResponse.Builder> {
  public static final ProtoAdapter<TheResponse> ADAPTER = new ProtoAdapter_TheResponse();

  private static final long serialVersionUID = 0L;

  public TheResponse() {
    this(ByteString.EMPTY);
  }

  public TheResponse(ByteString unknownFields) {
    super(ADAPTER, unknownFields);
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof TheResponse)) return false;
    TheResponse o = (TheResponse) other;
    return unknownFields().equals(o.unknownFields());
  }

  @Override
  public int hashCode() {
    return unknownFields().hashCode();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    return builder.replace(0, 2, "TheResponse{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<TheResponse, Builder> {
    public Builder() {
    }

    @Override
    public TheResponse build() {
      return new TheResponse(super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_TheResponse extends ProtoAdapter<TheResponse> {
    public ProtoAdapter_TheResponse() {
      super(FieldEncoding.LENGTH_DELIMITED, TheResponse.class, "type.googleapis.com/squareup.wire.protos.roots.TheResponse", Syntax.PROTO_2, null, "service_root.proto");
    }

    @Override
    public int encodedSize(TheResponse value) {
      int result = 0;
      result += value.unknownFields().size();
      return result;
    }

    @Override
    public void encode(ProtoWriter writer, TheResponse value) throws IOException {
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public void encode(ReverseProtoWriter writer, TheResponse value) throws IOException {
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public TheResponse decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public TheResponse redact(TheResponse value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
