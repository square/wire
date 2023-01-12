// Code generated by Wire protocol buffer compiler, do not edit.
// Source: squareup.protos.depend_on_kotlin_option.Letter in depend_on_kotlin_option.proto
package com.squareup.wire.protos.depend_on_kotlin_option;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.ReverseProtoWriter;
import com.squareup.wire.Syntax;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import com.squareup.wire.protos.custom_options.MyFieldOptionOneOption;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import okio.ByteString;

public final class Letter extends Message<Letter, Letter.Builder> {
  public static final ProtoAdapter<Letter> ADAPTER = new ProtoAdapter_Letter();

  private static final long serialVersionUID = 0L;

  public static final String DEFAULT_SUBJECT = "";

  @MyFieldOptionOneOption(12)
  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String subject;

  public Letter(String subject) {
    this(subject, ByteString.EMPTY);
  }

  public Letter(String subject, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.subject = subject;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.subject = subject;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof Letter)) return false;
    Letter o = (Letter) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(subject, o.subject);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (subject != null ? subject.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (subject != null) builder.append(", subject=").append(Internal.sanitize(subject));
    return builder.replace(0, 2, "Letter{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<Letter, Builder> {
    public String subject;

    public Builder() {
    }

    public Builder subject(String subject) {
      this.subject = subject;
      return this;
    }

    @Override
    public Letter build() {
      return new Letter(subject, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_Letter extends ProtoAdapter<Letter> {
    public ProtoAdapter_Letter() {
      super(FieldEncoding.LENGTH_DELIMITED, Letter.class, "type.googleapis.com/squareup.protos.depend_on_kotlin_option.Letter", Syntax.PROTO_2, null, "depend_on_kotlin_option.proto");
    }

    @Override
    public int encodedSize(Letter value) {
      int result = 0;
      result += ProtoAdapter.STRING.encodedSizeWithTag(1, value.subject);
      result += value.unknownFields().size();
      return result;
    }

    @Override
    public void encode(ProtoWriter writer, Letter value) throws IOException {
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.subject);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public void encode(ReverseProtoWriter writer, Letter value) throws IOException {
      writer.writeBytes(value.unknownFields());
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.subject);
    }

    @Override
    public Letter decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.subject(ProtoAdapter.STRING.decode(reader)); break;
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public Letter redact(Letter value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
