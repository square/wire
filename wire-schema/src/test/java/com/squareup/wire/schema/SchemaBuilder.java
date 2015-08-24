/*
 * Copyright (C) 2015 Square, Inc.
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
package com.squareup.wire.schema;

import com.squareup.wire.WireAdapter;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import okio.Buffer;
import okio.Okio;
import okio.Source;

/** Builds schemas for testing. */
class SchemaBuilder {
  final Map<String, String> paths = new LinkedHashMap<>();

  public SchemaBuilder add(String name, String protoFile) {
    paths.put(name, protoFile);
    return this;
  }

  public SchemaBuilder add(String path) throws IOException {
    File file = new File("../wire-runtime/src/test/proto/" + path);
    try (Source source = Okio.source(file)) {
      String protoFile = Okio.buffer(source).readUtf8();
      return add(path, protoFile);
    }
  }

  public Schema build() {
    Loader.IO io = new Loader.IO() {
      @Override public Location locate(String path) throws IOException {
        return Location.get(path);
      }

      @Override public Source open(Location location) throws IOException {
        String protoFile = paths.get(location.path());
        return new Buffer().writeUtf8(protoFile);
      }
    };

    try {
      return new Loader(io).load(paths.keySet());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public WireAdapter<Map<String, Object>> buildWireAdapter(String messageTypeName) {
    Type.Name typeName = Type.Name.get(null, messageTypeName);
    Schema schema = build();
    SchemaWireAdapterFactory factory = new SchemaWireAdapterFactory(schema);
    return factory.get(typeName);
  }
}
