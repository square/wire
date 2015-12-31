package com.squareup.wire.protos.roots;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.WireInternal;
import okio.ByteString;

import java.io.IOException;

public final class NotWiredMessage extends Message<NotWiredMessage, NotWiredMessage.Builder> {
  public static final ProtoAdapter<NotWiredMessage> ADAPTER = new ProtoAdapter_NotWiredMessage();

  private static final long serialVersionUID = 0L;

  public static final Integer DEFAULT_I = 0;

  public final Integer i;

  public NotWiredMessage(Integer i) {
    this(i, ByteString.EMPTY);
  }

  public NotWiredMessage(Integer i, ByteString unknownFields) {
    super(unknownFields);
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
    if (!(other instanceof NotWiredMessage)) return false;
    NotWiredMessage o = (NotWiredMessage) other;
    return WireInternal.equals(unknownFields(), o.unknownFields())
      && WireInternal.equals(i, o.i);
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
    return builder.replace(0, 2, "NotWiredMessage{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<NotWiredMessage, Builder> {
    public Integer i;

    public Builder() {
    }

    public Builder i(Integer i) {
      this.i = i;
      return this;
    }

    @Override
    public NotWiredMessage build() {
      return new NotWiredMessage(i, buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_NotWiredMessage extends ProtoAdapter<NotWiredMessage> {
    ProtoAdapter_NotWiredMessage() {
      super(FieldEncoding.LENGTH_DELIMITED, NotWiredMessage.class);
    }

    @Override
    public int encodedSize(NotWiredMessage value) {
      return (value.i != null ? ProtoAdapter.INT32.encodedSizeWithTag(1, value.i) : 0)
        + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, NotWiredMessage value) throws IOException {
      if (value.i != null) ProtoAdapter.INT32.encodeWithTag(writer, 1, value.i);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public NotWiredMessage decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.i(ProtoAdapter.INT32.decode(reader)); break;
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
    public NotWiredMessage redact(NotWiredMessage value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
