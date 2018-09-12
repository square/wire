package com.squareup.wire;

import java.io.IOException;
import okio.Buffer;
import okio.ByteString;

public class UnknownFieldsBuilder {
  public static final Object UNKNOWN_FIELD = new Object();

  ByteString byteString = ByteString.EMPTY;
  Buffer buffer = null;
  ProtoWriter writer = null;

  UnknownFieldsBuilder() {
    prepareUnknownFields();
  }

  private void prepareUnknownFields() {
    if (buffer == null) {
      buffer = new Buffer();
      writer = new ProtoWriter(buffer);
      try {
        writer.writeBytes(byteString);
      } catch (IOException _) {
        // Impossible; we’re not doing I/O.
      }
      byteString = ByteString.EMPTY;
    }
  }

  private void addUnknownField(int tag, FieldEncoding fieldEncoding, Object value) {
    ProtoAdapter protoAdapter = fieldEncoding.rawProtoAdapter();
    try {
      protoAdapter.encodeWithTag(writer, tag, value);
    } catch (IOException _) {
      // Impossible; we’re not doing I/O.
    }
  }

  private ByteString buildUnknownField() {
    if (buffer != null) {
      byteString = buffer.readByteString();
      buffer = null;
      writer = null;
    }
    return byteString;
  }

  public static ByteString decodeMessage(ProtoReader reader, TagHandler tagHandler)
    throws IOException {
    UnknownFieldsBuilder unknownFieldsBuilder = new UnknownFieldsBuilder();
    long token = reader.beginMessage();
    while (true) {
      int tag = reader.nextTag();
      if (tag == -1) break;
      if (tagHandler.decodeMessage(tag) == UnknownFieldsBuilder.UNKNOWN_FIELD) {
        FieldEncoding fieldEncoding = reader.peekFieldEncoding();
        Object value = fieldEncoding.rawProtoAdapter().decode(reader);
        unknownFieldsBuilder.addUnknownField(tag, fieldEncoding, value);
      }
    }
    reader.endMessage(token);
    return unknownFieldsBuilder.buildUnknownField();
  }

  public interface TagHandler {
    Object decodeMessage(int tagNumber);
  }
}
