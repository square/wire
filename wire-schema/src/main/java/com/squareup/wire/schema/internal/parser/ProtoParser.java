/*
 * Copyright (C) 2013 Square, Inc.
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
package com.squareup.wire.schema.internal.parser;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Range;
import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.ProtoFile;
import com.squareup.wire.schema.internal.Util;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Basic parser for {@code .proto} schema declarations. */
public final class ProtoParser {
  private final SyntaxReader reader;

  /** Parse a named {@code .proto} schema. */
  public static ProtoFileElement parse(Location location, String data) {
    return new ProtoParser(location, data.toCharArray()).readProtoFile();
  }

  private final ProtoFileElement.Builder fileBuilder;
  private final ImmutableList.Builder<String> publicImports = ImmutableList.builder();
  private final ImmutableList.Builder<String> imports = ImmutableList.builder();
  private final ImmutableList.Builder<TypeElement> nestedTypes = ImmutableList.builder();
  private final ImmutableList.Builder<ServiceElement> services = ImmutableList.builder();
  private final ImmutableList.Builder<ExtendElement> extendsList = ImmutableList.builder();
  private final ImmutableList.Builder<OptionElement> options = ImmutableList.builder();

  /** The number of declarations defined in the current file. */
  private int declarationCount = 0;

  /** The syntax of the file, or null if none is defined. */
  private ProtoFile.Syntax syntax;

  /** Output package name, or null if none yet encountered. */
  private String packageName;

  /** The current package name + nested type names, separated by dots. */
  private String prefix = "";

  ProtoParser(Location location, char[] data) {
    this.reader = new SyntaxReader(data, location);
    this.fileBuilder = ProtoFileElement.builder(location);
  }

  ProtoFileElement readProtoFile() {
    while (true) {
      String documentation = reader.readDocumentation();
      if (reader.exhausted()) {
        return fileBuilder.syntax(syntax)
            .publicImports(publicImports.build())
            .imports(imports.build())
            .types(nestedTypes.build())
            .services(services.build())
            .extendDeclarations(extendsList.build())
            .options(options.build())
            .build();
      }
      Object declaration = readDeclaration(documentation, Context.FILE);
      if (declaration instanceof TypeElement) {
        nestedTypes.add((TypeElement) declaration);
      } else if (declaration instanceof ServiceElement) {
        services.add((ServiceElement) declaration);
      } else if (declaration instanceof OptionElement) {
        options.add((OptionElement) declaration);
      } else if (declaration instanceof ExtendElement) {
        extendsList.add((ExtendElement) declaration);
      }
    }
  }

  private Object readDeclaration(String documentation, Context context) {
    int index = declarationCount++;

    // Skip unnecessary semicolons, occasionally used after a nested message declaration.
    if (reader.peekChar(';')) return null;

    Location location = reader.location();
    String label = reader.readWord();

    if (label.equals("package")) {
      if (!context.permitsPackage()) throw reader.unexpected(location, "'package' in " + context);
      if (packageName != null) throw reader.unexpected(location, "too many package names");
      packageName = reader.readName();
      fileBuilder.packageName(packageName);
      prefix = packageName + ".";
      reader.require(';');
      return null;
    } else if (label.equals("import")) {
      if (!context.permitsImport()) throw reader.unexpected(location, "'import' in " + context);
      String importString = reader.readString();
      if ("public".equals(importString)) {
        publicImports.add(reader.readString());
      } else {
        imports.add(importString);
      }
      reader.require(';');
      return null;
    } else if (label.equals("syntax")) {
      if (!context.permitsSyntax()) throw reader.unexpected(location, "'syntax' in " + context);
      reader.require('=');
      if (index != 0) {
        throw reader.unexpected(
            location, "'syntax' element must be the first declaration in a file");
      }
      String syntaxString = reader.readQuotedString();
      try {
        syntax = ProtoFile.Syntax.get(syntaxString);
      } catch (IllegalArgumentException e) {
        throw reader.unexpected(location, e.getMessage());
      }
      reader.require(';');
      return null;
    } else if (label.equals("option")) {
      OptionElement result = readOption('=');
      reader.require(';');
      return result;
    } else if (label.equals("reserved")) {
      return readReserved(location, documentation);
    } else if (label.equals("message")) {
      return readMessage(location, documentation);
    } else if (label.equals("enum")) {
      return readEnumElement(location, documentation);
    } else if (label.equals("service")) {
      return readService(location, documentation);
    } else if (label.equals("extend")) {
      return readExtend(location, documentation);
    } else if (label.equals("rpc")) {
      if (!context.permitsRpc()) throw reader.unexpected(location, "'rpc' in " + context);
      return readRpc(location, documentation);
    } else if (label.equals("oneof")) {
      if (!context.permitsOneOf()) {
        throw reader.unexpected(location, "'oneof' must be nested in message");
      }
      return readOneOf(documentation);
    } else if (label.equals("extensions")) {
      if (!context.permitsExtensions()) {
        throw reader.unexpected(location, "'extensions' must be nested");
      }
      return readExtensions(location, documentation);
    } else if (context == Context.MESSAGE || context == Context.EXTEND) {
      return readField(documentation, location, label);
    } else if (context == Context.ENUM) {
      return readEnumConstant(documentation, location, label);
    } else {
      throw reader.unexpected(location, "unexpected label: " + label);
    }
  }

  /** Reads a message declaration. */
  private MessageElement readMessage(Location location, String documentation) {
    String name = reader.readName();
    MessageElement.Builder builder = MessageElement.builder(location)
        .name(name)
        .documentation(documentation);

    String previousPrefix = prefix;
    prefix = prefix + name + ".";

    ImmutableList.Builder<FieldElement> fields = ImmutableList.builder();
    ImmutableList.Builder<OneOfElement> oneOfs = ImmutableList.builder();
    ImmutableList.Builder<TypeElement> nestedTypes = ImmutableList.builder();
    ImmutableList.Builder<ExtensionsElement> extensions = ImmutableList.builder();
    ImmutableList.Builder<OptionElement> options = ImmutableList.builder();
    ImmutableList.Builder<ReservedElement> reserveds = ImmutableList.builder();
    ImmutableList.Builder<GroupElement> groups = ImmutableList.builder();

    reader.require('{');
    while (true) {
      String nestedDocumentation = reader.readDocumentation();
      if (reader.peekChar('}')) break;

      Object declared = readDeclaration(nestedDocumentation, Context.MESSAGE);
      if (declared instanceof FieldElement) {
        fields.add((FieldElement) declared);
      } else if (declared instanceof OneOfElement) {
        oneOfs.add((OneOfElement) declared);
      } else if (declared instanceof GroupElement) {
        groups.add((GroupElement) declared);
      } else if (declared instanceof TypeElement) {
        nestedTypes.add((TypeElement) declared);
      } else if (declared instanceof ExtensionsElement) {
        extensions.add((ExtensionsElement) declared);
      } else if (declared instanceof OptionElement) {
        options.add((OptionElement) declared);
      } else if (declared instanceof ExtendElement) {
        // Extend declarations always add in a global scope regardless of nesting.
        extendsList.add((ExtendElement) declared);
      } else if (declared instanceof ReservedElement) {
        reserveds.add((ReservedElement) declared);
      }
    }
    prefix = previousPrefix;

    return builder.fields(fields.build())
        .oneOfs(oneOfs.build())
        .nestedTypes(nestedTypes.build())
        .extensions(extensions.build())
        .options(options.build())
        .reserveds(reserveds.build())
        .groups(groups.build())
        .build();
  }

  /** Reads an extend declaration. */
  private ExtendElement readExtend(Location location, String documentation) {
    String name = reader.readName();
    ExtendElement.Builder builder = ExtendElement.builder(location)
        .name(name)
        .documentation(documentation);

    reader.require('{');
    ImmutableList.Builder<FieldElement> fields = ImmutableList.builder();
    while (true) {
      String nestedDocumentation = reader.readDocumentation();
      if (reader.peekChar('}')) break;

      Object declared = readDeclaration(nestedDocumentation, Context.EXTEND);
      if (declared instanceof FieldElement) {
        fields.add((FieldElement) declared);
      }
    }
    return builder.fields(fields.build())
        .build();
  }

  /** Reads a service declaration and returns it. */
  private ServiceElement readService(Location location, String documentation) {
    String name = reader.readName();
    ServiceElement.Builder builder = ServiceElement.builder(location)
        .name(name)
        .documentation(documentation);

    reader.require('{');
    ImmutableList.Builder<RpcElement> rpcs = ImmutableList.builder();
    ImmutableList.Builder<OptionElement> options = ImmutableList.builder();
    while (true) {
      String rpcDocumentation = reader.readDocumentation();
      if (reader.peekChar('}')) break;

      Object declared = readDeclaration(rpcDocumentation, Context.SERVICE);
      if (declared instanceof RpcElement) {
        rpcs.add((RpcElement) declared);
      } else if (declared instanceof OptionElement) {
        options.add((OptionElement) declared);
      }
    }
    return builder.options(options.build())
        .rpcs(rpcs.build())
        .build();
  }

  /** Reads an enumerated type declaration and returns it. */
  private EnumElement readEnumElement(Location location, String documentation) {
    String name = reader.readName();
    EnumElement.Builder builder = EnumElement.builder(location)
        .name(name)
        .documentation(documentation);

    ImmutableList.Builder<EnumConstantElement> constants = ImmutableList.builder();
    ImmutableList.Builder<OptionElement> options = ImmutableList.builder();
    reader.require('{');
    while (true) {
      String valueDocumentation = reader.readDocumentation();
      if (reader.peekChar('}')) break;

      Object declared = readDeclaration(valueDocumentation, Context.ENUM);
      if (declared instanceof EnumConstantElement) {
        constants.add((EnumConstantElement) declared);
      } else if (declared instanceof OptionElement) {
        options.add((OptionElement) declared);
      }
    }
    return builder.options(options.build())
        .constants(constants.build())
        .build();
  }

  private Object readField(String documentation, Location location, String word) {
    Field.Label label;
    String type;
    switch (word) {
      case "required":
        if (syntax == ProtoFile.Syntax.PROTO_3) {
          throw reader.unexpected(
              location, "'required' label forbidden in proto3 field declarations");
        }
        label = Field.Label.REQUIRED;
        type = reader.readDataType();
        break;

      case "optional":
        if (syntax == ProtoFile.Syntax.PROTO_3) {
          throw reader.unexpected(
              location, "'optional' label forbidden in proto3 field declarations");
        }
        label = Field.Label.OPTIONAL;
        type = reader.readDataType();
        break;

      case "repeated":
        label = Field.Label.REPEATED;
        type = reader.readDataType();
        break;

      default:
        if (syntax != ProtoFile.Syntax.PROTO_3
            && (!word.equals("map") || reader.peekChar() != '<')) {
          throw reader.unexpected(location, "unexpected label: " + word);
        }
        label = null;
        type = reader.readDataType(word);
        break;
    }

    if (type.startsWith("map<") && label != null) {
      throw reader.unexpected(location, "'map' type cannot have label");
    }
    if (type.equals("group")) {
      return readGroup(documentation, label);
    }

    return readField(location, documentation, label, type);
  }

  /** Reads an field declaration and returns it. */
  private FieldElement readField(
      Location location, String documentation, @Nullable Field.Label label, String type) {
    String name = reader.readName();
    reader.require('=');
    int tag = reader.readInt();

    FieldElement.Builder builder = FieldElement.builder(location)
        .label(label)
        .type(type)
        .name(name)
        .tag(tag);

    ImmutableList.Builder<OptionElement> options = ImmutableList.builder();
    if (reader.peekChar('[')) {
      while (true) {
        OptionElement option = readOption('=');
        if (option.name().equals("default")) {
          builder.defaultValue(String.valueOf(option.value())); // Defaults aren't options!
        } else {
          options.add(option);
        }

        // Check for closing ']'
        if (reader.peekChar(']')) break;

        // Discard optional ','.
        reader.peekChar(',');
      }
    }
    reader.require(';');
    documentation = reader.tryAppendTrailingDocumentation(documentation);
    return builder.documentation(documentation)
        .options(options.build())
        .build();
  }

  private OneOfElement readOneOf(String documentation) {
    OneOfElement.Builder builder = OneOfElement.builder()
        .name(reader.readName())
        .documentation(documentation);
    ImmutableList.Builder<FieldElement> fields = ImmutableList.builder();
    ImmutableList.Builder<GroupElement> groups = ImmutableList.builder();

    reader.require('{');
    while (true) {
      String nestedDocumentation = reader.readDocumentation();
      if (reader.peekChar('}')) break;

      Location location = reader.location();
      String type = reader.readDataType();
      if (type.equals("group")) {
        groups.add(readGroup(nestedDocumentation, null));
      } else {
        fields.add(readField(location, nestedDocumentation, null, type));
      }
    }
    return builder.fields(fields.build())
        .groups(groups.build())
        .build();
  }

  private GroupElement readGroup(String documentation, Field.Label label) {
    String name = reader.readWord();
    reader.require('=');
    int tag = reader.readInt();

    GroupElement.Builder builder = GroupElement.builder()
        .label(label)
        .name(name)
        .tag(tag)
        .documentation(documentation);
    ImmutableList.Builder<FieldElement> fields = ImmutableList.builder();

    reader.require('{');
    while (true) {
      String nestedDocumentation = reader.readDocumentation();
      if (reader.peekChar('}')) break;

      Location location = reader.location();
      String fieldLabel = reader.readWord();
      Object field = readField(nestedDocumentation, location, fieldLabel);
      if (!(field instanceof FieldElement)) {
        throw reader.unexpected("expected field declaration, was " + field);
      }
      fields.add((FieldElement) field);
    }

    return builder.fields(fields.build())
        .build();
  }

  /** Reads a reserved tags and names list like "reserved 10, 12 to 14, 'foo';". */
  private ReservedElement readReserved(Location location, String documentation) {
    ImmutableList.Builder<Object> valuesBuilder = ImmutableList.builder();

    while (true) {
      char c = reader.peekChar();
      if (c == '"' || c == '\'') {
        valuesBuilder.add(reader.readQuotedString());
      } else {
        int tagStart = reader.readInt();

        c = reader.peekChar();
        if (c != ',' && c != ';') {
          if (!reader.readWord().equals("to")) {
            throw reader.unexpected("expected ',', ';', or 'to'");
          }
          int tagEnd = reader.readInt();
          valuesBuilder.add(Range.closed(tagStart, tagEnd));
        } else {
          valuesBuilder.add(tagStart);
        }
      }
      c = reader.readChar();
      if (c == ';') break;
      if (c != ',') throw reader.unexpected("expected ',' or ';'");
    }

    ImmutableList<Object> values = valuesBuilder.build();
    if (values.isEmpty()) {
      throw reader.unexpected("'reserved' must have at least one field name or tag");
    }
    return ReservedElement.create(location, documentation, values);
  }

  /** Reads extensions like "extensions 101;" or "extensions 101 to max;". */
  private ExtensionsElement readExtensions(Location location, String documentation) {
    int start = reader.readInt(); // Range start.
    int end = start;
    if (reader.peekChar() != ';') {
      if (!"to".equals(reader.readWord())) throw reader.unexpected("expected ';' or 'to'");
      String s = reader.readWord(); // Range end.
      if (s.equals("max")) {
        end = Util.MAX_TAG_VALUE;
      } else {
        end = Integer.parseInt(s);
      }
    }
    reader.require(';');
    return ExtensionsElement.create(location, start, end, documentation);
  }

  /** Reads a option containing a name, an '=' or ':', and a value. */
  private OptionElement readOption(char keyValueSeparator) {
    boolean isExtension = (reader.peekChar() == '[');
    boolean isParenthesized = (reader.peekChar() == '(');
    String name = reader.readName(); // Option name.
    if (isExtension) {
      name = "[" + name + "]";
    }
    String subName = null;
    char c = reader.readChar();
    if (c == '.') {
      // Read nested field name. For example "baz" in "(foo.bar).baz = 12".
      subName = reader.readName();
      c = reader.readChar();
    }
    if (keyValueSeparator == ':' && c == '{') {
      // In text format, values which are maps can omit a separator. Backtrack so it can be re-read.
      reader.pushBack('{');
    } else if (c != keyValueSeparator) {
      throw reader.unexpected("expected '" + keyValueSeparator + "' in option");
    }
    OptionKindAndValue kindAndValue = readKindAndValue();
    OptionElement.Kind kind = kindAndValue.kind();
    Object value = kindAndValue.value();
    if (subName != null) {
      value = OptionElement.create(subName, kind, value);
      kind = OptionElement.Kind.OPTION;
    }
    return OptionElement.create(name, kind, value, isParenthesized);
  }

  /** Reads an enum constant like "ROCK = 0;". The label is the constant name. */
  private EnumConstantElement readEnumConstant(
      String documentation, Location location, String label) {
    reader.require('=');

    int tag = reader.readInt();

    ImmutableList.Builder<OptionElement> options = ImmutableList.builder();
    if (reader.peekChar() == '[') {
      reader.readChar();
      while (true) {
        options.add(readOption('='));
        char c = reader.readChar();
        if (c == ']') {
          break;
        }
        if (c != ',') {
          throw reader.unexpected("Expected ',' or ']");
        }
      }
    }
    reader.require(';');
    documentation = reader.tryAppendTrailingDocumentation(documentation);

    return EnumConstantElement.builder(location)
        .name(label)
        .tag(tag)
        .documentation(documentation)
        .options(options.build())
        .build();
  }

  @AutoValue
  abstract static class OptionKindAndValue {
    static OptionKindAndValue of(OptionElement.Kind kind, Object value) {
      return new AutoValue_ProtoParser_OptionKindAndValue(kind, value);
    }

    abstract OptionElement.Kind kind();
    abstract Object value();
  }

  /** Reads a value that can be a map, list, string, number, boolean or enum. */
  private OptionKindAndValue readKindAndValue() {
    char peeked = reader.peekChar();
    switch (peeked) {
      case '{':
        return OptionKindAndValue.of(OptionElement.Kind.MAP, readMap('{', '}', ':'));
      case '[':
        return OptionKindAndValue.of(OptionElement.Kind.LIST, readList());
      case '"':
      case '\'':
        return OptionKindAndValue.of(OptionElement.Kind.STRING, reader.readString());
      default:
        if (Character.isDigit(peeked) || peeked == '-') {
          return OptionKindAndValue.of(OptionElement.Kind.NUMBER, reader.readWord());
        }
        String word = reader.readWord();
        switch (word) {
          case "true":
            return OptionKindAndValue.of(OptionElement.Kind.BOOLEAN, "true");
          case "false":
            return OptionKindAndValue.of(OptionElement.Kind.BOOLEAN, "false");
          default:
            return OptionKindAndValue.of(OptionElement.Kind.ENUM, word);
        }
    }
  }

  /**
   * Returns a map of string keys and values. This is similar to a JSON object,
   * with '{' and '}' surrounding the map, ':' separating keys from values, and
   * ',' separating entries.
   */
  @SuppressWarnings("unchecked")
  private Map<String, Object> readMap(char openBrace, char closeBrace, char keyValueSeparator) {
    if (reader.readChar() != openBrace) throw new AssertionError();
    Map<String, Object> result = new LinkedHashMap<>();
    while (true) {
      if (reader.peekChar(closeBrace)) {
        // If we see the close brace, finish immediately. This handles {}/[] and ,}/,] cases.
        return result;
      }

      OptionElement option = readOption(keyValueSeparator);
      String name = option.name();
      Object value = option.value();
      if (value instanceof OptionElement) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) result.get(name);
        if (nested == null) {
          nested = new LinkedHashMap<>();
          result.put(name, nested);
        }
        OptionElement valueOption = (OptionElement) value;
        nested.put(valueOption.name(), valueOption.value());
      } else {
        // Add the value(s) to any previous values with the same key
        Object previous = result.get(name);
        if (previous == null) {
          result.put(name, value);
        } else if (previous instanceof List) {
          // Add to previous List
          addToList((List<Object>) previous, value);
        } else {
          List<Object> newList = new ArrayList<>();
          newList.add(previous);
          addToList(newList, value);
          result.put(name, newList);
        }
      }

      // Discard optional ',' separator.
      reader.peekChar(',');
    }
  }

  /**
   * Adds an object or objects to a List.
   */
  private void addToList(List<Object> list, Object value) {
    if (value instanceof List) {
      list.addAll((List) value);
    } else {
      list.add(value);
    }
  }

  /**
   * Returns a list of values. This is similar to JSON with '[' and ']'
   * surrounding the list and ',' separating values.
   */
  private List<Object> readList() {
    reader.require('[');
    List<Object> result = new ArrayList<>();
    while (true) {
      // If we see the close brace, finish immediately. This handles [] and ,] cases.
      if (reader.peekChar(']')) return result;

      result.add(readKindAndValue().value());

      if (reader.peekChar(',')) continue;
      if (reader.peekChar() != ']') throw reader.unexpected("expected ',' or ']'");
    }
  }

  /** Reads an rpc and returns it. */
  private RpcElement readRpc(Location location, String documentation) {
    RpcElement.Builder builder = RpcElement.builder(location)
        .name(reader.readName())
        .documentation(documentation);

    reader.require('(');
    String type;
    String word = reader.readWord();
    if (word.equals("stream")) {
      builder.requestStreaming(true);
      type = reader.readDataType();
    } else {
      type = reader.readDataType(word);
    }
    builder.requestType(type);
    reader.require(')');

    if (!reader.readWord().equals("returns")) throw reader.unexpected("expected 'returns'");

    reader.require('(');
    word = reader.readWord();
    if (word.equals("stream")) {
      builder.responseStreaming(true);
      type = reader.readDataType();
    } else {
      type = reader.readDataType(word);
    }
    builder.responseType(type);
    reader.require(')');

    if (reader.peekChar('{')) {
      ImmutableList.Builder<OptionElement> options = ImmutableList.builder();
      while (true) {
        String rpcDocumentation = reader.readDocumentation();
        if (reader.peekChar('}')) {
          break;
        }
        Object declared = readDeclaration(rpcDocumentation, Context.RPC);
        if (declared instanceof OptionElement) {
          options.add((OptionElement) declared);
        }
      }
      builder.options(options.build());
    } else {
      reader.require(';');
    }

    return builder.build();
  }

  enum Context {
    FILE,
    MESSAGE,
    ENUM,
    RPC,
    EXTEND,
    SERVICE;

    public boolean permitsPackage() {
      return this == FILE;
    }

    public boolean permitsSyntax() {
      return this == FILE;
    }

    public boolean permitsImport() {
      return this == FILE;
    }

    public boolean permitsExtensions() {
      return this != FILE;
    }

    public boolean permitsRpc() {
      return this == SERVICE;
    }

    public boolean permitsOneOf() {
      return this == MESSAGE;
    }
  }
}
