// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: google/protobuf/descriptor.proto
package com.google.protobuf;

import com.squareup.wire.FieldEncoding;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoAdapter;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.ProtoWriter;
import com.squareup.wire.WireField;
import com.squareup.wire.internal.Internal;
import java.io.IOException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.List;
import okio.ByteString;

/**
 * Describes a service.
 */
public final class ServiceDescriptorProto extends Message<ServiceDescriptorProto, ServiceDescriptorProto.Builder> {
  public static final ProtoAdapter<ServiceDescriptorProto> ADAPTER = new ProtoAdapter_ServiceDescriptorProto();

  private static final long serialVersionUID = 0L;

  public static final String DEFAULT_NAME = "";

  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String name;

  @WireField(
      tag = 2,
      adapter = "com.google.protobuf.MethodDescriptorProto#ADAPTER",
      label = WireField.Label.REPEATED
  )
  public final List<MethodDescriptorProto> method;

  @WireField(
      tag = 3,
      adapter = "com.google.protobuf.ServiceOptions#ADAPTER"
  )
  public final ServiceOptions options;

  public ServiceDescriptorProto(String name, List<MethodDescriptorProto> method,
      ServiceOptions options) {
    this(name, method, options, ByteString.EMPTY);
  }

  public ServiceDescriptorProto(String name, List<MethodDescriptorProto> method,
      ServiceOptions options, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.name = name;
    this.method = Internal.immutableCopyOf("method", method);
    this.options = options;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.name = name;
    builder.method = Internal.copyOf(method);
    builder.options = options;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof ServiceDescriptorProto)) return false;
    ServiceDescriptorProto o = (ServiceDescriptorProto) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(name, o.name)
        && method.equals(o.method)
        && Internal.equals(options, o.options);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (name != null ? name.hashCode() : 0);
      result = result * 37 + method.hashCode();
      result = result * 37 + (options != null ? options.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (name != null) builder.append(", name=").append(Internal.sanitize(name));
    if (!method.isEmpty()) builder.append(", method=").append(method);
    if (options != null) builder.append(", options=").append(options);
    return builder.replace(0, 2, "ServiceDescriptorProto{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<ServiceDescriptorProto, Builder> {
    public String name;

    public List<MethodDescriptorProto> method;

    public ServiceOptions options;

    public Builder() {
      method = Internal.newMutableList();
    }

    public Builder name(String name) {
      this.name = name;
      return this;
    }

    public Builder method(List<MethodDescriptorProto> method) {
      Internal.checkElementsNotNull(method);
      this.method = method;
      return this;
    }

    public Builder options(ServiceOptions options) {
      this.options = options;
      return this;
    }

    @Override
    public ServiceDescriptorProto build() {
      return new ServiceDescriptorProto(name, method, options, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_ServiceDescriptorProto extends ProtoAdapter<ServiceDescriptorProto> {
    public ProtoAdapter_ServiceDescriptorProto() {
      super(FieldEncoding.LENGTH_DELIMITED, ServiceDescriptorProto.class, "type.googleapis.com/google.protobuf.ServiceDescriptorProto");
    }

    @Override
    public int encodedSize(ServiceDescriptorProto value) {
      return ProtoAdapter.STRING.encodedSizeWithTag(1, value.name)
          + MethodDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(2, value.method)
          + ServiceOptions.ADAPTER.encodedSizeWithTag(3, value.options)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, ServiceDescriptorProto value) throws IOException {
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name);
      MethodDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 2, value.method);
      ServiceOptions.ADAPTER.encodeWithTag(writer, 3, value.options);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public ServiceDescriptorProto decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.name(ProtoAdapter.STRING.decode(reader)); break;
          case 2: builder.method.add(MethodDescriptorProto.ADAPTER.decode(reader)); break;
          case 3: builder.options(ServiceOptions.ADAPTER.decode(reader)); break;
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public ServiceDescriptorProto redact(ServiceDescriptorProto value) {
      Builder builder = value.newBuilder();
      Internal.redactElements(builder.method, MethodDescriptorProto.ADAPTER);
      if (builder.options != null) builder.options = ServiceOptions.ADAPTER.redact(builder.options);
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
