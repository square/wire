package com.squareup.wire;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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
      assertEquals("Input map key is negative or zero", e.getMessage());
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
    assertEquals("One Two Three Four", joinValues(tagMap));
  }

  @Test
  public void testDenseTags() {
    Map<Integer, String> map = new LinkedHashMap<Integer, String>();
    map.put(1, "One");
    map.put(2, "Two");
    map.put(3, "Three");
    map.put(5, "Five");

    TagMap<String> tagMap = TagMap.of(map);
    assertTrue(tagMap instanceof TagMap.Compact);

    assertEquals(null, tagMap.get(0));
    assertEquals("One", tagMap.get(1));
    assertEquals("Two", tagMap.get(2));
    assertEquals("Three", tagMap.get(3));
    assertEquals(null, tagMap.get(4));
    assertEquals("Five", tagMap.get(5));
    assertEquals(null, tagMap.get(6));

    assertFalse(tagMap.containsKey(0));
    assertTrue(tagMap.containsKey(1));
    assertTrue(tagMap.containsKey(2));
    assertTrue(tagMap.containsKey(3));
    assertFalse(tagMap.containsKey(4));
    assertTrue(tagMap.containsKey(5));
    assertFalse(tagMap.containsKey(6));

    assertEquals("One Two Three Five", joinValues(tagMap));
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
    assertTrue(tagMap instanceof TagMap.Sparse);

    assertEquals(null, tagMap.get(0));
    assertEquals("One", tagMap.get(1));
    assertEquals("Two", tagMap.get(2));
    assertEquals("Three", tagMap.get(3));
    assertEquals(null, tagMap.get(4));
    assertEquals("Five", tagMap.get(5));
    assertEquals(null, tagMap.get(6));
    assertEquals("One Hundred", tagMap.get(100));
    assertEquals("Two Hundred", tagMap.get(200));

    assertFalse(tagMap.containsKey(0));
    assertTrue(tagMap.containsKey(1));
    assertTrue(tagMap.containsKey(2));
    assertTrue(tagMap.containsKey(3));
    assertFalse(tagMap.containsKey(4));
    assertTrue(tagMap.containsKey(5));
    assertTrue(tagMap.containsKey(100));
    assertTrue(tagMap.containsKey(200));

    assertEquals("One Two Three Five One Hundred Two Hundred", joinValues(tagMap));
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
