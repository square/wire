// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: name_collisions.proto
package com.squareup.wire.protos.namecollisions;

import com.squareup.wire.FieldEncoding;
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

public final class Message extends com.squareup.wire.Message<Message, Message.Builder> {
  public static final ProtoAdapter<Message> ADAPTER = new ProtoAdapter_Message();

  private static final long serialVersionUID = 0L;

  public static final String DEFAULT_UNKNOWNFIELDS = "";

  public static final String DEFAULT_OTHER = "";

  public static final String DEFAULT_O = "";

  public static final String DEFAULT_RESULT = "";

  public static final String DEFAULT_HASHCODE = "";

  public static final String DEFAULT_SERIALVERSIONUID_ = "";

  public static final String DEFAULT_ADAPTER_ = "";

  public static final String DEFAULT_MESSAGE_OPTIONS_ = "";

  public static final String DEFAULT_THIS_ = "";

  public static final String DEFAULT_MESSAGE = "";

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String unknownFields;

  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String other;

  @WireField(
      tag = 3,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String o;

  @WireField(
      tag = 4,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String result;

  @WireField(
      tag = 5,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String hashCode;

  @WireField(
      tag = 6,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String serialVersionUID_;

  @WireField(
      tag = 7,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String ADAPTER_;

  @WireField(
      tag = 8,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String MESSAGE_OPTIONS_;

  @WireField(
      tag = 9,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String this_;

  @WireField(
      tag = 10,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String message;

  public Message(String unknownFields, String other, String o, String result, String hashCode,
      String serialVersionUID_, String ADAPTER_, String MESSAGE_OPTIONS_, String this_,
      String message) {
    this(unknownFields, other, o, result, hashCode, serialVersionUID_, ADAPTER_, MESSAGE_OPTIONS_, this_, message, ByteString.EMPTY);
  }

  public Message(String unknownFields, String other, String o, String result, String hashCode,
      String serialVersionUID_, String ADAPTER_, String MESSAGE_OPTIONS_, String this_,
      String message, ByteString unknownFields_) {
    super(ADAPTER, unknownFields_);
    this.unknownFields = unknownFields;
    this.other = other;
    this.o = o;
    this.result = result;
    this.hashCode = hashCode;
    this.serialVersionUID_ = serialVersionUID_;
    this.ADAPTER_ = ADAPTER_;
    this.MESSAGE_OPTIONS_ = MESSAGE_OPTIONS_;
    this.this_ = this_;
    this.message = message;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.unknownFields = unknownFields;
    builder.other = other;
    builder.o = o;
    builder.result = result;
    builder.hashCode = hashCode;
    builder.serialVersionUID_ = serialVersionUID_;
    builder.ADAPTER_ = ADAPTER_;
    builder.MESSAGE_OPTIONS_ = MESSAGE_OPTIONS_;
    builder.this_ = this_;
    builder.message = message;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other_) {
    if (other_ == this) return true;
    if (!(other_ instanceof Message)) return false;
    Message o_ = (Message) other_;
    return unknownFields().equals(o_.unknownFields())
        && Internal.equals(unknownFields, o_.unknownFields)
        && Internal.equals(other, o_.other)
        && Internal.equals(o, o_.o)
        && Internal.equals(result, o_.result)
        && Internal.equals(hashCode, o_.hashCode)
        && Internal.equals(serialVersionUID_, o_.serialVersionUID_)
        && Internal.equals(ADAPTER_, o_.ADAPTER_)
        && Internal.equals(MESSAGE_OPTIONS_, o_.MESSAGE_OPTIONS_)
        && Internal.equals(this_, o_.this_)
        && Internal.equals(message, o_.message);
  }

  @Override
  public int hashCode() {
    int result_ = super.hashCode;
    if (result_ == 0) {
      result_ = unknownFields().hashCode();
      result_ = result_ * 37 + (unknownFields != null ? unknownFields.hashCode() : 0);
      result_ = result_ * 37 + (other != null ? other.hashCode() : 0);
      result_ = result_ * 37 + (o != null ? o.hashCode() : 0);
      result_ = result_ * 37 + (result != null ? result.hashCode() : 0);
      result_ = result_ * 37 + (hashCode != null ? hashCode.hashCode() : 0);
      result_ = result_ * 37 + (serialVersionUID_ != null ? serialVersionUID_.hashCode() : 0);
      result_ = result_ * 37 + (ADAPTER_ != null ? ADAPTER_.hashCode() : 0);
      result_ = result_ * 37 + (MESSAGE_OPTIONS_ != null ? MESSAGE_OPTIONS_.hashCode() : 0);
      result_ = result_ * 37 + (this_ != null ? this_.hashCode() : 0);
      result_ = result_ * 37 + (message != null ? message.hashCode() : 0);
      super.hashCode = result_;
    }
    return result_;
  }

  @Override
  public String toString() {
    int length = 9;
    length += 16 + unknownFields.length();
    length += 8 + other.length();
    length += 4 + o.length();
    length += 9 + result.length();
    length += 11 + hashCode.length();
    length += 19 + serialVersionUID_.length();
    length += 10 + ADAPTER_.length();
    length += 18 + MESSAGE_OPTIONS_.length();
    length += 7 + this_.length();
    length += 10 + message.length();
    StringBuilder builder = new StringBuilder(length);
    if (unknownFields != null) builder.append(", unknownFields=").append(unknownFields);
    if (other != null) builder.append(", other=").append(other);
    if (o != null) builder.append(", o=").append(o);
    if (result != null) builder.append(", result=").append(result);
    if (hashCode != null) builder.append(", hashCode=").append(hashCode);
    if (serialVersionUID_ != null) builder.append(", serialVersionUID=").append(serialVersionUID_);
    if (ADAPTER_ != null) builder.append(", ADAPTER=").append(ADAPTER_);
    if (MESSAGE_OPTIONS_ != null) builder.append(", MESSAGE_OPTIONS=").append(MESSAGE_OPTIONS_);
    if (this_ != null) builder.append(", this=").append(this_);
    if (message != null) builder.append(", message=").append(message);
    return builder.replace(0, 2, "Message{").append('}').toString();
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<Message, Builder> {
    public String unknownFields;

    public String other;

    public String o;

    public String result;

    public String hashCode;

    public String serialVersionUID_;

    public String ADAPTER_;

    public String MESSAGE_OPTIONS_;

    public String this_;

    public String message;

    public Builder() {
    }

    public Builder unknownFields(String unknownFields) {
      this.unknownFields = unknownFields;
      return this;
    }

    public Builder other(String other) {
      this.other = other;
      return this;
    }

    public Builder o(String o) {
      this.o = o;
      return this;
    }

    public Builder result(String result) {
      this.result = result;
      return this;
    }

    public Builder hashCode(String hashCode) {
      this.hashCode = hashCode;
      return this;
    }

    public Builder serialVersionUID_(String serialVersionUID_) {
      this.serialVersionUID_ = serialVersionUID_;
      return this;
    }

    public Builder ADAPTER_(String ADAPTER_) {
      this.ADAPTER_ = ADAPTER_;
      return this;
    }

    public Builder MESSAGE_OPTIONS_(String MESSAGE_OPTIONS_) {
      this.MESSAGE_OPTIONS_ = MESSAGE_OPTIONS_;
      return this;
    }

    public Builder this_(String this_) {
      this.this_ = this_;
      return this;
    }

    public Builder message(String message) {
      this.message = message;
      return this;
    }

    @Override
    public Message build() {
      return new Message(unknownFields, other, o, result, hashCode, serialVersionUID_, ADAPTER_, MESSAGE_OPTIONS_, this_, message, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_Message extends ProtoAdapter<Message> {
    public ProtoAdapter_Message() {
      super(FieldEncoding.LENGTH_DELIMITED, Message.class);
    }

    @Override
    public int encodedSize(Message value) {
      return ProtoAdapter.STRING.encodedSizeWithTag(1, value.unknownFields)
          + ProtoAdapter.STRING.encodedSizeWithTag(2, value.other)
          + ProtoAdapter.STRING.encodedSizeWithTag(3, value.o)
          + ProtoAdapter.STRING.encodedSizeWithTag(4, value.result)
          + ProtoAdapter.STRING.encodedSizeWithTag(5, value.hashCode)
          + ProtoAdapter.STRING.encodedSizeWithTag(6, value.serialVersionUID_)
          + ProtoAdapter.STRING.encodedSizeWithTag(7, value.ADAPTER_)
          + ProtoAdapter.STRING.encodedSizeWithTag(8, value.MESSAGE_OPTIONS_)
          + ProtoAdapter.STRING.encodedSizeWithTag(9, value.this_)
          + ProtoAdapter.STRING.encodedSizeWithTag(10, value.message)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, Message value) throws IOException {
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.unknownFields);
      ProtoAdapter.STRING.encodeWithTag(writer, 2, value.other);
      ProtoAdapter.STRING.encodeWithTag(writer, 3, value.o);
      ProtoAdapter.STRING.encodeWithTag(writer, 4, value.result);
      ProtoAdapter.STRING.encodeWithTag(writer, 5, value.hashCode);
      ProtoAdapter.STRING.encodeWithTag(writer, 6, value.serialVersionUID_);
      ProtoAdapter.STRING.encodeWithTag(writer, 7, value.ADAPTER_);
      ProtoAdapter.STRING.encodeWithTag(writer, 8, value.MESSAGE_OPTIONS_);
      ProtoAdapter.STRING.encodeWithTag(writer, 9, value.this_);
      ProtoAdapter.STRING.encodeWithTag(writer, 10, value.message);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public Message decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.unknownFields(ProtoAdapter.STRING.decode(reader)); break;
          case 2: builder.other(ProtoAdapter.STRING.decode(reader)); break;
          case 3: builder.o(ProtoAdapter.STRING.decode(reader)); break;
          case 4: builder.result(ProtoAdapter.STRING.decode(reader)); break;
          case 5: builder.hashCode(ProtoAdapter.STRING.decode(reader)); break;
          case 6: builder.serialVersionUID_(ProtoAdapter.STRING.decode(reader)); break;
          case 7: builder.ADAPTER_(ProtoAdapter.STRING.decode(reader)); break;
          case 8: builder.MESSAGE_OPTIONS_(ProtoAdapter.STRING.decode(reader)); break;
          case 9: builder.this_(ProtoAdapter.STRING.decode(reader)); break;
          case 10: builder.message(ProtoAdapter.STRING.decode(reader)); break;
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public Message redact(Message value) {
      Builder builder = value.newBuilder();
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
