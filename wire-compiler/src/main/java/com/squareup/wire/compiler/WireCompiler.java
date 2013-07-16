// Copyright 2013 Square, Inc.
package com.squareup.wire.compiler;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.ExtendDeclaration;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoSchemaParser;
import com.squareup.protoparser.Type;
import com.squareup.wire.ProtoEnum;
import com.squareup.wire.ProtoField;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import javax.lang.model.element.Modifier;

import static com.squareup.protoparser.MessageType.Field;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

/**
 * Compiler for Wire protocol buffers.
 */
public class WireCompiler {

  private static final Charset UTF_8 = Charset.forName("UTF8");
  private static final Map<String, String> JAVA_TYPES = new HashMap<String, String>();
  private static final Map<String, String> PROTO_FIELD_TYPES = new HashMap<String, String>();
  private static final Set<String> PACKABLE_TYPES = new HashSet<String>();
  private static final Set<String> JAVA_KEYWORDS = new HashSet<String>(Arrays.asList(
      "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
      "class", "const", "continue", "default", "do", "double", "else", "enum", "extends",
      "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
      "int", "interface", "long", "native", "new", "package", "private", "protected", "public",
      "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
      "throw", "throws", "transient", "try", "void", "volatile", "while"));

  private static final String PROTO_PATH_FLAG = "--proto_path=";
  private static final String JAVA_OUT_FLAG = "--java_out=";
  private static final String FILES_FLAG = "--files=";

  static {
    JAVA_TYPES.put("bool", "Boolean");
    JAVA_TYPES.put("bytes", "byte[]");
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

    PROTO_FIELD_TYPES.put("bool", "Wire.BOOL");
    PROTO_FIELD_TYPES.put("bytes", "Wire.BYTES");
    PROTO_FIELD_TYPES.put("double", "Wire.DOUBLE");
    PROTO_FIELD_TYPES.put("float", "Wire.FLOAT");
    PROTO_FIELD_TYPES.put("fixed32", "Wire.FIXED32");
    PROTO_FIELD_TYPES.put("fixed64", "Wire.FIXED64");
    PROTO_FIELD_TYPES.put("int32", "Wire.INT32");
    PROTO_FIELD_TYPES.put("int64", "Wire.INT64");
    PROTO_FIELD_TYPES.put("sfixed32", "Wire.SFIXED32");
    PROTO_FIELD_TYPES.put("sfixed64", "Wire.SFIXED64");
    PROTO_FIELD_TYPES.put("sint32", "Wire.SINT32");
    PROTO_FIELD_TYPES.put("sint64", "Wire.SINT64");
    PROTO_FIELD_TYPES.put("string", "Wire.STRING");
    PROTO_FIELD_TYPES.put("uint32", "Wire.UINT32");
    PROTO_FIELD_TYPES.put("uint64", "Wire.UINT64");

    PACKABLE_TYPES.add("bool");
    PACKABLE_TYPES.add("double");
    PACKABLE_TYPES.add("float");
    PACKABLE_TYPES.add("fixed32");
    PACKABLE_TYPES.add("fixed64");
    PACKABLE_TYPES.add("int32");
    PACKABLE_TYPES.add("int64");
    PACKABLE_TYPES.add("sfixed32");
    PACKABLE_TYPES.add("sfixed64");
    PACKABLE_TYPES.add("sint32");
    PACKABLE_TYPES.add("sint64");
    PACKABLE_TYPES.add("uint32");
    PACKABLE_TYPES.add("uint64");
  }

  private final String repoPath;
  private final ProtoFile protoFile;
  private final Set<String> loadedDependencies = new HashSet<String>();
  private final Map<String, String> javaSymbolMap = new HashMap<String, String>();
  private final Set<String> enumTypes = new HashSet<String>();
  private final Map<String, String> enumDefaults = new HashMap<String, String>();

  private String protoFileName;
  private String typeBeingGenerated = "";
  private JavaWriter writer;

  /**
   * Runs the compiler. Usage:
   *
   * <pre>
   * java WireCompiler --proto_path=<path> --java_out=<path> [--files=<protos.include>]
   *     [file [file...]]
   * </pre>
   */
  public static void main(String... args) throws Exception {
    String protoPath = null;
    String javaOut = null;
    List<String> sourceFilenames = new ArrayList<String>();

    int index = 0;
    while (index < args.length) {
      if (args[index].startsWith(PROTO_PATH_FLAG)) {
        protoPath = args[index].substring(PROTO_PATH_FLAG.length());
      } else if (args[index].startsWith(JAVA_OUT_FLAG)) {
        javaOut = args[index].substring(JAVA_OUT_FLAG.length());
      } else if (args[index].startsWith(FILES_FLAG)) {
        File files = new File(args[index].substring(FILES_FLAG.length()));
        String[] filenames = new Scanner(files, "UTF8").useDelimiter("\\A").next().split("\n");
        sourceFilenames.addAll(Arrays.asList(filenames));
      } else {
        sourceFilenames.add(args[index]);
      }
      index++;
    }
    if (protoPath == null) {
      System.err.println("Must specify " + PROTO_PATH_FLAG + " flag");
      System.exit(1);
    }
    if (javaOut == null) {
      System.err.println("Must specify " + JAVA_OUT_FLAG + " flag");
      System.exit(1);
    }
    for (String sourceFilename : sourceFilenames) {
      WireCompiler wireCompiler = new WireCompiler(protoPath, sourceFilename);
      wireCompiler.compile(javaOut);
    }
  }

  public WireCompiler(String protoPath, String sourceFilename) throws IOException {
    this.repoPath = protoPath;
    String filename = protoPath + "/" + sourceFilename;
    System.out.println("Reading proto source file " + filename);
    this.protoFile = ProtoSchemaParser.parse(new File(protoPath + "/" + sourceFilename));
  }

  public JavaWriter getJavaWriter(String javaOut, String className)
      throws IOException {
    String javaPackage = protoFile.getJavaPackage();
    String directory = javaOut + "/" + javaPackage.replace(".", "/");
    boolean created = new File(directory).mkdirs();
    if (created) {
      System.out.println("Created output directory " + directory);
    }

    String fileName = directory + "/" + className + ".java";
    System.out.println("Writing generated code to " + fileName);
    return new JavaWriter(new OutputStreamWriter(new FileOutputStream(fileName), UTF_8));
  }

  public void compile(String javaOut) throws IOException {
    protoFileName = protoFileName(protoFile.getFileName());
    loadSymbols(protoFile);

    if (hasExtends()) {
      try {
        writer = getJavaWriter(javaOut, "Ext_" + protoFileName);
        emitExtensionClass();
      } finally {
        writer.close();
      }
    }

    for (Type type : protoFile.getTypes()) {
      String savedType = typeBeingGenerated;
      typeBeingGenerated += type.getName() + ".";
      emitMessageClass(javaOut, type);
      typeBeingGenerated = savedType;
    }
  }

  private String protoFileName(String path) {
    int rindex = path.lastIndexOf('/');
    if (rindex != -1) {
      path = path.substring(rindex + 1);
    }
    if (path.endsWith(".proto")) {
      path = path.substring(0, path.length() - ".proto".length());
    }
    return path;
  }

  private void emitMessageClass(String javaOut, Type type) throws IOException {
    try {
      writer = getJavaWriter(javaOut, type.getName());
      writer.emitJavadoc("Code generated by \"Wire\" protobuf compiler, do not edit."
          + "\nSource file: %s", protoFile.getFileName());
      writer.emitPackage(protoFile.getJavaPackage());

      List<Type> types = getTypes(type);
      boolean hasMessage = hasMessage(types);
      boolean hasExtensions = hasExtensions(Arrays.asList(type));

      Set<String> imports = new HashSet<String>();
      if (hasMessage) {
        imports.add("com.squareup.wire.Message");
        if (hasFields(type)) {
          imports.add("com.squareup.wire.ProtoField");
          if (hasRequiredFields(types)) {
            imports.add("com.squareup.wire.UninitializedMessageException");
          }
          imports.add("com.squareup.wire.Wire");
        }
      }
      if (hasEnum(types)) {
        imports.add("com.squareup.wire.ProtoEnum");
      }
      if (hasRepeatedField(types)) {
        imports.add("java.util.Collections");
        imports.add("java.util.List");
      }
      if (hasExtensions) {
        imports.add("java.util.Collections");
        imports.add("java.util.Map");
        imports.add("java.util.TreeMap");
      }
      imports.addAll(getExternalTypes(type));
      writer.emitImports(imports);

      emitType(type, protoFile.getPackageName() + ".", true);
    } finally {
      writer.close();
    }
  }

  private List<Type> getTypes(Type root) {
    List<Type> types = new ArrayList<Type>();
    getTypes(root, types);
    return types;
  }

  private void getTypes(Type root, List<Type> types) {
    types.add(root);
    for (Type nestedType : root.getNestedTypes()) {
      getTypes(nestedType, types);
    }
  }

  private List<String> getExternalTypes(Type root) {
    List<String> types = new ArrayList<String>();
    getExternalTypes(root, types);
    return types;
  }

  private void getExternalTypes(Type root, List<String> types) {
    if (root instanceof MessageType) {
      MessageType messageType = (MessageType) root;
      for (Field field : messageType.getFields()) {
        String fqName = fqJavaName(messageType, field.getType());
        if (fqName != null && !fqName.startsWith(protoFile.getJavaPackage())) {
          types.add(fqName);
        }
      }
    }
    for (Type nestedType : root.getNestedTypes()) {
      getExternalTypes(nestedType, types);
    }
  }

  private List<String> getExtensionTypes() {
    List<String> extensionClasses = new ArrayList<String>();
    for (ExtendDeclaration extend : protoFile.getExtendDeclarations()) {
      String fullyQualifiedName = extend.getFullyQualifiedName();
      String javaName = javaName(null, fullyQualifiedName);
      String name = shortenJavaName(javaName);
      // Only include names outside our own package
      if (name.contains(".")) {
        extensionClasses.add(name);
      }
    }
    return extensionClasses;
  }

  private boolean hasExtends() {
    return !protoFile.getExtendDeclarations().isEmpty();
  }

  private void emitExtensionClass() throws IOException {
    writer.emitJavadoc("Code generated by \"Wire\" protobuf compiler, do not edit."
        + "\nSource file: %s", protoFile.getFileName());
    writer.emitPackage(protoFile.getJavaPackage());

    Set<String> imports = new HashSet<String>();
    imports.add("com.squareup.wire.Wire");
    imports.add("java.util.List");
    imports.addAll(getExtensionTypes());
    writer.emitImports(imports);
    writer.emitEmptyLine();

    writer.emitStaticImports("com.squareup.wire.Message.ExtendableMessage.Extension");
    writer.emitEmptyLine();

    String className = "Ext_" + protoFileName;
    writer.beginType(className, "class", EnumSet.of(PUBLIC, FINAL));
    writer.emitEmptyLine();

    // Private no-args constructor
    writer.beginMethod(null, className, EnumSet.of(PRIVATE));
    writer.endMethod();
    writer.emitEmptyLine();

    emitExtensions();
    writer.endType();
  }

  private void emitExtensions() throws IOException {
    for (ExtendDeclaration extend : protoFile.getExtendDeclarations()) {
      String fullyQualifiedName = extend.getFullyQualifiedName();
      String javaName = javaName(null, fullyQualifiedName);
      String name = shortenJavaName(javaName);
      for (MessageType.Field field : extend.getFields()) {
        String fieldType = field.getType();
        String type = javaName(null, fieldType);
        boolean isEnum = isEnum(fieldType);
        if (type == null) {
          String qualifiedFieldType = protoFile.getPackageName() + "." + fieldType;
          isEnum = isEnum(qualifiedFieldType);
          type = javaName(null, qualifiedFieldType);
        }
        type = shortenJavaName(type);
        String initialValue;
        String className = writer.compressType(name);
        if (isScalar(field)) {
          if (isRepeated(field)) {
            initialValue = String.format("Extension.getRepeatedExtension(%s.class, %s, %s, %s)",
                className, field.getTag(), protoFieldType(field.getType()), isPacked(field, false));
          } else {
            initialValue = String.format("Extension.getExtension(%s.class, %s, %s, Wire.%s)",
                className, field.getTag(), protoFieldType(field.getType()), field.getLabel());
          }
        } else if (isEnum) {
          if (isRepeated(field)) {
            initialValue =
                String.format("Extension.getRepeatedEnumExtension(%s.class, %s, %s, %s.class)",
                    className, field.getTag(), isPacked(field, true), type);
          } else {
            initialValue =
                String.format("Extension.getEnumExtension(%s.class, %s, Wire.%s, %s.class)",
                    className, field.getTag(), field.getLabel(), type);
          }
        } else {
          if (isRepeated(field)) {
            initialValue =
                String.format("Extension.getRepeatedMessageExtension(%s.class, %s, %s.class)",
                    className, field.getTag(), type);
          } else {
            initialValue =
                String.format("Extension.getMessageExtension(%s.class, %s, Wire.%s, %s.class)",
                    className, field.getTag(), field.getLabel(), type);
          }
        }
        if (isRepeated(field)) {
          type = "List<" + type + ">";
        }
        writer.emitField("Extension<" + name + ", " + type + ">", field.getName(),
            EnumSet.of(PUBLIC, STATIC, FINAL), initialValue);
      }
    }
  }

  private boolean hasExtensions(MessageType messageType) {
    return !messageType.getExtensions().isEmpty();
  }

  private void emitType(Type type, String currentType, boolean topLevel) throws IOException {
    writer.emitEmptyLine();
    if (type instanceof MessageType) {
      MessageType messageType = (MessageType) type;
      boolean hasExtensions = hasExtensions(messageType);

      String name = type.getName();
      Set<Modifier> modifiers = EnumSet.of(PUBLIC, FINAL);
      if (!topLevel) {
        modifiers.add(STATIC);
      }
      writer.beginType(name, "class", modifiers, null,
          hasExtensions ? "Message.ExtendableMessage<" + name + ">" : "Message");
      emitMessageDefaults(messageType);
      if (hasExtensions) {
        writer.emitEmptyLine();
        writer.emitField("Map<Extension<" + name + ", ?>, Object>",
            "extensionMap", EnumSet.of(PUBLIC, FINAL));
      }
      emitMessageFields(messageType);
      emitMessageConstructor(messageType);
      if (hasExtensions) {
        emitMessageGetExtension(messageType);
      }
      emitMessageEquals(messageType);
      emitMessageHashCode(messageType);
      emitMessageToString(messageType);
      emitBuilder(messageType);

      for (Type nestedType : type.getNestedTypes()) {
        emitType(nestedType, currentType + nestedType.getName() + ".", false);
      }

      writer.endType();
    } else if (type instanceof EnumType) {
      EnumType enumType = (EnumType) type;
      writer.beginType(enumType.getName(), "enum", EnumSet.of(PUBLIC));
      for (EnumType.Value value : enumType.getValues()) {
        writer.emitAnnotation(ProtoEnum.class, value.getTag());
        writer.emitEnumValue(value.getName());
      }
      writer.endType();
    }
  }

  // Example:
  //
  // public static final Integer DEFAULT_OPT_INT32 = 123;
  //
  private void emitMessageDefaults(MessageType messageType)
      throws IOException {
    List<Field> fields = messageType.getFields();
    if (!fields.isEmpty()) {
      writer.emitEmptyLine();
    }
    for (Field field : fields) {
      String javaName = getJavaType(messageType, field);
      String defaultValue = getDefaultValue(messageType, field);

      writer.emitField(javaName, "DEFAULT_" + sanitize(field.getName()).toUpperCase(Locale.US),
          EnumSet.of(PUBLIC, STATIC, FINAL), defaultValue);
    }
  }

  private String sanitize(String name) {
    if (JAVA_KEYWORDS.contains(name)) {
      return "_" + name;
    }
    return name;
  }

  // Example:
  //
  // /**
  //  * An optional int32
  //  */
  // @ProtoField(
  //   tag = 1,
  //   type = Wire.INT32
  // )
  // public final Integer optional_int32;
  //
  private void emitMessageFields(MessageType messageType)
      throws IOException {
    for (Field field : messageType.getFields()) {
      String javaName = javaName(messageType, field.getType());
      Map<String, String> map = new LinkedHashMap<String, String>();
      map.put("tag", String.valueOf(field.getTag()));

      String type = field.getType();
      boolean isEnum = false;
      if (isScalar(field)) {
        map.put("type", protoFieldType(type));
      } else {
        String fullyQualifiedName = fullyQualifiedName(messageType, type);
        isEnum = isEnum(fullyQualifiedName);
        if (isEnum) {
          map.put("type", "Wire.ENUM");
        }
      }

      if (isPacked(field, isEnum)) {
        map.put("packed", "true");
      }

      if (!isOptional(field)) {
        map.put("label", "Wire." + field.getLabel().toString());
      }

      writer.emitEmptyLine();
      String documentation = field.getDocumentation();
      if (hasDocumentation(documentation)) {
        writer.emitJavadoc(documentation.replace("%", "%%"));
      }
      writer.emitAnnotation(ProtoField.class, map);

      if (isRepeated(field)) {
        javaName = "List<" + javaName + ">";
      }
      writer.emitField(javaName, sanitize(field.getName()), EnumSet.of(PUBLIC, FINAL));
    }
  }

  // Example:
  //
  // private SimpleMessage(Builder builder) {
  //   this.optional_int32 = builder.optional_int32;
  // }
  //
  private void emitMessageConstructor(MessageType messageType) throws IOException {
    writer.emitEmptyLine();
    writer.beginMethod(null, messageType.getName(), EnumSet.of(PRIVATE), "Builder", "builder");
    for (Field field : messageType.getFields()) {
      if (isRepeated(field)) {
        writer.emitStatement("this.%1$s = Wire.unmodifiableCopyOf(builder.%1$s)",
            sanitize(field.getName()));
      } else {
        writer.emitStatement("this.%1$s = builder.%1$s", sanitize(field.getName()));
      }
    }
    if (hasExtensions(messageType)) {
      writer.emitStatement("this.extensionMap = Collections.unmodifiableMap(new TreeMap<Extension<"
          + messageType.getName() + ", ?>, Object>(builder.extensionMap))");
    }
    writer.endMethod();
  }

  // Example:
  //
  // @Override public <Type> Type getExtension(Extension<ExternalMessage, Type> extension) {
  //   return (Type) extensionMap.get(extension);
  // }
  //
  private void emitMessageGetExtension(MessageType messageType)
      throws IOException {
    writer.emitEmptyLine();
    writer.emitAnnotation(Override.class);
    writer.emitAnnotation(SuppressWarnings.class, "\"unchecked\"");
    writer.beginMethod("<Type> Type", "getExtension", EnumSet.of(PUBLIC),
        "Extension<" + messageType.getName() + ", Type>", "extension");
    writer.emitStatement("return (Type) extensionMap.get(extension)");
    writer.endMethod();
  }

  // Example:
  //
  // @Override
  // public boolean equals(Object other) {
  //   if (!(other instanceof SimpleMessage)) return false;
  //   SimpleMessage o = (SimpleMessage) other;
  //   if (!Wire.equals(optional_int32, o.optional_int32)) return false;
  //   return true;
  //
  private void emitMessageEquals(MessageType messageType)
      throws IOException {
    writer.emitEmptyLine();
    writer.emitAnnotation(Override.class);
    writer.beginMethod("boolean", "equals", EnumSet.of(PUBLIC), "Object", "other");

    List<Field> fields = messageType.getFields();
    if (fields.isEmpty()) {
      writer.emitStatement("return other instanceof %s", messageType.getName());
    } else {
      boolean hasExtensions = hasExtensions(messageType);
      if (fields.size() == 1 && !hasExtensions) {
        writer.emitStatement("if (!(other instanceof %s)) return false", messageType.getName());
        writer.emitStatement("return Wire.equals(%1$s, ((%2$s) other).%1$s)",
            sanitize(fields.get(0).getName()), messageType.getName());
      } else {
        writer.emitStatement("if (!(other instanceof %s)) return false", messageType.getName());
        writer.emitStatement("%1$s o = (%1$s) other", messageType.getName());
        if (hasExtensions) {
          writer.emitStatement("if (!extensionMap.equals(o.extensionMap)) return false");
        }
        for (Field field : fields) {
          writer.emitStatement("if (!Wire.equals(%1$s, o.%1$s)) return false",
              sanitize(field.getName()));
        }
        writer.emitStatement("return true");
      }
    }
    writer.endMethod();
  }

  // Example:
  //
  // @Override
  // public int hashCode() {
  //   int hashCode = extensionMap.hashCode();
  //   hashCode = hashCode * 37 + (f != null ? f.hashCode() : 0);
  //   return hashCode;
  // }
  //
  private void emitMessageHashCode(MessageType messageType)
      throws IOException {
    writer.emitEmptyLine();
    writer.emitAnnotation(Override.class);
    writer.beginMethod("int", "hashCode", EnumSet.of(PUBLIC));

    if (!hasFields(messageType) && !hasExtensions(messageType)) {
      writer.emitStatement("return 0");
    } else if (!hasExtensions(messageType) && messageType.getFields().size() == 1) {
      Field field = messageType.getFields().get(0);
      String name = sanitize(field.getName());
      if ("bytes".equals(field.getType())) {
        writer.emitStatement("return Wire.hashCode(%1$s)", name);
      } else {
        writer.emitStatement("return %1$s != null ? %1$s.hashCode() : 0", name);
      }
    } else {
      if (hasExtensions(messageType)) {
        writer.emitStatement("int hashCode = extensionMap.hashCode()");
      } else {
        writer.emitStatement("int hashCode = 0");
      }
      for (Field field : messageType.getFields()) {
        String name = sanitize(field.getName());
        if ("bytes".equals(field.getType())) {
          writer.emitStatement("hashCode = hashCode * 37 + Wire.hashCode(%1$s)", name);
        } else {
          writer.emitStatement("hashCode = hashCode * 37 + (%1$s != null ? %1$s.hashCode() : 0)",
              name);
        }
      }
      writer.emitStatement("return hashCode");
    }
    writer.endMethod();
  }

  // Example:
  //
  // @Override
  // public String toString() {
  //   return String.format("SimpleMessage{" +
  //     "optional_int32=%s}",
  //     optional_int32);
  // }
  //
  private void emitMessageToString(MessageType messageType)
      throws IOException {
    writer.emitEmptyLine();
    writer.emitAnnotation(Override.class);
    writer.beginMethod("String", "toString", EnumSet.of(PUBLIC));

    if (messageType.getFields().isEmpty()) {
      writer.emitStatement(String.format("return \"%s{}\"", messageType.getName()));
    } else {
      StringBuilder format = new StringBuilder();
      StringBuilder args = new StringBuilder();
      String formatSep = "\"";
      String argsSep = "";
      for (Field field : messageType.getFields()) {
        String sanitized = sanitize(field.getName());
        format.append(String.format("%s%s=%%s", formatSep, sanitized));
        if ("bytes".equals(field.getType())) {
          args.append(String.format("%s%s", argsSep, "Wire.toString(" + sanitized + ")"));
        } else {
          args.append(String.format("%s%s", argsSep, sanitized));
        }
        formatSep = ",\" +\n\"";
        argsSep = ",\n";
      }
      if (hasExtensions(messageType)) {
        format.append(String.format("%s{extensionMap=%%s", formatSep));
        args.append(String.format("%sWire.toString(extensionMap)", argsSep));
      }

      writer.emitStatement("return String.format(\"%s{\" +\n%s}\",\n%s)", messageType.getName(),
          format.toString(), args.toString());
    }
    writer.endMethod();
  }

  private void emitBuilder(MessageType messageType)
      throws IOException {
    writer.emitEmptyLine();
    writer.beginType("Builder", "class", EnumSet.of(PUBLIC, STATIC, FINAL), null,
        (hasExtensions(messageType) ? "ExtendableMessage.ExtendableBuilder<" : "Message.Builder<")
            + messageType.getName()
            + ">");
    emitBuilderFields(messageType);
    emitBuilderConstructors(messageType);
    emitBuilderSetters(messageType);
    if (hasExtensions(messageType)) {
      emitBuilderGetExtension(messageType);
      emitBuilderSetExtension(messageType);
    }
    emitBuilderIsInitialized(messageType);
    emitBuilderBuild(messageType);
    writer.endType();
  }

  private void emitBuilderFields(MessageType messageType) throws IOException {
    List<Field> fields = messageType.getFields();
    boolean hasExtensions = hasExtensions(messageType);
    String name = messageType.getName();

    if (!fields.isEmpty() || hasExtensions) {
      writer.emitEmptyLine();
    }
    if (hasExtensions) {
      writer.emitField("Map<Extension<" + name + ", ?>, Object>",
          "extensionMap", EnumSet.of(PRIVATE, FINAL),
          "new TreeMap<Extension<" + name + ", ?>, Object>()");
      writer.emitEmptyLine();
    }
    for (Field field : fields) {
      String javaName = getJavaType(messageType, field);
      writer.emitField(javaName, sanitize(field.getName()), EnumSet.of(PUBLIC));
    }
  }

  private void emitBuilderConstructors(MessageType messageType) throws IOException {
    writer.emitEmptyLine();
    writer.beginMethod(null, "Builder", EnumSet.of(PUBLIC));
    writer.endMethod();

    writer.emitEmptyLine();
    writer.beginMethod(null, "Builder", EnumSet.of(PUBLIC), messageType.getName(), "message");
    List<Field> fields = messageType.getFields();
    if (!fields.isEmpty()) {
      writer.emitStatement("if (message == null) return");
    }
    for (Field field : fields) {
      if (isRepeated(field)) {
        writer.emitStatement("this.%1$s = Wire.copyOf(message.%1$s)", sanitize(field.getName()));
      } else {
        writer.emitStatement("this.%1$s = message.%1$s", sanitize(field.getName()));
      }
    }
    if (hasExtensions(messageType)) {
      writer.emitStatement("this.extensionMap.putAll(message.extensionMap)");
    }
    writer.endMethod();
  }

  private String getDefaultValue(MessageType messageType, Field field) {
    String initialValue = field.getDefault();
    // Qualify message and enum values
    boolean isRepeated = field.getLabel() == MessageType.Label.REPEATED;
    if (isRepeated) {
      return "Collections.emptyList()";
    }
    String javaName = javaName(messageType, field.getType());
    if (!isScalar(field)) {
      if (initialValue != null) {
        return javaName + "." + initialValue;
      } else {
        String fullyQualifiedName = fullyQualifiedName(messageType, field.getType());
        if (isEnum(fullyQualifiedName)) {
          return field.getType() + "." + enumDefaults.get(fullyQualifiedName);
        } else {
          return "Wire.getDefaultInstance(" + writer.compressType(javaName) + ".class)";
        }
      }
    }
    if ("Boolean".equals(javaName)) {
      return initialValue == null ? "false" : initialValue;
    } else if ("Double".equals(javaName)) {
      return initialValue == null ? "0D" : initialValue + "D";
    } else if ("Integer".equals(javaName)) {
      return initialValue == null ? "0" : initialValue;
    } else if ("Long".equals(javaName)) {
      // Add an 'L' to Long values
      return initialValue == null ? "0L" : initialValue + "L";
    } else  if ("Float".equals(javaName)) {
      // Add an 'F' to Float values
      return initialValue == null ? "0F" : initialValue + "F";
    } else if ("String".equals(javaName)) {
      // Quote String values
      return "\"" + (initialValue == null
          ? "" : initialValue.replaceAll("[\\\\]", "\\\\").replaceAll("[\"]", "\\\"")) + "\"";
    } else if ("byte[]".equals(javaName)) {
      return "new byte[0]";
    } else {
      return "null";
    }
  }

  private void emitBuilderSetters(MessageType messageType) throws IOException {
    for (Field field : messageType.getFields()) {
      String javaName = getJavaType(messageType, field);
      List<String> args = new ArrayList<String>();
      args.add(javaName);
      String sanitized = sanitize(field.getName());
      args.add(sanitized);

      writer.emitEmptyLine();
      writer.beginMethod("Builder", sanitized, EnumSet.of(PUBLIC), args, null);
      writer.emitStatement("this.%1$s = %1$s", sanitized);
      writer.emitStatement("return this");
      writer.endMethod();
    }
  }

  private String getJavaType(MessageType messageType, Field field) {
    String javaName = javaName(messageType, field.getType());
    if (isRepeated(field)) {
      javaName = "List<" + javaName + ">";
    }
    return javaName;
  }

  private void emitBuilderGetExtension(MessageType messageType)
      throws IOException {
    emitMessageGetExtension(messageType);
  }

  // Example:
  //
  // @Override
  // public <Type> Builder setExtension(Extension<ExternalMessage, Type> extension, Type value) {
  //   extensionMap.put(extension, value);
  //   return this;
  // }
  //
  private void emitBuilderSetExtension(MessageType messageType)
      throws IOException {
    writer.emitEmptyLine();
    writer.emitAnnotation(Override.class);
    writer.beginMethod("<Type> Builder", "setExtension", EnumSet.of(PUBLIC),
        "Extension<" + messageType.getName() + ", Type>", "extension", "Type", "value");
    writer.emitStatement("extensionMap.put(extension, value)");
    writer.emitStatement("return this");
    writer.endMethod();
  }

  private void emitBuilderIsInitialized(MessageType messageType)
      throws IOException {
    writer.emitEmptyLine();
    writer.emitAnnotation(Override.class);
    writer.beginMethod("boolean", "isInitialized", EnumSet.of(PUBLIC));
    for (Field field : messageType.getFields()) {
      if (isRequired(field)) {
        writer.emitStatement("if (%s == null) return false", field.getName());
      }
    }
    writer.emitStatement("return true");
    writer.endMethod();
  }

  private void emitBuilderBuild(MessageType messageType) throws IOException {
    writer.emitEmptyLine();
    writer.emitAnnotation(Override.class);
    writer.beginMethod(messageType.getName(), "build", EnumSet.of(PUBLIC));
    if (hasRequiredFields(messageType)) {
      writer.emitStatement("if (!isInitialized()) throw new UninitializedMessageException()");
    }
    writer.emitStatement("return new %s(this)", messageType.getName());
    writer.endMethod();
  }

  private void addTypes(List<Type> types, String javaPrefix) {
    for (Type type : types) {
      String name = type.getName();
      String fqName = type.getFullyQualifiedName();
      javaSymbolMap.put(fqName, javaPrefix + name);
      if (type instanceof EnumType) {
        enumTypes.add(fqName);
        enumDefaults.put(fqName, ((EnumType) type).getValues().get(0).getName());
      }
      addTypes(type.getNestedTypes(), javaPrefix + name + ".");
    }
  }

  private void loadSymbols(ProtoFile protoFile) throws IOException {
    // Load symbols from imports
    for (String dependency : protoFile.getDependencies()) {
      if (!loadedDependencies.contains(dependency)) {
        File dep = new File(repoPath + "/" + dependency);
        ProtoFile dependencyFile = ProtoSchemaParser.parse(dep);
        loadSymbols(dependencyFile);
        loadedDependencies.add(dependency);
      }
    }

    addTypes(protoFile.getTypes(), protoFile.getJavaPackage() + ".");
  }

  private boolean hasEnum(List<Type> types) {
    for (Type type : types) {
      if (type instanceof EnumType) {
        return true;
      }
      if (hasEnum(type.getNestedTypes())) {
        return true;
      }
    }
    return false;
  }

  private boolean hasExtensions(List<Type> types) {
    for (Type type : types) {
      if (type instanceof MessageType && hasExtensions(((MessageType) type))) {
        return true;
      }
      if (hasExtensions(type.getNestedTypes())) {
        return true;
      }
    }
    return false;
  }

  private boolean hasMessage(List<Type> types) {
    for (Type type : types) {
      if (type instanceof MessageType) {
        return true;
      }
      if (hasMessage(type.getNestedTypes())) {
        return true;
      }
    }
    return false;
  }

  private boolean hasRepeatedField(List<Type> types) {
    for (Type type : types) {
      if (type instanceof MessageType) {
        for (Field field : ((MessageType) type).getFields()) {
          if (isRepeated(field)) {
            return true;
          }
        }
      }
      if (hasRepeatedField(type.getNestedTypes())) {
        return true;
      }
    }
    return false;
  }

  private boolean hasFields(Type type) {
    return type instanceof MessageType && !((MessageType) type).getFields().isEmpty();
  }

  private boolean hasRequiredFields(Type type) {
    if (type instanceof MessageType) {
      for (MessageType.Field field : ((MessageType) type).getFields()) {
        if (field.getLabel() == MessageType.Label.REQUIRED) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasRequiredFields(List<Type> types) {
    for (Type type : types) {
      if (hasRequiredFields(type)) {
        return true;
      }
    }
    return false;
  }

  private boolean hasDocumentation(String documentation) {
    return documentation != null && !documentation.isEmpty();
  }

  private String protoFieldType(String type) {
    String protoFieldType = PROTO_FIELD_TYPES.get(type);
    return protoFieldType != null ? protoFieldType : type + ".class";
  }

  private boolean isScalar(Field field) {
    return PROTO_FIELD_TYPES.containsKey(field.getType());
  }

  private boolean isEnum(String fieldType) {
    return enumTypes.contains(fieldType);
  }

  private boolean isOptional(Field field) {
    return field.getLabel() == MessageType.Label.OPTIONAL;
  }

  private boolean isRepeated(Field field) {
    return field.getLabel() == MessageType.Label.REPEATED;
  }

  private boolean isPacked(Field field, boolean isEnum) {
    return "true".equals(field.getExtensions().get("packed"))
        && (PACKABLE_TYPES.contains(field.getType()) || isEnum);
  }

  private boolean isRequired(Field field) {
    return field.getLabel() == MessageType.Label.REQUIRED;
  }

  private String fullyQualifiedName(MessageType messageType, String type) {
    if (type.contains(".")) {
      return type;
    } else {
      String prefix = messageType.getFullyQualifiedName();
      while (prefix.contains(".")) {
        String fqname = prefix + "." + type;
        if (javaSymbolMap.containsKey(fqname)) {
          return fqname;
        }
        prefix = prefix.substring(0, prefix.lastIndexOf('.'));
      }
    }
    throw new RuntimeException("Unknown type " + type + " in message " + messageType.getName());
  }

  private String shortenJavaName(String fullyQualifiedName) {
    String javaPackage = protoFile.getJavaPackage() + ".";
    if (fullyQualifiedName.startsWith(javaPackage)) {
      fullyQualifiedName = fullyQualifiedName.substring(javaPackage.length());
    }
    if (fullyQualifiedName.startsWith(typeBeingGenerated)) {
      fullyQualifiedName = fullyQualifiedName.substring(typeBeingGenerated.length());
    }
    return fullyQualifiedName;
  }

  private String javaName(MessageType messageType, String type) {
    String scalarType = JAVA_TYPES.get(type);
    if (scalarType != null) {
      return scalarType;
    }

    // Assume names containing a '.' are already fully-qualified
    if (type.contains(".")) {
      return javaSymbolMap.get(type);
    } else {
      String prefix = messageType != null ? messageType.getFullyQualifiedName() : "";
      while (prefix.contains(".")) {
        String fqname = prefix + "." + type;
        String javaName = javaSymbolMap.get(fqname);
        if (javaName != null) {
          return shortenJavaName(javaName);
        }
        prefix = prefix.substring(0, prefix.lastIndexOf('.'));
      }
    }
    return null;
  }

  private String fqJavaName(MessageType messageType, String type) {
    String scalarType = JAVA_TYPES.get(type);
    if (scalarType != null) {
      return null;
    }

    // Assume names containing a '.' are already fully-qualified
    if (type.contains(".")) {
      return javaSymbolMap.get(type);
    } else {
      String prefix = messageType != null ? messageType.getFullyQualifiedName() : "";
      while (prefix.contains(".")) {
        String fqname = prefix + "." + type;
        String javaName = javaSymbolMap.get(fqname);
        if (javaName != null) {
          return javaName;
        }
        prefix = prefix.substring(0, prefix.lastIndexOf('.'));
      }
    }
    return null;
  }
}
