package com.squareup.wire;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * A read-only subset of Map&lt;Integer, T&gt; optimized for the case where the set of tags is
 * reasonably small or dense (defined in terms of the ratio of the number of active entries to the
 * max tag value).
 *
 * @param <T> the output type of the map.
 */
abstract class TagMap<T> {
  // Allow direct lookup if the resulting array will 64 or fewer entries.
  private static final int SIZE_THRESHOLD = 64;
  // Allow direct lookup if the resulting array will be at least 3/4 full.
  private static final float RATIO_THRESHOLD = 3.0f / 4.0f;

  // Sort Map.Entry<Integer, ?> instances by their key.
  private static final Comparator<? super Map.Entry<Integer, ?>> COMPARATOR =
      new Comparator<Map.Entry<Integer, ?>>() {
        @Override public int compare(Map.Entry<Integer, ?> o1, Map.Entry<Integer, ?> o2) {
          return o1.getKey().compareTo(o2.getKey());
        }
      };

  List<T> values;

  /**
   * Creates a TagMap based on the entries of an incoming map.
   */
  public static <T> TagMap<T> of(Map<Integer, T> map) {
    int maxTag = maxTag(map);
    if (isCompact(map.size(), maxTag)) {
      return TagMap.Compact.compactTagMapOf(map, maxTag);
    } else {
      return TagMap.Sparse.sparseTagMapOf(map);
    }
  }

  private static boolean isCompact(int size, int maxTag) {
    return maxTag <= SIZE_THRESHOLD || ((float) size / maxTag) > RATIO_THRESHOLD;
  }

  private static <T> int maxTag(Map<Integer, T> map) {
    int maxTag = -1;
    for (int tag : map.keySet()) {
      if (tag > maxTag) {
        maxTag = tag;
      }
    }
    return maxTag;
  }

  private static <T> List<T> sortedValues(Map<Integer, T> map) {
    TreeSet<Map.Entry<Integer, T>> entries = new TreeSet<Map.Entry<Integer, T>>(COMPARATOR);
    entries.addAll(map.entrySet());

    List<T> sortedValues = new ArrayList<T>();
    for (Map.Entry<Integer, T> entry : entries) {
      sortedValues.add(entry.getValue());
    }
    return sortedValues;
  }

  protected TagMap(Map<Integer, T> map) {
    this.values = sortedValues(map);
  }

  public Collection<T> values() {
    return values;
  }

  public abstract T get(int tag);

  public abstract boolean containsKey(int tag);

  /** Compact implementation class. */
  static final class Compact<T> extends TagMap<T> {
    Object[] elementsByTag;
    int maxTag = -1;

    public static <T> Compact<T> compactTagMapOf(Map<Integer, T> map, int maxTag) {
      return new Compact<T>(map, maxTag);
    }

    private Compact(Map<Integer, T> map, int maxTag) {
      super(map);
      this.maxTag = maxTag;

      elementsByTag = new Object[maxTag + 1];
      for (Map.Entry<Integer, T> entry : map.entrySet()) {
        Integer key = entry.getKey();
        if (key <= 0) {
          throw new IllegalArgumentException("Input map key is negative or zero");
        }

        elementsByTag[key] = entry.getValue();
      }
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(int tag) {
      if (tag > maxTag) return null;
      return (T) elementsByTag[tag];
    }

    @Override
    public boolean containsKey(int tag) {
      if (tag > maxTag) return false;
      return elementsByTag[tag] != null;
    }
  }

  /** Sparse implementation class. */
  static final class Sparse<T> extends TagMap<T> {
    Map<Integer, T> map;

    public static <T> Sparse<T> sparseTagMapOf(Map<Integer, T> map) {
      return new Sparse<T>(map);
    }

    private Sparse(Map<Integer, T> map) {
      super(map);
      this.map = map;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T get(int tag) {
      return map.get(tag);
    }

    @Override
    public boolean containsKey(int tag) {
      return map.containsKey(tag);
    }
  }
}
