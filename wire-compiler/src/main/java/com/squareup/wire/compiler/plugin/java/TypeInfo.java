package com.squareup.wire.compiler.plugin.java;

import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.Option;
import com.squareup.protoparser.Type;
import com.squareup.wire.Message;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utilities for type analysis.
 */
public final class TypeInfo {

  private static final Map<String, String> JAVA_TYPES = new LinkedHashMap<>();
  static {
    JAVA_TYPES.put("bool", "Boolean");
    JAVA_TYPES.put("bytes", "ByteString");
    JAVA_TYPES.put("double", "Double");
    JAVA_TYPES.put("float", "Float");
    JAVA_TYPES.put("fixed32", "Integer");
    JAVA_TYPES.put("fixed64", "Long");
    JAVA_TYPES.put("int32", "Integer");
    JAVA_TYPES.put("int64", "Long");
    JAVA_TYPES.put("sfixed32", "Integer");
    JAVA_TYPES.put("sfixed64", "Long");
    JAVA_TYPES.put("sint32", "Integer");
    JAVA_TYPES.put("sint64", "Long");
    JAVA_TYPES.put("string", "String");
    JAVA_TYPES.put("uint32", "Integer");
    JAVA_TYPES.put("uint64", "Long");
  }

  /**
   * Field options that don't trigger generation of a FIELD_OPTIONS_* field.
   */
  private static final Set<String> DEFAULT_FIELD_OPTION_KEYS =
      new LinkedHashSet<>(Arrays.asList("default", "deprecated", "packed"));
  static final Set<String> JAVA_KEYWORDS = new LinkedHashSet<String>(
          Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
              "class", "const", "continue", "default", "do", "double", "else", "enum", "extends",
              "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
              "int", "interface", "long", "native", "new", "package", "private", "protected", "public",
              "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
              "throw", "throws", "transient", "try", "void", "volatile", "while"));
  private final Set<String> enumTypes;
  private final boolean generateOptions;

  /**
   * Returns true if the given type name is one of the standard .proto
   * scalar types, e.g., {@code int32}, {@code string}, etc.
   */
  public static boolean isScalar(String type) {
    return JAVA_TYPES.containsKey(type);
  }

  /**
   * Returns the Java type associated with a standard .proto
   * scalar type, e.g., {@code int32}, {@code string}, etc.,
   * or null if the name is not that of a scalar type.
   */
  public static String scalarType(String type) {
    return JAVA_TYPES.get(type);
  }

  Set<String> imports = new LinkedHashSet<>();
  List<String> staticImports = new ArrayList<>();

  boolean hasMessage;
  boolean hasFields;
  boolean hasBytesField;
  boolean hasEnum;
  boolean hasRepeatedField;
  boolean hasExtensions;
  boolean hasFieldOption;
  boolean hasMessageOption;
  boolean hasEnumOption;

  Collection<Message.Datatype> datatypes = new LinkedHashSet<>();
  Collection<Message.Label> labels = new LinkedHashSet<>();

  TypeInfo(Type type, Set<String> enumTypes, boolean generateOptions) {
    this.enumTypes = enumTypes;
    this.generateOptions = generateOptions;

    computeTypeInfo(type);
    computeImports();
  }

  private void computeTypeInfo(Type type) {
    if (type instanceof MessageType) {
      if (!type.getOptions().isEmpty()) {
        hasMessageOption = true;
      }
      if (((MessageType) type).getExtensions().isEmpty()) {
        hasMessage = true;
      } else {
        hasExtensions = true;
      }

      for (MessageType.Field field : ((MessageType) type).getFields()) {
        hasFields = true;
        if (FieldInfo.isRepeated(field)) hasRepeatedField = true;
        if ("bytes".equals(field.getType())) hasBytesField = true;

        String fieldType = field.getType();
        Message.Datatype datatype = Message.Datatype.of(fieldType);
        // If not scalar, determine whether it is an enum
        if (datatype == null && isEnum(field.getType())) {
          datatype = Message.Datatype.ENUM;
        }
        if (datatype != null) datatypes.add(datatype);

        // Convert Protoparser label to Wire label
        MessageType.Label label = field.getLabel();
        switch (label) {
          case OPTIONAL:
            labels.add(Message.Label.OPTIONAL);
            break;
          case REQUIRED:
            labels.add(Message.Label.REQUIRED);
            break;
          case REPEATED:
            if (FieldInfo.isPacked(field, datatype == Message.Datatype.ENUM)) {
              labels.add(Message.Label.PACKED);
            } else {
              labels.add(Message.Label.REPEATED);
            }
            break;
          default:
            throw new AssertionError("Unknown label " + label);
        }

        for (Option option : field.getOptions()) {
          if (!DEFAULT_FIELD_OPTION_KEYS.contains(option.getName())) {
            hasFieldOption = true;
          }
        }
      }
    } else if (type instanceof EnumType) {
      hasEnum = true;
      if (!type.getOptions().isEmpty()) {
        hasEnumOption = true;
      }
    }
    for (Type nestedType : type.getNestedTypes()) {
      computeTypeInfo(nestedType);
    }
  }

  private void computeImports() {
    if (hasMessage) {
      imports.add("com.squareup.wire.Message");
    }
    if (hasMessage || hasExtensions) {
      if (hasFields) {
        imports.add("com.squareup.wire.ProtoField");
      }
    }
    if (hasBytesField) {
      imports.add("okio.ByteString");
    }
    if (hasEnum) {
      imports.add("com.squareup.wire.ProtoEnum");
    }
    if (hasRepeatedField) {
      imports.add("java.util.Collections");
      imports.add("java.util.List");
    }
    if (hasExtensions) {
      imports.add("com.squareup.wire.ExtendableMessage");
      imports.add("com.squareup.wire.Extension");
    }
    if (generateOptions) {
      if (hasFieldOption) {
        imports.add("com.google.protobuf.FieldOptions");
      }
      if (hasMessageOption) {
        imports.add("com.google.protobuf.MessageOptions");
      }
      if (hasEnumOption) {
        imports.add("com.google.protobuf.EnumOptions");
      }
    }

    for (Message.Datatype datatype : datatypes) {
      staticImports.add("com.squareup.wire.Message.Datatype." + datatype.toString());
    }
    for (Message.Label label : labels) {
      if (label == Message.Label.OPTIONAL) {
        continue;
      }
      staticImports.add("com.squareup.wire.Message.Label." + label.toString());
    }
  }

  private boolean isEnum(String type) {
    return enumTypes.contains(type);
  }

  public Collection<String> getImports() {
    return imports;
  }

  public Collection<String> getStaticImports() {
    return staticImports;
  }
}
