// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ../wire-runtime/src/test/proto/interceptors.proto
package com.squareup.wire.protos.interceptors;

import com.google.protobuf.MessageOptions;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;

import static com.squareup.wire.Message.Datatype.STRING;

public final class Unencrypted extends Message {

  public static final MessageOptions MESSAGE_OPTIONS = new MessageOptions.Builder()
      .setExtension(Ext_interceptors.encrypt_payload, false)
      .build();

  public static final String DEFAULT_NAME = "";
  public static final String DEFAULT_PAYLOAD = "";

  @ProtoField(tag = 1, type = STRING)
  public final String name;

  @ProtoField(tag = 2, type = STRING)
  public final String payload;

  public Unencrypted(String name, String payload) {
    this.name = name;
    this.payload = payload;
  }

  private Unencrypted(Builder builder) {
    this(builder.name, builder.payload);
    setBuilder(builder);
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof Unencrypted)) return false;
    Unencrypted o = (Unencrypted) other;
    return equals(name, o.name)
        && equals(payload, o.payload);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    if (result == 0) {
      result = name != null ? name.hashCode() : 0;
      result = result * 37 + (payload != null ? payload.hashCode() : 0);
      hashCode = result;
    }
    return result;
  }

  public static final class Builder extends Message.Builder<Unencrypted> {

    public String name;
    public String payload;

    public Builder() {
    }

    public Builder(Unencrypted message) {
      super(message);
      if (message == null) return;
      this.name = message.name;
      this.payload = message.payload;
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder payload(String payload) {
      this.payload = payload;
      return this;
    }

    @Override
    public Unencrypted build() {
      return new Unencrypted(this);
    }
  }
}
