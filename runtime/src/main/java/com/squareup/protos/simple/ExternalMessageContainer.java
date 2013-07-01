/**
 * Code generated by Square Omar protobuf compiler, do not edit.
 * Source file: runtime/src/main/proto/external_message.proto
 */
package com.squareup.protos.simple;

import com.squareup.omar.ExtensionRegistry;
import com.squareup.omar.Message;
import com.squareup.omar.Omar;
import com.squareup.omar.ProtoField;
import com.squareup.omar.UninitializedMessageException;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class ExternalMessageContainer {

  private ExternalMessageContainer() {
  }

  public static void registerAllExtensions(ExtensionRegistry registry) {
  }

  public static final class ExternalMessage
      implements Message.ExtendableMessage<ExternalMessage> {

    public final Map<Extension<ExternalMessage, ?>, Object> extensionMap;

    public static final Float f_default = 20F;

    @ProtoField(
      tag = 1,
      type = Omar.FLOAT,
      defaultValue = "20F"
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
      if (!Omar.equals(f, o.f)) return false;
      return true;
    }

    @Override
    public int hashCode() {
      return f != null ? f.hashCode() : 0;
    }

    @Override
    public String toString() {
      return String.format("ExternalMessage{" +
          "f=%s}",
          f);
    }

    public static final class Builder
        implements ExtendableMessage.ExtendableBuilder<ExternalMessage> {

      private final Map<Extension<ExternalMessage, ?>, Object> extensionMap =
          new TreeMap<Extension<ExternalMessage, ?>, Object>();

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
