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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.squareup.protoparser.OptionElement;
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
  private final ProtoTypeName optionType;
  private final String packageName;
  private final ImmutableList<OptionElement> optionElements;
  private ImmutableMap<WireField, Object> map;

  public Options(ProtoTypeName optionType, String packageName, List<OptionElement> elements) {
    this.optionType = optionType;
    this.packageName = packageName;
    this.optionElements = ImmutableList.copyOf(elements);
  }

  public String packageName() {
    return packageName;
  }

  /**
   * Returns a map with the values for these options. Map values may be either a single entry, like
   * {@code {default: "5"}}, or more sophisticated, with nested maps and lists.
   *
   * <p>The map keys are always {@link WireField} instances, even for nested maps. The values are
   * always either lists, maps, or strings.
   */
  public Map<WireField, Object> map() {
    return map;
  }

  public Object get(String name) {
    checkNotNull(name, "name");

    OptionElement found = null;
    for (OptionElement option : optionElements) {
      if (option.name().equals(name)) {
        if (found != null) {
          throw new IllegalStateException("Multiple options match name: " + name);
        }
        found = option;
      }
    }
    return found != null ? found.value() : null;
  }

  /**
   * Returns true if any of the options in {@code options} matches both of the regular expressions
   * provided: its name matches the option's name and its value matches the option's value.
   */
  public boolean optionMatches(String namePattern, String valuePattern) {
    Matcher nameMatcher = Pattern.compile(namePattern).matcher("");
    Matcher valueMatcher = Pattern.compile(valuePattern).matcher("");
    for (OptionElement option : optionElements) {
      if (nameMatcher.reset(option.name()).matches()
          && valueMatcher.reset(String.valueOf(option.value())).matches()) {
        return true;
      }
    }
    return false;
  }

  void link(Linker linker) {
    Map<WireField, Object> map = new LinkedHashMap<WireField, Object>();
    for (OptionElement option : optionElements) {
      Map<WireField, Object> canonicalOption = canonicalizeOption(linker, optionType, option);
      if (canonicalOption != null) {
        map = union(map, canonicalOption);
      }
    }

    this.map = ImmutableMap.copyOf(map);
  }

  Map<WireField, Object> canonicalizeOption(
      Linker linker, ProtoTypeName extensionType, OptionElement option) {
    Map<String, WireField> extensionsForType = linker.extensions(extensionType);
    if (extensionsForType == null) {
      return null; // No known extensions for the given extension type.
    }

    String[] path = resolveFieldPath(option.name(), extensionsForType.keySet());
    if (path == null && packageName != null) {
      // If the path couldn't be resolved, attempt again by prefixing it with the package name.
      path = resolveFieldPath(packageName + "." + option.name(), extensionsForType.keySet());
    }
    if (path == null) {
      return null; // Unable to find the root of this field path.
    }

    Map<WireField, Object> result = new LinkedHashMap<WireField, Object>();
    Map<WireField, Object> last = result;

    WireField field = extensionsForType.get(path[0]);
    for (int i = 1; i < path.length; i++) {
      Map<WireField, Object> nested = new LinkedHashMap<WireField, Object>();
      last.put(field, nested);
      last = nested;
      field = linker.dereference(packageName, field, path[i]);
    }

    last.put(field, canonicalizeValue(linker, field, option.value()));
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

  private Object canonicalizeValue(Linker linker, WireField context, Object value) {
    if (value instanceof OptionElement) {
      ImmutableMap.Builder<WireField, Object> result = ImmutableMap.builder();
      OptionElement option = (OptionElement) value;
      WireField field = linker.dereference(packageName, context, option.name());
      result.put(field, canonicalizeValue(linker, field, option.value()));
      return coerceValueForField(context, result.build());
    }

    if (value instanceof Map) {
      ImmutableMap.Builder<WireField, Object> result = ImmutableMap.builder();
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
        String name = (String) entry.getKey();
        WireField field = linker.dereference(packageName, context, name);
        result.put(field, canonicalizeValue(linker, field, entry.getValue()));
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

  private Object coerceValueForField(WireField context, Object value) {
    if (context.isRepeated()) {
      return value instanceof List ? value : ImmutableList.of(value);
    } else {
      return value instanceof List ? getOnlyElement((List) value) : value;
    }
  }

  /** Combine values for the same key, resolving conflicts based on their type. */
  @SuppressWarnings("unchecked")
  private Object union(Object a, Object b) {
    if (a instanceof List) {
      return union((List<?>) a, (List<?>) b);
    } else if (a instanceof Map) {
      return union((Map<WireField, Object>) a, (Map<WireField, Object>) b);
    } else {
      throw new IllegalArgumentException("Unable to union values: " + a + ", " + b);
    }
  }

  private Map<WireField, Object> union(Map<WireField, Object> a, Map<WireField, Object> b) {
    Map<WireField, Object> result = new LinkedHashMap<WireField, Object>(a);
    for (Map.Entry<WireField, Object> entry : b.entrySet()) {
      Object aValue = result.get(entry.getKey());
      Object bValue = entry.getValue();
      Object union = aValue != null ? union(aValue, bValue) : bValue;
      result.put(entry.getKey(), union);
    }
    return ImmutableMap.copyOf(result);
  }

  private ImmutableList<Object> union(List<?> a, List<?> b) {
    return ImmutableList.builder().addAll(a).addAll(b).build();
  }

  public ImmutableSet<WireField> fields() {
    ImmutableSet.Builder<WireField> result = ImmutableSet.builder();
    gatherFields(result, map);
    return result.build();
  }

  private void gatherFields(ImmutableSet.Builder<WireField> sink, Object o) {
    if (o instanceof Map) {
      for (Map.Entry<?, ?> entry : ((Map<?, ?>) o).entrySet()) {
        sink.add((WireField) entry.getKey());
        gatherFields(sink, entry.getValue());
      }
    } else if (o instanceof List) {
      for (Object e : (List) o) {
        gatherFields(sink, e);
      }
    }
  }
}
