// Copyright 2013 Square, Inc.
package com.squareup.wire;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * An adapter than can check for the presence of required fields on a {@link Message.Builder}.
 *
 * @param <B> the Builder class handled by this adapter.
 */
class BuilderAdapter<B extends Message.Builder> {

  private static final int SUFFIX_LENGTH = "$Builder".length();
  private static final String INDENT = "  ";

  private static final Comparator<Field> FIELD_COMPARATOR = new Comparator<Field>() {
    @Override public int compare(Field field1, Field field2) {
      return field1.getName().compareTo(field2.getName());
    }
  };

  private final List<Field> requiredFields = new ArrayList<Field>();

  @SuppressWarnings("unchecked")
  public BuilderAdapter(Class<B> builderType) {
    String builderTypeName = builderType.getName();
    String messageTypeName = builderTypeName.substring(0, builderTypeName.length() - SUFFIX_LENGTH);
    Class<? extends Message> messageType;
    try {
      messageType = (Class<? extends Message>) Class.forName(messageTypeName);
    } catch (ClassNotFoundException e) {
      throw new AssertionError("No message class found for builder type "
          + builderTypeName);
    }

    // Cache fields annotated with '@ProtoField(label = REQUIRED)'
    for (Field field : messageType.getDeclaredFields()) {
      ProtoField annotation = field.getAnnotation(ProtoField.class);
      if (annotation != null && annotation.label() == Wire.REQUIRED) {
        try {
          requiredFields.add(builderType.getField(field.getName()));
        } catch (NoSuchFieldException e) {
          throw new AssertionError("No builder field found for message field "
              + field.getName());
        }
      }
    }
    // Sort by name
    Collections.sort(requiredFields, FIELD_COMPARATOR);
  }

  public <B extends Message.Builder> void checkRequiredFields(B builder) {
    StringBuilder sb = null;
    String plural = "";
    try {
      // Avoid creating an iterator
      for (int i = 0, size = requiredFields.size(); i < size; i++) {
        Field f = requiredFields.get(i);
        if (f.get(builder) == null) {
          if (sb == null) {
            sb = new StringBuilder();
          } else {
            // Found more than one missing field
            plural = "s";
          }
          sb.append(INDENT);
          sb.append(f.getName());
          sb.append('\n');
        }
      }
      if (sb != null) {
        throw new IllegalStateException("Required field" + plural + " not set:\n" + sb.toString());
      }
    } catch (IllegalAccessException e) {
      throw new AssertionError("Unable to access required fields");
    }
  }
}
