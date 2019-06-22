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

public final class ExtensionRangeOptions extends Message<ExtensionRangeOptions, ExtensionRangeOptions.Builder> {
  public static final ProtoAdapter<ExtensionRangeOptions> ADAPTER = new ProtoAdapter_ExtensionRangeOptions();

  private static final long serialVersionUID = 0L;

  /**
   * The parser stores options it doesn't recognize here. See above.
   */
  @WireField(
      tag = 999,
      adapter = "com.google.protobuf.UninterpretedOption#ADAPTER",
      label = WireField.Label.REPEATED
  )
  public final List<UninterpretedOption> uninterpreted_option;

  public ExtensionRangeOptions(List<UninterpretedOption> uninterpreted_option) {
    this(uninterpreted_option, ByteString.EMPTY);
  }

  public ExtensionRangeOptions(List<UninterpretedOption> uninterpreted_option,
      ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.uninterpreted_option = Internal.immutableCopyOf("uninterpreted_option", uninterpreted_option);
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.uninterpreted_option = Internal.copyOf("uninterpreted_option", uninterpreted_option);
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof ExtensionRangeOptions)) return false;
    ExtensionRangeOptions o = (ExtensionRangeOptions) other;
    return unknownFields().equals(o.unknownFields())
        && uninterpreted_option.equals(o.uninterpreted_option);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + uninterpreted_option.hashCode();
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (!uninterpreted_option.isEmpty()) builder.append(", uninterpreted_option=").append(uninterpreted_option);
    return builder.replace(0, 2, "ExtensionRangeOptions{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<ExtensionRangeOptions, Builder> {
    public List<UninterpretedOption> uninterpreted_option;

    public Builder() {
      uninterpreted_option = Internal.newMutableList();
    }

    /**
     * The parser stores options it doesn't recognize here. See above.
     */
    public Builder uninterpreted_option(List<UninterpretedOption> uninterpreted_option) {
      Internal.checkElementsNotNull(uninterpreted_option);
      this.uninterpreted_option = uninterpreted_option;
      return this;
    }

    @Override
    public ExtensionRangeOptions build() {
      return new ExtensionRangeOptions(uninterpreted_option, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_ExtensionRangeOptions extends ProtoAdapter<ExtensionRangeOptions> {
    public ProtoAdapter_ExtensionRangeOptions() {
      super(FieldEncoding.LENGTH_DELIMITED, JvmClassMappingKt.getKotlinClass(ExtensionRangeOptions.class));
    }

    @Override
    public int encodedSize(ExtensionRangeOptions value) {
      return UninterpretedOption.ADAPTER.asRepeated().encodedSizeWithTag(999, value.uninterpreted_option)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, ExtensionRangeOptions value) throws IOException {
      UninterpretedOption.ADAPTER.asRepeated().encodeWithTag(writer, 999, value.uninterpreted_option);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public ExtensionRangeOptions decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 999: builder.uninterpreted_option.add(UninterpretedOption.ADAPTER.decode(reader)); break;
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public ExtensionRangeOptions redact(ExtensionRangeOptions value) {
      Builder builder = value.newBuilder();
      Internal.redactElements(builder.uninterpreted_option, UninterpretedOption.ADAPTER);
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
