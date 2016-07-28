/*
 * Copyright (C) 2016 Square, Inc.
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
package com.squareup.wire.java.internal;

import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.SyntaxReader;

/** Parses {@code .wire} files. */
public final class ProfileParser {
  private final SyntaxReader reader;

  private final ProfileFileElement.Builder fileBuilder;
  private final ImmutableList.Builder<String> imports = ImmutableList.builder();
  private final ImmutableList.Builder<TypeConfigElement> typeConfigs = ImmutableList.builder();

  /** Output package name, or null if none yet encountered. */
  private String packageName;

  public ProfileParser(Location location, String data) {
    this.reader = new SyntaxReader(data.toCharArray(), location);
    this.fileBuilder = ProfileFileElement.builder(location);
  }

  public ProfileFileElement read() {
    String label = reader.readWord();
    if (!label.equals("syntax")) throw reader.unexpected("expected 'syntax'");
    reader.require('=');
    String syntaxString = reader.readQuotedString();
    if (!syntaxString.equals("wire2")) throw reader.unexpected("expected 'wire2'");
    reader.require(';');

    while (true) {
      String documentation = reader.readDocumentation();
      if (reader.exhausted()) {
        return fileBuilder.packageName(packageName)
            .imports(imports.build())
            .typeConfigs(typeConfigs.build())
            .build();
      }

      readDeclaration(documentation);
    }
  }

  private void readDeclaration(String documentation) {
    Location location = reader.location();
    String label = reader.readWord();

    if (label.equals("package")) {
      if (packageName != null) throw reader.unexpected(location, "too many package names");
      packageName = reader.readName();
      reader.require(';');
    } else if (label.equals("import")) {
      String importString = reader.readString();
      imports.add(importString);
      reader.require(';');
    } else if (label.equals("type")) {
      typeConfigs.add(readTypeConfig(location, documentation));
    } else {
      throw reader.unexpected(location, "unexpected label: " + label);
    }
  }

  /** Reads a type config and returns it. */
  private TypeConfigElement readTypeConfig(Location location, String documentation) {
    String name = reader.readDataType();
    String target = null;
    String adapter = null;

    reader.require('{');
    while (!reader.peekChar('}')) {
      Location wordLocation = reader.location();
      String word = reader.readWord();
      switch (word) {
        case "target":
          if (target != null) throw reader.unexpected(wordLocation, "too many targets");
          target = reader.readWord();
          if (!reader.readWord().equals("using")) throw reader.unexpected("expected 'using'");
          String adapterType = reader.readWord();
          reader.require('#');
          String adapterConstant = reader.readWord();
          reader.require(';');
          adapter = adapterType + '#' + adapterConstant;
          break;

        default:
          throw reader.unexpected(wordLocation, "unexpected label: " + word);
      }
    }

    return TypeConfigElement.builder(location)
        .type(name)
        .documentation(documentation)
        .target(target)
        .adapter(adapter)
        .build();
  }
}
