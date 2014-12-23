package com.squareup.wire;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.Option;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.Type;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.Modifier;

import static com.squareup.protoparser.MessageType.Field;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

public class MessageWriter {

  private static final Set<String> JAVA_KEYWORDS = new LinkedHashSet<String>(
      Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
          "class", "const", "continue", "default", "do", "double", "else", "enum", "extends",
          "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
          "int", "interface", "long", "native", "new", "package", "private", "protected", "public",
          "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
          "throw", "throws", "transient", "try", "void", "volatile", "while"));
  private static final String URL_CHARS = "[-!#$%&'()*+,./0-9:;=?@A-Z\\[\\]_a-z~]";
  private final WireCompiler compiler;

  public MessageWriter(WireCompiler compiler) {
    this.compiler = compiler;
  }

  public static void emitDocumentation(JavaWriter writer, String documentation) throws IOException {
    if (hasDocumentation(documentation)) {
      writer.emitJavadoc(sanitizeJavadoc(documentation));
    }
  }

  /**
   * A grab-bag of fixes for things that can go wrong when converting to javadoc.
   */
  static String sanitizeJavadoc(String documentation) {
    // JavaWriter will pass the doc through String.format, so escape all '%' chars
    documentation = documentation.replace("%", "%%");
    // Remove trailing whitespace on each line.
    documentation = documentation.replaceAll("[^\\S\n]+\n", "\n");
    documentation = documentation.replaceAll("\\s+$", "");
    // Rewrite '@see <url>' to use an html anchor tag
    documentation =
        documentation.replaceAll("@see (http:" + URL_CHARS + "+)", "@see <a href=\"$1\">$1</a>");
    return documentation;
  }

  public void emitHeader(JavaWriter writer, Set<String> imports,
      Collection<Message.Datatype> datatypes, Collection<Message.Label> labels) throws IOException {
    writer.emitImports(imports);

    if (!datatypes.isEmpty() || !labels.isEmpty()) {
      writer.emitEmptyLine();
    }
    for (Message.Datatype datatype : datatypes) {
      writer.emitStaticImports("com.squareup.wire.Message.Datatype." + datatype.toString());
    }
    for (Message.Label label : labels) {
      writer.emitStaticImports("com.squareup.wire.Message.Label." + label.toString());
    }
  }

  private static class EnumValueOptionInfo implements Comparable<EnumValueOptionInfo> {
    public final String type;
    public final String name;

    EnumValueOptionInfo(String type, String name) {
      this.type = type;
      this.name = name;
    }

    @Override public int compareTo(EnumValueOptionInfo other) {
      return name.compareTo(other.name);
    }
  }

  public void emitType(JavaWriter writer, Type type, String currentType, Map<String, ?> optionsMap,
      boolean topLevel)
      throws IOException {
    writer.emitEmptyLine();
    if (type instanceof MessageType) {
      emitAll(writer, (MessageType) type, optionsMap, topLevel);
      for (Type nestedType : type.getNestedTypes()) {
        emitType(writer, nestedType, currentType + nestedType.getName() + ".", optionsMap, false);
      }
      writer.endType();
    } else if (type instanceof EnumType) {
      EnumType enumType = (EnumType) type;
      OptionsMapMaker mapMaker = new OptionsMapMaker(compiler);
      // Generate a list of all the options used by any value of this enum type.
      List<EnumValueOptionInfo> options = getEnumValueOptions(enumType, mapMaker);

      writer.beginType(enumType.getName(), "enum", EnumSet.of(PUBLIC), null, "ProtoEnum");
      List<EnumType.Value> values = enumType.getValues();
      for (int i = 0, count = values.size(); i < count; i++) {
        EnumType.Value value = values.get(i);
        MessageWriter.emitDocumentation(writer, value.getDocumentation());

        List<String> initializers = new ArrayList<String>();
        initializers.add(String.valueOf(value.getTag()));
        addEnumValueOptionInitializers(value, options, mapMaker, initializers);

        writer.emitEnumValue(value.getName() + "(" + join(initializers, ", ") + ")",
            (i == count - 1));
      }

      if (compiler.shouldEmitOptions()) {
        emitEnumOptions(writer, mapMaker.createEnumOptionsMap(enumType));
      }

      // Output Private tag field
      writer.emitEmptyLine();
      writer.emitField("int", "value", EnumSet.of(PRIVATE, FINAL));

      // Output extension fields.
      for (EnumValueOptionInfo option : options) {
        writer.emitField(option.type, trailingSegment(option.name), EnumSet.of(PUBLIC, FINAL));
      }
      writer.emitEmptyLine();

      // Private Constructor
      List<String> parameters = new ArrayList<String>();
      parameters.add("int");
      parameters.add("value");
      for (EnumValueOptionInfo option : options) {
        parameters.add(option.type);
        parameters.add(trailingSegment(option.name));
      }

      writer.beginConstructor(EnumSet.of(PRIVATE), parameters, null);
      writer.emitStatement("this.value = value");
      for (EnumValueOptionInfo option : options) {
        String name = trailingSegment(option.name);
        writer.emitStatement("this.%s = %s", name, name);
      }
      writer.endConstructor();
      writer.emitEmptyLine();

      // Public Getter
      writer.emitAnnotation(Override.class);
      writer.beginMethod("int", "getValue", EnumSet.of(PUBLIC));
      writer.emitStatement("return value");
      writer.endMethod();
      writer.endType();
    }
  }

  private List<EnumValueOptionInfo> getEnumValueOptions(EnumType enumType,
      OptionsMapMaker mapMaker) {
    if (!compiler.shouldEmitOptions() && compiler.enumOptions().isEmpty()) {
      return Collections.emptyList();
    }

    Map<String, ?> optionsMap = mapMaker.createEnumValueOptionsMap(enumType);
    if (optionsMap == null || optionsMap.isEmpty()) {
      return Collections.emptyList();
    }

    List<EnumValueOptionInfo> result = new ArrayList<EnumValueOptionInfo>();

    Set<String> optionNames = new TreeSet<String>();
    for (EnumType.Value value : enumType.getValues()) {
      for (Option option : value.getOptions()) {
        optionNames.add(option.getName());
      }
    }

    Set<String> fqNames = optionsMap.keySet();
    for (String optionName : optionNames) {
      for (String fqName : fqNames) {
        if (fqName.equals(optionName) || fqName.endsWith("." + optionName)) {
          if (compiler.enumOptions().contains(fqName)) {
            ExtensionInfo info = compiler.getExtension(fqName);
            result.add(new EnumValueOptionInfo(compiler.javaName(null, info.fqType), optionName));
          }
        }
      }
    }

    return result;
  }

  private void addEnumValueOptionInitializers(EnumType.Value value,
      List<EnumValueOptionInfo> optionInfo, OptionsMapMaker mapMaker, List<String> initializers) {
    Map<String, ?> enumValueOptionsMap = mapMaker.createSingleEnumValueOptionMap(value);
    List<Option> valueOptions = value.getOptions();
    for (EnumValueOptionInfo option : optionInfo) {
      Option optionByName = Option.findByName(valueOptions, option.name);
      String initializer = null;
      if (optionByName != null) {
        for (Map.Entry<String, ?> entry : enumValueOptionsMap.entrySet()) {
          String fqName = entry.getKey();
          ExtensionInfo info = compiler.getExtension(fqName);
          String name = optionByName.getName();
          if (fqName.equals(name) || fqName.endsWith("." + name)) {
            initializer = mapMaker.createOptionInitializer(entry.getValue(), "", "", info.fqType,
                false, 1);
            break;
          }
        }
      }
      initializers.add(initializer);
    }
  }

  private String trailingSegment(String s) {
    int index = s.lastIndexOf('.');
    return index == -1 ? s : s.substring(index + 1);
  }

  private String join(List<String> values, String separator) {
    StringBuilder sb = new StringBuilder();
    String sep = "";
    for (String value : values) {
      sb.append(sep);
      sb.append(value);
      sep = separator;
    }
    return sb.toString();
  }

  private void emitAll(JavaWriter writer, MessageType messageType, Map<String, ?> optionsMap,
      boolean topLevel)
      throws IOException {
    Set<Modifier> modifiers = EnumSet.of(PUBLIC, FINAL);
    if (!topLevel) {
      modifiers.add(STATIC);
    }

    String name = messageType.getName();
    emitDocumentation(writer, messageType.getDocumentation());
    writer.beginType(name, "class", modifiers,
        compiler.hasExtensions(messageType) ? "ExtendableMessage<" + name + ">" : "Message");

    emitMessageOptions(writer, optionsMap);
    if (compiler.shouldEmitOptions()) {
      emitMessageFieldOptions(writer, messageType);
    }
    emitMessageFieldDefaults(writer, messageType);
    emitMessageFields(writer, messageType);
    emitMessageFieldsConstructor(writer, messageType);
    emitMessageBuilderConstructor(writer, messageType);
    emitMessageEquals(writer, messageType);
    emitMessageHashCode(writer, messageType);
    emitBuilder(writer, messageType);
  }

  private void emitMessageOptions(JavaWriter writer, Map<String, ?> optionsMap) throws IOException {
    if (optionsMap != null) {
      StringBuilder sb = new StringBuilder();
      sb.append("new MessageOptions.Builder()");
      for (Map.Entry<String, ?> entry : optionsMap.entrySet()) {
        String fqName = entry.getKey();
        ExtensionInfo info = compiler.getExtension(fqName);
        sb.append(String.format("%n.setExtension(Ext_%s.%s, %s)",
            info.location, compiler.getTrailingSegment(fqName),
            compiler.getOptionsMapMaker().createOptionInitializer(entry.getValue(), "", "",
                info.fqType, false, 0)));
      }
      sb.append("\n.build()");
      writer.emitEmptyLine();
      writer.emitField("MessageOptions", "MESSAGE_OPTIONS", EnumSet.of(PUBLIC, STATIC, FINAL),
          sb.toString());
    }
  }

  private void emitEnumOptions(JavaWriter writer, Map<String, ?> optionsMap) throws IOException {
    if (optionsMap != null) {
      StringBuilder sb = new StringBuilder();
      sb.append("new EnumOptions.Builder()");
      for (Map.Entry<String, ?> entry : optionsMap.entrySet()) {
        String fqName = entry.getKey();
        ExtensionInfo info = compiler.getExtension(fqName);
        sb.append(String.format("%n.setExtension(Ext_%s.%s, %s)",
            info.location, compiler.getTrailingSegment(fqName),
            compiler.getOptionsMapMaker().createOptionInitializer(entry.getValue(), "", "",
                info.fqType, false, 0)));
      }
      sb.append("\n.build()");
      writer.emitEmptyLine();
      writer.emitField("EnumOptions", "ENUM_OPTIONS", EnumSet.of(PUBLIC, STATIC, FINAL),
          sb.toString());
    }
  }

  private void emitMessageFieldOptions(JavaWriter writer, MessageType messageType)
      throws IOException {
    Map<String, List<Option>> fieldOptions = new LinkedHashMap<String, List<Option>>();

    for (Field field : messageType.getFields()) {
      List<Option> options = new ArrayList<Option>(field.getOptions());
      for (Iterator<Option> iterator = options.iterator(); iterator.hasNext();) {
        // Remove non-custom key
        String name = iterator.next().getName();
        if (WireCompiler.DEFAULT_FIELD_OPTION_KEYS.contains(name)) {
          iterator.remove();
        }
      }
      if (!options.isEmpty()) {
        fieldOptions.put(field.getName(), options);
      }
    }

    if (!fieldOptions.isEmpty()) {
      writer.emitEmptyLine();
    }

    for (Map.Entry<String, List<Option>> entry : fieldOptions.entrySet()) {
      Map<String, ?> fieldOptionsMap =
          compiler.getOptionsMapMaker().createFieldOptionsMap(messageType, entry.getValue());
      emitFieldOptions(writer, entry.getKey(), fieldOptionsMap);
    }
  }

  private void emitFieldOptions(JavaWriter writer, String fieldName, Map<String, ?> optionsMap)
      throws IOException {
    if (optionsMap == null) return;

    StringBuilder sb = new StringBuilder();
    sb.append("new FieldOptions.Builder()");
    for (Map.Entry<String, ?> entry : optionsMap.entrySet()) {
      String fqName = entry.getKey();
      ExtensionInfo info = compiler.getExtension(fqName);
      if (info == null) {
        throw new WireCompilerException("No extension info for " + fqName);
      }
      sb.append(String.format("%n.setExtension(Ext_%s.%s, %s)",
          info.location,
          compiler.getTrailingSegment(fqName), compiler.getOptionsMapMaker()
          .createOptionInitializer(entry.getValue(), "", "", info.fqType, false, 0)));
    }
    sb.append("\n.build()");
    writer.emitField("FieldOptions", "FIELD_OPTIONS_" + fieldName.toUpperCase(Locale.US),
        EnumSet.of(PUBLIC, STATIC, FINAL), sb.toString());
  }

  // Example:
  //
  // public static final Integer DEFAULT_OPT_INT32 = 123;
  //
  private void emitMessageFieldDefaults(JavaWriter writer, MessageType messageType)
      throws IOException {
    List<Field> defaultFields = new ArrayList<Field>();
    for (Field field : messageType.getFields()) {
      // Message types cannot have defaults
      if (!isMessageType(messageType, field) || FieldInfo.isRepeated(field)) {
        defaultFields.add(field);
      }
    }

    if (!defaultFields.isEmpty()) {
      writer.emitEmptyLine();
    }

    for (Field field : defaultFields) {
      String javaName = getJavaFieldType(messageType, field);
      if (javaName == null) {
        throw new WireCompilerException(
            "Unknown type for field " + field + " in message " + messageType.getName());
      }
      String defaultValue = getDefaultValue(messageType, field);

      writer.emitField(javaName, "DEFAULT_" + field.getName().toUpperCase(Locale.US),
          EnumSet.of(PUBLIC, STATIC, FINAL), defaultValue);
    }
  }

  // Example:
  //
  // /**
  //  * An optional int32
  //  */
  // @ProtoField(
  //   tag = 1,
  //   type = INT32
  // )
  // public final Integer optional_int32;
  //
  private void emitMessageFields(JavaWriter writer, MessageType messageType) throws IOException {
    for (Field field : messageType.getFields()) {
      int tag = field.getTag();

      String fieldType = field.getType();
      String javaName = compiler.javaName(messageType, fieldType);
      Map<String, String> map = new LinkedHashMap<String, String>();
      map.put("tag", String.valueOf(tag));

      boolean isScalar = false;
      boolean isEnum = false;
      if (TypeInfo.isScalar(fieldType)) {
        isScalar = true;
        map.put("type", scalarTypeConstant(fieldType));
      } else {
        String fullyQualifiedName = compiler.fullyQualifiedName(messageType, fieldType);
        isEnum = compiler.isEnum(fullyQualifiedName);
        if (isEnum) map.put("type", "ENUM");
      }

      if (!FieldInfo.isOptional(field)) {
        if (FieldInfo.isPacked(field, isEnum)) {
          map.put("label", "PACKED");
        } else {
          map.put("label", field.getLabel().toString());
        }
      }

      if (FieldInfo.isRepeated(field) && !isScalar) {
        map.put(isEnum ? "enumType" : "messageType", javaName + ".class");
      }

      if (field.isDeprecated()) {
        map.put("deprecated", "true");
      }

      // Scan for redacted fields.
      for (Option option : field.getOptions()) {
        // We allow any package name to be used as long as it ends with '.redacted'.
        if (compiler.isRedacted(option)) {
          map.put("redacted", "true");
          break;
        }
      }

      writer.emitEmptyLine();
      emitDocumentation(writer, field.getDocumentation());
      writer.emitAnnotation(ProtoField.class, map);

      if (field.isDeprecated()) {
        writer.emitAnnotation(Deprecated.class);
      }

      if (FieldInfo.isRepeated(field)) javaName = "List<" + javaName + ">";
      writer.emitField(javaName, sanitize(field.getName()), EnumSet.of(PUBLIC, FINAL));
    }
  }

  // Example:
  //
  // public SimpleMessage(int optional_int32, long optional_int64) {
  //   this.optional_int32 = optional_int32;
  //   this.optional_int64 = optional_int64;
  // }
  //
  private void emitMessageFieldsConstructor(JavaWriter writer, MessageType messageType)
      throws IOException {
    List<String> params = new ArrayList<String>();
    for (Field field : messageType.getFields()) {
      String javaName = getJavaFieldType(messageType, field);
      params.add(javaName);
      params.add(sanitize(field.getName()));
    }

    writer.emitEmptyLine();
    writer.beginMethod(null, messageType.getName(), EnumSet.of(PUBLIC), params, null);
    for (Field field : messageType.getFields()) {
      String sanitizedName = sanitize(field.getName());
      if (FieldInfo.isRepeated(field)) {
        writer.emitStatement("this.%1$s = immutableCopyOf(%1$s)", sanitizedName);
      } else {
        writer.emitStatement("this.%1$s = %1$s", sanitizedName);
      }
    }
    writer.endMethod();
  }

  // Example:
  //
  // private SimpleMessage(Builder builder) {
  //   this(builder.optional_int32, builder.optional_int64);
  //   setBuilder(builder);
  // }
  //
  private void emitMessageBuilderConstructor(JavaWriter writer, MessageType messageType)
      throws IOException {
    writer.emitEmptyLine();
    writer.beginMethod(null, messageType.getName(), EnumSet.of(PRIVATE), "Builder", "builder");
    StringBuilder params = new StringBuilder();
    for (Field field : messageType.getFields()) {
      if (params.length() > 0) {
        params.append(", ");
      }
      params.append("builder.");
      params.append(sanitize(field.getName()));
    }
    if (params.length() > 0) {
      writer.emitStatement("this(%1$s)", params);
    }
    writer.emitStatement("setBuilder(builder)");
    writer.endMethod();
  }

  // Example:
  //
  // @Override
  // public boolean equals(Object other) {
  //   if (other == this) return true;
  //   if (!(other instanceof SimpleMessage)) return false;
  //   SimpleMessage o = (SimpleMessage) other;
  //   if (!Wire.equals(optional_int32, o.optional_int32)) return false;
  //   return true;
  //
  private void emitMessageEquals(JavaWriter writer, MessageType messageType) throws IOException {
    writer.emitEmptyLine();
    writer.emitAnnotation(Override.class);
    writer.beginMethod("boolean", "equals", EnumSet.of(PUBLIC), "Object", "other");

    List<Field> fields = messageType.getFields();
    if (fields.isEmpty()) {
      writer.emitStatement("return other instanceof %s", messageType.getName());
    } else {
      writer.emitStatement("if (other == this) return true");
      writer.emitStatement("if (!(other instanceof %s)) return false", messageType.getName());
      if (hasOnlyOneField(messageType)) {
        String name = sanitize(fields.get(0).getName());
        // If the field is named "other" or "o", qualify the field reference with 'this'
        writer.emitStatement("return equals(%1$s, ((%2$s) other).%3$s)",
            addThisIfOneOf(name, "other", "o"), messageType.getName(), name);
      } else {
        writer.emitStatement("%1$s o = (%1$s) other", messageType.getName());
        if (compiler.hasExtensions(messageType)) {
          writer.emitStatement("if (!extensionsEqual(o)) return false");
        }
        StringBuilder sb = new StringBuilder();
        String prefix = "return ";
        for (Field field : fields) {
          sb.append(prefix);
          prefix = "\n&& ";
          // If the field is named "other" or "o", qualify the field reference with 'this'
          String name = sanitize(field.getName());
          sb.append(String.format("equals(%1$s, o.%2$s)",
              addThisIfOneOf(name, "other", "o"), name));
        }
        writer.emitStatement(sb.toString());
      }
    }
    writer.endMethod();
  }

  // Example:
  //
  // @Override
  // public int hashCode() {
  //   if (hashCode == 0) {
  //     int result = super.extensionsHashCode();
  //     result = result * 37 + (f != null ? f.hashCode() : 0);
  //     hashCode = result;
  //   }
  //   return hashCode;
  // }
  //
  // For repeated fields, the final "0" in the example above changes to a "1"
  // in order to be the same as the system hash code for an empty list.
  //
  private void emitMessageHashCode(JavaWriter writer, MessageType messageType)
      throws IOException {
    writer.emitEmptyLine();
    writer.emitAnnotation(Override.class);
    writer.beginMethod("int", "hashCode", EnumSet.of(PUBLIC));

    if (!compiler.hasFields(messageType) && !compiler.hasExtensions(messageType)) {
      writer.emitStatement("return 0");
    } else if (hasOnlyOneField(messageType)) {
      Field field = messageType.getFields().get(0);
      String name = sanitize(field.getName());
      // If the field is named "result", qualify the field reference with 'this'
      name = addThisIfOneOf(name, "result");
      writer.emitStatement("int result = hashCode");
      writer.emitStatement(
          "return result != 0 ? result : (hashCode = %1$s != null ? %1$s.hashCode() : %2$s)", name,
          nullHashValue(field));
    } else {
      writer.emitStatement("int result = hashCode");
      writer.beginControlFlow("if (result == 0)");
      boolean afterFirstAssignment = false;
      if (compiler.hasExtensions(messageType)) {
        writer.emitStatement("result = extensionsHashCode()");
        afterFirstAssignment = true;
      }
      for (Field field : messageType.getFields()) {
        String name = sanitize(field.getName());
        // If the field is named "result", qualify the field reference with 'this'
        name = addThisIfOneOf(name, "result");
        if (afterFirstAssignment) {
          writer.emitStatement("result = result * 37 + (%1$s != null ? %1$s.hashCode() : %2$s)",
              name, nullHashValue(field));
        } else {
          writer.emitStatement("result = %1$s != null ? %1$s.hashCode() : %2$s", name,
              nullHashValue(field));
          afterFirstAssignment = true;
        }
      }
      writer.emitStatement("hashCode = result");
      writer.endControlFlow();
      writer.emitStatement("return result");
    }
    writer.endMethod();
  }

  private int nullHashValue(Field field) {
    return FieldInfo.isRepeated(field) ? 1 : 0;
  }

  private void emitBuilder(JavaWriter writer, MessageType messageType) throws IOException {
    writer.emitEmptyLine();
    writer.beginType("Builder", "class", EnumSet.of(PUBLIC, STATIC, FINAL),
        (compiler.hasExtensions(messageType) ? "ExtendableBuilder<" : "Message.Builder<")
            + messageType.getName()
            + ">");
    emitBuilderFields(writer, messageType);
    emitBuilderConstructors(writer, messageType);
    emitBuilderSetters(writer, messageType);
    if (compiler.hasExtensions(messageType)) emitBuilderSetExtension(writer, messageType);
    emitBuilderBuild(writer, messageType);
    writer.endType();
  }

  private void emitBuilderFields(JavaWriter writer, MessageType messageType) throws IOException {
    List<Field> fields = messageType.getFields();

    if (!fields.isEmpty()) writer.emitEmptyLine();
    for (Field field : fields) {
      String javaName = getJavaFieldType(messageType, field);
      writer.emitField(javaName, sanitize(field.getName()), EnumSet.of(PUBLIC));
    }
  }

  // Example:
  //
  // public Builder() {
  // }
  //
  // public Builder(SimpleMessage message) {
  //   super(message);
  //   if (message == null) return;
  //   this.optional_int32 = message.optional_int32;
  //   ...
  // }
  //
  private void emitBuilderConstructors(JavaWriter writer, MessageType messageType)
      throws IOException {
    writer.emitEmptyLine();
    writer.beginMethod(null, "Builder", EnumSet.of(PUBLIC));
    writer.endMethod();

    writer.emitEmptyLine();
    writer.beginMethod(null, "Builder", EnumSet.of(PUBLIC), messageType.getName(), "message");
    writer.emitStatement("super(message)");
    List<Field> fields = messageType.getFields();
    if (!fields.isEmpty()) writer.emitStatement("if (message == null) return");
    for (Field field : fields) {
      if (FieldInfo.isRepeated(field)) {
        writer.emitStatement("this.%1$s = copyOf(message.%1$s)",
            sanitize(field.getName()));
      } else {
        writer.emitStatement("this.%1$s = message.%1$s", sanitize(field.getName()));
      }
    }
    writer.endMethod();
  }

  private void emitBuilderSetters(JavaWriter writer, MessageType messageType) throws IOException {
    for (Field field : messageType.getFields()) {
      String javaName = getJavaFieldType(messageType, field);
      List<String> args = new ArrayList<String>();
      args.add(javaName);
      String sanitized = sanitize(field.getName());
      args.add(sanitized);

      writer.emitEmptyLine();

      emitDocumentation(writer, field.getDocumentation());

      if (field.isDeprecated()) {
        writer.emitAnnotation(Deprecated.class);
      }

      writer.beginMethod("Builder", sanitized, EnumSet.of(PUBLIC), args, null);
      if (FieldInfo.isRepeated(field)) {
        writer.emitStatement("this.%1$s = checkForNulls(%1$s)", sanitized);
      } else {
        writer.emitStatement("this.%1$s = %1$s", sanitized);
      }
      writer.emitStatement("return this");
      writer.endMethod();
    }
  }

  // Example:
  //
  // @Override
  // public <E> Builder setExtension(Extension<ExternalMessage, E> extension, E value) {
  //   super.setExtension(extension, value);
  //   return this;
  // }
  //
  private void emitBuilderSetExtension(JavaWriter writer, MessageType messageType)
      throws IOException {
    writer.emitEmptyLine();
    writer.emitAnnotation(Override.class);
    writer.beginMethod("<E> Builder", "setExtension", EnumSet.of(PUBLIC),
        "Extension<" + messageType.getName() + ", E>", "extension", "E", "value");
    writer.emitStatement("super.setExtension(extension, value)");
    writer.emitStatement("return this");
    writer.endMethod();
  }

  // Example:
  //
  // @Override
  // public SimpleMessage build() {
  //   checkRequiredFields();
  //   return new SimpleMessage(this);
  // }
  //
  // The call to checkRequiredFields will be emitted only if the message has
  // required fields.
  //
  private void emitBuilderBuild(JavaWriter writer, MessageType messageType) throws IOException {
    writer.emitEmptyLine();
    writer.emitAnnotation(Override.class);
    writer.beginMethod(messageType.getName(), "build", EnumSet.of(PUBLIC));
    if (hasRequiredFields(messageType)) {
      writer.emitStatement("checkRequiredFields()");
    }
    writer.emitStatement("return new %s(this)", messageType.getName());
    writer.endMethod();
  }

  private String addThisIfOneOf(String name, String... matches) {
    for (String match : matches) {
      if (match.equals(name)) {
        return "this." + name;
      }
    }
    return name;
  }

  private String getDefaultValue(MessageType messageType, Field field) {
    String initialValue = field.getDefault();
    if (FieldInfo.isRepeated(field)) return "Collections.emptyList()";
    String javaName = compiler.javaName(messageType, field.getType());
    if (TypeInfo.isScalar(field.getType())) {
      return compiler.getInitializerForType(initialValue, javaName);
    } else {
      if (initialValue != null) {
        return javaName + "." + initialValue;
      } else {
        String fullyQualifiedName = compiler.fullyQualifiedName(messageType, field.getType());
        if (compiler.isEnum(fullyQualifiedName)) {
          return javaName + "." + compiler.getEnumDefault(fullyQualifiedName);
        } else {
          throw new WireCompilerException(
              "Field " + field + " cannot have default value");
        }
      }
    }
  }

  private String getJavaFieldType(MessageType messageType, Field field) {
    return getJavaFieldType(compiler.getProtoFile(), messageType, field);
  }

  private String getJavaFieldType(ProtoFile protoFile, MessageType messageType, Field field) {
    String javaName = compiler.javaName(protoFile, messageType, field.getType());
    if (FieldInfo.isRepeated(field)) javaName = "List<" + javaName + ">";
    return javaName;
  }

  private static boolean hasDocumentation(String documentation) {
    return documentation != null && !documentation.isEmpty();
  }

  private boolean hasOnlyOneField(MessageType messageType) {
    return messageType.getFields().size() == 1 && !compiler.hasExtensions(messageType);
  }

  private boolean hasRequiredFields(Type type) {
    if (type instanceof MessageType) {
      for (Field field : ((MessageType) type).getFields()) {
        if (FieldInfo.isRequired(field)) return true;
      }
    }
    return false;
  }

  private boolean isMessageType(MessageType messageType, Field field) {
    return !TypeInfo.isScalar(field.getType())
        && !compiler.isEnum(compiler.fullyQualifiedName(messageType, field.getType()));
  }

  private String sanitize(String name) {
    return JAVA_KEYWORDS.contains(name) ? "_" + name : name;
  }

  /**
   * Returns the name of the {@code Message} type constant (e.g.,
   * {@code INT32} or {@code STRING}) associated
   * with the given scalar type (e.g., {@code int32} or {@code string}).
   */
  private String scalarTypeConstant(String type) {
    return type.toUpperCase(Locale.US);
  }
}
