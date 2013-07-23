/**
 * Code generated by Wire protobuf compiler, do not edit.
 * Source file: ../wire-runtime/src/test/proto/edge_cases.proto
 */
package com.squareup.wire.protos.edgecases;

import com.squareup.wire.ByteString;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoField;

public final class OneBytesField extends Message {

  public static final ByteString DEFAULT_OPT_BYTES = ByteString.of("");

  @ProtoField(
    tag = 1,
    type = BYTES
  )
  public final ByteString opt_bytes;

  private OneBytesField(Builder builder) {
    super(builder);
    this.opt_bytes = builder.opt_bytes;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof OneBytesField)) return false;
    return equals(opt_bytes, ((OneBytesField) other).opt_bytes);
  }

  @Override
  public int hashCode() {
    int result = hashCode;
    return result != 0 ? result : (hashCode = opt_bytes != null ? opt_bytes.hashCode() : 0);
  }

  public static final class Builder extends Message.Builder<OneBytesField> {

    public ByteString opt_bytes;

    public Builder() {
    }

    public Builder(OneBytesField message) {
      super(message);
      if (message == null) return;
      this.opt_bytes = message.opt_bytes;
    }

    public Builder opt_bytes(ByteString opt_bytes) {
      this.opt_bytes = opt_bytes;
      return this;
    }

    @Override
    public OneBytesField build() {
      return new OneBytesField(this);
    }
  }
}
