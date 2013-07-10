/**
 * Code generated by "Wire" little protobuf compiler, do not edit.
 * Source file: runtime/src/main/proto/external_message.proto
 */
package com.squareup.protos.simple;

import com.squareup.wire.Message;
import com.squareup.wire.Wire;
import com.squareup.wire.ProtoField;
import com.squareup.wire.UninitializedMessageException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class ExternalMessageContainer {

  private ExternalMessageContainer() {
  }

  public static final class ExternalMessage
      implements Message.ExtendableMessage<ExternalMessage> {

    public final Map<Extension<ExternalMessage, ?>, Object> extensionMap;

    public static final Float f_default = 20F;

    @ProtoField(
      tag = 1,
      type = Wire.FLOAT
    )
    public final Float f;

    private ExternalMessage(Builder builder) {
      this.f = builder.f;
      this.extensionMap = Collections.unmodifiableMap(new TreeMap<Extension<ExternalMessage, ?>, Object>(builder.extensionMap));
    }

    @Override
    public <Type> Type getExtension(Extension<ExternalMessage, Type> extension) {
      return (Type) extensionMap.get(extension);
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof ExternalMessage)) return false;
      ExternalMessage o = (ExternalMessage) other;
      if (!extensionMap.equals(o.extensionMap)) return false;
      if (!Wire.equals(f, o.f)) return false;
      return true;
    }

    @Override
    public int hashCode() {
      int hashCode = extensionMap.hashCode();
      hashCode = hashCode * 37 + (f != null ? f.hashCode() : 0);
      return hashCode;
    }

    @Override
    public String toString() {
      return String.format("ExternalMessage{" +
          "f=%s," +
          "{extensionMap=%s}",
          f,
          Wire.toString(extensionMap));
    }

    public static final class Builder
        implements ExtendableMessage.ExtendableBuilder<ExternalMessage> {

      private final Map<Extension<ExternalMessage, ?>, Object> extensionMap = new TreeMap<Extension<ExternalMessage, ?>, Object>();

      public Float f;

      public Builder() {
      }

      public Builder(ExternalMessage message) {
        this.f = message.f;
        this.extensionMap.putAll(message.extensionMap);
      }

      public Builder f(Float f) {
        this.f = f;
        return this;
      }

      @Override
      public <Type> Type getExtension(Extension<ExternalMessage, Type> extension) {
        return (Type) extensionMap.get(extension);
      }

      @Override
      public <Type> Builder setExtension(Extension<ExternalMessage, Type> extension, Type value) {
        extensionMap.put(extension, value);
        return this;
      }

      @Override
      public boolean isInitialized() {
        return true;
      }

      @Override
      public ExternalMessage build() {
        if (!isInitialized()) throw new UninitializedMessageException();
        return new ExternalMessage(this);
      }
    }
  }
}
