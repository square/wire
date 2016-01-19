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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.squareup.wire.schema.internal.parser.OptionElement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;

/**
 * A set of options declared on a message declaration, field declaration, enum declaration, enum
 * constant declaration, service declaration, RPC method declaration, or proto file declaration.
 * Options values may be arbitrary protocol buffer messages, but must be valid protocol buffer
 * messages.
 */
public final class Options {
  public static final ProtoType FILE_OPTIONS = ProtoType.get("google.protobuf.FileOptions");
  public static final ProtoType MESSAGE_OPTIONS = ProtoType.get("google.protobuf.MessageOptions");
  public static final ProtoType FIELD_OPTIONS = ProtoType.get("google.protobuf.FieldOptions");
  public static final ProtoType ENUM_OPTIONS = ProtoType.get("google.protobuf.EnumOptions");
  public static final ProtoType ENUM_VALUE_OPTIONS
      = ProtoType.get("google.protobuf.EnumValueOptions");
  public static final ProtoType SERVICE_OPTIONS = ProtoType.get("google.protobuf.ServiceOptions");
  public static final ProtoType METHOD_OPTIONS = ProtoType.get("google.protobuf.MethodOptions");

  private final ProtoType optionType;
  private final ImmutableList<OptionElement> optionElements;
  private ImmutableMap<ProtoMember, Object> map;

  public Options(ProtoType optionType, List<OptionElement> elements) {
    this.optionType = optionType;
    this.optionElements = ImmutableList.copyOf(elements);
  }

  /**
   * Returns a map with the values for these options. Map values may be either a single entry, like
   * {@code {deprecated: "true"}}, or more sophisticated, with nested maps and lists.
   *
   * <p>The map keys are always {@link ProtoMember} instances, even for nested maps. The values are
   * always either lists, maps, or strings.
   */
  public Map<ProtoMember, Object> map() {
    return map;
  }

  public Object get(ProtoMember protoMember) {
    checkNotNull(protoMember, "protoMember");
    return map.get(protoMember);
  }

  /**
   * Returns true if any of the options in {@code options} matches both of the regular expressions
   * provided: its name matches the option's name and its value matches the option's value.
   */
  public boolean optionMatches(String namePattern, String valuePattern) {
    Matcher nameMatcher = Pattern.compile(namePattern).matcher("");
    Matcher valueMatcher = Pattern.compile(valuePattern).matcher("");
    for (Map.Entry<ProtoMember, Object> entry : map.entrySet()) {
      if (nameMatcher.reset(entry.getKey().member()).matches()
          && valueMatcher.reset(String.valueOf(entry.getValue())).matches()) {
        return true;
      }
    }
    return false;
  }

  ImmutableList<OptionElement> toElements() {
    return optionElements;
  }

  void link(Linker linker) {
    ImmutableMap<ProtoMember, Object> map = ImmutableMap.of();
    for (OptionElement option : optionElements) {
      Map<ProtoMember, Object> canonicalOption = canonicalizeOption(linker, optionType, option);
      if (canonicalOption != null) {
        map = union(linker, map, canonicalOption);
      }
    }

    this.map = map;
  }

  Map<ProtoMember, Object> canonicalizeOption(
      Linker linker, ProtoType extensionType, OptionElement option) {
    Type type = linker.get(extensionType);
    if (!(type instanceof MessageType)) {
      return null; // No known extensions for the given extension type.
    }
    MessageType messageType = (MessageType) type;

    String[] path;
    Field field = messageType.field(option.name());
    if (field != null) {
      // This is an option declared by descriptor.proto.
      path = new String[] {option.name()};
    } else {
      // This is an option declared by an extension.
      Map<String, Field> extensionsForType = messageType.extensionFieldsMap();

      path = resolveFieldPath(option.name(), extensionsForType.keySet());
      String packageName = linker.packageName();
      if (path == null && packageName != null) {
        // If the path couldn't be resolved, attempt again by prefixing it with the package name.
        path = resolveFieldPath(packageName + "." + option.name(), extensionsForType.keySet());
      }
      if (path == null) {
        return null; // Unable to find the root of this field path.
      }

      field = extensionsForType.get(path[0]);
    }

    Map<ProtoMember, Object> result = new LinkedHashMap<>();
    Map<ProtoMember, Object> last = result;
    ProtoType lastProtoType = messageType.type();
    for (int i = 1; i < path.length; i++) {
      Map<ProtoMember, Object> nested = new LinkedHashMap<>();
      last.put(ProtoMember.get(lastProtoType, field), nested);
      lastProtoType = field.type();
      last = nested;
      field = linker.dereference(field, path[i]);
      if (field == null) {
        return null; // Unable to dereference this path segment.
      }
    }

    last.put(ProtoMember.get(lastProtoType, field),
        canonicalizeValue(linker, field, option.value()));
    return result;
  }

  /**
   * Given a path like {@code a.b.c.d} and a set of paths like {@code {a.b.c, a.f.g, h.j}}, this
   * returns the original path split on dots such that the first element is in the set. For the
   * above example it would return the array {@code [a.b.c, d]}.
   *
   * <p>Typically the input path is a package name like {@code a.b}, followed by a dot and a
   * sequence of field names. The first field name is an extension field; subsequent field names
   * make a path within that extension.
   *
   * <p>Note that a single input may yield multiple possible answers, such as when package names
   * and field names collide. This method prefers shorter package names though that is an
   * implementation detail.
   */
  static String[] resolveFieldPath(String name, Set<String> fullyQualifiedNames) {
    // Try to resolve a local name.
    for (int i = 0; i < name.length(); i++) {
      i = name.indexOf('.', i);
      if (i == -1) i = name.length();

      String candidate = name.substring(0, i);
      if (fullyQualifiedNames.contains(candidate)) {
        String[] path = name.substring(i).split("\\.", -1);
        path[0] = name.substring(0, i);
        return path;
      }
    }

    return null;
  }

  private Object canonicalizeValue(Linker linker, Field context, Object value) {
    if (value instanceof OptionElement) {
      ImmutableMap.Builder<ProtoMember, Object> result = ImmutableMap.builder();
      OptionElement option = (OptionElement) value;
      Field field = linker.dereference(context, option.name());
      if (field == null) {
        linker.addError("unable to resolve option %s on %s", option.name(), context.type());
      } else {
        ProtoMember protoMember = ProtoMember.get(context.type(), field);
        result.put(protoMember, canonicalizeValue(linker, field, option.value()));
      }
      return coerceValueForField(context, result.build());
    }

    if (value instanceof Map) {
      ImmutableMap.Builder<ProtoMember, Object> result = ImmutableMap.builder();
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
        String name = (String) entry.getKey();
        Field field = linker.dereference(context, name);
        if (field == null) {
          linker.addError("unable to resolve option %s on %s", name, context.type());
        } else {
          ProtoMember protoMember = ProtoMember.get(context.type(), field);
          result.put(protoMember, canonicalizeValue(linker, field, entry.getValue()));
        }
      }
      return coerceValueForField(context, result.build());
    }

    if (value instanceof List) {
      ImmutableList.Builder<Object> result = ImmutableList.builder();
      for (Object element : (List<?>) value) {
        result.addAll((List) canonicalizeValue(linker, context, element));
      }
      return coerceValueForField(context, result.build());
    }

    if (value instanceof String) {
      return coerceValueForField(context, value);
    }

    throw new IllegalArgumentException("Unexpected option value: " + value);
  }

  private Object coerceValueForField(Field context, Object value) {
    if (context.isRepeated()) {
      return value instanceof List ? value : ImmutableList.of(value);
    } else {
      return value instanceof List ? getOnlyElement((List) value) : value;
    }
  }

  /** Combine values for the same key, resolving conflicts based on their type. */
  @SuppressWarnings("unchecked")
  private Object union(Linker linker, Object a, Object b) {
    if (a instanceof List) {
      return union((List<?>) a, (List<?>) b);
    } else if (a instanceof Map) {
      return union(linker, (Map<ProtoMember, Object>) a, (Map<ProtoMember, Object>) b);
    } else {
      linker.addError("conflicting options: %s, %s", a, b);
      return a; // Just return any placeholder.
    }
  }

  private ImmutableMap<ProtoMember, Object> union(
      Linker linker, Map<ProtoMember, Object> a, Map<ProtoMember, Object> b) {
    Map<ProtoMember, Object> result = new LinkedHashMap<>(a);
    for (Map.Entry<ProtoMember, Object> entry : b.entrySet()) {
      Object aValue = result.get(entry.getKey());
      Object bValue = entry.getValue();
      Object union = aValue != null ? union(linker, aValue, bValue) : bValue;
      result.put(entry.getKey(), union);
    }
    return ImmutableMap.copyOf(result);
  }

  private ImmutableList<Object> union(List<?> a, List<?> b) {
    return ImmutableList.builder().addAll(a).addAll(b).build();
  }

  Multimap<ProtoType, ProtoMember> fields() {
    Multimap<ProtoType, ProtoMember> result = LinkedHashMultimap.create();
    gatherFields(result, optionType, map);
    return result;
  }

  private void gatherFields(Multimap<ProtoType, ProtoMember> sink, ProtoType type, Object o) {
    if (o instanceof Map) {
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) o).entrySet()) {
        ProtoMember protoMember = (ProtoMember) entry.getKey();
        sink.put(type, protoMember);
        gatherFields(sink, protoMember.type(), entry.getValue());
      }
    } else if (o instanceof List) {
      for (Object e : (List) o) {
        gatherFields(sink, type, e);
      }
    }
  }

  Options retainAll(Schema schema, MarkSet markSet) {
    if (map.isEmpty()) return this; // Nothing to prune.
    Options result = new Options(optionType, optionElements);
    Object mapOrNull = retainAll(schema, markSet, optionType, map);
    result.map = mapOrNull != null
        ? (ImmutableMap<ProtoMember, Object>) mapOrNull
        : ImmutableMap.<ProtoMember, Object>of();
    return result;
  }

  /** Returns an object of the same type as {@code o}, or null if it is not retained. */
  private Object retainAll(Schema schema, MarkSet markSet, ProtoType type, Object o) {
    if (!markSet.contains(type)) {
      return null; // Prune this type.

    } else if (o instanceof Map) {
      ImmutableMap.Builder<ProtoMember, Object> builder = ImmutableMap.builder();
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) o).entrySet()) {
        ProtoMember protoMember = (ProtoMember) entry.getKey();
        if (!markSet.contains(protoMember)) continue; // Prune this field.
        Field field = schema.getField(protoMember);
        Object retainedValue = retainAll(schema, markSet, field.type(), entry.getValue());
        if (retainedValue != null) {
          builder.put(protoMember, retainedValue); // This retained field is non-empty.
        }
      }
      ImmutableMap<ProtoMember, Object> map = builder.build();
      return !map.isEmpty() ? map : null;

    } else if (o instanceof List) {
      ImmutableList.Builder<Object> builder = ImmutableList.builder();
      for (Object value : ((List) o)) {
        Object retainedValue = retainAll(schema, markSet, type, value);
        if (retainedValue != null) {
          builder.add(retainedValue); // This retained value is non-empty.
        }
      }
      ImmutableList<Object> list = builder.build();
      return !list.isEmpty() ? list : null;

    } else {
      return o;
    }
  }
}
