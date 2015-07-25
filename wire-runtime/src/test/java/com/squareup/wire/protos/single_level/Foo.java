// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ../wire-runtime/src/test/proto/single_level.proto at 20:1
package com.squareup.wire.protos.single_level;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.TypeAdapter;
import java.io.IOException;
import java.lang.Integer;
import java.lang.Override;

public final class Foo extends Message<Foo> {
  private static final long serialVersionUID = 0L;

  public static final TypeAdapter<Foo> ADAPTER = new TypeAdapter.MessageAdapter<Foo>() {
    @Override
    public Foo read(ProtoReader reader) throws IOException {
      return Foo.read(reader);
    }
  };

  public static final Integer DEFAULT_BAR = 0;

  public final Integer bar;

  public Foo(Integer bar) {
    super("Foo");
    this.bar = bar;
  }

  private Foo(Builder builder) {
    this(builder.bar);
    setBuilder(builder);
  }

  @Override
  protected void visitFields(Message.Visitor visitor) {
    visitor.value(1, "bar", bar, TypeAdapter.INT32, false);
    visitor.unknowns(this);
  }

  public static Foo read(ProtoReader reader) throws IOException {
    Builder builder = new Builder();
    while (reader.hasNext()) {
      int tag = reader.nextTag();
      switch (tag) {
        case 1: builder.bar = reader.value(TypeAdapter.INT32); break;
        default: builder.readUnknown(tag, reader); break;
      }
    }
    return builder.build();
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<Foo> {
    public Integer bar;

    public Builder() {
    }

    public Builder(Foo message) {
      super(message);
      if (message == null) return;
      this.bar = message.bar;
    }

    public Builder bar(Integer bar) {
      this.bar = bar;
      return this;
    }

    @Override
    public Foo build() {
      return new Foo(this);
    }
  }
}
