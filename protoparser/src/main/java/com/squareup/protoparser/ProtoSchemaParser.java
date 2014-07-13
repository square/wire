// Copyright 2013 Square, Inc.
package com.squareup.protoparser;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Basic parser for {@code .proto} schema declarations.
 *
 * <p>This parser throws away data that it doesn't care about. In particular,
 * unrecognized options, and extensions are discarded. It doesn't retain nesting
 * within types.
 */
public final class ProtoSchemaParser {
  /** Parse a {@code .proto} definition file. */
  public static ProtoFile parse(File file) throws IOException {
    return new ProtoSchemaParser(file.getName(), fileToCharArray(file)).readProtoFile();
  }

  /** Parse a named {@code .proto} schema. The {@code InputStream} is not closed. */
  public static ProtoFile parseUtf8(String name, InputStream is) throws IOException {
    return new ProtoSchemaParser(name, streamToCharArray(is)).readProtoFile();
  }

  /** Parse a named {@code .proto} schema. The {@code Reader} is not closed. */
  public static ProtoFile parse(String name, Reader reader) throws IOException {
    return new ProtoSchemaParser(name, readerToCharArray(reader)).readProtoFile();
  }

  /** Parse a named {@code .proto} schema. */
  public static ProtoFile parse(String name, String data) {
    return new ProtoSchemaParser(name, data.toCharArray()).readProtoFile();
  }

  /** The path to the {@code .proto} file. */
  private final String fileName;

  /** The entire document. */
  private final char[] data;

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

  /** Imported files. */
  private final List<String> dependencies = new ArrayList<String>();

  /** Public imported files. */
  private final List<String> publicDependencies = new ArrayList<String>();

  /** Declared message types and enum types. */
  private final List<Type> types = new ArrayList<Type>();

  /** Declared services. */
  private final List<Service> services = new ArrayList<Service>();

  /** Declared 'extend's. */
  private final List<ExtendDeclaration> extendDeclarations = new ArrayList<ExtendDeclaration>();

  /** Global options. */
  private final List<Option> options = new ArrayList<Option>();

  ProtoSchemaParser(String fileName, char[] data) {
    this.fileName = fileName;
    this.data = data;
  }

  private static char[] fileToCharArray(File file) throws IOException {
    FileInputStream is = new FileInputStream(file);
    try {
      return streamToCharArray(is);
    } finally {
      is.close();
    }
  }

  private static char[] streamToCharArray(InputStream is) throws IOException {
    return readerToCharArray(new InputStreamReader(is, "UTF-8"));
  }

  private static char[] readerToCharArray(Reader reader) throws IOException {
    CharArrayWriter writer = new CharArrayWriter();
    char[] buffer = new char[1024];
    int count;
    while ((count = reader.read(buffer)) != -1) {
      writer.write(buffer, 0, count);
    }
    return writer.toCharArray();
  }

  ProtoFile readProtoFile() {
    while (true) {
      String documentation = readDocumentation();
      if (pos == data.length) {
        return new ProtoFile(fileName, packageName, dependencies, publicDependencies, types,
            services, options, extendDeclarations);
      }
      Object declaration = readDeclaration(documentation, Context.FILE);
      if (declaration instanceof Type) {
        types.add((Type) declaration);
      } else if (declaration instanceof Service) {
        services.add((Service) declaration);
      } else if (declaration instanceof Option) {
        options.add((Option) declaration);
      } else if (declaration instanceof ExtendDeclaration) {
        extendDeclarations.add((ExtendDeclaration) declaration);
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
      prefix = packageName + ".";
      if (readChar() != ';') throw unexpected("expected ';'");
      return null;
    } else if (label.equals("import")) {
      if (!context.permitsImport()) throw unexpected("import in " + context);
      String importString = readString();
      if ("public".equals(importString)) {
        publicDependencies.add(readString());
      } else {
        dependencies.add(importString);
      }
      if (readChar() != ';') throw unexpected("expected ';'");
      return null;
    } else if (label.equals("option")) {
      Option result = readOption('=');
      if (readChar() != ';') throw unexpected("expected ';'");
      return result;
    } else if (label.equals("message")) {
      return readMessage(documentation);
    } else if (label.equals("enum")) {
      return readEnumType(documentation);
    } else if (label.equals("service")) {
      return readService(documentation);
    } else if (label.equals("extend")) {
      return readExtend(documentation);
    } else if (label.equals("rpc")) {
      if (!context.permitsRpc()) throw unexpected("rpc in " + context);
      return readRpc(documentation);
    } else if (label.equals("required") || label.equals("optional") || label.equals("repeated")) {
      if (!context.permitsField()) throw unexpected("fields must be nested");
      return readField(documentation, label);
    } else if (label.equals("extensions")) {
      if (!context.permitsExtensions()) throw unexpected("extensions must be nested");
      return readExtensions(documentation);
    } else if (context == Context.ENUM) {
      List<Option> options = new ArrayList<Option>();

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
      return new EnumType.Value(label, tag, documentation, options);
    } else {
      throw unexpected("unexpected label: " + label);
    }
  }

  /** Reads a message declaration. */
  private MessageType readMessage(String documentation) {
    String previousPrefix = prefix;
    String name = readName();
    prefix = prefix + name + ".";
    List<MessageType.Field> fields = new ArrayList<MessageType.Field>();
    List<Type> nestedTypes = new ArrayList<Type>();
    List<Extensions> extensions = new ArrayList<Extensions>();
    List<Option> options = new ArrayList<Option>();
    if (readChar() != '{') throw unexpected("expected '{'");
    while (true) {
      String nestedDocumentation = readDocumentation();
      if (peekChar() == '}') {
        pos++;
        break;
      }
      Object declared = readDeclaration(nestedDocumentation, Context.MESSAGE);
      if (declared instanceof MessageType.Field) {
        fields.add((MessageType.Field) declared);
      } else if (declared instanceof Type) {
        nestedTypes.add((Type) declared);
      } else if (declared instanceof Extensions) {
        extensions.add((Extensions) declared);
      } else if (declared instanceof Option) {
        options.add((Option) declared);
      } else if (declared instanceof ExtendDeclaration) {
        // Extend declarations always add in a global scope regardless of nesting.
        extendDeclarations.add((ExtendDeclaration) declared);
      }
    }
    prefix = previousPrefix;
    return new MessageType(name, prefix + name, documentation, fields, nestedTypes, extensions,
        options);
  }

  /** Reads an extend declaration. */
  private ExtendDeclaration readExtend(String documentation) {
    String name = readName();
    List<MessageType.Field> fields = new ArrayList<MessageType.Field>();
    if (readChar() != '{') throw unexpected("expected '{'");
    while (true) {
      String nestedDocumentation = readDocumentation();
      if (peekChar() == '}') {
        pos++;
        break;
      }
      Object declared = readDeclaration(nestedDocumentation, Context.EXTEND);
      if (declared instanceof MessageType.Field) {
        fields.add((MessageType.Field) declared);
      }
    }
    String fqname = name;
    if (!name.contains(".") && packageName != null) {
      fqname = packageName + "." + name;
    }
    return new ExtendDeclaration(name, fqname, documentation, fields);
  }

  /** Reads a service declaration and returns it. */
  private Service readService(String documentation) {
    String name = readName();
    List<Option> options = new ArrayList<Option>();
    List<Service.Method> methods = new ArrayList<Service.Method>();
    if (readChar() != '{') throw unexpected("expected '{'");
    while (true) {
      String methodDocumentation = readDocumentation();
      if (peekChar() == '}') {
        pos++;
        break;
      }
      Object declared = readDeclaration(methodDocumentation, Context.SERVICE);
      if (declared instanceof Service.Method) {
        methods.add((Service.Method) declared);
      } else if (declared instanceof Option) {
        options.add((Option) declared);
      }
    }
    return new Service(name, prefix + name, documentation, options, methods);
  }

  /** Reads an enumerated type declaration and returns it. */
  private EnumType readEnumType(String documentation) {
    String name = readName();
    List<Option> options = new ArrayList<Option>();
    List<EnumType.Value> values = new ArrayList<EnumType.Value>();
    if (readChar() != '{') throw unexpected("expected '{'");
    while (true) {
      String valueDocumentation = readDocumentation();
      if (peekChar() == '}') {
        pos++;
        break;
      }
      Object declared = readDeclaration(valueDocumentation, Context.ENUM);
      if (declared instanceof EnumType.Value) {
        values.add((EnumType.Value) declared);
      } else if (declared instanceof Option) {
        options.add((Option) declared);
      }
    }
    return new EnumType(name, prefix + name, documentation, options, values);
  }

  /** Reads an field declaration and returns it. */
  private MessageType.Field readField(String documentation, String label) {
    MessageType.Label labelEnum = MessageType.Label.valueOf(label.toUpperCase(Locale.US));
    String type = readName();
    String name = readName();
    if (readChar() != '=') throw unexpected("expected '='");
    int tag = readInt();
    List<Option> options = new ArrayList<Option>();

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
      return new MessageType.Field(labelEnum, type, name, tag, documentation, options);
    }
    throw unexpected("expected ';'");
  }

  /** Reads extensions like "extensions 101;" or "extensions 101 to max;". */
  private Extensions readExtensions(String documentation) {
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
    return new Extensions(documentation, start, end);
  }

  /** Reads a option containing a name, an '=' or ':', and a value. */
  private Option readOption(char keyValueSeparator) {
    boolean isExtension = (peekChar() == '[');
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
    return new Option(name, subName != null ? new Option(subName, value) : value);
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
        if (word.equals("true")) {
          return true;
        } else if (word.equals("false")) {
          return false;
        } else {
          return EnumType.Value.anonymous(word);
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
    Map<String, Object> result = new LinkedHashMap<String, Object>();
    while (true) {
      if (peekChar() == closeBrace) {
        // If we see the close brace, finish immediately. This handles {}/[] and ,}/,] cases.
        pos++;
        return result;
      }

      Option option = readOption(keyValueSeparator);
      String name = option.getName();
      Object value = option.getValue();
      if (value instanceof Option) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) result.get(name);
        if (nested == null) {
          nested = new LinkedHashMap<String, Object>();
          result.put(name, nested);
        }
        Option valueOption = (Option) value;
        nested.put(valueOption.getName(), valueOption.getValue());
      } else {
        // Add the value(s) to any previous values with the same key
        Object previous = result.get(name);
        if (previous == null) {
          result.put(name, value);
        } else if (previous instanceof List) {
          // Add to previous List
          addToList((List<Object>) previous, value);
        } else {
          List<Object> newList = new ArrayList<Object>();
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
    List<Object> result = new ArrayList<Object>();
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

  /** Reads an rpc method and returns it. */
  private Service.Method readRpc(String documentation) {
    String name = readName();

    if (readChar() != '(') throw unexpected("expected '('");
    String requestType = readName();
    if (readChar() != ')') throw unexpected("expected ')'");

    if (!readWord().equals("returns")) throw unexpected("expected 'returns'");

    if (readChar() != '(') throw unexpected("expected '('");
    String responseType = readName();
    if (readChar() != ')') throw unexpected("expected ')'");

    List<Option> options = new ArrayList<Option>();
    if (peekChar() == '{') {
      pos++;
      while (true) {
        String methodDocumentation = readDocumentation();
        if (peekChar() == '}') {
          pos++;
          break;
        }
        Object declared = readDeclaration(methodDocumentation, Context.RPC);
        if (declared instanceof Option) {
          options.add((Option) declared);
        }
      }
    } else if (readChar() != ';') throw unexpected("expected ';'");

    return new Service.Method(name, documentation, requestType, responseType, options);
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
        String.format("Syntax error in %s at %d:%d: %s", fileName, line(), column(), message));
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
  }
}
