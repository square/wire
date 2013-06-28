// Copyright 2013 Square, Inc.
package com.squareup.omar;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtoField {
  // Marker value for annotations that don't specify messageType()
  public static class NotAMessage implements Message {
    private NotAMessage() {}
  }

  int tag();
  int type() default Omar.MESSAGE;
  int label() default Omar.OPTIONAL;
  Class<? extends Message> messageType() default NotAMessage.class;
  String defaultValue() default "";
}
