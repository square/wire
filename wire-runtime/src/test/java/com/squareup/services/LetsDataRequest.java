// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ../wire-runtime/src/test/proto/rxjava_service2.proto at 13:1
package com.squareup.services;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.TypeAdapter;
import java.io.IOException;
import java.lang.Override;
import okio.ByteString;

public final class LetsDataRequest extends Message<LetsDataRequest> {
  private static final long serialVersionUID = 0L;

  public static final TypeAdapter<LetsDataRequest> ADAPTER = new TypeAdapter.MessageAdapter<LetsDataRequest>() {
    @Override
    public LetsDataRequest read(ProtoReader reader) throws IOException {
      return LetsDataRequest.read(reader);
    }
  };

  public static final ByteString DEFAULT_DATA = ByteString.EMPTY;

  public final ByteString data;

  public LetsDataRequest(ByteString data) {
    super("LetsDataRequest");
    this.data = data;
  }

  private LetsDataRequest(Builder builder) {
    this(builder.data);
    setBuilder(builder);
  }

  @Override
  protected void visitFields(Message.Visitor visitor) {
    visitor.value(1, "data", data, TypeAdapter.BYTES, false);
    visitor.unknowns(this);
  }

  public static LetsDataRequest read(ProtoReader reader) throws IOException {
    Builder builder = new Builder();
    while (reader.hasNext()) {
      int tag = reader.nextTag();
      switch (tag) {
        case 1: builder.data = reader.value(TypeAdapter.BYTES); break;
        default: builder.readUnknown(tag, reader); break;
      }
    }
    return builder.build();
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<LetsDataRequest> {
    public ByteString data;

    public Builder() {
    }

    public Builder(LetsDataRequest message) {
      super(message);
      if (message == null) return;
      this.data = message.data;
    }

    public Builder data(ByteString data) {
      this.data = data;
      return this;
    }

    @Override
    public LetsDataRequest build() {
      return new LetsDataRequest(this);
    }
  }
}
