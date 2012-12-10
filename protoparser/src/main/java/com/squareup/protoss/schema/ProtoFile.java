/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.protoss.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single {@code .proto} file.
 */
final class ProtoFile {
  final String fileName;
  final String packageName;
  final List<String> dependencies;
  final List<MessageType> messageTypes;
  final List<EnumType> enumTypes;

  ProtoFile(String fileName, String packageName, List<String> dependencies,
      List<MessageType> messageTypes, List<EnumType> enumTypes) {
    if (fileName == null) throw new NullPointerException("fileName");
    if (dependencies == null) throw new NullPointerException("dependencies");
    if (messageTypes == null) throw new NullPointerException("messageTypes");
    if (enumTypes == null) throw new NullPointerException("enumTypes");

    this.fileName = fileName;
    this.packageName = packageName;
    this.dependencies = Collections.unmodifiableList(new ArrayList<String>(dependencies));
    this.messageTypes = Collections.unmodifiableList(new ArrayList<MessageType>(messageTypes));
    this.enumTypes = Collections.unmodifiableList(new ArrayList<EnumType>(enumTypes));
  }

  @Override public boolean equals(Object other) {
    if (other instanceof ProtoFile) {
      ProtoFile that = (ProtoFile) other;
      return eq(fileName, that.fileName)
          && eq(packageName, that.packageName)
          && eq(dependencies, that.dependencies)
          && eq(messageTypes, that.messageTypes)
          && eq(enumTypes, that.enumTypes);
    }
    return false;
  }

  private static boolean eq(Object a, Object b) {
    return a == b || a != null && a.equals(b);
  }

  @Override public int hashCode() {
    return fileName.hashCode();
  }
}
