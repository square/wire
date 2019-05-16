// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: packed_encoding.proto
package squareup.protos.packed_encoding;

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
import kotlin.jvm.JvmClassMappingKt;
import okio.ByteString;

public final class OuterMessage extends Message<OuterMessage, OuterMessage.Builder> {
  public static final ProtoAdapter<OuterMessage> ADAPTER = new ProtoAdapter_OuterMessage();

  private static final long serialVersionUID = 0L;

  public static final Integer DEFAULT_OUTER_NUMBER_BEFORE = 0;

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  public final Integer outer_number_before;

  @WireField(
      tag = 2,
      adapter = "squareup.protos.packed_encoding.EmbeddedMessage#ADAPTER"
  )
  public final EmbeddedMessage embedded_message;

  public OuterMessage(Integer outer_number_before, EmbeddedMessage embedded_message) {
    this(outer_number_before, embedded_message, ByteString.EMPTY);
  }

  public OuterMessage(Integer outer_number_before, EmbeddedMessage embedded_message,
      ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.outer_number_before = outer_number_before;
    this.embedded_message = embedded_message;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.outer_number_before = outer_number_before;
    builder.embedded_message = embedded_message;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof OuterMessage)) return false;
    OuterMessage o = (OuterMessage) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(outer_number_before, o.outer_number_before)
        && Internal.equals(embedded_message, o.embedded_message);
  }

  @Override
  public int hashCode() {
    int result = super.cachedHashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (outer_number_before != null ? outer_number_before.hashCode() : 0);
      result = result * 37 + (embedded_message != null ? embedded_message.hashCode() : 0);
      super.cachedHashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (outer_number_before != null) builder.append(", outer_number_before=").append(outer_number_before);
    if (embedded_message != null) builder.append(", embedded_message=").append(embedded_message);
    return builder.replace(0, 2, "OuterMessage{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<OuterMessage, Builder> {
    public Integer outer_number_before;

    public EmbeddedMessage embedded_message;

    public Builder() {
    }

    public Builder outer_number_before(Integer outer_number_before) {
      this.outer_number_before = outer_number_before;
      return this;
    }

    public Builder embedded_message(EmbeddedMessage embedded_message) {
      this.embedded_message = embedded_message;
      return this;
    }

    @Override
    public OuterMessage build() {
      return new OuterMessage(outer_number_before, embedded_message, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_OuterMessage extends ProtoAdapter<OuterMessage> {
    public ProtoAdapter_OuterMessage() {
      super(FieldEncoding.LENGTH_DELIMITED, JvmClassMappingKt.getKotlinClass(OuterMessage.class));
    }

    @Override
    public int encodedSize(OuterMessage value) {
      return ProtoAdapter.INT32.encodedSizeWithTag(1, value.outer_number_before)
          + EmbeddedMessage.ADAPTER.encodedSizeWithTag(2, value.embedded_message)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, OuterMessage value) throws IOException {
      ProtoAdapter.INT32.encodeWithTag(writer, 1, value.outer_number_before);
      EmbeddedMessage.ADAPTER.encodeWithTag(writer, 2, value.embedded_message);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public OuterMessage decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.outer_number_before(ProtoAdapter.INT32.decode(reader)); break;
          case 2: builder.embedded_message(EmbeddedMessage.ADAPTER.decode(reader)); break;
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
    public OuterMessage redact(OuterMessage value) {
      Builder builder = value.newBuilder();
      if (builder.embedded_message != null) builder.embedded_message = EmbeddedMessage.ADAPTER.redact(builder.embedded_message);
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
