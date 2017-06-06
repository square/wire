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
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.List;
import okio.ByteString;

/**
 * Describes a complete .proto file.
 */
public final class FileDescriptorProto extends Message<FileDescriptorProto, FileDescriptorProto.Builder> {
  public static final ProtoAdapter<FileDescriptorProto> ADAPTER = new ProtoAdapter_FileDescriptorProto();

  private static final long serialVersionUID = 0L;

  public static final String DEFAULT_NAME = "";

  public static final String DEFAULT_PACKAGE_ = "";

  public static final String DEFAULT_SYNTAX = "";

  /**
   * file name, relative to root of source tree
   */
  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String name;

  /**
   * e.g. "foo", "foo.bar", etc.
   */
  @WireField(
      tag = 2,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String package_;

  /**
   * Names of files imported by this file.
   */
  @WireField(
      tag = 3,
      adapter = "com.squareup.wire.ProtoAdapter#STRING",
      label = WireField.Label.REPEATED
  )
  public final List<String> dependency;

  /**
   * Indexes of the public imported files in the dependency list above.
   */
  @WireField(
      tag = 10,
      adapter = "com.squareup.wire.ProtoAdapter#INT32",
      label = WireField.Label.REPEATED
  )
  public final List<Integer> public_dependency;

  /**
   * Indexes of the weak imported files in the dependency list.
   * For Google-internal migration only. Do not use.
   */
  @WireField(
      tag = 11,
      adapter = "com.squareup.wire.ProtoAdapter#INT32",
      label = WireField.Label.REPEATED
  )
  public final List<Integer> weak_dependency;

  /**
   * All top-level definitions in this file.
   */
  @WireField(
      tag = 4,
      adapter = "com.google.protobuf.DescriptorProto#ADAPTER",
      label = WireField.Label.REPEATED
  )
  public final List<DescriptorProto> message_type;

  @WireField(
      tag = 5,
      adapter = "com.google.protobuf.EnumDescriptorProto#ADAPTER",
      label = WireField.Label.REPEATED
  )
  public final List<EnumDescriptorProto> enum_type;

  @WireField(
      tag = 6,
      adapter = "com.google.protobuf.ServiceDescriptorProto#ADAPTER",
      label = WireField.Label.REPEATED
  )
  public final List<ServiceDescriptorProto> service;

  @WireField(
      tag = 7,
      adapter = "com.google.protobuf.FieldDescriptorProto#ADAPTER",
      label = WireField.Label.REPEATED
  )
  public final List<FieldDescriptorProto> extension;

  @WireField(
      tag = 8,
      adapter = "com.google.protobuf.FileOptions#ADAPTER"
  )
  public final FileOptions options;

  /**
   * This field contains optional information about the original source code.
   * You may safely remove this entire field without harming runtime
   * functionality of the descriptors -- the information is needed only by
   * development tools.
   */
  @WireField(
      tag = 9,
      adapter = "com.google.protobuf.SourceCodeInfo#ADAPTER"
  )
  public final SourceCodeInfo source_code_info;

  /**
   * The syntax of the proto file.
   * The supported values are "proto2" and "proto3".
   */
  @WireField(
      tag = 12,
      adapter = "com.squareup.wire.ProtoAdapter#STRING"
  )
  public final String syntax;

  public FileDescriptorProto(String name, String package_, List<String> dependency,
      List<Integer> public_dependency, List<Integer> weak_dependency,
      List<DescriptorProto> message_type, List<EnumDescriptorProto> enum_type,
      List<ServiceDescriptorProto> service, List<FieldDescriptorProto> extension,
      FileOptions options, SourceCodeInfo source_code_info, String syntax) {
    this(name, package_, dependency, public_dependency, weak_dependency, message_type, enum_type, service, extension, options, source_code_info, syntax, ByteString.EMPTY);
  }

  public FileDescriptorProto(String name, String package_, List<String> dependency,
      List<Integer> public_dependency, List<Integer> weak_dependency,
      List<DescriptorProto> message_type, List<EnumDescriptorProto> enum_type,
      List<ServiceDescriptorProto> service, List<FieldDescriptorProto> extension,
      FileOptions options, SourceCodeInfo source_code_info, String syntax,
      ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.name = name;
    this.package_ = package_;
    this.dependency = Internal.immutableCopyOf("dependency", dependency);
    this.public_dependency = Internal.immutableCopyOf("public_dependency", public_dependency);
    this.weak_dependency = Internal.immutableCopyOf("weak_dependency", weak_dependency);
    this.message_type = Internal.immutableCopyOf("message_type", message_type);
    this.enum_type = Internal.immutableCopyOf("enum_type", enum_type);
    this.service = Internal.immutableCopyOf("service", service);
    this.extension = Internal.immutableCopyOf("extension", extension);
    this.options = options;
    this.source_code_info = source_code_info;
    this.syntax = syntax;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.name = name;
    builder.package_ = package_;
    builder.dependency = Internal.copyOf("dependency", dependency);
    builder.public_dependency = Internal.copyOf("public_dependency", public_dependency);
    builder.weak_dependency = Internal.copyOf("weak_dependency", weak_dependency);
    builder.message_type = Internal.copyOf("message_type", message_type);
    builder.enum_type = Internal.copyOf("enum_type", enum_type);
    builder.service = Internal.copyOf("service", service);
    builder.extension = Internal.copyOf("extension", extension);
    builder.options = options;
    builder.source_code_info = source_code_info;
    builder.syntax = syntax;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof FileDescriptorProto)) return false;
    FileDescriptorProto o = (FileDescriptorProto) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(name, o.name)
        && Internal.equals(package_, o.package_)
        && dependency.equals(o.dependency)
        && public_dependency.equals(o.public_dependency)
        && weak_dependency.equals(o.weak_dependency)
        && message_type.equals(o.message_type)
        && enum_type.equals(o.enum_type)
        && service.equals(o.service)
        && extension.equals(o.extension)
        && Internal.equals(options, o.options)
        && Internal.equals(source_code_info, o.source_code_info)
        && Internal.equals(syntax, o.syntax);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (name != null ? name.hashCode() : 0);
      result = result * 37 + (package_ != null ? package_.hashCode() : 0);
      result = result * 37 + dependency.hashCode();
      result = result * 37 + public_dependency.hashCode();
      result = result * 37 + weak_dependency.hashCode();
      result = result * 37 + message_type.hashCode();
      result = result * 37 + enum_type.hashCode();
      result = result * 37 + service.hashCode();
      result = result * 37 + extension.hashCode();
      result = result * 37 + (options != null ? options.hashCode() : 0);
      result = result * 37 + (source_code_info != null ? source_code_info.hashCode() : 0);
      result = result * 37 + (syntax != null ? syntax.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (name != null) builder.append(", name=").append(name);
    if (package_ != null) builder.append(", package=").append(package_);
    if (!dependency.isEmpty()) builder.append(", dependency=").append(dependency);
    if (!public_dependency.isEmpty()) builder.append(", public_dependency=").append(public_dependency);
    if (!weak_dependency.isEmpty()) builder.append(", weak_dependency=").append(weak_dependency);
    if (!message_type.isEmpty()) builder.append(", message_type=").append(message_type);
    if (!enum_type.isEmpty()) builder.append(", enum_type=").append(enum_type);
    if (!service.isEmpty()) builder.append(", service=").append(service);
    if (!extension.isEmpty()) builder.append(", extension=").append(extension);
    if (options != null) builder.append(", options=").append(options);
    if (source_code_info != null) builder.append(", source_code_info=").append(source_code_info);
    if (syntax != null) builder.append(", syntax=").append(syntax);
    return builder.replace(0, 2, "FileDescriptorProto{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<FileDescriptorProto, Builder> {
    public String name;

    public String package_;

    public List<String> dependency;

    public List<Integer> public_dependency;

    public List<Integer> weak_dependency;

    public List<DescriptorProto> message_type;

    public List<EnumDescriptorProto> enum_type;

    public List<ServiceDescriptorProto> service;

    public List<FieldDescriptorProto> extension;

    public FileOptions options;

    public SourceCodeInfo source_code_info;

    public String syntax;

    public Builder() {
      dependency = Internal.newMutableList();
      public_dependency = Internal.newMutableList();
      weak_dependency = Internal.newMutableList();
      message_type = Internal.newMutableList();
      enum_type = Internal.newMutableList();
      service = Internal.newMutableList();
      extension = Internal.newMutableList();
    }

    public Builder(boolean useDefaults) {
      this();
      if (useDefaults) {
        name = DEFAULT_NAME;
        package_ = DEFAULT_PACKAGE_;
        syntax = DEFAULT_SYNTAX;
      }
    }

    /**
     * file name, relative to root of source tree
     */
    public Builder name(String name) {
      this.name = name;
      return this;
    }

    /**
     * e.g. "foo", "foo.bar", etc.
     */
    public Builder package_(String package_) {
      this.package_ = package_;
      return this;
    }

    /**
     * Names of files imported by this file.
     */
    public Builder dependency(List<String> dependency) {
      Internal.checkElementsNotNull(dependency);
      this.dependency = dependency;
      return this;
    }

    /**
     * Indexes of the public imported files in the dependency list above.
     */
    public Builder public_dependency(List<Integer> public_dependency) {
      Internal.checkElementsNotNull(public_dependency);
      this.public_dependency = public_dependency;
      return this;
    }

    /**
     * Indexes of the weak imported files in the dependency list.
     * For Google-internal migration only. Do not use.
     */
    public Builder weak_dependency(List<Integer> weak_dependency) {
      Internal.checkElementsNotNull(weak_dependency);
      this.weak_dependency = weak_dependency;
      return this;
    }

    /**
     * All top-level definitions in this file.
     */
    public Builder message_type(List<DescriptorProto> message_type) {
      Internal.checkElementsNotNull(message_type);
      this.message_type = message_type;
      return this;
    }

    public Builder enum_type(List<EnumDescriptorProto> enum_type) {
      Internal.checkElementsNotNull(enum_type);
      this.enum_type = enum_type;
      return this;
    }

    public Builder service(List<ServiceDescriptorProto> service) {
      Internal.checkElementsNotNull(service);
      this.service = service;
      return this;
    }

    public Builder extension(List<FieldDescriptorProto> extension) {
      Internal.checkElementsNotNull(extension);
      this.extension = extension;
      return this;
    }

    public Builder options(FileOptions options) {
      this.options = options;
      return this;
    }

    /**
     * This field contains optional information about the original source code.
     * You may safely remove this entire field without harming runtime
     * functionality of the descriptors -- the information is needed only by
     * development tools.
     */
    public Builder source_code_info(SourceCodeInfo source_code_info) {
      this.source_code_info = source_code_info;
      return this;
    }

    /**
     * The syntax of the proto file.
     * The supported values are "proto2" and "proto3".
     */
    public Builder syntax(String syntax) {
      this.syntax = syntax;
      return this;
    }

    @Override
    public FileDescriptorProto build() {
      return new FileDescriptorProto(name, package_, dependency, public_dependency, weak_dependency, message_type, enum_type, service, extension, options, source_code_info, syntax, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_FileDescriptorProto extends ProtoAdapter<FileDescriptorProto> {
    public ProtoAdapter_FileDescriptorProto() {
      super(FieldEncoding.LENGTH_DELIMITED, FileDescriptorProto.class);
    }

    @Override
    public int encodedSize(FileDescriptorProto value) {
      return ProtoAdapter.STRING.encodedSizeWithTag(1, value.name)
          + ProtoAdapter.STRING.encodedSizeWithTag(2, value.package_)
          + ProtoAdapter.STRING.asRepeated().encodedSizeWithTag(3, value.dependency)
          + ProtoAdapter.INT32.asRepeated().encodedSizeWithTag(10, value.public_dependency)
          + ProtoAdapter.INT32.asRepeated().encodedSizeWithTag(11, value.weak_dependency)
          + DescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(4, value.message_type)
          + EnumDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(5, value.enum_type)
          + ServiceDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(6, value.service)
          + FieldDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(7, value.extension)
          + FileOptions.ADAPTER.encodedSizeWithTag(8, value.options)
          + SourceCodeInfo.ADAPTER.encodedSizeWithTag(9, value.source_code_info)
          + ProtoAdapter.STRING.encodedSizeWithTag(12, value.syntax)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, FileDescriptorProto value) throws IOException {
      ProtoAdapter.STRING.encodeWithTag(writer, 1, value.name);
      ProtoAdapter.STRING.encodeWithTag(writer, 2, value.package_);
      ProtoAdapter.STRING.asRepeated().encodeWithTag(writer, 3, value.dependency);
      ProtoAdapter.INT32.asRepeated().encodeWithTag(writer, 10, value.public_dependency);
      ProtoAdapter.INT32.asRepeated().encodeWithTag(writer, 11, value.weak_dependency);
      DescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 4, value.message_type);
      EnumDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 5, value.enum_type);
      ServiceDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 6, value.service);
      FieldDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 7, value.extension);
      FileOptions.ADAPTER.encodeWithTag(writer, 8, value.options);
      SourceCodeInfo.ADAPTER.encodeWithTag(writer, 9, value.source_code_info);
      ProtoAdapter.STRING.encodeWithTag(writer, 12, value.syntax);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public FileDescriptorProto decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.name(ProtoAdapter.STRING.decode(reader)); break;
          case 2: builder.package_(ProtoAdapter.STRING.decode(reader)); break;
          case 3: builder.dependency.add(ProtoAdapter.STRING.decode(reader)); break;
          case 4: builder.message_type.add(DescriptorProto.ADAPTER.decode(reader)); break;
          case 5: builder.enum_type.add(EnumDescriptorProto.ADAPTER.decode(reader)); break;
          case 6: builder.service.add(ServiceDescriptorProto.ADAPTER.decode(reader)); break;
          case 7: builder.extension.add(FieldDescriptorProto.ADAPTER.decode(reader)); break;
          case 8: builder.options(FileOptions.ADAPTER.decode(reader)); break;
          case 9: builder.source_code_info(SourceCodeInfo.ADAPTER.decode(reader)); break;
          case 10: builder.public_dependency.add(ProtoAdapter.INT32.decode(reader)); break;
          case 11: builder.weak_dependency.add(ProtoAdapter.INT32.decode(reader)); break;
          case 12: builder.syntax(ProtoAdapter.STRING.decode(reader)); break;
          default: {
            FieldEncoding fieldEncoding = reader.peekFieldEncoding();
            Object value = fieldEncoding.rawProtoAdapter().decode(reader);
            builder.addUnknownField(tag, fieldEncoding, value);
          }
        }
      }
      reader.endMessage(token);
      return builder.build();
    }

    @Override
    public FileDescriptorProto redact(FileDescriptorProto value) {
      Builder builder = value.newBuilder();
      Internal.redactElements(builder.message_type, DescriptorProto.ADAPTER);
      Internal.redactElements(builder.enum_type, EnumDescriptorProto.ADAPTER);
      Internal.redactElements(builder.service, ServiceDescriptorProto.ADAPTER);
      Internal.redactElements(builder.extension, FieldDescriptorProto.ADAPTER);
      if (builder.options != null) builder.options = FileOptions.ADAPTER.redact(builder.options);
      if (builder.source_code_info != null) builder.source_code_info = SourceCodeInfo.ADAPTER.redact(builder.source_code_info);
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
