// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ../wire-runtime/src/test/proto/service_root.proto at 7:1
package com.squareup.wire.protos.roots;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.TagMap;
import java.io.ObjectStreamException;
import java.lang.Object;
import java.lang.Override;

public final class UnnecessaryResponse extends Message<UnnecessaryResponse> {
  public static final ProtoAdapter<UnnecessaryResponse> ADAPTER = ProtoAdapter.newMessageAdapter(UnnecessaryResponse.class);

  private static final long serialVersionUID = 0L;

  public UnnecessaryResponse() {
    this(null);
  }

  public UnnecessaryResponse(TagMap tagMap) {
    super(tagMap);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof UnnecessaryResponse;
  }

  @Override
  public int hashCode() {
    return tagMap() != null ? tagMap().hashCode() : 0;
  }

  private Object writeReplace() throws ObjectStreamException {
    return super.createSerializedForm();
  }

  public static final class Builder extends com.squareup.wire.Message.Builder<UnnecessaryResponse, Builder> {
    public Builder() {
    }

    public Builder(UnnecessaryResponse message) {
      super(message);
    }

    @Override
    public UnnecessaryResponse build() {
      return new UnnecessaryResponse(buildTagMap());
    }
  }
}
