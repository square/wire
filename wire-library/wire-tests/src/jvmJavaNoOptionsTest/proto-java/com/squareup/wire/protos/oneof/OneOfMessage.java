// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.oneof.OneOfMessage in one_of.proto
package com.squareup.wire.protos.oneof;

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

public final class OneOfMessage extends Message<OneOfMessage, OneOfMessage.Builder> {
  public static final ProtoAdapter<OneOfMessage> ADAPTER = new ProtoAdapter_OneOfMessage();

  private static final long serialVersionUID = 0L;

  public static final Integer DEFAULT_FOO = 0;

  public static final String DEFAULT_BAR = "";

  public static final String DEFAULT_BAZ = "";

  /**
   * What foo.
   */
  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#INT32",
      oneofName = "choice"
  )
  public final Integer foo;

  /**
   * Such bar.
   */
  @WireField(
      tag = 3,
      adapter = "com.squareup.wire.ProtoAdapter#STRING",
      oneofName = "choice"
  )
  public final String bar;

  /**
   * Nice baz.
   */
  @WireField(
      tag = 4,
      adapter = "com.squareup.wire.ProtoAdapter#STRING",
      oneofName = "choice"
  )
  public final String baz;

  public OneOfMessage(Integer foo, String bar, String baz) {
    this(foo, bar, baz, ByteString.EMPTY);
  }

  public OneOfMessage(Integer foo, String bar, String baz, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    if (Internal.countNonNull(foo, bar, baz) > 1) {
      throw new IllegalArgumentException("at most one of foo, bar, baz may be non-null");
    }
    this.foo = foo;
    this.bar = bar;
    this.baz = baz;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.foo = foo;
    builder.bar = bar;
    builder.baz = baz;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof OneOfMessage)) return false;
    OneOfMessage o = (OneOfMessage) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(foo, o.foo)
        && Internal.equals(bar, o.bar)
        && Internal.equals(baz, o.baz);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (foo != null ? foo.hashCode() : 0);
      result = result * 37 + (bar != null ? bar.hashCode() : 0);
      result = result * 37 + (baz != null ? baz.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (foo != null) builder.append(", foo=").append(foo);
    if (bar != null) builder.append(", bar=").append(Internal.sanitize(bar));
    if (baz != null) builder.append(", baz=").append(Internal.sanitize(baz));
    return builder.replace(0, 2, "OneOfMessage{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<OneOfMessage, Builder> {
    public Integer foo;

    public String bar;

    public String baz;

    public Builder() {
    }

    /**
     * What foo.
     */
    public Builder foo(Integer foo) {
      this.foo = foo;
      this.bar = null;
      this.baz = null;
      return this;
    }

    /**
     * Such bar.
     */
    public Builder bar(String bar) {
      this.bar = bar;
      this.foo = null;
      this.baz = null;
      return this;
    }

    /**
     * Nice baz.
     */
    public Builder baz(String baz) {
      this.baz = baz;
      this.foo = null;
      this.bar = null;
      return this;
    }

    @Override
    public OneOfMessage build() {
      return new OneOfMessage(foo, bar, baz, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_OneOfMessage extends ProtoAdapter<OneOfMessage> {
    public ProtoAdapter_OneOfMessage() {
      super(FieldEncoding.LENGTH_DELIMITED, OneOfMessage.class, "type.googleapis.com/squareup.protos.oneof.OneOfMessage", Syntax.PROTO_2, null);
    }

    @Override
    public int encodedSize(OneOfMessage value) {
      int result = 0;
      result += ProtoAdapter.INT32.encodedSizeWithTag(1, value.foo);
      result += ProtoAdapter.STRING.encodedSizeWithTag(3, value.bar);
      result += ProtoAdapter.STRING.encodedSizeWithTag(4, value.baz);
      result += value.unknownFields().size();
      return result;
    }

    @Override
    public void encode(ProtoWriter writer, OneOfMessage value) throws IOException {
      ProtoAdapter.INT32.encodeWithTag(writer, 1, value.foo);
      ProtoAdapter.STRING.encodeWithTag(writer, 3, value.bar);
      ProtoAdapter.STRING.encodeWithTag(writer, 4, value.baz);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public void encode(ReverseProtoWriter writer, OneOfMessage value) throws IOException {
      writer.writeBytes(value.unknownFields());
      ProtoAdapter.STRING.encodeWithTag(writer, 4, value.baz);
      ProtoAdapter.STRING.encodeWithTag(writer, 3, value.bar);
      ProtoAdapter.INT32.encodeWithTag(writer, 1, value.foo);
    }

    @Override
    public OneOfMessage decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.foo(ProtoAdapter.INT32.decode(reader)); break;
          case 3: builder.bar(ProtoAdapter.STRING.decode(reader)); break;
          case 4: builder.baz(ProtoAdapter.STRING.decode(reader)); break;
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public OneOfMessage redact(OneOfMessage value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
