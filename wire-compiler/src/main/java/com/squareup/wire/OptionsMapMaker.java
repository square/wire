package com.squareup.wire;

import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.Option;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OptionsMapMaker {

  private final WireCompiler compiler;

  public OptionsMapMaker(WireCompiler compiler) {
    this.compiler = compiler;
  }

  /**
   * Builds a nested map from the options defined on a {@link MessageType}.
   */
  public Map<String, ?> createMessageOptionsMap(MessageType type) {
    List<Option> options = type.getOptions();
    if (options.isEmpty()) {
      return null;
    }

    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (Option option : options) {
      insertOption(option.getName(), option.getValue(), type.getFullyQualifiedName(), map);
    }
    return map;
  }

  /**
   * Builds a map from the options defined on an {@link EnumType}.
   */
  public Map<String, ?> createEnumOptionsMap(EnumType type) {
    List<Option> options = type.getOptions();
    if (options.isEmpty()) {
      return null;
    }

    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (Option option : type.getOptions()) {
      insertOption(option.getName(), option.getValue(), "", map);
    }
    return map.isEmpty() ? null : map;
  }

  /**
   * Builds a map from the options defined on the values of an {@link EnumType}.
   */
  public Map<String, ?> createEnumValueOptionsMap(EnumType type) {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (EnumType.Value value : type.getValues()) {
      for (Option option : value.getOptions()) {
        insertOption(option.getName(), option.getValue(), "", map);
      }
    }
    return map.isEmpty() ? null : map;
  }

  /**
   * Builds a map from the options defined on a single value of an {@link EnumType}.
   */
  public Map<String, ?> createSingleEnumValueOptionMap(EnumType.Value value) {
    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (Option option : value.getOptions()) {
      insertOption(option.getName(), option.getValue(), "", map);
    }
    return map.isEmpty() ? null : map;
  }

  public Map<String, ?> createFieldOptionsMap(MessageType type, List<Option> options) {
    if (options.isEmpty()) {
      return null;
    }

    Map<String, Object> map = new LinkedHashMap<String, Object>();
    for (Option option : options) {
      String key = option.getName();
      if (WireCompiler.DEFAULT_FIELD_OPTION_KEYS.contains(key)) {
        continue;
      }
      insertOption(key, option.getValue(), type.getFullyQualifiedName(), map);
    }
    return map;
  }

  /**
   * Builds a nested map from a set of options. Each level of the map corresponding to
   * a message has an extra key "class" whose value is the Java type name for the
   * message. For example, consider the following options:
   *
   * <pre>
   *
   * package mypackage;
   *
   * extend google.protobuf.MessageOptions {
   *   optional FooBar my_message_option_one = 50001;
   *   optional float my_message_option_two = 50002;
   *   optional FooBar my_message_option_three = 50003;
   *   optional FooBar.FooBarBazEnum my_message_option_four = 50004;
   * }
   *
   * message MessageWithOptions {
   *   option (my_message_option_one).foo = 1234;
   *   option (my_message_option_one.bar) = "5678";
   *   option (my_message_option_one.baz.value) = BAZ;
   *   option (my_message_option_one).qux = 18446744073709551615;
   *   option (my_message_option_one).fred = 123.0;
   *   option (my_message_option_one).fred = 321.0;
   *   option (my_message_option_one).daisy = 456.0;
   *   option (my_message_option_two) = 91011.0;
   *   option (my_message_option_three) = { foo: 11, bar: "22", baz: { value: BAR }};
   * }
   *
   * message FooBar {
   *   optional int32 foo = 1;
   *   optional string bar = 2;
   *   optional Nested baz = 3;
   *   optional uint64 qux = 4;
   *   repeated float fred = 5;
   *   optional double daisy = 6;
   *
   *   message Nested {
   *    optional FooBarBazEnum value = 1;
   *   }
   *
   *   enum FooBarBazEnum {
   *    FOO = 1;
   *    BAR = 2;
   *    BAZ = 3;
   *   }
   * }
   * </pre>
   *
   * The generated map has the following structure:
   *
   * <pre>
   * {
   *   "my_message_option_one": {
   *     "@type": "mypackage.FooBar",
   *     "foo": "1234",
   *     "bar": "\"5678\"",
   *     "baz": {
   *       "@type": "FooBar.FooBarBazEnum",
   *       "value": "FooBar.FooBarBazEnum.BAZ"
   *     }
   *     "qux": "-1L", // note: wrapped to signed 64-bits
   *     "fred": ["123.0F", "321.0F"]
   *     "daisy": "456.0D"
   *   }
   *   "my_message_option_two": "91011.0",
   *   "my_message_option_three": {
   *     "@type": "mypackage.FooBar",
   *     "foo": "11",
   *     "bar": "\"22\"",
   *     "baz": {
   *       "@type": "mypackage.FooBar.FooBarBazEnum",
   *       "value": "mypackage.FooBar.FooBarBazEnum.BAR"
   *     }
   *   }
   * }
   * </pre>
   */
  @SuppressWarnings("unchecked")
  private void insertOption(String name, Object value, String enclosingType,
      Map<String, Object> map) {
    // Append the current package name if no prefix of the given name matches a known extension
    String fqName = getExtensionPrefix(name);
    if (fqName.isEmpty()) {
      name = compiler.prefixWithPackageName(name);
    }
    insertOptionHelper(name, value, enclosingType, map);
  }

  @SuppressWarnings("unchecked")
  private void insertOptionHelper(String name, Object value, String enclosingType,
      Map<String, Object> map) {
    // Strip square brackets (indicating an extension) from the option name
    name = stripSquareBrackets(name);

    // If the option name has dots in it after the longest prefix that is a known extension name,
    // break off the first level and recurse
    String fqName = getExtensionPrefix(name);
    int firstDotAfterExtensionIndex = name.indexOf('.', fqName.length());
    if (firstDotAfterExtensionIndex != -1) {
      String prefix = name.substring(0, firstDotAfterExtensionIndex);
      String suffix = name.substring(firstDotAfterExtensionIndex + 1);
      String fieldType = getFieldType(enclosingType, prefix);
      insertOptionHelper(prefix, new Option(suffix, value), fieldType, map);
      return;
    }

    // See if the name refers to an extension
    ExtensionInfo info = compiler.isEnum(enclosingType) ? null : getExtensionInfo(name);

    // Deal with names that start with a suffix of the package name
    if (info == null && compiler.getProtoFile().getPackageName().endsWith("." + name)
        && value instanceof Option) {
      name = compiler.prefixWithPackageName(((Option) value).getName());
      info = compiler.getExtension(name);
      value = ((Option) value).getValue();
    }

    if (info != null) {
      enclosingType = info.fqType;
    }

    // Place simple entries into the map, recurse on nested entries
    String fieldType = info == null ? getFieldType(enclosingType, name) : info.type;
    if (fieldType == null) {
      fieldType = enclosingType;
    }

    if (value instanceof String) {
      MessageType.Label fieldLabel = getFieldLabel(enclosingType, name);
      if (info != null) {
        fieldLabel = info.label;
      }
      insertStringOption(name, (String) value, map, fieldType, fieldLabel);
    } else if (value instanceof List) {
      insertListOption(name, (List<?>) value, enclosingType, map, fieldType);
    } else if (value instanceof Option) {
      insertOptionOption(name, (Option) value, enclosingType, map);
    } else if (value instanceof Map) {
      insertMapOption(name, (Map<String, ?>) value, enclosingType, map);
    } else {
      throw new RuntimeException("value is not an Option, String, List, or Map");
    }
  }

  private ExtensionInfo getExtensionInfo(String name) {
    ExtensionInfo info = compiler.getExtension(name);
    if (info == null) {
      info = compiler.getExtension(compiler.prefixWithPackageName(name));
    }
    return info;
  }

  private MessageType.Label getFieldLabel(String enclosingType, String nestedName) {
    FieldInfo fieldInfo = compiler.getField(enclosingType + "$" + nestedName);
    return fieldInfo == null ? null : fieldInfo.label;
  }

  private String stripSquareBrackets(String name) {
    int lastIndex = name.length() - 1;
    if (name.charAt(0) == '[' && name.charAt(lastIndex) == ']') {
      name = name.substring(1, lastIndex);
      if (!name.contains(".")) {
        name = compiler.prefixWithPackageName(name);
      }
    }
    return name;
  }

  private String getFieldType(String enclosingType, String nestedName) {
    FieldInfo fieldInfo = compiler.getField(enclosingType + "$" + nestedName);
    return getFieldType(fieldInfo);
  }

  private String getExtensionPrefix(String name) {
    int endIndex = name.length();
    String fqName;
    while (endIndex != -1) {
      fqName = name.substring(0, endIndex);
      if (compiler.getExtension(fqName) != null) return fqName;
      endIndex = name.lastIndexOf('.', endIndex - 1);
    }
    return "";
  }

  private String getFieldType(FieldInfo info) {
    return info == null ? null : info.name;
  }

  @SuppressWarnings("unchecked")
  private void insertStringOption(String name, String value, Map<String, Object> map,
      String fieldType, MessageType.Label fieldLabel) {
    value = getOptionInitializer(value, fieldType);
    if (fieldLabel == MessageType.Label.REPEATED) {
      List<String> list = (List<String>) map.get(name);
      if (list == null) {
        list = new ArrayList<String>();
        map.put(name, list);
      }
      list.add(value);
    } else {
      map.put(name, value);
    }
  }

  @SuppressWarnings("unchecked")
  private void insertListOption(String name, List<?> value, String enclosingType,
      Map<String, Object> map, String fieldType) {
    List<Object> valueList = new ArrayList<Object>();
    for (Object objectValue : value) {
      if (objectValue instanceof String) {
        String stringValue = getOptionInitializer((String) objectValue, fieldType);
        valueList.add(stringValue);
      } else if (objectValue instanceof Map) {
        Map<String, Object> entryMap = new LinkedHashMap<String, Object>();
        entryMap.put("@type", enclosingType);
        insertOptionsFromMap(enclosingType, (Map<String, ?>) objectValue, entryMap);
        valueList.add(entryMap);
      } else {
        throw new RuntimeException("List contains " + objectValue.getClass().getName()
            + ", not String or Map");
      }
    }
    map.put(name, valueList);
  }

  private String getOptionInitializer(String stringValue, String fieldType) {
    if (TypeInfo.isScalar(fieldType)) {
      String javaTypeName = TypeInfo.scalarType(fieldType);
      return compiler.getInitializerForType(stringValue, javaTypeName);
    } else if (compiler.isEnum(fieldType)) {
      String javaName = compiler.javaName(fieldType);
      String javaPackage = compiler.getJavaPackage();
      if (javaName.startsWith(javaPackage + ".")) {
        javaName = javaName.substring(javaPackage.length() + 1);
      }
      String typeBeingGenerated = compiler.getTypeBeingGenerated();
      if (javaName.startsWith(typeBeingGenerated + ".")) {
        javaName = javaName.substring(typeBeingGenerated.length() + 1);
      }
      return javaName + "." + compiler.getTrailingSegment(stringValue);
    } else {
      return stringValue;
    }
  }

  private void insertOptionsFromMap(String enclosingType, Map<String, ?> inputMap,
      Map<String, Object> outputMap) {
    for (Map.Entry<String, ?> valueEntry : inputMap.entrySet()) {
      String nestedName = valueEntry.getKey();
      String fieldType = getFieldType(enclosingType, nestedName);
      Object val = qualifyEnum(enclosingType, nestedName, fieldType, valueEntry.getValue());
      insertOptionHelper(nestedName, val, fieldType, outputMap);
    }
  }

  private void insertOptionOption(String name, Option value, String enclosingType,
                                  Map<String, Object> map) {
    Map<String, Object> entryMap = getOrCreateFromMap(map, name);
    entryMap.put("@type", enclosingType);

    String nestedName = value.getName();
    String fieldType = getFieldType(enclosingType, nestedName);
    // Only rewrite enum initializers, others will be rewritten at the final insertion level
    Object val = qualifyEnum(enclosingType, nestedName, fieldType, value.getValue());
    insertOptionHelper(nestedName, val, enclosingType, entryMap);
  }

  private Object qualifyEnum(String enclosingType, String optionValueName, String fieldType,
                     Object value) {
    if (compiler.isEnum(getFieldType(enclosingType, optionValueName))) {
      return fieldType + "." + value;
    }
    return value;
  }

  private void insertMapOption(String name, Map<String, ?> value, String enclosingType,
                               Map<String, Object> map) {
    Map<String, Object> entryMap = getOrCreateFromMap(map, name);
    entryMap.put("@type", enclosingType);
    insertOptionsFromMap(enclosingType, value, entryMap);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> getOrCreateFromMap(Map<String, Object> map, String name) {
    Object entry = map.get(name);
    if (entry == null) {
      entry = new LinkedHashMap<String, Object>();
      map.put(name, entry);
    }
    return (Map<String, Object>) entry;
  }

  @SuppressWarnings("unchecked")
  String createOptionInitializer(Object listOrMap, String parentType,
      String parentField, String fieldType, boolean skipAsList, int level) {
    level++;

    StringBuilder sb = new StringBuilder();
    if (listOrMap instanceof Map) {
      Map<String, Object> map = (Map<String, Object>) listOrMap;
      String fullyQualifiedName = compiler.javaName(fieldType);
      String dollarName = parentType + "$" + parentField;
      FieldInfo fieldInfo = compiler.getField(dollarName);
      boolean emitAsList = !skipAsList && fieldInfo != null && fieldInfo.isRepeated();
      if (emitAsList) {
        sb.append("java.util.Arrays.asList(");
      }
      String shortName = compiler.shortenJavaName(fullyQualifiedName);
      sb.append("new ").append(shortName).append(".Builder()");
      for (Map.Entry<String, ?> entry : map.entrySet()) {
        String key = entry.getKey();
        if (isMetadata(key)) {
          continue;
        }
        sb.append("\n");
        indent(sb, level);
        sb.append(".");

        ExtensionInfo extension = compiler.getExtension(key);
        if (extension != null) {
          key = compiler.getTrailingSegment(key);
          sb.append(String.format("setExtension(Ext_%s.%s, ", extension.location, key));
        } else {
          sb.append(key).append("(");
        }

        FieldInfo info = compiler.getField(fieldType + "$" + key);
        String nestedFieldType;
        if (info == null) {
          ExtensionInfo extInfo = compiler.getExtension(entry.getKey());
          if (extInfo != null) {
            nestedFieldType = extInfo.fqType;
          } else {
            throw new RuntimeException("Unknown name " + entry.getKey());
          }
        } else {
          nestedFieldType = getFieldType(info);
        }
        String optionInitializer = createOptionInitializer(entry.getValue(), fieldType, key,
            nestedFieldType, false, level);
        sb.append(optionInitializer).append(")");
      }
      sb.append("\n");
      indent(sb, level);
      sb.append(".build()");
      if (emitAsList) {
        sb.append(")");
      }
    } else if (listOrMap instanceof List) {
      sb.append("java.util.Arrays.asList(");
      String sep = "\n";
      for (Object objectValue : (List<Object>) listOrMap) {
        sb.append(sep);
        indent(sb, level);
        if (objectValue instanceof String) {
          sb.append((String) objectValue);
        } else if (objectValue instanceof Map) {
          sb.append(createOptionInitializer(objectValue, parentType, parentField, fieldType, true,
              level));
        }
        sep = ",\n";
      }
      sb.append(")");
    } else {
      sb.append((String) listOrMap);
    }
    return sb.toString();
  }

  private void indent(StringBuilder sb, int level) {
    for (int i = 0; i < level; i++) {
      sb.append(WireCompiler.LINE_WRAP_INDENT);
    }
  }

  private boolean isMetadata(String key) {
    return key.charAt(0) == '@';
  }

  @SuppressWarnings("unchecked")
  public void getOptionTypes(Map<String, ?> optionsMap, List<String> types) {
    if (optionsMap == null) return;
    for (Map.Entry<String, ?> entry : optionsMap.entrySet()) {
      String key = entry.getKey();

      ExtensionInfo info = compiler.getExtension(key);
      if (info != null && !info.fqLocation.startsWith(compiler.getJavaPackage())) {
        types.add(info.fqLocation);
      }

      if ("@type".equals(key)) {
        String type = (String) entry.getValue();
        String javaName = compiler.javaName(type);
        if (compiler.fullyQualifiedNameIsOutsidePackage(javaName)) {
          types.add(javaName);
        }
      } else if (entry.getValue() instanceof List) {
        for (Object objectValue : (List<?>) entry.getValue()) {
          if (objectValue instanceof Map) {
            getOptionTypes((Map<String, ?>) objectValue, types);
          }
        }
      } else if (entry.getValue() instanceof Map) {
        getOptionTypes((Map<String, ?>) entry.getValue(), types);
      }
    }
  }
}
