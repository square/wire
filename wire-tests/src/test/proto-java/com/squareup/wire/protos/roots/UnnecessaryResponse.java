// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: service_root.proto
package com.squareup.wire.protos.roots;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import kotlin.jvm.JvmClassMappingKt;
import okio.ByteString;

public final class UnnecessaryResponse extends Message<UnnecessaryResponse, UnnecessaryResponse.Builder> {
  public static final ProtoAdapter<UnnecessaryResponse> ADAPTER = new ProtoAdapter_UnnecessaryResponse();

  private static final long serialVersionUID = 0L;

  public UnnecessaryResponse() {
    this(ByteString.EMPTY);
  }

  public UnnecessaryResponse(ByteString unknownFields) {
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
    return other instanceof UnnecessaryResponse;
  }

  @Override
  public int hashCode() {
    return unknownFields().hashCode();
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    return builder.replace(0, 2, "UnnecessaryResponse{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<UnnecessaryResponse, Builder> {
    public Builder() {
    }

    @Override
    public UnnecessaryResponse build() {
      return new UnnecessaryResponse(super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_UnnecessaryResponse extends ProtoAdapter<UnnecessaryResponse> {
    public ProtoAdapter_UnnecessaryResponse() {
      super(FieldEncoding.LENGTH_DELIMITED, JvmClassMappingKt.getKotlinClass(UnnecessaryResponse.class));
    }

    @Override
    public int encodedSize(UnnecessaryResponse value) {
      return value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, UnnecessaryResponse value) throws IOException {
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public UnnecessaryResponse decode(ProtoReader reader) throws IOException {
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
    public UnnecessaryResponse redact(UnnecessaryResponse value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
