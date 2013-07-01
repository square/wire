// Copyright 2013 Square, Inc.
package com.squareup.omar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation placed on {@link Message} fields in generated code to provide additional
 * metadata by the protocol buffer runtime to perform serialization and deserialization.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtoField {
  // Marker value for annotations that don't specify messageType()
  public static class NotAMessage implements Message {
    private NotAMessage() {}
  }

  /** The tag number used to store the field's value. */
  int tag();

  /**
   * The field's protocol buffer datatype, e.g., {@code Omar.INT32},
   * {@code Omar.MESSAGE}, or {@code Omar.ENUM}. Defaults to
   * {@code Omar.MESSAGE}.
   */
  int type() default Omar.MESSAGE;

  /**
   * The field's protocol buffer label, one of {@code Omar.OPTIONAL},
   * {@code Omar.REQUIRED}, or {@code Omar.REPEATED}. Defaults to
   * {@code Omar.OPTIONAL}.
   */
  int label() default Omar.OPTIONAL;

  /**
   * The class type of the message stored in this field, when
   * {@code type() == Omar.Message}. Note that we cannot infer this
   * from the field's declared type for repeated fields, since the
   * message type in {@code List<SomeMessage>} is erased.
   */
  Class<? extends Message> messageType() default NotAMessage.class;

  /**
   * The default value for the field as defined in the protocol buffer
   * source, for example {@code "17"}, {@code "22L"}, {@code "false"}, etc.
   */
  String defaultValue() default "";
}
