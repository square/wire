// Copyright 2013 Square, Inc.
package com.squareup.proto;

import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
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
    return new ProtoSchemaParser(file).readProtoFile();
  }

  /** Parse a named {@code .proto} schema. */
  public static ProtoFile parse(String name, String data) {
    return new ProtoSchemaParser(name, data).readProtoFile();
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

  /** Imported files. */
  private List<String> dependencies = new ArrayList<String>();

  /** Declared message types and enum types. */
  private List<Type> types = new ArrayList<Type>();

  /** Global options. */
  private Map<String, Object> options = new LinkedHashMap<String, Object>();

  ProtoSchemaParser(String fileName, String data) {
    this.fileName = fileName;
    this.data = data.toCharArray();
  }

  ProtoSchemaParser(File file) throws IOException {
    this.fileName = file.getPath();
    this.data = fileToCharArray(file);
  }

  private char[] fileToCharArray(File file) throws IOException {
    Reader reader = new InputStreamReader(new FileInputStream(file), "UTF-8");
    CharArrayWriter writer = new CharArrayWriter();
    char[] buffer = new char[1024];
    int count;
    while ((count = reader.read(buffer)) != -1) {
      writer.write(buffer, 0, count);
    }
    return writer.toCharArray();
  }

  public ProtoFile readProtoFile() {
    while (true) {
      String documentation = readDocumentation();
      if (pos == data.length) {
        return new ProtoFile(fileName, packageName, dependencies, types, options);
      }
      Object declaration = readDeclaration(documentation, Context.FILE);
      if (declaration instanceof Type) {
        types.add((Type) declaration);
      } else if (declaration instanceof Option) {
        Option option = (Option) declaration;
        options.put(option.getName(), option.getValue());
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
      if (readChar() != ';') throw unexpected("expected ';'");
      return null;
    } else if (label.equals("import")) {
      if (!context.permitsImport()) throw unexpected("import in " + context);
      dependencies.add(readString());
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
      readService();
      return null;
    } else if (label.equals("extend")) {
      readExtend();
      return null;
    } else if (label.equals("rpc")) {
      if (!context.permitsRpc()) throw unexpected("rpc in " + context);
      readRpc();
      return null;
    } else if (label.equals("required") || label.equals("optional") || label.equals("repeated")) {
      if (!context.permitsField()) throw unexpected("fields must be nested");
      return readField(documentation, label);
    } else if (label.equals("extensions")) {
      if (!context.permitsExtensions()) throw unexpected("extensions must be nested");
      readExtensions();
      return null;
    } else if (context == Context.ENUM) {
      if (readChar() != '=') throw unexpected("expected '='");
      int tag = readInt();
      if (readChar() != ';') throw unexpected("expected ';'");
      return new EnumType.Value(label, tag, documentation);
    } else {
      throw unexpected("unexpected label: " + label);
    }
  }

  /** Reads a message declaration. */
  private MessageType readMessage(String documentation) {
    String name = readName();
    List<MessageType.Field> fields = new ArrayList<MessageType.Field>();
    List<Type> nestedTypes = new ArrayList<Type>();
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
      }
    }
    return new MessageType(name, documentation, fields, nestedTypes);
  }

  /** Reads an extend declaration (just ignores the content). */
  private void readExtend() {
    readName(); // Ignore name.
    if (readChar() != '{') throw unexpected("expected '{'");
    while (true) {
      String nestedDocumentation = readDocumentation();
      if (peekChar() == '}') {
        pos++;
        break;
      }
      readDeclaration(nestedDocumentation, Context.EXTEND);
    }
  }

  /** Reads a service declaration (just ignores the content). */
  private void readService() {
    readName(); // Ignore name.
    if (readChar() != '{') throw unexpected("expected '{'");
    while (true) {
      String nestedDocumentation = readDocumentation();
      if (peekChar() == '}') {
        pos++;
        break;
      }
      readDeclaration(nestedDocumentation, Context.SERVICE);
    }
  }

  /** Reads an enumerated type declaration and returns it. */
  private EnumType readEnumType(String documentation) {
    String name = readName();
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
      }
    }
    return new EnumType(name, documentation, values);
  }

  /** Reads an field declaration and returns it. */
  private MessageType.Field readField(String documentation, String label) {
    MessageType.Label labelEnum = MessageType.Label.valueOf(label.toUpperCase(Locale.US));
    String type = readName();
    String name = readName();
    if (readChar() != '=') throw unexpected("expected '='");
    int tag = readInt();
    char c = peekChar();
    Map<String, Object> options;
    if (c == '[') {
      options = readMap('[', ']', '=');
      c = peekChar();
    } else {
      options = new LinkedHashMap<String, Object>();
    }
    if (c == ';') {
      pos++;
      return new MessageType.Field(labelEnum, type, name, tag, documentation, options);
    }
    throw unexpected("expected ';'");
  }

  /** Reads extensions like "extensions 101;" or "extensions 101 to max;". */
  private void readExtensions() {
    readWord(); // Range start.
    if (peekChar() != ';') {
      readWord(); // Literal 'to'
      readWord(); // Range end.
    }
    if (readChar() != ';') throw unexpected("expected ';'");
  }

  /** Reads a option containing a name, an '=' or ':', and a value. */
  private Option readOption(char keyValueSeparator) {
    String name = readName(); // Option name.
    if (readChar() != keyValueSeparator) {
      throw unexpected("expected '" + keyValueSeparator + "' in option");
    }
    Object value = peekChar() == '{' ? readMap('{', '}', ':') : readString();
    return new Option(name, value);
  }

  /**
   * Returns a map of string keys and values. This is similar to a JSON object,
   * with '{' and '}' surrounding the map, ':' separating keys from values, and
   * ',' separating entries.
   */
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
      result.put(option.getName(), option.getValue());

      char c = peekChar();
      if (c == ',') {
        pos++;
      } else if (c != closeBrace) {
        throw unexpected("expected ',' or '" + closeBrace + "'");
      }
    }
  }

  /** Reads an rpc method and ignores it. */
  private void readRpc() {
    readName(); // Read method name, ignore.
    readName(); // Read request type, ignore.
    readWord(); // Read returns keyword
    readName(); // Read response type, ignore.

    char c = readChar();
    if (c == '{') {
      while (true) {
        String nestedDocumentation = readDocumentation();
        if (peekChar() == '}') {
          pos++;
          break;
        }
        readDeclaration(nestedDocumentation, Context.RPC); // Read and ignore.
      }
    } else if (c != ';') {
      throw unexpected("expected '{' or ';'");
    }
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
      }

      result.append(c);
      if (c == '\n') newline();
    }
    throw unexpected("unterminated string");
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
        return result != null ? cleanUpDocumentation(result) : "";
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
      int start = pos;
      while (pos + 1 < data.length) {
        if (data[pos] == '*' && data[pos + 1] == '/') {
          pos += 2;
          return new String(data, start, pos - 2 - start);
        } else {
          char c = data[pos++];
          if (c == '\n') newline();
        }
      }
      throw unexpected("unterminated comment");
    } else if (commentType == '/') {
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
   * Returns a string like {@code comment}, but without leading whitespace or
   * asterisks.
   */
  private String cleanUpDocumentation(String comment) {
    StringBuilder result = new StringBuilder();
    boolean beginningOfLine = true;
    for (int i = 0; i < comment.length(); i++) {
      char c = comment.charAt(i);
      if (!beginningOfLine || (c != ' ' && c != '\t' && c != '*')) {
        result.append(c);
        beginningOfLine = false;
      }
      if (c == '\n') {
        beginningOfLine = true;
      }
    }
    return result.toString().trim();
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

  /** Call this everytime a '\n' is encountered. */
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
