// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: service_root.proto at 6:1
package com.squareup.wire.protos.roots;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.TagMap;
import java.lang.Object;
import java.lang.Override;

public final class TheResponse extends Message<TheResponse> {
  public static final ProtoAdapter<TheResponse> ADAPTER = ProtoAdapter.newMessageAdapter(TheResponse.class);

  private static final long serialVersionUID = 0L;

  public TheResponse() {
    this(TagMap.EMPTY);
  }

  public TheResponse(TagMap tagMap) {
    super(tagMap);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TheResponse;
  }

  @Override
  public int hashCode() {
    return tagMap().hashCode();
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<TheResponse, Builder> {
    public Builder() {
    }

    public Builder(TheResponse message) {
      super(message);
    }

    @Override
    public TheResponse build() {
      return new TheResponse(buildTagMap());
    }
  }
}
