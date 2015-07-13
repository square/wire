package com.squareup.wire;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

public class TagMapTest {

  @Test
  public void testZeroTag() {
    Map<Integer, String> map = new LinkedHashMap<Integer, String>();
    map.put(0, "Zero");

    try {
      TagMap.of(map);
      fail();
    } catch (Exception e) {
      assertThat(e).hasMessage("Input map key is negative or zero");
    }
  }

  @Test
  public void testUnsortedTags() {
    Map<Integer, String> map = new LinkedHashMap<Integer, String>();
    map.put(1, "One");
    map.put(2, "Two");
    map.put(4, "Four");
    map.put(3, "Three");

    TagMap<String> tagMap = TagMap.of(map);
    assertThat(joinValues(tagMap)).isEqualTo("One Two Three Four");
  }

  @Test
  public void testDenseTags() {
    Map<Integer, String> map = new LinkedHashMap<Integer, String>();
    map.put(1, "One");
    map.put(2, "Two");
    map.put(3, "Three");
    map.put(5, "Five");

    TagMap<String> tagMap = TagMap.of(map);
    assertThat(tagMap).isInstanceOf(TagMap.Compact.class);

    assertThat(tagMap.get(0)).isNull();
    assertThat(tagMap.get(1)).isEqualTo("One");
    assertThat(tagMap.get(2)).isEqualTo("Two");
    assertThat(tagMap.get(3)).isEqualTo("Three");
    assertThat(tagMap.get(4)).isNull();
    assertThat(tagMap.get(5)).isEqualTo("Five");
    assertThat(tagMap.get(6)).isNull();

    assertThat(tagMap.containsKey(0)).isFalse();
    assertThat(tagMap.containsKey(1)).isTrue();
    assertThat(tagMap.containsKey(2)).isTrue();
    assertThat(tagMap.containsKey(3)).isTrue();
    assertThat(tagMap.containsKey(4)).isFalse();
    assertThat(tagMap.containsKey(5)).isTrue();
    assertThat(tagMap.containsKey(6)).isFalse();

    assertThat(joinValues(tagMap)).isEqualTo("One Two Three Five");
  }

  @Test
  public void testSparseTags() {
    Map<Integer, String> map = new LinkedHashMap<Integer, String>();
    map.put(1, "One");
    map.put(2, "Two");
    map.put(3, "Three");
    map.put(5, "Five");
    map.put(100, "One Hundred");
    map.put(200, "Two Hundred");

    TagMap<String> tagMap = TagMap.of(map);
    assertThat(tagMap).isInstanceOf(TagMap.Sparse.class);

    assertThat(tagMap.get(0)).isNull();
    assertThat(tagMap.get(1)).isEqualTo("One");
    assertThat(tagMap.get(2)).isEqualTo("Two");
    assertThat(tagMap.get(3)).isEqualTo("Three");
    assertThat(tagMap.get(4)).isNull();
    assertThat(tagMap.get(5)).isEqualTo("Five");
    assertThat(tagMap.get(6)).isNull();
    assertThat(tagMap.get(100)).isEqualTo("One Hundred");
    assertThat(tagMap.get(200)).isEqualTo("Two Hundred");

    assertThat(tagMap.containsKey(0)).isFalse();
    assertThat(tagMap.containsKey(1)).isTrue();
    assertThat(tagMap.containsKey(2)).isTrue();
    assertThat(tagMap.containsKey(3)).isTrue();
    assertThat(tagMap.containsKey(4)).isFalse();
    assertThat(tagMap.containsKey(5)).isTrue();
    assertThat(tagMap.containsKey(100)).isTrue();
    assertThat(tagMap.containsKey(200)).isTrue();

    assertThat(joinValues(tagMap)).isEqualTo("One Two Three Five One Hundred Two Hundred");
  }

  private String joinValues(TagMap<String> tagMap) {
    StringBuilder sb = new StringBuilder();
    for (String s : tagMap.values()) {
      if (sb.length() > 0) {
        sb.append(" ");
      }
      sb.append(s);
    }
    return sb.toString();
  }
}
