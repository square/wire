package com.squareup.wire.protos.person;

import com.google.protobuf.FieldOptions;
import com.google.protobuf.MessageOptions;
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

public final class Bar extends Message<Bar, Bar.Builder> {
  public static final ProtoAdapter<Bar> ADAPTER = new ProtoAdapter_Bar();

  private static final long serialVersionUID = 0L;

  public static final MessageOptions MESSAGE_OPTIONS = new MessageOptions.Builder()
      .pop(12)
      .build();

  public static final FieldOptions FIELD_OPTIONS_A = new FieldOptions.Builder()
      .foo_options(new FooOptions.Builder()
          .opt1(123)
          .opt2("baz")
          .build())
      .build();

  public static final FieldOptions FIELD_OPTIONS_B = new FieldOptions.Builder()
      .foo_options(new FooOptions.Builder()
          .opt1(456)
          .opt2("quux")
          .build())
      .build();

  public static final Integer DEFAULT_A = 0;

  public static final Integer DEFAULT_B = 0;

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  public final Integer a;

  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  public final Integer b;

  public Bar(Integer a, Integer b) {
    this(a, b, ByteString.EMPTY);
  }

  public Bar(Integer a, Integer b, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.a = a;
    this.b = b;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.a = a;
    builder.b = b;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof Bar)) return false;
    Bar o = (Bar) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(a, o.a)
        && Internal.equals(b, o.b);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (a != null ? a.hashCode() : 0);
      result = result * 37 + (b != null ? b.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (a != null) builder.append(", a=").append(a);
    if (b != null) builder.append(", b=").append(b);
    return builder.replace(0, 2, "Bar{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<Bar, Builder> {
    public Integer a;

    public Integer b;

    public Builder() {
    }

    public Builder a(Integer a) {
      this.a = a;
      return this;
    }

    public Builder b(Integer b) {
      this.b = b;
      return this;
    }

    @Override
    public Bar build() {
      return new Bar(a, b, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_Bar extends ProtoAdapter<Bar> {
    public ProtoAdapter_Bar() {
      super(FieldEncoding.LENGTH_DELIMITED, Bar.class);
    }

    @Override
    public int encodedSize(Bar value) {
      return ProtoAdapter.INT32.encodedSizeWithTag(1, value.a)
          + ProtoAdapter.INT32.encodedSizeWithTag(2, value.b)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, Bar value) throws IOException {
      ProtoAdapter.INT32.encodeWithTag(writer, 1, value.a);
      ProtoAdapter.INT32.encodeWithTag(writer, 2, value.b);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public Bar decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.a(ProtoAdapter.INT32.decode(reader)); break;
          case 2: builder.b(ProtoAdapter.INT32.decode(reader)); break;
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
    public Bar redact(Bar value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
