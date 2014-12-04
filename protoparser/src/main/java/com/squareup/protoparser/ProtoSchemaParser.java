// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

/** Basic parser for {@code .proto} schema declarations. */
public final class ProtoSchemaParser {
  /** Parse a {@code .proto} definition file. */
  public static ProtoFile parseUtf8(File file) throws IOException {
    try (InputStream is = new FileInputStream(file)) {
      return parseUtf8(file.getPath(), is);
    }
  }

  /** Parse a {@code .proto} definition file. */
  public static ProtoFile parseUtf8(Path path) throws IOException {
    try (Reader reader = Files.newBufferedReader(path, UTF_8)) {
      return parse(path.toString(), reader);
    }
  }

  /** Parse a named {@code .proto} schema. The {@code InputStream} is not closed. */
  public static ProtoFile parseUtf8(String name, InputStream is) throws IOException {
    return parse(name, new InputStreamReader(is, UTF_8));
  }

  /** Parse a named {@code .proto} schema. The {@code Reader} is not closed. */
  public static ProtoFile parse(String name, Reader reader) throws IOException {
    CharArrayWriter writer = new CharArrayWriter();
    char[] buffer = new char[1024];
    int count;
    while ((count = reader.read(buffer)) != -1) {
      writer.write(buffer, 0, count);
    }
    return new ProtoSchemaParser(name, writer.toCharArray()).readProtoFile();
  }

  /** Parse a named {@code .proto} schema. */
  public static ProtoFile parse(String name, String data) {
    return new ProtoSchemaParser(name, data.toCharArray()).readProtoFile();
  }

  private final String filePath;
  private final char[] data;
  private final ProtoFile.Builder builder;

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

  ProtoSchemaParser(String filePath, char[] data) {
    this.filePath = filePath;
    this.data = data;
    this.builder = ProtoFile.builder(filePath);
  }

  ProtoFile readProtoFile() {
    while (true) {
      String documentation = readDocumentation();
      if (pos == data.length) {
        return builder.build();
      }
      Object declaration = readDeclaration(documentation, Context.FILE);
      if (declaration instanceof TypeElement) {
        builder.addType((TypeElement) declaration);
      } else if (declaration instanceof ServiceElement) {
        builder.addService((ServiceElement) declaration);
      } else if (declaration instanceof OptionElement) {
        builder.addOption((OptionElement) declaration);
      } else if (declaration instanceof ExtendElement) {
        builder.addExtendDeclaration((ExtendElement) declaration);
      }
    }
  }

  private Object readDeclaration(String documentation, Context context) {
    // Skip unnecessary semicolons, occasionally used after a nested message declaration.
    if (peekChar() == ';') {
      pos++;
      return null;
    }

    String label = readWord();

    if (label.equals("package")) {
      if (!context.permitsPackage()) throw unexpected("package in " + context);
      if (packageName != null) throw unexpected("too many package names");
      packageName = readName();
      builder.setPackageName(packageName);
      prefix = packageName + ".";
      if (readChar() != ';') throw unexpected("expected ';'");
      return null;
    } else if (label.equals("import")) {
      if (!context.permitsImport()) throw unexpected("import in " + context);
      String importString = readString();
      if ("public".equals(importString)) {
        builder.addPublicDependency(readString());
      } else {
        builder.addDependency(importString);
      }
      if (readChar() != ';') throw unexpected("expected ';'");
      return null;
    } else if (label.equals("option")) {
      OptionElement result = readOption('=');
      if (readChar() != ';') throw unexpected("expected ';'");
      return result;
    } else if (label.equals("message")) {
      return readMessage(documentation);
    } else if (label.equals("enum")) {
      return readEnumElement(documentation);
    } else if (label.equals("service")) {
      return readService(documentation);
    } else if (label.equals("extend")) {
      return readExtend(documentation);
    } else if (label.equals("rpc")) {
      if (!context.permitsRpc()) throw unexpected("rpc in " + context);
      return readRpc(documentation);
    } else if (label.equals("required") || label.equals("optional") || label.equals("repeated")) {
      if (!context.permitsField()) throw unexpected("fields must be nested");
      MessageElement.Label labelEnum = MessageElement.Label.valueOf(label.toUpperCase(Locale.US));
      return readField(documentation, labelEnum);
    } else if (label.equals("oneof")) {
      if (!context.permitsOneOf()) throw unexpected("oneofs must be nested in message");
      return readOneOf(documentation);
    } else if (label.equals("extensions")) {
      if (!context.permitsExtensions()) throw unexpected("extensions must be nested");
      return readExtensions(documentation);
    } else if (context == Context.ENUM) {
      List<OptionElement> options = new ArrayList<>();

      if (readChar() != '=') throw unexpected("expected '='");
      int tag = readInt();
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
      return EnumConstantElement.create(label, tag, documentation, options);
    } else {
      throw unexpected("unexpected label: " + label);
    }
  }

  /** Reads a message declaration. */
  private MessageElement readMessage(String documentation) {
    String previousPrefix = prefix;
    String name = readName();
    prefix = prefix + name + ".";
    List<FieldElement> fields = new ArrayList<>();
    List<OneOfElement> oneOfs = new ArrayList<>();
    List<TypeElement> nestedElements = new ArrayList<>();
    List<ExtensionsElement> extensions = new ArrayList<>();
    List<OptionElement> options = new ArrayList<>();
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
      } else if (declared instanceof TypeElement) {
        nestedElements.add((TypeElement) declared);
      } else if (declared instanceof ExtensionsElement) {
        extensions.add((ExtensionsElement) declared);
      } else if (declared instanceof OptionElement) {
        options.add((OptionElement) declared);
      } else if (declared instanceof ExtendElement) {
        // Extend declarations always add in a global scope regardless of nesting.
        builder.addExtendDeclaration((ExtendElement) declared);
      }
    }
    prefix = previousPrefix;
    return MessageElement.create(name, prefix + name, documentation, fields, oneOfs, nestedElements,
        extensions, options);
  }

  /** Reads an extend declaration. */
  private ExtendElement readExtend(String documentation) {
    String name = readName();
    List<FieldElement> fields = new ArrayList<>();
    if (readChar() != '{') throw unexpected("expected '{'");
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
    String fqname = name;
    if (!name.contains(".") && packageName != null) {
      fqname = packageName + "." + name;
    }
    return ExtendElement.create(name, fqname, documentation, fields);
  }

  /** Reads a service declaration and returns it. */
  private ServiceElement readService(String documentation) {
    String name = readName();
    List<OptionElement> options = new ArrayList<>();
    List<RpcElement> rpcs = new ArrayList<>();
    if (readChar() != '{') throw unexpected("expected '{'");
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
    return ServiceElement.create(name, prefix + name, documentation, options, rpcs);
  }

  /** Reads an enumerated type declaration and returns it. */
  private EnumElement readEnumElement(String documentation) {
    String name = readName();
    List<OptionElement> options = new ArrayList<>();
    List<EnumConstantElement> values = new ArrayList<>();
    if (readChar() != '{') throw unexpected("expected '{'");
    while (true) {
      String valueDocumentation = readDocumentation();
      if (peekChar() == '}') {
        pos++;
        break;
      }
      Object declared = readDeclaration(valueDocumentation, Context.ENUM);
      if (declared instanceof EnumConstantElement) {
        values.add((EnumConstantElement) declared);
      } else if (declared instanceof OptionElement) {
        options.add((OptionElement) declared);
      }
    }
    return EnumElement.create(name, prefix + name, documentation, options, values);
  }

  /** Reads an field declaration and returns it. */
  private FieldElement readField(String documentation, MessageElement.Label label) {
    String type = readName();
    String name = readName();
    if (readChar() != '=') throw unexpected("expected '='");
    int tag = readInt();
    List<OptionElement> options = new ArrayList<>();

    if (peekChar() == '[') {
      pos++;
      while (true) {
        options.add(readOption('='));

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
    if (readChar() == ';') {
      return FieldElement.create(label, type, name, tag, documentation, options);
    }
    throw unexpected("expected ';'");
  }

  private OneOfElement readOneOf(String documentation) {
    String name = readName();
    List<FieldElement> fields = new ArrayList<>();
    if (readChar() != '{') throw unexpected("expected '{'");
    while (true) {
      String nestedDocumentation = readDocumentation();
      if (peekChar() == '}') {
        pos++;
        break;
      }
      fields.add(readField(nestedDocumentation, MessageElement.Label.ONE_OF));
    }
    return OneOfElement.create(name, documentation, fields);
  }

  /** Reads extensions like "extensions 101;" or "extensions 101 to max;". */
  private ExtensionsElement readExtensions(String documentation) {
    int start = readInt(); // Range start.
    int end = start;
    if (peekChar() != ';') {
      if (!"to".equals(readWord())) throw unexpected("expected ';' or 'to'");
      String s = readWord(); // Range end.
      if (s.equals("max")) {
        end = ProtoFile.MAX_TAG_VALUE;
      } else {
        end = Integer.parseInt(s);
      }
    }
    if (readChar() != ';') throw unexpected("expected ';'");
    return ExtensionsElement.create(documentation, start, end);
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
    if (c != keyValueSeparator) {
      throw unexpected("expected '" + keyValueSeparator + "' in option");
    }
    Object value = readValue();
    Object valueOrSubOption =
        subName != null ? OptionElement.create(subName, value, isParenthesized) : value;
    return OptionElement.create(name, valueOrSubOption, isParenthesized);
  }

  /** Reads a value that can be a map, list, string, number, boolean or enum. */
  private Object readValue() {
    switch (peekChar()) {
      case '{':
        return readMap('{', '}', ':');
      case '[':
        return readList();
      case '"':
        return readString();
      default:
        if (Character.isDigit(peekChar())) {
          return readInt();
        }
        String word = readWord();
        switch (word) {
          case "true":
            return true;
          case "false":
            return false;
          default:
            return EnumConstantElement.anonymous(word);
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

      result.add(readValue());

      char c = peekChar();
      if (c == ',') {
        pos++;
      } else if (c != ']') {
        throw unexpected("expected ',' or ']'");
      }
    }
  }

  /** Reads an rpc and returns it. */
  private RpcElement readRpc(String documentation) {
    String name = readName();

    if (readChar() != '(') throw unexpected("expected '('");
    String requestType = readName();
    if (readChar() != ')') throw unexpected("expected ')'");

    if (!readWord().equals("returns")) throw unexpected("expected 'returns'");

    if (readChar() != '(') throw unexpected("expected '('");
    String responseType = readName();
    if (readChar() != ')') throw unexpected("expected ')'");

    List<OptionElement> options = new ArrayList<>();
    if (peekChar() == '{') {
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
    } else if (readChar() != ';') throw unexpected("expected ';'");

    return RpcElement.create(name, documentation, requestType, responseType, options);
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
    return peekChar() == '"' ? readQuotedString() : readWord();
  }

  private String readQuotedString() {
    if (readChar() != '"') throw new AssertionError();
    StringBuilder result = new StringBuilder();
    while (pos < data.length) {
      char c = data[pos++];
      if (c == '"') return result.toString();

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
      if (tag.startsWith("0x")) {
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

  private int column() {
    return pos - lineStart + 1;
  }

  private int line() {
    return line + 1;
  }

  private RuntimeException unexpected(String message) {
    throw new IllegalStateException(
        String.format("Syntax error in %s at %d:%d: %s", filePath, line(), column(), message));
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

    public boolean permitsImport() {
      return this == FILE;
    }

    public boolean permitsField() {
      return this == MESSAGE || this == EXTEND;
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
