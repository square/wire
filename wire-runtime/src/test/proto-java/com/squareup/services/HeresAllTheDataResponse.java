// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: simple_service2.proto at 9:1
package com.squareup.services;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.TagMap;
import com.squareup.wire.WireField;
import java.lang.Object;
import java.lang.Override;
import okio.ByteString;

public final class HeresAllTheDataResponse extends Message<HeresAllTheDataResponse> {
  public static final ProtoAdapter<HeresAllTheDataResponse> ADAPTER = ProtoAdapter.newMessageAdapter(HeresAllTheDataResponse.class);

  private static final long serialVersionUID = 0L;

  public static final ByteString DEFAULT_DATA = ByteString.EMPTY;

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#BYTES"
  )
  public final ByteString data;

  public HeresAllTheDataResponse(ByteString data) {
    this(data, TagMap.EMPTY);
  }

  public HeresAllTheDataResponse(ByteString data, TagMap tagMap) {
    super(tagMap);
    this.data = data;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof HeresAllTheDataResponse)) return false;
    HeresAllTheDataResponse o = (HeresAllTheDataResponse) other;
    return equals(tagMap(), o.tagMap())
        && equals(data, o.data);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = tagMap().hashCode();
      result = result * 37 + (data != null ? data.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<HeresAllTheDataResponse, Builder> {
    public ByteString data;

    public Builder() {
    }

    public Builder(HeresAllTheDataResponse message) {
      super(message);
      if (message == null) return;
      this.data = message.data;
    }

    public Builder data(ByteString data) {
      this.data = data;
      return this;
    }

    @Override
    public HeresAllTheDataResponse build() {
      return new HeresAllTheDataResponse(data, buildTagMap());
    }
  }
}
