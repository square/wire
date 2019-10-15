// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: one_extension.proto
package com.squareup.wire.protos.one_extension;

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

public final class OneExtension extends Message<OneExtension, OneExtension.Builder> {
  public static final ProtoAdapter<OneExtension> ADAPTER = new ProtoAdapter_OneExtension();

  private static final long serialVersionUID = 0L;

  public static final String DEFAULT_ID = "";

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String id;

  /**
   * Extension source: one_extension.proto
   */
  @WireField(
      tag = 1000,
      adapter = "com.squareup.wire.protos.one_extension.Foo#ADAPTER"
  )
  public final Foo foo;

  public OneExtension(String id, Foo foo) {
    this(id, foo, ByteString.EMPTY);
  }

  public OneExtension(String id, Foo foo, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.id = id;
    this.foo = foo;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.id = id;
    builder.foo = foo;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof OneExtension)) return false;
    OneExtension o = (OneExtension) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(id, o.id)
        && Internal.equals(foo, o.foo);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (id != null ? id.hashCode() : 0);
      result = result * 37 + (foo != null ? foo.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    int length = 14;
    length += 5 + id.length();
    length += 6 + foo.toString().length();
    StringBuilder builder = new StringBuilder(length);
    if (id != null) builder.append(", id=").append(id);
    if (foo != null) builder.append(", foo=").append(foo);
    return builder.replace(0, 2, "OneExtension{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<OneExtension, Builder> {
    public String id;

    public Foo foo;

    public Builder() {
    }

    public Builder id(String id) {
      this.id = id;
      return this;
    }

    public Builder foo(Foo foo) {
      this.foo = foo;
      return this;
    }

    @Override
    public OneExtension build() {
      return new OneExtension(id, foo, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_OneExtension extends ProtoAdapter<OneExtension> {
    public ProtoAdapter_OneExtension() {
      super(FieldEncoding.LENGTH_DELIMITED, OneExtension.class);
    }

    @Override
    public int encodedSize(OneExtension value) {
      return ProtoAdapter.STRING.encodedSizeWithTag(1, value.id)
          + Foo.ADAPTER.encodedSizeWithTag(1000, value.foo)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, OneExtension value) throws IOException {
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.id);
      Foo.ADAPTER.encodeWithTag(writer, 1000, value.foo);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public OneExtension decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.id(ProtoAdapter.STRING.decode(reader)); break;
          case 1000: builder.foo(Foo.ADAPTER.decode(reader)); break;
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public OneExtension redact(OneExtension value) {
      Builder builder = value.newBuilder();
      if (builder.foo != null) builder.foo = Foo.ADAPTER.redact(builder.foo);
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
