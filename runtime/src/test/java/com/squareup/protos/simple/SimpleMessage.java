/**
 * Code generated by "Wire" protobuf compiler, do not edit.
 * Source file: ../runtime/src/main/proto/simple_message.proto
 */
package com.squareup.protos.simple;

import com.squareup.wire.Message;
import com.squareup.wire.ProtoEnum;
import com.squareup.wire.ProtoField;
import com.squareup.wire.UninitializedMessageException;
import com.squareup.wire.Wire;
import java.util.List;

public final class SimpleMessage
    implements Message {

  public static final Integer optional_int32_default = 123;
  public static final NestedMessage optional_nested_msg_default = Wire.getDefaultInstance(NestedMessage.class);
  public static final ExternalMessage optional_external_msg_default = Wire.getDefaultInstance(ExternalMessage.class);
  public static final NestedEnum default_nested_enum_default = NestedEnum.BAZ;
  public static final Integer required_int32_default = 456;
  public static final List<Double> repeated_double_default = java.util.Collections.emptyList();

  /**
   * An optional int32
   */
  @ProtoField(
    tag = 1,
    type = Wire.INT32
  )
  public final Integer optional_int32;

  /**
   * An optional NestedMessage
   */
  @ProtoField(
    tag = 2
  )
  public final NestedMessage optional_nested_msg;

  /**
   * An optional ExternalMessage
   */
  @ProtoField(
    tag = 3
  )
  public final ExternalMessage optional_external_msg;

  @ProtoField(
    tag = 4,
    type = Wire.ENUM
  )
  public final NestedEnum default_nested_enum;

  /**
   * A required int32
   */
  @ProtoField(
    tag = 5,
    type = Wire.INT32,
    label = Wire.REQUIRED
  )
  public final Integer required_int32;

  /**
   * A repeated double
   */
  @ProtoField(
    tag = 6,
    type = Wire.DOUBLE,
    label = Wire.REPEATED
  )
  public final List<Double> repeated_double;

  private SimpleMessage(Builder builder) {
    this.optional_int32 = builder.optional_int32;
    this.optional_nested_msg = builder.optional_nested_msg;
    this.optional_external_msg = builder.optional_external_msg;
    this.default_nested_enum = builder.default_nested_enum;
    this.required_int32 = builder.required_int32;
    this.repeated_double = Wire.unmodifiableCopyOf(builder.repeated_double);
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof SimpleMessage)) return false;
    SimpleMessage o = (SimpleMessage) other;
    if (!Wire.equals(optional_int32, o.optional_int32)) return false;
    if (!Wire.equals(optional_nested_msg, o.optional_nested_msg)) return false;
    if (!Wire.equals(optional_external_msg, o.optional_external_msg)) return false;
    if (!Wire.equals(default_nested_enum, o.default_nested_enum)) return false;
    if (!Wire.equals(required_int32, o.required_int32)) return false;
    if (!Wire.equals(repeated_double, o.repeated_double)) return false;
    return true;
  }

  @Override
  public int hashCode() {
    int hashCode = 0;
    hashCode = hashCode * 37 + (optional_int32 != null ? optional_int32.hashCode() : 0);
    hashCode = hashCode * 37 + (optional_nested_msg != null ? optional_nested_msg.hashCode() : 0);
    hashCode = hashCode * 37 + (optional_external_msg != null ? optional_external_msg.hashCode() : 0);
    hashCode = hashCode * 37 + (default_nested_enum != null ? default_nested_enum.hashCode() : 0);
    hashCode = hashCode * 37 + (required_int32 != null ? required_int32.hashCode() : 0);
    hashCode = hashCode * 37 + (repeated_double != null ? repeated_double.hashCode() : 0);
    return hashCode;
  }

  @Override
  public String toString() {
    return String.format("SimpleMessage{" +
        "optional_int32=%s," +
        "optional_nested_msg=%s," +
        "optional_external_msg=%s," +
        "default_nested_enum=%s," +
        "required_int32=%s," +
        "repeated_double=%s}",
        optional_int32,
        optional_nested_msg,
        optional_external_msg,
        default_nested_enum,
        required_int32,
        repeated_double);
  }

  public static final class Builder
      implements Message.Builder<SimpleMessage> {

    public Integer optional_int32;
    public NestedMessage optional_nested_msg;
    public ExternalMessage optional_external_msg;
    public NestedEnum default_nested_enum;
    public Integer required_int32;
    public List<Double> repeated_double;

    public Builder() {
    }

    public Builder(SimpleMessage message) {
      if (message == null) return;
      this.optional_int32 = message.optional_int32;
      this.optional_nested_msg = message.optional_nested_msg;
      this.optional_external_msg = message.optional_external_msg;
      this.default_nested_enum = message.default_nested_enum;
      this.required_int32 = message.required_int32;
      this.repeated_double = Wire.copyOf(message.repeated_double);
    }

    public Builder optional_int32(Integer optional_int32) {
      this.optional_int32 = optional_int32;
      return this;
    }

    public Builder optional_nested_msg(NestedMessage optional_nested_msg) {
      this.optional_nested_msg = optional_nested_msg;
      return this;
    }

    public Builder optional_external_msg(ExternalMessage optional_external_msg) {
      this.optional_external_msg = optional_external_msg;
      return this;
    }

    public Builder default_nested_enum(NestedEnum default_nested_enum) {
      this.default_nested_enum = default_nested_enum;
      return this;
    }

    public Builder required_int32(Integer required_int32) {
      this.required_int32 = required_int32;
      return this;
    }

    public Builder repeated_double(List<Double> repeated_double) {
      this.repeated_double = repeated_double;
      return this;
    }

    @Override
    public boolean isInitialized() {
      if (required_int32 == null) return false;
      return true;
    }

    @Override
    public SimpleMessage build() {
      if (!isInitialized()) throw new UninitializedMessageException();
      return new SimpleMessage(this);
    }
  }

  public static final class NestedMessage
      implements Message {

    public static final Integer bb_default = 0;

    /**
     * An optional int32
     */
    @ProtoField(
      tag = 1,
      type = Wire.INT32
    )
    public final Integer bb;

    private NestedMessage(Builder builder) {
      this.bb = builder.bb;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof NestedMessage)) return false;
      return Wire.equals(bb, ((NestedMessage) other).bb);
    }

    @Override
    public int hashCode() {
      return bb != null ? bb.hashCode() : 0;
    }

    @Override
    public String toString() {
      return String.format("NestedMessage{" +
          "bb=%s}",
          bb);
    }

    public static final class Builder
        implements Message.Builder<NestedMessage> {

      public Integer bb;

      public Builder() {
      }

      public Builder(NestedMessage message) {
        if (message == null) return;
        this.bb = message.bb;
      }

      public Builder bb(Integer bb) {
        this.bb = bb;
        return this;
      }

      @Override
      public boolean isInitialized() {
        return true;
      }

      @Override
      public NestedMessage build() {
        return new NestedMessage(this);
      }
    }
  }

  public enum NestedEnum {
    @ProtoEnum(1)
    FOO,
    @ProtoEnum(2)
    BAR,
    @ProtoEnum(3)
    BAZ,
  }
}
