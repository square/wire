package com.squareup.wire.parser;

import com.squareup.protoparser.ProtoFile;
import com.squareup.protoparser.Type;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

import static java.util.Collections.unmodifiableSet;

final class ProtoUtils {
  private static final Set<String> PRIMITIVES = new LinkedHashSet<String>();

  static {
    // TODO promote to protoparser
    PRIMITIVES.add("bool");
    PRIMITIVES.add("bytes");
    PRIMITIVES.add("double");
    PRIMITIVES.add("float");
    PRIMITIVES.add("fixed32");
    PRIMITIVES.add("fixed64");
    PRIMITIVES.add("int32");
    PRIMITIVES.add("int64");
    PRIMITIVES.add("sfixed32");
    PRIMITIVES.add("sfixed64");
    PRIMITIVES.add("sint32");
    PRIMITIVES.add("sint64");
    PRIMITIVES.add("string");
    PRIMITIVES.add("uint32");
    PRIMITIVES.add("uint64");
  }

  static boolean isPrimitiveType(String type) {
    return PRIMITIVES.contains(type);
  }

  /** Aggregate a set of all fully-qualified types contained in the supplied proto files. */
  static Set<String> collectAllTypes(Set<ProtoFile> protoFiles) {
    Set<String> types = new LinkedHashSet<String>();

    // Seed the type resolution queue with all the top-level types from each proto file.
    Deque<Type> typeQueue = new ArrayDeque<Type>();
    for (ProtoFile protoFile : protoFiles) {
      typeQueue.addAll(protoFile.getTypes());
    }

    while (!typeQueue.isEmpty()) {
      Type type = typeQueue.removeFirst();
      String typeFqName = type.getFullyQualifiedName();

      // Check for fully-qualified type name collisions.
      if (types.contains(typeFqName)) {
        StringBuilder fileNames = new StringBuilder();
        boolean first = true;
        for (ProtoFile protoFile : protoFiles) {
          if (!first) {
            fileNames.append(", ");
          }
          first = false;
          fileNames.append(protoFile.getFileName());
        }
        throw new IllegalStateException(
            "Duplicate type " + typeFqName + " defined in " + fileNames);
      }
      types.add(typeFqName);

      typeQueue.addAll(type.getNestedTypes());
    }

    return unmodifiableSet(types);
  }

  private ProtoUtils() {
    throw new AssertionError("No instances.");
  }
}
