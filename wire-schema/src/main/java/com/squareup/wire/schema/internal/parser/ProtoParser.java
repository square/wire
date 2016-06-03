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
  /** Parse a named {@code .proto} schema. */
  public static ProtoFileElement parse(Location location, String data) {
    return new ProtoParser(location, data.toCharArray()).readProtoFile();
  }

  private final Location location;
  private final char[] data;
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
  /** Our cursor within the document. {@code data[pos]} is the next character to be read. */
  private int pos;
  /** The number of newline characters encountered thus far. */
  private int line;
  /** The index of the most recent newline character. */
  private int lineStart;
  /** Output package name, or null if none yet encountered. */
  private String packageName;

  /** The current package name + nested type names, separated by dots. */
  private String prefix = "";

  ProtoParser(Location location, char[] data) {
    this.location = location;
    this.data = data;
    this.fileBuilder = ProtoFileElement.builder(location);
  }

  ProtoFileElement readProtoFile() {
    while (true) {
      String documentation = readDocumentation();
      if (pos == data.length) {
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
    if (peekChar() == ';') {
      pos++;
      return null;
    }

    Location location = location();
    String label = readWord();

    if (label.equals("package")) {
      if (!context.permitsPackage()) throw unexpected(location, "'package' in " + context);
      if (packageName != null) throw unexpected(location, "too many package names");
      packageName = readName();
      fileBuilder.packageName(packageName);
      prefix = packageName + ".";
      if (readChar() != ';') throw unexpected("expected ';'");
      return null;
    } else if (label.equals("import")) {
      if (!context.permitsImport()) throw unexpected(location, "'import' in " + context);
      String importString = readString();
      if ("public".equals(importString)) {
        publicImports.add(readString());
      } else {
        imports.add(importString);
      }
      if (readChar() != ';') throw unexpected("expected ';'");
      return null;
    } else if (label.equals("syntax")) {
      if (!context.permitsSyntax()) throw unexpected(location, "'syntax' in " + context);
      if (readChar() != '=') throw unexpected("expected '='");
      if (index != 0) {
        throw unexpected(location, "'syntax' element must be the first declaration in a file");
      }
      String syntaxString = readQuotedString();
      try {
        syntax = ProtoFile.Syntax.get(syntaxString);
      } catch (IllegalArgumentException e) {
        throw unexpected(location, e.getMessage());
      }
      if (readChar() != ';') throw unexpected("expected ';'");
      return null;
    } else if (label.equals("option")) {
      OptionElement result = readOption('=');
      if (readChar() != ';') throw unexpected("expected ';'");
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
      if (!context.permitsRpc()) throw unexpected(location, "'rpc' in " + context);
      return readRpc(location, documentation);
    } else if (label.equals("oneof")) {
      if (!context.permitsOneOf()) throw unexpected(location, "'oneof' must be nested in message");
      return readOneOf(documentation);
    } else if (label.equals("extensions")) {
      if (!context.permitsExtensions()) throw unexpected(location, "'extensions' must be nested");
      return readExtensions(location, documentation);
    } else if (context == Context.MESSAGE || context == Context.EXTEND) {
      return readField(documentation, location, label);
    } else if (context == Context.ENUM) {
      if (readChar() != '=') throw unexpected("expected '='");

      EnumConstantElement.Builder builder = EnumConstantElement.builder(location)
          .name(label)
          .tag(readInt());

      ImmutableList.Builder<OptionElement> options = ImmutableList.builder();
      if (peekChar() == '[') {
        readChar();
        while (true) {
          options.add(readOption('='));
          char c = readChar();
          if (c == ']') {
            break;
          }
          if (c != ',') {
            throw unexpected("Expected ',' or ']");
          }
        }
      }
      if (readChar() != ';') throw unexpected("expected ';'");
      documentation = tryAppendTrailingDocumentation(documentation);
      return builder.documentation(documentation)
          .options(options.build())
          .build();
    } else {
      throw unexpected(location, "unexpected label: " + label);
    }
  }

  /** Reads a message declaration. */
  private MessageElement readMessage(Location location, String documentation) {
    String name = readName();
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

    if (readChar() != '{') throw unexpected("expected '{'");
    while (true) {
      String nestedDocumentation = readDocumentation();
      if (peekChar() == '}') {
        pos++;
        break;
      }
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
    String name = readName();
    ExtendElement.Builder builder = ExtendElement.builder(location)
        .name(name)
        .documentation(documentation);

    if (readChar() != '{') throw unexpected("expected '{'");
    ImmutableList.Builder<FieldElement> fields = ImmutableList.builder();
    while (true) {
      String nestedDocumentation = readDocumentation();
      if (peekChar() == '}') {
        pos++;
        break;
      }
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
    String name = readName();
    ServiceElement.Builder builder = ServiceElement.builder(location)
        .name(name)
        .documentation(documentation);

    if (readChar() != '{') throw unexpected("expected '{'");
    ImmutableList.Builder<RpcElement> rpcs = ImmutableList.builder();
    ImmutableList.Builder<OptionElement> options = ImmutableList.builder();
    while (true) {
      String rpcDocumentation = readDocumentation();
      if (peekChar() == '}') {
        pos++;
        break;
      }
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
    String name = readName();
    EnumElement.Builder builder = EnumElement.builder(location)
        .name(name)
        .documentation(documentation);

    if (readChar() != '{') throw unexpected("expected '{'");
    ImmutableList.Builder<EnumConstantElement> constants = ImmutableList.builder();
    ImmutableList.Builder<OptionElement> options = ImmutableList.builder();
    while (true) {
      String valueDocumentation = readDocumentation();
      if (peekChar() == '}') {
        pos++;
        break;
      }
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
          throw unexpected(location, "'required' label forbidden in proto3 field declarations");
        }
        label = Field.Label.REQUIRED;
        type = readDataType();
        break;

      case "optional":
        if (syntax == ProtoFile.Syntax.PROTO_3) {
          throw unexpected(location, "'optional' label forbidden in proto3 field declarations");
        }
        label = Field.Label.OPTIONAL;
        type = readDataType();
        break;

      case "repeated":
        label = Field.Label.REPEATED;
        type = readDataType();
        break;

      default:
        if (syntax != ProtoFile.Syntax.PROTO_3 && (!word.equals("map") || peekChar() != '<')) {
          throw unexpected(location, "unexpected label: " + word);
        }
        label = null;
        type = readDataType(word);
        break;
    }

    if (type.startsWith("map<") && label != null) {
      throw unexpected(location, "'map' type cannot have label");
    }
    if (type.equals("group")) {
      return readGroup(documentation, label);
    }

    return readField(location, documentation, label, type);
  }

  /** Reads an field declaration and returns it. */
  private FieldElement readField(
      Location location, String documentation, @Nullable Field.Label label, String type) {
    String name = readName();
    if (readChar() != '=') throw unexpected("expected '='");
    int tag = readInt();

    FieldElement.Builder builder = FieldElement.builder(location)
        .label(label)
        .type(type)
        .name(name)
        .tag(tag);

    ImmutableList.Builder<OptionElement> options = ImmutableList.builder();
    if (peekChar() == '[') {
      pos++;
      while (true) {
        OptionElement option = readOption('=');
        if (option.name().equals("default")) {
          builder.defaultValue(String.valueOf(option.value())); // Defaults aren't options!
        } else {
          options.add(option);
        }

        // Check for optional ',' or closing ']'
        char c = peekChar();
        if (c == ']') {
          pos++;
          break;
        } else if (c == ',') {
          pos++;
        }
      }
    }
    if (readChar() != ';') {
      throw unexpected("expected ';'");
    }
    documentation = tryAppendTrailingDocumentation(documentation);
    return builder.documentation(documentation)
        .options(options.build())
        .build();
  }

  private OneOfElement readOneOf(String documentation) {
    OneOfElement.Builder builder = OneOfElement.builder()
        .name(readName())
        .documentation(documentation);
    ImmutableList.Builder<FieldElement> fields = ImmutableList.builder();
    ImmutableList.Builder<GroupElement> groups = ImmutableList.builder();

    if (readChar() != '{') throw unexpected("expected '{'");
    while (true) {
      String nestedDocumentation = readDocumentation();
      if (peekChar() == '}') {
        pos++;
        break;
      }
      Location location = location();
      String type = readDataType();
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
    String name = readWord();
    if (readChar() != '=') {
      throw unexpected("expected '='");
    }
    int tag = readInt();

    GroupElement.Builder builder = GroupElement.builder()
        .label(label)
        .name(name)
        .tag(tag)
        .documentation(documentation);
    ImmutableList.Builder<FieldElement> fields = ImmutableList.builder();

    if (readChar() != '{') throw unexpected("expected '{'");
    while (true) {
      String nestedDocumentation = readDocumentation();
      if (peekChar() == '}') {
        pos++;
        break;
      }
      Location location = location();
      String fieldLabel = readWord();
      Object field = readField(nestedDocumentation, location, fieldLabel);
      if (!(field instanceof FieldElement)) {
        throw unexpected("expected field declaration, was " + field);
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
      char c = peekChar();
      if (c == '"' || c == '\'') {
        valuesBuilder.add(readQuotedString());
      } else {
        int tagStart = readInt();

        c = peekChar();
        if (c != ',' && c != ';') {
          if (!readWord().equals("to")) {
            throw unexpected("expected ',', ';', or 'to'");
          }
          int tagEnd = readInt();
          valuesBuilder.add(Range.closed(tagStart, tagEnd));
        } else {
          valuesBuilder.add(tagStart);
        }
      }
      c = readChar();
      if (c == ';') break;
      if (c != ',') throw unexpected("expected ',' or ';'");
    }

    ImmutableList<Object> values = valuesBuilder.build();
    if (values.isEmpty()) {
      throw unexpected("'reserved' must have at least one field name or tag");
    }
    return ReservedElement.create(location, documentation, values);
  }

  /** Reads extensions like "extensions 101;" or "extensions 101 to max;". */
  private ExtensionsElement readExtensions(Location location, String documentation) {
    int start = readInt(); // Range start.
    int end = start;
    if (peekChar() != ';') {
      if (!"to".equals(readWord())) throw unexpected("expected ';' or 'to'");
      String s = readWord(); // Range end.
      if (s.equals("max")) {
        end = Util.MAX_TAG_VALUE;
      } else {
        end = Integer.parseInt(s);
      }
    }
    if (readChar() != ';') throw unexpected("expected ';'");
    return ExtensionsElement.create(location, start, end, documentation);
  }

  /** Reads a option containing a name, an '=' or ':', and a value. */
  private OptionElement readOption(char keyValueSeparator) {
    boolean isExtension = (peekChar() == '[');
    boolean isParenthesized = (peekChar() == '(');
    String name = readName(); // Option name.
    if (isExtension) {
      name = "[" + name + "]";
    }
    String subName = null;
    char c = readChar();
    if (c == '.') {
      // Read nested field name. For example "baz" in "(foo.bar).baz = 12".
      subName = readName();
      c = readChar();
    }
    if (keyValueSeparator == ':' && c == '{') {
      // In text format, values which are maps can omit a separator. Backtrack so it can be re-read.
      pos--;
    } else if (c != keyValueSeparator) {
      throw unexpected("expected '" + keyValueSeparator + "' in option");
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
    char peeked = peekChar();
    switch (peeked) {
      case '{':
        return OptionKindAndValue.of(OptionElement.Kind.MAP, readMap('{', '}', ':'));
      case '[':
        return OptionKindAndValue.of(OptionElement.Kind.LIST, readList());
      case '"':
      case '\'':
        return OptionKindAndValue.of(OptionElement.Kind.STRING, readString());
      default:
        if (Character.isDigit(peeked) || peeked == '-') {
          return OptionKindAndValue.of(OptionElement.Kind.NUMBER, readWord());
        }
        String word = readWord();
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
    if (readChar() != openBrace) throw new AssertionError();
    Map<String, Object> result = new LinkedHashMap<>();
    while (true) {
      if (peekChar() == closeBrace) {
        // If we see the close brace, finish immediately. This handles {}/[] and ,}/,] cases.
        pos++;
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

      // ',' separator is optional, skip if present
      if (peekChar() == ',') {
        pos++;
      }
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
    if (readChar() != '[') throw new AssertionError();
    List<Object> result = new ArrayList<>();
    while (true) {
      if (peekChar() == ']') {
        // If we see the close brace, finish immediately. This handles [] and ,] cases.
        pos++;
        return result;
      }

      result.add(readKindAndValue().value());

      char c = peekChar();
      if (c == ',') {
        pos++;
      } else if (c != ']') {
        throw unexpected("expected ',' or ']'");
      }
    }
  }

  /** Reads an rpc and returns it. */
  private RpcElement readRpc(Location location, String documentation) {
    RpcElement.Builder builder = RpcElement.builder(location)
        .name(readName())
        .documentation(documentation);

    if (readChar() != '(') throw unexpected("expected '('");
    String type;
    String word = readWord();
    if (word.equals("stream")) {
      builder.requestStreaming(true);
      type = readDataType();
    } else {
      type = readDataType(word);
    }
    builder.requestType(type);
    if (readChar() != ')') throw unexpected("expected ')'");

    if (!readWord().equals("returns")) throw unexpected("expected 'returns'");

    if (readChar() != '(') throw unexpected("expected '('");
    word = readWord();
    if (word.equals("stream")) {
      builder.responseStreaming(true);
      type = readDataType();
    } else {
      type = readDataType(word);
    }
    builder.responseType(type);
    if (readChar() != ')') throw unexpected("expected ')'");

    if (peekChar() == '{') {
      ImmutableList.Builder<OptionElement> options = ImmutableList.builder();
      pos++;
      while (true) {
        String rpcDocumentation = readDocumentation();
        if (peekChar() == '}') {
          pos++;
          break;
        }
        Object declared = readDeclaration(rpcDocumentation, Context.RPC);
        if (declared instanceof OptionElement) {
          options.add((OptionElement) declared);
        }
      }
      builder.options(options.build());
    } else if (readChar() != ';') throw unexpected("expected ';'");

    return builder.build();
  }

  /** Reads a non-whitespace character and returns it. */
  private char readChar() {
    char result = peekChar();
    pos++;
    return result;
  }

  /**
   * Peeks a non-whitespace character and returns it. The only difference
   * between this and {@code readChar} is that this doesn't consume the char.
   */
  private char peekChar() {
    skipWhitespace(true);
    if (pos == data.length) throw unexpected("unexpected end of file");
    return data[pos];
  }

  /** Reads a quoted or unquoted string and returns it. */
  private String readString() {
    skipWhitespace(true);
    char c = peekChar();
    return c == '"' || c == '\'' ? readQuotedString() : readWord();
  }

  private String readQuotedString() {
    char startQuote = readChar();
    if (startQuote != '"' && startQuote != '\'') throw new AssertionError();
    StringBuilder result = new StringBuilder();
    while (pos < data.length) {
      char c = data[pos++];
      if (c == startQuote) {
        if (peekChar() == '"' || peekChar() == '\'') {
          // Adjacent strings are concatenated. Consume new quote and continue reading.
          startQuote = readChar();
          continue;
        }
        return result.toString();
      }

      if (c == '\\') {
        if (pos == data.length) throw unexpected("unexpected end of file");
        c = data[pos++];
        switch (c) {
          case 'a': c = 0x7; break;
          case 'b': c = '\b'; break;
          case 'f': c = '\f'; break;
          case 'n': c = '\n'; break;
          case 'r': c = '\r'; break;
          case 't': c = '\t'; break;
          case 'v': c = 0xb; break;
          case 'x':case 'X':
            c = readNumericEscape(16, 2);
            break;
          case '0':case '1':case '2':case '3':case '4':case '5':case '6':case '7':
            --pos;
            c = readNumericEscape(8, 3);
            break;
          default:
            // use char as-is
            break;
        }
      }

      result.append(c);
      if (c == '\n') newline();
    }
    throw unexpected("unterminated string");
  }

  private char readNumericEscape(int radix, int len) {
    int value = -1;
    for (int endPos = Math.min(pos + len, data.length); pos < endPos; pos++) {
      int digit = hexDigit(data[pos]);
      if (digit == -1 || digit >= radix) break;
      if (value < 0) {
        value = digit;
      } else {
        value = value * radix + digit;
      }
    }
    if (value < 0) throw unexpected("expected a digit after \\x or \\X");
    return (char) value;
  }

  private int hexDigit(char c) {
    if (c >= '0' && c <= '9') return c - '0';
    else if (c >= 'a' && c <= 'f') return c - 'a' + 10;
    else if (c >= 'A' && c <= 'F') return c - 'A' + 10;
    else return -1;
  }

  /** Reads a (paren-wrapped), [square-wrapped] or naked symbol name. */
  private String readName() {
    String optionName;
    char c = peekChar();
    if (c == '(') {
      pos++;
      optionName = readWord();
      if (readChar() != ')') throw unexpected("expected ')'");
    } else if (c == '[') {
      pos++;
      optionName = readWord();
      if (readChar() != ']') throw unexpected("expected ']'");
    } else {
      optionName = readWord();
    }
    return optionName;
  }

  /** Reads a scalar, map, or type name. */
  private String readDataType() {
    String name = readWord();
    return readDataType(name);
  }

  /** Reads a scalar, map, or type name with {@code name} as a prefix word. */
  private String readDataType(String name) {
    if (name.equals("map")) {
      if (readChar() != '<') throw unexpected("expected '<'");
      String keyType = readDataType();
      if (readChar() != ',') throw unexpected("expected ','");
      String valueType = readDataType();
      if (readChar() != '>') throw unexpected("expected '>'");
      return String.format("map<%s, %s>", keyType, valueType);
    } else {
      return name;
    }
  }

  /** Reads a non-empty word and returns it. */
  private String readWord() {
    skipWhitespace(true);
    int start = pos;
    while (pos < data.length) {
      char c = data[pos];
      if ((c >= 'a' && c <= 'z')
          || (c >= 'A' && c <= 'Z')
          || (c >= '0' && c <= '9')
          || (c == '_')
          || (c == '-')
          || (c == '.')) {
        pos++;
      } else {
        break;
      }
    }
    if (start == pos) throw unexpected("expected a word");
    return new String(data, start, pos - start);
  }

  /** Reads an integer and returns it. */
  private int readInt() {
    String tag = readWord();
    try {
      int radix = 10;
      if (tag.startsWith("0x") || tag.startsWith("0X")) {
        tag = tag.substring("0x".length());
        radix = 16;
      }
      return Integer.valueOf(tag, radix);
    } catch (Exception e) {
      throw unexpected("expected an integer but was " + tag);
    }
  }

  /**
   * Like {@link #skipWhitespace}, but this returns a string containing all
   * comment text. By convention, comments before a declaration document that
   * declaration.
   */
  private String readDocumentation() {
    String result = null;
    while (true) {
      skipWhitespace(false);
      if (pos == data.length || data[pos] != '/') {
        return result != null ? result : "";
      }
      String comment = readComment();
      result = (result == null) ? comment : (result + "\n" + comment);
    }
  }

  /** Reads a comment and returns its body. */
  private String readComment() {
    if (pos == data.length || data[pos] != '/') throw new AssertionError();
    pos++;
    int commentType = pos < data.length ? data[pos++] : -1;
    if (commentType == '*') {
      StringBuilder result = new StringBuilder();
      boolean startOfLine = true;

      for (; pos + 1 < data.length; pos++) {
        char c = data[pos];
        if (c == '*' && data[pos + 1] == '/') {
          pos += 2;
          return result.toString().trim();
        }
        if (c == '\n') {
          result.append('\n');
          newline();
          startOfLine = true;
        } else if (!startOfLine) {
          result.append(c);
        } else if (c == '*') {
          if (data[pos + 1] == ' ') {
            pos += 1; // Skip a single leading space, if present.
          }
          startOfLine = false;
        } else if (!Character.isWhitespace(c)) {
          result.append(c);
          startOfLine = false;
        }
      }
      throw unexpected("unterminated comment");
    } else if (commentType == '/') {
      if (pos < data.length && data[pos] == ' ') {
        pos += 1; // Skip a single leading space, if present.
      }
      int start = pos;
      while (pos < data.length) {
        char c = data[pos++];
        if (c == '\n') {
          newline();
          break;
        }
      }
      return new String(data, start, pos - 1 - start);
    } else {
      throw unexpected("unexpected '/'");
    }
  }

  private String tryAppendTrailingDocumentation(String documentation) {
    // Search for a '/' character ignoring spaces and tabs.
    while (pos < data.length) {
      char c = data[pos];
      if (c == ' ' || c == '\t') {
        pos++;
      } else if (c == '/') {
        pos++;
        break;
      } else {
        // Not a whitespace or comment-starting character. Return original documentation.
        return documentation;
      }
    }

    if (pos == data.length || (data[pos] != '/' && data[pos] != '*')) {
      pos--; // Backtrack to start of comment.
      throw unexpected("expected '//' or '/*'");
    }
    boolean isStar = data[pos] == '*';
    pos++;

    if (pos < data.length && data[pos] == ' ') {
      pos++; // Skip a single leading space, if present.
    }

    int start = pos;
    int end;

    if (isStar) {
      // Consume star comment until it closes on the same line.
      while (true) {
        if (pos == data.length || data[pos] == '\n') {
          throw unexpected("trailing comment must be closed on the same line");
        }
        if (data[pos] == '*' && pos + 1 < data.length && data[pos + 1] == '/') {
          end = pos - 1; // The character before '*'.
          pos += 2; // Skip to the character after '/'.
          break;
        }
        pos++;
      }
      // Ensure nothing follows a trailing star comment.
      while (pos < data.length) {
        char c = data[pos++];
        if (c == '\n') {
          newline();
          break;
        }
        if (c != ' ' && c != '\t') {
          throw unexpected("no syntax may follow trailing comment");
        }
      }
    } else {
      // Consume comment until newline.
      while (true) {
        if (pos == data.length) {
          end = pos - 1;
          break;
        }
        char c = data[pos++];
        if (c == '\n') {
          newline();
          end = pos - 2; // Account for stepping past the newline.
          break;
        }
      }
    }

    // Remove trailing whitespace.
    while (end > start && (data[end] == ' ' || data[end] == '\t')) {
      end--;
    }

    if (end == start) {
      return documentation;
    }
    String trailingDocumentation = new String(data, start, end - start + 1);
    if (documentation.isEmpty()) {
      return trailingDocumentation;
    }
    return documentation + '\n' + trailingDocumentation;
  }

  /**
   * Skips whitespace characters and optionally comments. When this returns,
   * either {@code pos == data.length} or a non-whitespace character.
   */
  private void skipWhitespace(boolean skipComments) {
    while (pos < data.length) {
      char c = data[pos];
      if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
        pos++;
        if (c == '\n') newline();
      } else if (skipComments && c == '/') {
        readComment();
      } else {
        break;
      }
    }
  }

  /** Call this every time a '\n' is encountered. */
  private void newline() {
    line++;
    lineStart = pos;
  }

  private Location location() {
    return location.at(line + 1, pos - lineStart + 1);
  }

  private RuntimeException unexpected(String message) {
    return unexpected(location(), message);
  }

  private RuntimeException unexpected(Location location, String message) {
    throw new IllegalStateException(String.format("Syntax error in %s: %s", location, message));
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
