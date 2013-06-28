// Copyright 2013 Square, Inc.
package com.squareup.omar;

public interface Message {

  public static interface Builder<T extends Message> {
    boolean isInitialized();
    T build();
  }

  public static interface ExtendableMessage<MessageType extends ExtendableMessage> extends Message {

    public static class Extension<ContainingType extends ExtendableMessage, Type> implements Comparable<Extension> {
      private final Class<ContainingType> containingType;
      private final Class<? extends Message> messageType;
      private final Class<? extends Enum> enumType;
      private final int tag;
      private final int type;
      private final int label;

      public static <C extends ExtendableMessage, Type> Extension<C, Type> getExtension(Class<C> containingType,
          int tag, int type, int label) {
        return new Extension<C, Type>(containingType, tag, type, label, null, null);
      }

      public static <C extends ExtendableMessage, Type> Extension<C, Type> getMessageExtension(Class<C> containingType,
          int tag, int label, Class<? extends Message> messageType) {
        return new Extension<C, Type>(containingType, tag, Omar.MESSAGE, label, messageType, null);
      }

      public static <C extends ExtendableMessage, Type> Extension<C, Type> getEnumExtension(Class<C> containingType,
          int tag, int label, Class<? extends Enum> enumType) {
        return new Extension<C, Type>(containingType, tag, Omar.ENUM, label, null, enumType);
      }

      private Extension(Class <ContainingType> containingType, int tag, int type, int label,
          Class<? extends Message> messageType, Class<? extends Enum> enumType) {
        this.containingType = containingType;
        this.messageType = messageType;
        this.enumType = enumType;
        this.tag = tag;
        this.type = type;
        this.label = label;
      }

      @Override public final int compareTo(Extension o) {
        return tag - o.tag;
      }

      public Class<ContainingType> getContainingType() {
        return containingType;
      }

      public Class<? extends Message> getMessageType() {
        return messageType;
      }

      public Class<? extends Enum> getEnumType() {
        return enumType;
      }

      public int getTag() {
        return tag;
      }

      public int getType() {
        return type;
      }

      public int getLabel() {
        return label;
      }
    }

    <Type> Type getExtension(Extension<MessageType, Type> extension);

    public static interface ExtendableBuilder<MessageType extends ExtendableMessage> extends Builder<MessageType> {
      <Type> Type getExtension(Extension<MessageType, Type> extension);
      <Type> ExtendableBuilder<MessageType> setExtension(Extension<MessageType, Type> extension, Type val);
    }
  }
}
