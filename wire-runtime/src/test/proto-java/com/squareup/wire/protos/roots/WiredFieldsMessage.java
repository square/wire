// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: roots.proto at 43:1
package com.squareup.wire.protos.roots;

import com.squareup.wire.Message;
import com.squareup.wire.WireField;
import okio.ByteString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class WiredFieldsMessage extends Message<WiredFieldsMessage, WiredFieldsMessage.Builder> {

  private static final long serialVersionUID = 0L;

  public static final Integer DEFAULT_VAL = 0;
  public static final List<Integer> DEFAULT_LIST = Collections.EMPTY_LIST;
  public static final C DEFAULT_C = new C(0);

  @WireField(tag = 1, adapter = "com.squareup.wire.ProtoAdapter#INT32", label = WireField.Label.REQUIRED, redacted = true)
  public final Integer val;

  @WireField(tag = 2, adapter = "com.squareup.wire.ProtoAdapter#INT32", label = WireField.Label.REPEATED, redacted = false)
  public final List<Integer> list;

  @WireField(tag = 3, adapter = "com.squareup.wire.protos.roots.C#ADAPTER", label = WireField.Label.ONE_OF, redacted = false)
  public final C c;

  public WiredFieldsMessage(Integer val, List<Integer> list, C c) {
    this(val, list, c, ByteString.EMPTY);
  }

  public WiredFieldsMessage(Integer val, List<Integer> list, C c, ByteString unknownFields) {
    super(unknownFields);
    this.val = val;
    this.list = list;
    this.c = c;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.val = val;
    builder.list = list;
    builder.c = c;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  public static final class Builder extends Message.Builder<WiredFieldsMessage, Builder> {
    public Integer val;
    public List<Integer> list = new ArrayList<>();
    public C c;

    public Builder() {
    }

    public Builder val(Integer val) {
      this.val = val;
      return this;
    }

    public Builder list(List<Integer> list) {
      this.list = list;
      return this;
    }

    public Builder c(C c) {
      this.c = c;
      return this;
    }

    @Override
    public WiredFieldsMessage build() {
      return new WiredFieldsMessage(val, list, c, buildUnknownFields());
    }
  }
}
