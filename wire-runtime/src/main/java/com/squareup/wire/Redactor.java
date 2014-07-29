package com.squareup.wire;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Creates redacted copies of objects. */
public class Redactor<T extends Message> {
  private static final Redactor<?> NOOP_REDACTOR = new Redactor<Message>(null, null, null, null) {
    @Override
    public Message redact(Message message) {
      return message;
    }
  };

  /** Lazily-populated redactors. Guarded by Redactor.class. */
  private static final Map<Class<? extends Message>, Redactor> redactors =
      new LinkedHashMap<Class<? extends Message>, Redactor>();

  private final Constructor<?> builderConstructor;
  private final List<Field> redactedFields;
  private final List<Field> messageFields;
  private final List<Redactor<?>> messageRedactors;

  Redactor(Constructor<?> builderConstructor, List<Field> redactedFields,
      List<Field> messageFields, List<Redactor<?>> messageRedactors) {
    this.builderConstructor = builderConstructor;
    this.redactedFields = redactedFields;
    this.messageFields = messageFields;
    this.messageRedactors = messageRedactors;
  }

  /** Returns a Redactor for {@code messageClass}. */
  @SuppressWarnings("unchecked") // Field and member redactors always agree.
  public static synchronized <T extends Message> Redactor<T> get(Class<T> messageClass) {
    Redactor<T> existingRedactor = redactors.get(messageClass);
    if (existingRedactor != null) {
      return existingRedactor;
    }

    // Prevent infinite recursion by putting a placeholder in our cache until we finish initializing
    // the redactor. This is necessary because this is recursive and a field might include
    // messageClass.
    FutureRedactor<T> futureRedactor = new FutureRedactor<T>();
    redactors.put(messageClass, futureRedactor);

    try {
      Class<?> builderClass = Class.forName(messageClass.getName() + "$Builder");
      List<Field> redactedFields = new ArrayList<Field>();
      List<Field> messageFields = new ArrayList<Field>();
      List<Redactor<?>> messageRedactors = new ArrayList<Redactor<?>>();

      for (Field messageField : messageClass.getDeclaredFields()) {
        if (Modifier.isStatic(messageField.getModifiers())) continue;

        // Process fields annotated with '@ProtoField'.
        ProtoField annotation = messageField.getAnnotation(ProtoField.class);
        if (annotation != null && annotation.redacted()) {
          if (annotation.label() == Message.Label.REQUIRED) {
            throw new IllegalArgumentException(
                String.format("Field %s is REQUIRED and cannot be redacted.", messageField));
          }

          redactedFields.add(builderClass.getDeclaredField(messageField.getName()));
        } else if (Message.class.isAssignableFrom(messageField.getType())) {
          // If the field is a Message, it needs its own Redactor.
          Field field = builderClass.getDeclaredField(messageField.getName());
          Redactor<?> fieldRedactor = get((Class) field.getType());

          // This message doesn't redact any fields, so we don't recursively call it.
          if (fieldRedactor == NOOP_REDACTOR) continue;

          messageFields.add(field);
          messageRedactors.add(fieldRedactor);
        }
      }

      Redactor<T> redactor;
      if (redactedFields.isEmpty() && messageFields.isEmpty()) {
        redactor = (Redactor<T>) NOOP_REDACTOR;
      } else {
        Constructor<?> builderConstructor = (Constructor) builderClass.getConstructor(messageClass);
        redactor = new Redactor<T>(builderConstructor, redactedFields, messageFields,
            messageRedactors);
      }

      futureRedactor.setDelegate(redactor);
      redactors.put(messageClass, redactor);

      return redactor;
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new AssertionError(e); // We don't expect any reflection exceptions.
    }
  }

  /**
   * Returns a copy of {@code message} with all redacted fields set to null.
   * This operation is recursive: nested messages are themselves redacted in the
   * returned object.
   */
  @SuppressWarnings("unchecked") // Field and member redactors always agree.
  public T redact(T message) {
    if (message == null) return null;

    try {
      Message.Builder<T> builder = (Message.Builder<T>) builderConstructor.newInstance(message);

      for (Field field : redactedFields) {
        field.set(builder, null);
      }

      for (int i = 0; i < messageFields.size(); i++) {
        Field field = messageFields.get(i);
        Redactor<Message> r = (Redactor<Message>) messageRedactors.get(i);
        field.set(builder, r.redact((Message) field.get(builder)));
      }

      return builder.build();
    } catch (Exception e) {
      throw new AssertionError(e.getMessage());
    }
  }

  private static class FutureRedactor<T extends Message> extends Redactor<T> {
    private Redactor<T> delegate;

    public FutureRedactor() {
      super(null, null, null, null);
    }

    public void setDelegate(Redactor<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public T redact(T message) {
      if (delegate == null) {
        throw new IllegalStateException("Delegate was not set.");
      }

      return delegate.redact(message);
    }
  }
}
