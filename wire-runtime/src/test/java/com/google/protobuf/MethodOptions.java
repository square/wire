// Code generated by Wire protocol buffer compiler, do not edit.
// Source file: ../wire-runtime/src/test/proto/google/protobuf/descriptor.proto at 411:1
package com.google.protobuf;

import com.squareup.wire.ExtendableMessage;
import com.squareup.wire.Message;
import com.squareup.wire.ProtoReader;
import com.squareup.wire.TypeAdapter;
import java.io.IOException;
import java.lang.Override;
import java.util.Collections;
import java.util.List;

public final class MethodOptions extends ExtendableMessage<MethodOptions> {
  private static final long serialVersionUID = 0L;

  public static final TypeAdapter<MethodOptions> ADAPTER = new TypeAdapter.MessageAdapter<MethodOptions>() {
    @Override
    public MethodOptions read(ProtoReader reader) throws IOException {
      return MethodOptions.read(reader);
    }
  };

  /**
   * Note:  Field numbers 1 through 32 are reserved for Google's internal RPC
   *   framework.  We apologize for hoarding these numbers to ourselves, but
   *   we were already using them long before we decided to release Protocol
   *   Buffers.
   * The parser stores options it doesn't recognize here. See above.
   */
  public final List<UninterpretedOption> uninterpreted_option;

  public MethodOptions(List<UninterpretedOption> uninterpreted_option) {
    super("MethodOptions");
    this.uninterpreted_option = immutableCopyOf(uninterpreted_option);
  }

  private MethodOptions(Builder builder) {
    this(builder.uninterpreted_option);
    setBuilder(builder);
  }

  @Override
  protected void visitFields(Message.Visitor visitor) {
    visitor.repeated(999, "uninterpreted_option", uninterpreted_option, UninterpretedOption.ADAPTER, false);
    visitor.extensions(this);
    visitor.unknowns(this);
  }

  public static MethodOptions read(ProtoReader reader) throws IOException {
    Builder builder = new Builder();
    while (reader.hasNext()) {
      int tag = reader.nextTag();
      switch (tag) {
        case 999: builder.uninterpreted_option = repeatedMessage(builder.uninterpreted_option, reader, UninterpretedOption.ADAPTER); break;
        default: builder.readExtensionOrUnknown(tag, reader); break;
      }
    }
    return builder.build();
  }

  public static final class Builder extends ExtendableMessage.ExtendableBuilder<MethodOptions, Builder> {
    public List<UninterpretedOption> uninterpreted_option = Collections.emptyList();

    public Builder() {
      super(MethodOptions.class, Builder.class);
    }

    public Builder(MethodOptions message) {
      super(MethodOptions.class, Builder.class, message);
      if (message == null) return;
      this.uninterpreted_option = copyOf(message.uninterpreted_option);
    }

    /**
     * Note:  Field numbers 1 through 32 are reserved for Google's internal RPC
     *   framework.  We apologize for hoarding these numbers to ourselves, but
     *   we were already using them long before we decided to release Protocol
     *   Buffers.
     * The parser stores options it doesn't recognize here. See above.
     */
    public Builder uninterpreted_option(List<UninterpretedOption> uninterpreted_option) {
      this.uninterpreted_option = canonicalizeList(uninterpreted_option);
      return this;
    }

    @Override
    public MethodOptions build() {
      return new MethodOptions(this);
    }
  }
}
