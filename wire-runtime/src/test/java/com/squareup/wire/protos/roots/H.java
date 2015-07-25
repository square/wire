// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ../wire-runtime/src/test/proto/roots.proto at 65:1
package com.squareup.wire.protos.roots;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.TypeAdapter;
import java.io.IOException;
import java.lang.Override;

public final class H extends Message<H> {
  private static final long serialVersionUID = 0L;

  public static final TypeAdapter<H> ADAPTER = new TypeAdapter.MessageAdapter<H>() {
    @Override
    public H read(ProtoReader reader) throws IOException {
      return H.read(reader);
    }
  };

  public final E.F ef;

  public H(E.F ef) {
    super("H");
    this.ef = ef;
  }

  private H(Builder builder) {
    this(builder.ef);
    setBuilder(builder);
  }

  @Override
  protected void visitFields(Message.Visitor visitor) {
    visitor.value(1, "ef", ef, E.F.ADAPTER, false);
    visitor.unknowns(this);
  }

  public static H read(ProtoReader reader) throws IOException {
    Builder builder = new Builder();
    while (reader.hasNext()) {
      int tag = reader.nextTag();
      switch (tag) {
        case 1: builder.ef = message(reader, E.F.ADAPTER); break;
        default: builder.readUnknown(tag, reader); break;
      }
    }
    return builder.build();
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<H> {
    public E.F ef;

    public Builder() {
    }

    public Builder(H message) {
      super(message);
      if (message == null) return;
      this.ef = message.ef;
    }

    public Builder ef(E.F ef) {
      this.ef = ef;
      return this;
    }

    @Override
    public H build() {
      return new H(this);
    }
  }
}
