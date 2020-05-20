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
import com.squareup.wire.protos.custom_options.FooBar;
import java.io.IOException;
import java.lang.Boolean;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.util.List;
import okio.ByteString;

public final class EnumValueOptions extends Message<EnumValueOptions, EnumValueOptions.Builder> {
  public static final ProtoAdapter<EnumValueOptions> ADAPTER = new ProtoAdapter_EnumValueOptions();

  private static final long serialVersionUID = 0L;

  public static final Boolean DEFAULT_DEPRECATED = false;

  public static final Integer DEFAULT_ENUM_VALUE_OPTION = 0;

  public static final Boolean DEFAULT_FOREIGN_ENUM_VALUE_OPTION = false;

  public static final Integer DEFAULT_TEXT_ALIGNMENT = 0;

  /**
   * Is this enum value deprecated?
   * Depending on the target platform, this can emit Deprecated annotations
   * for the enum value, or it will be completely ignored; in the very least,
   * this is a formalization for deprecating enum values.
   */
  @WireField(
      tag = 1,
      adapter = "com.squareup.wire.ProtoAdapter#BOOL"
  )
  public final Boolean deprecated;

  /**
   * The parser stores options it doesn't recognize here. See above.
   */
  @WireField(
      tag = 999,
      adapter = "com.google.protobuf.UninterpretedOption#ADAPTER",
      label = WireField.Label.REPEATED,
      encodeMode = WireField.EncodeMode.REPEATED
  )
  public final List<UninterpretedOption> uninterpreted_option;

  /**
   * Extension source: custom_options.proto
   */
  @WireField(
      tag = 70000,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  public final Integer enum_value_option;

  /**
   * Extension source: custom_options.proto
   */
  @WireField(
      tag = 70001,
      adapter = "com.squareup.wire.protos.custom_options.FooBar$More#ADAPTER"
  )
  public final FooBar.More complex_enum_value_option;

  /**
   * Extension source: foreign.proto
   */
  @WireField(
      tag = 70002,
      adapter = "com.squareup.wire.ProtoAdapter#BOOL"
  )
  public final Boolean foreign_enum_value_option;

  /**
   * Extension source: options.proto
   */
  @WireField(
      tag = 54000,
      adapter = "com.squareup.wire.ProtoAdapter#INT32"
  )
  public final Integer text_alignment;

  public EnumValueOptions(Boolean deprecated, List<UninterpretedOption> uninterpreted_option,
      Integer enum_value_option, FooBar.More complex_enum_value_option,
      Boolean foreign_enum_value_option, Integer text_alignment) {
    this(deprecated, uninterpreted_option, enum_value_option, complex_enum_value_option, foreign_enum_value_option, text_alignment, ByteString.EMPTY);
  }

  public EnumValueOptions(Boolean deprecated, List<UninterpretedOption> uninterpreted_option,
      Integer enum_value_option, FooBar.More complex_enum_value_option,
      Boolean foreign_enum_value_option, Integer text_alignment, ByteString unknownFields) {
    super(ADAPTER, unknownFields);
    this.deprecated = deprecated;
    this.uninterpreted_option = Internal.immutableCopyOf("uninterpreted_option", uninterpreted_option);
    this.enum_value_option = enum_value_option;
    this.complex_enum_value_option = complex_enum_value_option;
    this.foreign_enum_value_option = foreign_enum_value_option;
    this.text_alignment = text_alignment;
  }

  @Override
  public Builder newBuilder() {
    Builder builder = new Builder();
    builder.deprecated = deprecated;
    builder.uninterpreted_option = Internal.copyOf(uninterpreted_option);
    builder.enum_value_option = enum_value_option;
    builder.complex_enum_value_option = complex_enum_value_option;
    builder.foreign_enum_value_option = foreign_enum_value_option;
    builder.text_alignment = text_alignment;
    builder.addUnknownFields(unknownFields());
    return builder;
  }

  @Override
  public boolean equals(Object other) {
    if (other == this) return true;
    if (!(other instanceof EnumValueOptions)) return false;
    EnumValueOptions o = (EnumValueOptions) other;
    return unknownFields().equals(o.unknownFields())
        && Internal.equals(deprecated, o.deprecated)
        && uninterpreted_option.equals(o.uninterpreted_option)
        && Internal.equals(enum_value_option, o.enum_value_option)
        && Internal.equals(complex_enum_value_option, o.complex_enum_value_option)
        && Internal.equals(foreign_enum_value_option, o.foreign_enum_value_option)
        && Internal.equals(text_alignment, o.text_alignment);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode;
    if (result == 0) {
      result = unknownFields().hashCode();
      result = result * 37 + (deprecated != null ? deprecated.hashCode() : 0);
      result = result * 37 + uninterpreted_option.hashCode();
      result = result * 37 + (enum_value_option != null ? enum_value_option.hashCode() : 0);
      result = result * 37 + (complex_enum_value_option != null ? complex_enum_value_option.hashCode() : 0);
      result = result * 37 + (foreign_enum_value_option != null ? foreign_enum_value_option.hashCode() : 0);
      result = result * 37 + (text_alignment != null ? text_alignment.hashCode() : 0);
      super.hashCode = result;
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    if (deprecated != null) builder.append(", deprecated=").append(deprecated);
    if (!uninterpreted_option.isEmpty()) builder.append(", uninterpreted_option=").append(uninterpreted_option);
    if (enum_value_option != null) builder.append(", enum_value_option=").append(enum_value_option);
    if (complex_enum_value_option != null) builder.append(", complex_enum_value_option=").append(complex_enum_value_option);
    if (foreign_enum_value_option != null) builder.append(", foreign_enum_value_option=").append(foreign_enum_value_option);
    if (text_alignment != null) builder.append(", text_alignment=").append(text_alignment);
    return builder.replace(0, 2, "EnumValueOptions{").append('}').toString();
  }

  public static final class Builder extends Message.Builder<EnumValueOptions, Builder> {
    public Boolean deprecated;

    public List<UninterpretedOption> uninterpreted_option;

    public Integer enum_value_option;

    public FooBar.More complex_enum_value_option;

    public Boolean foreign_enum_value_option;

    public Integer text_alignment;

    public Builder() {
      uninterpreted_option = Internal.newMutableList();
    }

    /**
     * Is this enum value deprecated?
     * Depending on the target platform, this can emit Deprecated annotations
     * for the enum value, or it will be completely ignored; in the very least,
     * this is a formalization for deprecating enum values.
     */
    public Builder deprecated(Boolean deprecated) {
      this.deprecated = deprecated;
      return this;
    }

    /**
     * The parser stores options it doesn't recognize here. See above.
     */
    public Builder uninterpreted_option(List<UninterpretedOption> uninterpreted_option) {
      Internal.checkElementsNotNull(uninterpreted_option);
      this.uninterpreted_option = uninterpreted_option;
      return this;
    }

    public Builder enum_value_option(Integer enum_value_option) {
      this.enum_value_option = enum_value_option;
      return this;
    }

    public Builder complex_enum_value_option(FooBar.More complex_enum_value_option) {
      this.complex_enum_value_option = complex_enum_value_option;
      return this;
    }

    public Builder foreign_enum_value_option(Boolean foreign_enum_value_option) {
      this.foreign_enum_value_option = foreign_enum_value_option;
      return this;
    }

    public Builder text_alignment(Integer text_alignment) {
      this.text_alignment = text_alignment;
      return this;
    }

    @Override
    public EnumValueOptions build() {
      return new EnumValueOptions(deprecated, uninterpreted_option, enum_value_option, complex_enum_value_option, foreign_enum_value_option, text_alignment, super.buildUnknownFields());
    }
  }

  private static final class ProtoAdapter_EnumValueOptions extends ProtoAdapter<EnumValueOptions> {
    public ProtoAdapter_EnumValueOptions() {
      super(FieldEncoding.LENGTH_DELIMITED, EnumValueOptions.class, "type.googleapis.com/google.protobuf.EnumValueOptions");
    }

    @Override
    public int encodedSize(EnumValueOptions value) {
      return ProtoAdapter.BOOL.encodedSizeWithTag(1, value.deprecated)
          + UninterpretedOption.ADAPTER.asRepeated().encodedSizeWithTag(999, value.uninterpreted_option)
          + ProtoAdapter.INT32.encodedSizeWithTag(70000, value.enum_value_option)
          + FooBar.More.ADAPTER.encodedSizeWithTag(70001, value.complex_enum_value_option)
          + ProtoAdapter.BOOL.encodedSizeWithTag(70002, value.foreign_enum_value_option)
          + ProtoAdapter.INT32.encodedSizeWithTag(54000, value.text_alignment)
          + value.unknownFields().size();
    }

    @Override
    public void encode(ProtoWriter writer, EnumValueOptions value) throws IOException {
      ProtoAdapter.BOOL.encodeWithTag(writer, 1, value.deprecated);
      UninterpretedOption.ADAPTER.asRepeated().encodeWithTag(writer, 999, value.uninterpreted_option);
      ProtoAdapter.INT32.encodeWithTag(writer, 70000, value.enum_value_option);
      FooBar.More.ADAPTER.encodeWithTag(writer, 70001, value.complex_enum_value_option);
      ProtoAdapter.BOOL.encodeWithTag(writer, 70002, value.foreign_enum_value_option);
      ProtoAdapter.INT32.encodeWithTag(writer, 54000, value.text_alignment);
      writer.writeBytes(value.unknownFields());
    }

    @Override
    public EnumValueOptions decode(ProtoReader reader) throws IOException {
      Builder builder = new Builder();
      long token = reader.beginMessage();
      for (int tag; (tag = reader.nextTag()) != -1;) {
        switch (tag) {
          case 1: builder.deprecated(ProtoAdapter.BOOL.decode(reader)); break;
          case 999: builder.uninterpreted_option.add(UninterpretedOption.ADAPTER.decode(reader)); break;
          case 54000: builder.text_alignment(ProtoAdapter.INT32.decode(reader)); break;
          case 70000: builder.enum_value_option(ProtoAdapter.INT32.decode(reader)); break;
          case 70001: builder.complex_enum_value_option(FooBar.More.ADAPTER.decode(reader)); break;
          case 70002: builder.foreign_enum_value_option(ProtoAdapter.BOOL.decode(reader)); break;
          default: {
            reader.readUnknownField(tag);
          }
        }
      }
      builder.addUnknownFields(reader.endMessageAndGetUnknownFields(token));
      return builder.build();
    }

    @Override
    public EnumValueOptions redact(EnumValueOptions value) {
      Builder builder = value.newBuilder();
      Internal.redactElements(builder.uninterpreted_option, UninterpretedOption.ADAPTER);
      if (builder.complex_enum_value_option != null) builder.complex_enum_value_option = FooBar.More.ADAPTER.redact(builder.complex_enum_value_option);
      builder.clearUnknownFields();
      return builder.build();
    }
  }
}
