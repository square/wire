package com.squareup.wire.protos.roots;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.WireInternal;
import okio.ByteString;

import java.io.IOException;

public final class CustomBuilderNameMessage extends Message<CustomBuilderNameMessage, CustomBuilderNameMessage.CustomBuilder> {
  public static final ProtoAdapter<CustomBuilderNameMessage> ADAPTER = new ProtoAdapter<CustomBuilderNameMessage>(FieldEncoding.LENGTH_DELIMITED, CustomBuilderNameMessage.class) {
    @Override
    public int encodedSize(CustomBuilderNameMessage value) {
      return (value.i != null ? ProtoAdapter.INT32.encodedSizeWithTag(1, value.i) : 0)
        + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, CustomBuilderNameMessage value) throws IOException {
      if (value.i != null) ProtoAdapter.INT32.encodeWithTag(writer, 1, value.i);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public CustomBuilderNameMessage decode(ProtoReader reader) throws IOException {
      CustomBuilder builder = new CustomBuilder();
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
    public CustomBuilderNameMessage redact(CustomBuilderNameMessage value) {
      CustomBuilder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  };

  private static final long serialVersionUID = 0L;

  public static final Integer DEFAULT_I = 0;

  public final Integer i;

  public CustomBuilderNameMessage(Integer i) {
    this(i, ByteString.EMPTY);
  }

  public CustomBuilderNameMessage(Integer i, ByteString unknownFields) {
    super(unknownFields);
    this.i = i;
  }

  @Override
  public CustomBuilder newBuilder() {
    CustomBuilder builder = new CustomBuilder();
    builder.i = i;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof CustomBuilderNameMessage)) return false;
    CustomBuilderNameMessage o = (CustomBuilderNameMessage) other;
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
    return builder.replace(0, 2, "CustomBuilderNameMessage{").append('}').toString();
  }

  public static class CustomBuilder extends Message.Builder<CustomBuilderNameMessage, CustomBuilder> {
    public Integer i;

    private CustomBuilder() {
    }

    public CustomBuilder i(Integer i) {
      this.i = i;
      return this;
    }

    @Override
    public CustomBuilderNameMessage build() {
      return new CustomBuilderNameMessage(i, buildUnknownFields());
    }
  }

  public static class ExtendedCustomBuilder extends CustomBuilder {
    public Integer i;

    public ExtendedCustomBuilder(Object fakeArg) {

    }

    public ExtendedCustomBuilder i(Integer i) {
      this.i = i;
      return this;
    }

    @Override
    public CustomBuilderNameMessage build() {
      return new CustomBuilderNameMessage(i, buildUnknownFields());
    }
  }

}

