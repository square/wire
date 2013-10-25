package com.squareup.wire;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.Type;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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
  private final JavaWriter writer;

  public MessageWriter(WireCompiler compiler) {
    this.compiler = compiler;
    this.writer = compiler.getWriter();
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

  public void emitHeader(Set<String> imports,
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

  public void emitType(Type type, String currentType, Map<String, ?> optionsMap, boolean topLevel)
      throws IOException {
    writer.emitEmptyLine();
    if (type instanceof MessageType) {
      emitAll((MessageType) type, optionsMap, topLevel);
      for (Type nestedType : type.getNestedTypes()) {
        emitType(nestedType, currentType + nestedType.getName() + ".", optionsMap, false);
      }
      writer.endType();
    } else if (type instanceof EnumType) {
      EnumType enumType = (EnumType) type;
      writer.beginType(enumType.getName(), "enum", EnumSet.of(PUBLIC));
      for (EnumType.Value value : enumType.getValues()) {
        MessageWriter.emitDocumentation(writer, value.getDocumentation());
        writer.emitAnnotation(ProtoEnum.class, value.getTag());
        writer.emitEnumValue(value.getName());
      }
      writer.endType();
    }
  }

  private void emitAll(MessageType messageType, Map<String, ?> optionsMap, boolean topLevel)
      throws IOException {
    Set<Modifier> modifiers = EnumSet.of(PUBLIC, FINAL);
    if (!topLevel) {
      modifiers.add(STATIC);
    }

    String name = messageType.getName();
    emitDocumentation(writer, messageType.getDocumentation());
    writer.beginType(name, "class", modifiers,
        compiler.hasExtensions(messageType) ? "ExtendableMessage<" + name + ">" : "Message");

    emitMessageOptions(optionsMap);
    emitMessageDefaults(messageType);
    emitMessageFields(messageType);
    emitMessageConstructor(messageType);
    emitMessageEquals(messageType);
    emitMessageHashCode(messageType);
    emitBuilder(messageType);
  }

  private void emitMessageOptions(Map<String, ?> optionsMap) throws IOException {
    if (optionsMap != null) {
      StringBuilder sb = new StringBuilder();
      sb.append("new MessageOptions.Builder()");
      for (Map.Entry<String, ?> entry : optionsMap.entrySet()) {
        String fqName = entry.getKey();
        ExtensionInfo info = compiler.getExtension(fqName);
        sb.append(String.format("%n%s.setExtension(Ext_%s.%s, %s)",
            WireCompiler.INDENT + WireCompiler.LINE_WRAP_INDENT,
            info.location, compiler.getTrailingSegment(fqName),
            compiler.getMessageOptionsMapMaker().createOptionInitializer(entry.getValue(), "", "",
                info.fqType, false, 1)));
      }
      sb.append("\n").append(WireCompiler.INDENT)
          .append(WireCompiler.LINE_WRAP_INDENT).append(".build()");
      writer.emitEmptyLine();
      writer.emitField("MessageOptions", "MESSAGE_OPTIONS", EnumSet.of(PUBLIC, STATIC, FINAL),
          sb.toString());
    }
  }

  // Example:
  //
  // public static final Integer DEFAULT_OPT_INT32 = 123;
  //
  private void emitMessageDefaults(MessageType messageType) throws IOException {
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
        throw new IllegalArgumentException(
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
  private void emitMessageFields(MessageType messageType) throws IOException {
    Set<Integer> tags = new HashSet<Integer>();

    for (Field field : messageType.getFields()) {
      // Check for duplicate tags
      int tag = field.getTag();
      if (tags.contains(tag)) {
        throw new RuntimeException("Duplicate tag value for field "
            + messageType.getFullyQualifiedName() + "." + field.getName());
      }
      tags.add(tag);

      String fieldType = field.getType();
      String javaName = compiler.javaName(messageType, fieldType);
      Map<String, String> map = new LinkedHashMap<String, String>();
      map.put("tag", String.valueOf(tag));

      boolean isEnum = false;
      if (TypeInfo.isScalar(fieldType)) {
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

      writer.emitEmptyLine();
      emitDocumentation(writer, field.getDocumentation());
      writer.emitAnnotation(ProtoField.class, map);

      if (FieldInfo.isRepeated(field)) javaName = "List<" + javaName + ">";
      writer.emitField(javaName, sanitize(field.getName()), EnumSet.of(PUBLIC, FINAL));
    }
  }

  // Example:
  //
  // private SimpleMessage(Builder builder) {
  //   super(builder);
  //   this.optional_int32 = builder.optional_int32;
  // }
  //
  private void emitMessageConstructor(MessageType messageType) throws IOException {
    writer.emitEmptyLine();
    writer.beginMethod(null, messageType.getName(), EnumSet.of(PRIVATE), "Builder", "builder");
    writer.emitStatement("super(builder)");
    for (Field field : messageType.getFields()) {
      if (FieldInfo.isRepeated(field)) {
        writer.emitStatement("this.%1$s = immutableCopyOf(builder.%1$s)",
            sanitize(field.getName()));
      } else {
        writer.emitStatement("this.%1$s = builder.%1$s", sanitize(field.getName()));
      }
    }
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
  private void emitMessageEquals(MessageType messageType) throws IOException {
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
  private void emitMessageHashCode(MessageType messageType) throws IOException {
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
          "return result != 0 ? result : (hashCode = %1$s != null ? %1$s.hashCode() : 0)", name);
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
          writer.emitStatement("result = result * 37 + (%1$s != null ? %1$s.hashCode() : 0)", name);
        } else {
          writer.emitStatement("result = %1$s != null ? %1$s.hashCode() : 0", name);
          afterFirstAssignment = true;
        }
      }
      writer.emitStatement("hashCode = result");
      writer.endControlFlow();
      writer.emitStatement("return result");
    }
    writer.endMethod();
  }

  private void emitBuilder(MessageType messageType) throws IOException {
    writer.emitEmptyLine();
    writer.beginType("Builder", "class", EnumSet.of(PUBLIC, STATIC, FINAL),
        (compiler.hasExtensions(messageType) ? "ExtendableBuilder<" : "Message.Builder<")
            + messageType.getName()
            + ">");
    emitBuilderFields(messageType);
    emitBuilderConstructors(messageType);
    emitBuilderSetters(messageType);
    if (compiler.hasExtensions(messageType)) emitBuilderSetExtension(messageType);
    emitBuilderBuild(messageType);
    writer.endType();
  }

  private void emitBuilderFields(MessageType messageType) throws IOException {
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
  private void emitBuilderConstructors(MessageType messageType) throws IOException {
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

  private void emitBuilderSetters(MessageType messageType) throws IOException {
    for (Field field : messageType.getFields()) {
      String javaName = getJavaFieldType(messageType, field);
      List<String> args = new ArrayList<String>();
      args.add(javaName);
      String sanitized = sanitize(field.getName());
      args.add(sanitized);

      writer.emitEmptyLine();

      emitDocumentation(writer, field.getDocumentation());
      writer.beginMethod("Builder", sanitized, EnumSet.of(PUBLIC), args, null);
      writer.emitStatement("this.%1$s = %1$s", sanitized);
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
  private void emitBuilderSetExtension(MessageType messageType) throws IOException {
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
  private void emitBuilderBuild(MessageType messageType) throws IOException {
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
          throw new IllegalArgumentException(
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
