// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: service_root.proto at 5:1
package com.squareup.wire.protos.roots;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.TagMap;
import java.lang.Object;
import java.lang.Override;

public final class TheRequest extends Message<TheRequest> {
  public static final ProtoAdapter<TheRequest> ADAPTER = ProtoAdapter.newMessageAdapter(TheRequest.class);

  private static final long serialVersionUID = 0L;

  public TheRequest() {
    this(TagMap.EMPTY);
  }

  public TheRequest(TagMap tagMap) {
    super(tagMap);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TheRequest;
  }

  @Override
  public int hashCode() {
    return tagMap().hashCode();
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<TheRequest, Builder> {
    public Builder() {
    }

    public Builder(TheRequest message) {
      super(message);
    }

    @Override
    public TheRequest build() {
      return new TheRequest(buildTagMap());
    }
  }
}
