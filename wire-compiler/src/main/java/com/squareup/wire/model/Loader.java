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
package com.squareup.wire.model;

import com.squareup.protoparser.ProtoFile;
import com.squareup.wire.IO;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Load proto files and their transitive dependencies, parse them, and prepare for linking. The
 * returned values are not linked and should not be used prior to linking.
 */
public final class Loader {
  private final String repoPath;
  private final IO io;
  private final Set<String> protoFileNames = new LinkedHashSet<String>();
  private final List<WireProtoFile> loaded = new ArrayList<WireProtoFile>();

  public Loader(String repoPath, IO io) {
    this.repoPath = repoPath;
    this.io = io;
  }

  /** Recursively add {@code protoFile} and its dependencies. */
  public void add(String protoFileName) throws IOException {
    if (!protoFileNames.add(protoFileName)) {
      return;
    }

    ProtoFile protoFile = io.parse(repoPath + File.separator + protoFileName);
    WireProtoFile wireProtoFile = WireProtoFile.get(protoFile);
    loaded.add(wireProtoFile);

    // Recursively add dependencies.
    for (String dependency : protoFile.dependencies()) {
      add(dependency);
    }
  }

  public List<WireProtoFile> loaded() {
    return Util.immutableList(loaded);
  }
}
