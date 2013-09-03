// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ../wire-runtime/src/test/proto/custom_options.proto
package com.squareup.wire.protos.custom_options;

import com.google.protobuf.MessageOptions;
import com.squareup.wire.Message;

public final class MessageWithOptions extends Message {

  public static final MessageOptions MESSAGE_OPTIONS = new MessageOptions.Builder()
      .setExtension(Ext_custom_options.my_message_option_one, new FooBar.Builder()
          .foo(1234)
          .bar("5678")
          .baz(new FooBar.Nested.Builder()
              .value(FooBar.FooBarBazEnum.BAZ)
              .build())
          .qux(-1L)
          .fred(123.0F)
          .daisy(456.0D)
          .build())
      .setExtension(Ext_custom_options.my_message_option_two, 91011.0F)
      .setExtension(Ext_custom_options.my_message_option_three, new FooBar.Builder()
          .foo(11)
          .bar("22")
          .baz(new FooBar.Nested.Builder()
              .value(FooBar.FooBarBazEnum.BAR)
              .build())
          .build())
      .setExtension(Ext_custom_options.my_message_option_four, FooBar.FooBarBazEnum.FOO)
      .build();

  private MessageWithOptions(Builder builder) {
    super(builder);
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof MessageWithOptions;
  }

  @Override
  public int hashCode() {
    return 0;
  }

  public static final class Builder extends Message.Builder<MessageWithOptions> {

    public Builder() {
    }

    public Builder(MessageWithOptions message) {
      super(message);
    }

    @Override
    public MessageWithOptions build() {
      return new MessageWithOptions(this);
    }
  }
}
