/*
 * Copyright 2013 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire.compiler;

import com.squareup.javawriter.JavaWriter;
import com.squareup.protoparser.EnumType;
import com.squareup.protoparser.ExtendDeclaration;
import com.squareup.protoparser.MessageType;
import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.ProtoSchemaParser;
import com.squareup.protoparser.Type;
import com.squareup.wire.Base64;
import com.squareup.wire.ProtoEnum;
import com.squareup.wire.ProtoField;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import javax.lang.model.element.Modifier;

import static com.squareup.protoparser.MessageType.Field;
import static com.squareup.wire.Message.Datatype;
import static com.squareup.wire.Message.Label;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

/** Compiler for Wire protocol buffers. */
public class WireCompiler {

  private static final Charset UTF_8 = Charset.forName("UTF8");
  private static final Charset ISO_8859_1 = Charset.forName("ISO_8859_1");
  private static final Map<String, String> JAVA_TYPES = new LinkedHashMap<String, String>();
  private static final Set<String> JAVA_KEYWORDS = new HashSet<String>(
      Arrays.asList("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
          "class", "const", "continue", "default", "do", "double", "else", "enum", "extends",
          "final", "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
          "int", "interface", "long", "native", "new", "package", "private", "protected", "public",
          "return", "short", "static", "strictfp", "super", "switch", "synchronized", "this",
          "throw", "throws", "transient", "try", "void", "volatile", "while"));
  private static final String PROTO_PATH_FLAG = "--proto_path=";
  private static final String JAVA_OUT_FLAG = "--java_out=";
  private static final String FILES_FLAG = "--files=";
  private static final String URL_CHARS = "[-!#$%&'()*+,./0-9:;=?@A-Z\\[\\]_a-z~]";
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
  private static final String CODE_GENERATED_BY_WIRE_PROTOBUF_COMPILER_DO_NOT_EDIT =
      "Code generated by Wire protobuf compiler, do not edit.";
  private final String repoPath;
  private final ProtoFile protoFile;
  private final Set<String> loadedDependencies = new HashSet<String>();
  private final Map<String, String> javaSymbolMap = new LinkedHashMap<String, String>();
  private final Set<String> enumTypes = new HashSet<String>();
  private final Map<String, String> enumDefaults = new LinkedHashMap<String, String>();
  private String protoFileName;
  private String typeBeingGenerated = "";
  private JavaWriter writer;

  public WireCompiler(String protoPath, String sourceFilename) throws IOException {
    this.repoPath = protoPath;
    String filename = protoPath + "/" + sourceFilename;
    System.out.println("Reading proto source file " + filename);
    this.protoFile = ProtoSchemaParser.parse(new File(protoPath + "/" + sourceFilename));
  }

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
    List<String> sourceFileNames = new ArrayList<String>();

    int index = 0;
    while (index < args.length) {
      if (args[index].startsWith(PROTO_PATH_FLAG)) {
        protoPath = args[index].substring(PROTO_PATH_FLAG.length());
      } else if (args[index].startsWith(JAVA_OUT_FLAG)) {
        javaOut = args[index].substring(JAVA_OUT_FLAG.length());
      } else if (args[index].startsWith(FILES_FLAG)) {
        File files = new File(args[index].substring(FILES_FLAG.length()));
        String[] fileNames = new Scanner(files, "UTF-8").useDelimiter("\\A").next().split("\n");
        sourceFileNames.addAll(Arrays.asList(fileNames));
      } else {
        sourceFileNames.add(args[index]);
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
    for (String sourceFilename : sourceFileNames) {
      WireCompiler wireCompiler = new WireCompiler(protoPath, sourceFilename);
      wireCompiler.compile(javaOut);
    }
  }

  public JavaWriter getJavaWriter(String javaOut, String className) throws IOException {
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

  private String protoFileName(String path) {
    int slashIndex = path.lastIndexOf('/');
    if (slashIndex != -1) {
      path = path.substring(slashIndex + 1);
    }
    if (path.endsWith(".proto")) {
      path = path.substring(0, path.length() - ".proto".length());
    }
    return path;
  }

  private void emitMessageClass(String javaOut, Type type) throws IOException {
    try {
      writer = getJavaWriter(javaOut, type.getName());
      writer.emitJavadoc(CODE_GENERATED_BY_WIRE_PROTOBUF_COMPILER_DO_NOT_EDIT + "\nSource file: %s",
          protoFile.getFileName());
      writer.emitPackage(protoFile.getJavaPackage());

      List<Type> types = new ArrayList<Type>();
      getTypes(type, types);
      boolean hasMessage = hasMessage(types);
      boolean hasExtensions = hasExtensions(Arrays.asList(type));

      Set<String> imports = new HashSet<String>();
      if (hasMessage) {
        imports.add("com.squareup.wire.Message");
      }
      if (hasMessage || hasExtensions) {
        if (hasFields(type)) {
          imports.add("com.squareup.wire.ProtoField");
        }
      }
      if (hasBytesField(types)) {
        imports.add("com.squareup.wire.ByteString");
      }
      if (hasEnum(types)) {
        imports.add("com.squareup.wire.ProtoEnum");
      }
      if (hasRepeatedField(types)) {
        imports.add("java.util.Collections");
        imports.add("java.util.List");
      }
      if (hasExtensions) {
        imports.add("com.squareup.wire.ExtendableMessage");
        imports.add("com.squareup.wire.Extension");
      }
      List<String> externalTypes = new ArrayList<String>();
      getExternalTypes(type, externalTypes);
      imports.addAll(externalTypes);
      writer.emitImports(imports);

      // Emit static imports for Datatype. and Label. enums
      Collection<Datatype> datatypes = new TreeSet<Datatype>(Datatype.ORDER_BY_NAME);
      Collection<Label> labels = new TreeSet<Label>(Label.ORDER_BY_NAME);
      getDatatypesAndLabels(type, datatypes, labels);
      // No need to emit 'label = OPTIONAL' since it is the default
      labels.remove(Label.OPTIONAL);

      if (!datatypes.isEmpty() || !labels.isEmpty()) {
        writer.emitEmptyLine();
      }
      for (Datatype datatype : datatypes) {
        writer.emitStaticImports("com.squareup.wire.Message.Datatype." + datatype.toString());
      }
      for (Label label : labels) {
        writer.emitStaticImports("com.squareup.wire.Message.Label." + label.toString());
      }

      emitType(type, protoFile.getPackageName() + ".", true);
    } finally {
      writer.close();
    }
  }

  private void getTypes(Type root, List<Type> types) {
    types.add(root);
    for (Type nestedType : root.getNestedTypes()) {
      getTypes(nestedType, types);
    }
  }

  private void getExternalTypes(Type root, List<String> types) {
    if (root instanceof MessageType) {
      MessageType messageType = (MessageType) root;
      for (Field field : messageType.getFields()) {
        String fqName = fullyQualifiedJavaName(messageType, field.getType());
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
    writer.emitJavadoc(CODE_GENERATED_BY_WIRE_PROTOBUF_COMPILER_DO_NOT_EDIT + "\nSource file: %s",
        protoFile.getFileName());
    writer.emitPackage(protoFile.getJavaPackage());

    Set<String> imports = new HashSet<String>();
    if (hasByteStringExtension()) {
      imports.add("com.squareup.wire.ByteString");
    }
    imports.add("com.squareup.wire.Extension");
    if (hasRepeatedExtension()) {
      imports.add("java.util.List");
    }
    imports.addAll(getExtensionTypes());
    writer.emitImports(imports);
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
        if (type == null) {
          type = javaName(null, protoFile.getPackageName() + "." + fieldType);
        }
        type = shortenJavaName(type);
        String initialValue;
        String className = writer.compressType(name);
        String extensionName = field.getName();
        String fqName = removeTrailingSegment(extend.getFullyQualifiedName()) + "."
            + field.getName();
        int tag = field.getTag();

        boolean isScalar = isScalar(fieldType);
        boolean isEnum = !isScalar && isEnum(fullyQualifiedName(null, fieldType));
        String labelString = getLabelString(field, isEnum);
        if (isScalar) {
          initialValue = String.format("Extension\n"
              + "      .%sExtending(%s.class)\n"
              + "      .setName(\"%s\")\n"
              + "      .setTag(%d)\n"
              + "      .build%s()",
              field.getType(), className, fqName, tag, labelString);
        } else if (isEnum) {
          initialValue = String.format("Extension\n"
              + "      .enumExtending(%s.class, %s.class)\n"
              + "      .setName(\"%s\")\n"
              + "      .setTag(%d)\n"
              + "      .build%s()",
              type, className, fqName, tag, labelString);
        } else {
          initialValue = String.format("Extension\n"
              + "      .messageExtending(%s.class, %s.class)\n"
              + "      .setName(\"%s\")\n"
              + "      .setTag(%d)\n"
              + "      .build%s()",
              type, className, fqName, tag, labelString);
        }

        if (isRepeated(field)) {
          type = "List<" + type + ">";
        }
        writer.emitField("Extension<" + name + ", " + type + ">", extensionName,
            EnumSet.of(PUBLIC, STATIC, FINAL), initialValue);
      }
    }
  }

  private String getLabelString(Field field, boolean isEnum) {
    switch (field.getLabel()) {
      case OPTIONAL: return "Optional";
      case REQUIRED: return "Required";
      case REPEATED:
        return isPacked(field, isEnum) ? "Packed" : "Repeated";
      default:
        throw new RuntimeException("Unknown extension label \"" + field.getLabel() + "\"");
    }
  }

  private boolean hasByteStringExtension() {
    for (ExtendDeclaration extend : protoFile.getExtendDeclarations()) {
      for (MessageType.Field field : extend.getFields()) {
        String fieldType = field.getType();
        if ("bytes".equals(fieldType)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasRepeatedExtension() {
    for (ExtendDeclaration extend : protoFile.getExtendDeclarations()) {
      for (MessageType.Field field : extend.getFields()) {
        if (field.getLabel() == MessageType.Label.REPEATED) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasScalarExtension() {
    for (ExtendDeclaration extend : protoFile.getExtendDeclarations()) {
      for (MessageType.Field field : extend.getFields()) {
        if (isScalar(field.getType())) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean hasExtensions(MessageType messageType) {
    return !messageType.getExtensions().isEmpty();
  }

  private void emitType(Type type, String currentType, boolean topLevel) throws IOException {
    writer.emitEmptyLine();
    if (type instanceof MessageType) {
      MessageType messageType = (MessageType) type;
      Set<Modifier> modifiers = EnumSet.of(PUBLIC, FINAL);
      if (!topLevel) {
        modifiers.add(STATIC);
      }
      String name = messageType.getName();
      writer.beginType(name, "class", modifiers,
          hasExtensions(messageType) ? "ExtendableMessage<" + name + ">" : "Message");
      emitMessageDefaults(messageType);
      emitMessageFields(messageType);
      emitMessageConstructor(messageType);
      emitMessageEquals(messageType);
      emitMessageHashCode(messageType);
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
  private void emitMessageDefaults(MessageType messageType) throws IOException {
    List<Field> fields = messageType.getFields();
    if (!fields.isEmpty()) {
      writer.emitEmptyLine();
    }
    for (Field field : fields) {
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

  private String getJavaFieldType(MessageType messageType, Field field) {
    String javaName = javaName(messageType, field.getType());
    if (isRepeated(field)) javaName = "List<" + javaName + ">";
    return javaName;
  }

  private String sanitize(String name) {
    return JAVA_KEYWORDS.contains(name) ? "_" + name : name;
  }

  private String getDefaultValue(MessageType messageType, Field field) {
    String initialValue = field.getDefault();
    // Qualify message and enum values
    if (isRepeated(field)) return "Collections.emptyList()";
    String javaName = javaName(messageType, field.getType());
    if (isScalar(field.getType())) {
      return getInitializerForType(initialValue, javaName);
    } else {
      if (initialValue != null) {
        return javaName + "." + initialValue;
      } else {
        String fullyQualifiedName = fullyQualifiedName(messageType, field.getType());
        if (isEnum(fullyQualifiedName)) {
          return javaName + "." + enumDefaults.get(fullyQualifiedName);
        } else {
          return "getDefaultInstance(" + writer.compressType(javaName) + ".class)";
        }
      }
    }
  }

  private String getInitializerForType(String initialValue, String javaTypeName) {
    if ("Boolean".equals(javaTypeName)) {
      return initialValue == null ? "false" : initialValue;
    } else if ("Integer".equals(javaTypeName)) {
      // Wrap unsigned values
      return initialValue == null ? "0" : toInt(initialValue);
    } else if ("Long".equals(javaTypeName)) {
      // Wrap unsigned values and add an 'L'
      return initialValue == null ? "0L" : toLong(initialValue) + "L";
    } else if ("Float".equals(javaTypeName)) {
      return initialValue == null ? "0F" : initialValue + "F";
    } else if ("Double".equals(javaTypeName)) {
      return initialValue == null ? "0D" : initialValue + "D";
    } else if ("String".equals(javaTypeName)) {
      return quoteString(initialValue);
    } else if ("ByteString".equals(javaTypeName)) {
      if (initialValue == null) {
        return "ByteString.EMPTY";
      } else {
        return "ByteString.of(\"" + Base64.encode(initialValue.getBytes(ISO_8859_1)) + "\")";
      }
    } else {
      throw new IllegalArgumentException(javaTypeName + " is not an allowed scalar type");
    }
  }

  private String toInt(String value) {
    return Integer.toString(new BigDecimal(value).intValue());
  }

  private String toLong(String value) {
    return Long.toString(new BigDecimal(value).longValue());
  }

  private String quoteString(String initialValue) {
    return initialValue == null ? "\"\"" : JavaWriter.stringLiteral(initialValue);
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
    for (Field field : messageType.getFields()) {
      String javaName = javaName(messageType, field.getType());
      Map<String, String> map = new LinkedHashMap<String, String>();
      map.put("tag", String.valueOf(field.getTag()));

      String type = field.getType();
      boolean isEnum = false;
      if (isScalar(field.getType())) {
        map.put("type", scalarTypeConstant(type));
      } else {
        String fullyQualifiedName = fullyQualifiedName(messageType, type);
        isEnum = isEnum(fullyQualifiedName);
        if (isEnum) map.put("type", "ENUM");
      }

      if (!isOptional(field)) {
        if (isPacked(field, isEnum)) {
          map.put("label", "PACKED");
        } else {
          map.put("label", field.getLabel().toString());
        }
      }

      writer.emitEmptyLine();
      String documentation = field.getDocumentation();
      if (hasDocumentation(documentation)) {
        writer.emitJavadoc(sanitizeJavadoc(documentation));
      }
      writer.emitAnnotation(ProtoField.class, map);

      if (isRepeated(field)) javaName = "List<" + javaName + ">";
      writer.emitField(javaName, sanitize(field.getName()), EnumSet.of(PUBLIC, FINAL));
    }
  }

  /**
   * A grab-bag of fixes for things that can go wrong when converting to javadoc.
   */
  private String sanitizeJavadoc(String documentation) {
    // JavaWriter will pass the doc through String.format, so escape all '%' chars
    documentation = documentation.replace("%", "%%");
    // Rewrite '@see <url>' to use an html anchor tag
    documentation =
        documentation.replaceAll("@see (http:" + URL_CHARS + "+)", "@see <a href=\"$1\">$1</a>");
    return documentation;
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
      if (isRepeated(field)) {
        writer.emitStatement("this.%1$s = unmodifiableCopyOf(builder.%1$s)",
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
      writer.emitStatement("if (!(other instanceof %s)) return false", messageType.getName());
      if (hasOnlyOneField(messageType)) {
        writer.emitStatement("return equals(%1$s, ((%2$s) other).%1$s)",
            sanitize(fields.get(0).getName()), messageType.getName());
      } else {
        writer.emitStatement("%1$s o = (%1$s) other", messageType.getName());
        if (hasExtensions(messageType)) {
          writer.emitStatement("if (!extensionsEqual(o)) return false");
        }
        StringBuilder sb = new StringBuilder();
        String prefix = "return ";
        for (Field field : fields) {
          sb.append(prefix);
          prefix = "\n&& ";
          sb.append(String.format("equals(%1$s, o.%1$s)", sanitize(field.getName())));
        }
        writer.emitStatement(sb.toString());
      }
    }
    writer.endMethod();
  }

  private boolean hasOnlyOneField(MessageType messageType) {
    return messageType.getFields().size() == 1 && !hasExtensions(messageType);
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

    if (!hasFields(messageType) && !hasExtensions(messageType)) {
      writer.emitStatement("return 0");
    } else if (hasOnlyOneField(messageType)) {
      Field field = messageType.getFields().get(0);
      String name = sanitize(field.getName());
      writer.emitStatement("int result = hashCode");
      writer.emitStatement(
          "return result != 0 ? result : (hashCode = %1$s != null ? %1$s.hashCode() : 0)", name);
    } else {
      writer.emitStatement("int result = hashCode");
      writer.beginControlFlow("if (result == 0)");
      boolean afterFirstAssignment = false;
      if (hasExtensions(messageType)) {
        writer.emitStatement("result = extensionsHashCode()");
        afterFirstAssignment = true;
      }
      for (Field field : messageType.getFields()) {
        String name = sanitize(field.getName());
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
        (hasExtensions(messageType) ? "ExtendableBuilder<" : "Message.Builder<")
            + messageType.getName()
            + ">");
    emitBuilderFields(messageType);
    emitBuilderConstructors(messageType);
    emitBuilderSetters(messageType);
    if (hasExtensions(messageType)) emitBuilderSetExtension(messageType);
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
      if (isRepeated(field)) {
        writer.emitStatement("this.%1$s = copyOf(message.%1$s)", sanitize(field.getName()));
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

  private boolean hasEnum(List<Type> types) {
    for (Type type : types) {
      if (type instanceof EnumType || hasEnum(type.getNestedTypes())) return true;
    }
    return false;
  }

  private boolean hasExtensions(List<Type> types) {
    for (Type type : types) {
      if (type instanceof MessageType && hasExtensions(((MessageType) type))) return true;
      if (hasExtensions(type.getNestedTypes())) return true;
    }
    return false;
  }

  private boolean hasMessage(List<Type> types) {
    for (Type type : types) {
      if (type instanceof MessageType && !hasExtensions(((MessageType) type))) return true;
      if (hasMessage(type.getNestedTypes())) return true;
    }
    return false;
  }

  private boolean hasRepeatedField(List<Type> types) {
    for (Type type : types) {
      if (type instanceof MessageType) {
        for (Field field : ((MessageType) type).getFields()) {
          if (isRepeated(field)) return true;
        }
      }
      if (hasRepeatedField(type.getNestedTypes())) return true;
    }
    return false;
  }

  private boolean hasBytesField(List<Type> types) {
    for (Type type : types) {
      if (type instanceof MessageType) {
        for (Field field : ((MessageType) type).getFields()) {
          if ("bytes".equals(field.getType())) return true;
        }
      }
      if (hasBytesField(type.getNestedTypes())) return true;
    }
    return false;
  }

  private boolean hasFields(Type type) {
    return type instanceof MessageType && !((MessageType) type).getFields().isEmpty();
  }

  private boolean hasRequiredFields(Type type) {
    if (type instanceof MessageType) {
      for (MessageType.Field field : ((MessageType) type).getFields()) {
        if (isRequired(field)) return true;
      }
    }
    return false;
  }

  private void getDatatypesAndLabels(Type type, Collection<Datatype> types,
      Collection<Label> labels) {
    if (type instanceof MessageType) {
      for (MessageType.Field field : ((MessageType) type).getFields()) {
        String fieldType = field.getType();
        Datatype datatype = Datatype.of(fieldType);
        // If not scalar, determine whether it is an enum
        if (datatype == null && isEnum(fullyQualifiedName((MessageType) type, field.getType()))) {
          datatype = Datatype.ENUM;
        }
        if (datatype != null) types.add(datatype);

        // Convert Protoparser label to Wire label
        MessageType.Label label = field.getLabel();
        switch (label) {
          case OPTIONAL:
            labels.add(Label.OPTIONAL);
            break;
          case REQUIRED:
            labels.add(Label.REQUIRED);
            break;
          case REPEATED:
            if (isPacked(field, false)) {
              labels.add(Label.PACKED);
            } else {
              labels.add(Label.REPEATED);
            }
            break;
          default:
            throw new AssertionError("Unknown label " + label);
        }
      }

      for (Type nestedType : type.getNestedTypes()) {
        getDatatypesAndLabels(nestedType, types, labels);
      }
    }
  }

  private boolean hasDocumentation(String documentation) {
    return documentation != null && !documentation.isEmpty();
  }

  /**
   * Returns the name of the {@code Message} type constant (e.g.,
   * {@code INT32} or {@code STRING}) associated
   * with the given scalar type (e.g., {@code int32} or {@code string}).
   */
  private String scalarTypeConstant(String type) {
    return type.toUpperCase(Locale.US);
  }

  /**
   * Returns true if the given type name is one of the standard .proto
   * scalar types, e.g., {@code int32}, {@code string}, etc.
   */
  private boolean isScalar(String type) {
    return JAVA_TYPES.containsKey(type);
  }

  /**
   * Returns the Java type associated with a standard .proto
   * scalar type, e.g., {@code int32}, {@code string}, etc.,
   * or null if the name is not that of a scalar type.
   */
  private String scalarType(String type) {
    return JAVA_TYPES.get(type);
  }

  /**
   * Returns true if the given fully-qualified name (with a .proto package name)
   * refers to an .proto enumerated type.
   */
  private boolean isEnum(String type) {
    return enumTypes.contains(type);
  }

  private boolean isOptional(Field field) {
    return field.getLabel() == MessageType.Label.OPTIONAL;
  }

  private boolean isRepeated(Field field) {
    return field.getLabel() == MessageType.Label.REPEATED;
  }

  private boolean isRequired(Field field) {
    return field.getLabel() == MessageType.Label.REQUIRED;
  }

  private boolean isPacked(Field field, boolean isEnum) {
    return "true".equals(field.getExtensions().get("packed"))
        && (isEnum || isPackableScalar(field));
  }

  private boolean isPackableScalar(Field field) {
    String type = field.getType();
    return isScalar(type) && !"string".equals(type) && !"bytes".equals(type);
  }

  private String javaName(MessageType messageType, String type) {
    String scalarType = scalarType(type);
    return scalarType != null
        ? scalarType : shortenJavaName(javaName(fullyQualifiedName(messageType, type)));
  }

  private String javaName(String fqName) {
    return javaSymbolMap.get(fqName);
  }

  private String fullyQualifiedName(MessageType messageType, String type) {
    if (typeIsComplete(type)) {
      return type;
    } else {
      String prefix = messageType == null
          ? protoFile.getPackageName() : messageType.getFullyQualifiedName();
      while (prefix.contains(".")) {
        String fqname = prefix + "." + type;
        if (javaSymbolMap.containsKey(fqname)) return fqname;
        prefix = removeTrailingSegment(prefix);
      }
    }
    throw new RuntimeException("Unknown type " + type + " in message "
        + (messageType == null ? "<unknown>" : messageType.getName()));
  }

  private boolean typeIsComplete(String type) {
    return javaSymbolMap.containsKey(type);
  }

  private String fullyQualifiedJavaName(MessageType messageType, String type) {
    return isScalar(type) ? null : javaName(fullyQualifiedName(messageType, type));
  }

  private String removeTrailingSegment(String name) {
    return name.substring(0, name.lastIndexOf('.'));
  }

  private String shortenJavaName(String fullyQualifiedName) {
    if (fullyQualifiedName == null) return null;
    String javaPackage = protoFile.getJavaPackage() + ".";
    fullyQualifiedName = removePrefixIfPresent(fullyQualifiedName, javaPackage);
    fullyQualifiedName = removePrefixIfPresent(fullyQualifiedName, typeBeingGenerated);
    return fullyQualifiedName;
  }

  private String removePrefixIfPresent(String s, String prefix) {
    return s.startsWith(prefix) ? s.substring(prefix.length()) : s;
  }
}
