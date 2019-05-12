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
import kotlin.jvm.JvmClassMappingKt;
import okio.ByteString;

/**
 * The protocol compiler can output a FileDescriptorSet containing the .proto
 * files it parses.
 */
public final class FileDescriptorSet extends Message<FileDescriptorSet, FileDescriptorSet.Builder> {
  public static final ProtoAdapter<FileDescriptorSet> ADAPTER = new ProtoAdapter_FileDescriptorSet();

  private static final long serialVersionUID = 0L;

  @WireField(
      tag = 1,
      adapter = "com.google.protobuf.FileDescriptorProto#ADAPTER",
      label = WireField.Label.REPEATED
  )
  public final List<FileDescriptorProto> file;

  public FileDescriptorSet(List<FileDescriptorProto> file) {
    this(file, ByteString.EMPTY);
  }

  public FileDescriptorSet(List<FileDescriptorProto> file, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.file = Internal.immutableCopyOf("file", file);
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.file = Internal.copyOf("file", file);
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof FileDescriptorSet)) return false;
    FileDescriptorSet o = (FileDescriptorSet) other;
    return unknownFields().equals(o.unknownFields())
        && file.equals(o.file);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + file.hashCode();
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (!file.isEmpty()) builder.append(", file=").append(file);
    return builder.replace(0, 2, "FileDescriptorSet{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<FileDescriptorSet, Builder> {
    public List<FileDescriptorProto> file;

    public Builder() {
      file = Internal.newMutableList();
    }

    public Builder file(List<FileDescriptorProto> file) {
      Internal.checkElementsNotNull(file);
      this.file = file;
      return this;
    }

    @Override
    public FileDescriptorSet build() {
      return new FileDescriptorSet(file, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_FileDescriptorSet extends ProtoAdapter<FileDescriptorSet> {
    public ProtoAdapter_FileDescriptorSet() {
      super(FieldEncoding.LENGTH_DELIMITED, JvmClassMappingKt.getKotlinClass(FileDescriptorSet.class));
    }

    @Override
    public int encodedSize(FileDescriptorSet value) {
      return FileDescriptorProto.ADAPTER.asRepeated().encodedSizeWithTag(1, value.file)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, FileDescriptorSet value) throws IOException {
      FileDescriptorProto.ADAPTER.asRepeated().encodeWithTag(writer, 1, value.file);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public FileDescriptorSet decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.file.add(FileDescriptorProto.ADAPTER.decode(reader)); break;
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
    public FileDescriptorSet redact(FileDescriptorSet value) {
      Builder builder = value.newBuilder();
      Internal.redactElements(builder.file, FileDescriptorProto.ADAPTER);
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
