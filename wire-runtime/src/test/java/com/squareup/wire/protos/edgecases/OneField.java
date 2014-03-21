// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ../wire-runtime/src/test/proto/edge_cases.proto
package com.squareup.wire.protos.edgecases;

import com.squareup.wire.ProtoField;

import static com.squareup.wire.Message.Datatype.INT32;

public final class OneField extends com.squareup.wire.Message {

  public static final Integer DEFAULT_OPT_INT32 = 0;

  @ProtoField(tag = 1, type = INT32)
  public final Integer opt_int32;

  private OneField(Builder builder) {
    super(builder);
    this.opt_int32 = builder.opt_int32;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof OneField)) return false;
    return equals(opt_int32, ((OneField) other).opt_int32);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    return result != 0 ? result : (hashCode = opt_int32 != null ? opt_int32.hashCode() : 0);
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<OneField> {

    public Integer opt_int32;

    public Builder() {
    }

    public Builder(OneField message) {
      super(message);
      if (message == null) return;
      this.opt_int32 = message.opt_int32;
    }

    public Builder opt_int32(Integer opt_int32) {
      this.opt_int32 = opt_int32;
      return this;
    }

    @Override
    public OneField build() {
      return new OneField(this);
    }
  }
}
