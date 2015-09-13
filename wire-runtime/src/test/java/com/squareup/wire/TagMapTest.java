/*
 * Copyright 2015 Square Inc.
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
package com.squareup.wire;

import com.google.protobuf.FieldDescriptorProto.Type;
import com.google.protobuf.FileOptions;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import okio.Buffer;
import okio.ByteString;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class TagMapTest {
  final Extension<FileOptions, Object> unknownA = Extension.unknown(
      FileOptions.class, 1, FieldEncoding.FIXED64);
  final Extension<FileOptions, Double> extensionA = Extension.get(FileOptions.class,
      WireField.Label.OPTIONAL, "a", 1,
      "com.squareup.wire.ProtoAdapter#DOUBLE");
  final Extension<FileOptions, Object> unknownB = Extension.unknown(
      FileOptions.class, 2, FieldEncoding.LENGTH_DELIMITED);
  final Extension<FileOptions, String> extensionB = Extension.get(FileOptions.class,
      WireField.Label.OPTIONAL, "b", 2,
      "com.squareup.wire.ProtoAdapter#STRING");
  final Extension<FileOptions, Object> unknownC = Extension.unknown(
      FileOptions.class, 3, FieldEncoding.VARINT);
  final Extension<FileOptions, Type> extensionC = Extension.get(FileOptions.class,
      WireField.Label.OPTIONAL, "c", 3,
      "com.google.protobuf.FieldDescriptorProto$Type#ADAPTER");
  final Extension<FileOptions, List<Double>> extensionD = Extension.get(FileOptions.class,
      WireField.Label.PACKED, "d", 4,
      "com.squareup.wire.ProtoAdapter#DOUBLE");
  final Extension<FileOptions, List<String>> extensionE = Extension.get(FileOptions.class,
      WireField.Label.REPEATED, "e", 5,
      "com.squareup.wire.ProtoAdapter#STRING");

  @Test public void putAndGetExtensionValues() throws Exception {
    TagMap.Builder tagMap = new TagMap.Builder();
    tagMap.add(extensionA, 3.14159);
    tagMap.add(extensionB, "hello");
    tagMap.add(extensionC, Type.TYPE_SINT64);
    assertThat(tagMap.get(extensionA)).isEqualTo(3.14159);
    assertThat(tagMap.get(extensionB)).isEqualTo("hello");
    assertThat(tagMap.get(extensionC)).isEqualTo(Type.TYPE_SINT64);
  }

  @Test public void equalsAndHashCode() throws Exception {
    TagMap tagMap1 = new TagMap.Builder()
        .add(extensionA, 3.14159)
        .add(extensionB, "hello")
        .build();

    TagMap tagMap2 = new TagMap.Builder()
        .add(extensionA, 3.14159)
        .add(extensionB, "hello")
        .build();

    TagMap tagMap3 = new TagMap.Builder().build();

    TagMap tagMap4 = new TagMap.Builder()
        .add(extensionC, Type.TYPE_SINT64)
        .build();

    assertThat(tagMap1.equals(tagMap1)).isTrue();
    assertThat(tagMap1.equals(tagMap2)).isTrue();
    assertThat(tagMap1.equals(tagMap3)).isFalse();
    assertThat(tagMap1.equals(tagMap4)).isFalse();

    assertThat(tagMap3.equals(tagMap1)).isFalse();
    assertThat(tagMap3.equals(tagMap3)).isTrue();
    assertThat(tagMap3.equals(tagMap4)).isFalse();

    assertThat(tagMap4.equals(tagMap1)).isFalse();
    assertThat(tagMap4.equals(tagMap3)).isFalse();
    assertThat(tagMap4.equals(tagMap4)).isTrue();

    assertThat(tagMap1.hashCode()).isEqualTo(tagMap2.hashCode());
    assertThat(tagMap1.hashCode()).isNotEqualTo(tagMap3.hashCode());
    assertThat(tagMap1.hashCode()).isNotEqualTo(tagMap4.hashCode());
    assertThat(tagMap3.hashCode()).isNotEqualTo(tagMap4.hashCode());
  }

  @Test public void putAndGetRepeatedValues() throws Exception {
    TagMap tagMap = new TagMap.Builder()
        .add(extensionD, 1.0)
        .add(extensionD, 2.0)
        .add(extensionD, 3.0)
        .add(extensionE, "hacker")
        .add(extensionE, "slacker")
        .add(extensionE, "cracker")
        .build();
    assertThat(tagMap.get(extensionD)).isEqualTo(Arrays.asList(1.0, 2.0, 3.0));
    assertThat(tagMap.get(extensionE)).isEqualTo(Arrays.asList("hacker", "slacker", "cracker"));
  }

  /** Confirm that the implementation doubles from 8 to 16 and 16 to 32 elements. */
  @Test public void manyRepeatedValues() throws Exception {
    TagMap tagMap = new TagMap.Builder()
        .add(extensionD, 1.0)
        .add(extensionD, 2.0)
        .add(extensionD, 3.0)
        .add(extensionD, 4.0)
        .add(extensionD, 5.0)
        .add(extensionD, 6.0)
        .add(extensionD, 7.0)
        .add(extensionD, 8.0)
        .add(extensionD, 9.0)
        .add(extensionD, 10.0)
        .add(extensionD, 11.0)
        .add(extensionD, 12.0)
        .add(extensionD, 13.0)
        .add(extensionD, 14.0)
        .add(extensionD, 15.0)
        .add(extensionD, 16.0)
        .add(extensionD, 17.0)
        .build();
    assertThat(tagMap.get(extensionD)).isEqualTo(Arrays.asList(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0,
        8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0, 17.0));
  }

  @Test public void rawToExtensionConversionForDouble() throws Exception {
    TagMap tagMap = new TagMap.Builder()
        .add(unknownA, 4614256650576692846L)
        .build();
    assertThat(tagMap.get(extensionA)).isEqualTo(3.14159);
  }

  @Test public void rawToExtensionConversionForString() throws Exception {
    TagMap tagMap = new TagMap.Builder()
        .add(unknownB, ByteString.encodeUtf8("hello"))
        .build();
    assertThat(tagMap.get(extensionB)).isEqualTo("hello");
  }

  @Test public void rawToExtensionConversionForEnum() throws Exception {
    TagMap tagMap = new TagMap.Builder()
        .add(unknownC, 18L)
        .build();
    assertThat(tagMap.get(extensionC)).isEqualTo(Type.TYPE_SINT64);
  }

  @Test public void rawToExtensionConversionForUnknownEnum() throws Exception {
    TagMap tagMap = new TagMap.Builder()
        .add(unknownC, 2828L)
        .build();
    assertThat(tagMap.get(extensionC)).isEqualTo(2828L);
  }

  @Test public void copyConstructor() throws Exception {
    TagMap a = new TagMap.Builder()
        .add(extensionD, 1.0)
        .build();

    TagMap b = new TagMap.Builder(a).build();
    assertThat(b.get(extensionD)).isEqualTo(Arrays.asList(1.0));
    assertThat(b.size()).isEqualTo(1);

    TagMap c = new TagMap.Builder(a)
        .add(extensionD, 2.0)
        .build();
    assertThat(c.get(extensionD)).isEqualTo(Arrays.asList(1.0, 2.0));
    assertThat(c.size()).isEqualTo(2);
    assertThat(b.get(extensionD)).isEqualTo(Arrays.asList(1.0));
    assertThat(b.size()).isEqualTo(1);

    TagMap d = new TagMap.Builder(b)
        .add(extensionD, 3.0)
        .build();
    assertThat(c.get(extensionD)).isEqualTo(Arrays.asList(1.0, 2.0));
    assertThat(c.size()).isEqualTo(2);
    assertThat(d.get(extensionD)).isEqualTo(Arrays.asList(1.0, 3.0));
    assertThat(d.size()).isEqualTo(2);
  }

  @Test public void removeAllNotFound() throws Exception {
    TagMap map = new TagMap.Builder()
        .add(extensionA, 1.0)
        .add(extensionB, 2.0)
        .add(extensionC, 3.0)
        .add(extensionD, 4.0)
        .removeAll(extensionE.getTag())
        .build();

    TagMap expected = new TagMap.Builder()
        .add(extensionA, 1.0)
        .add(extensionB, 2.0)
        .add(extensionC, 3.0)
        .add(extensionD, 4.0)
        .build();

    assertThat(map).isEqualTo(expected);
    assertThat(map.size()).isEqualTo(4);
  }

  @Test public void removeAllClearsMap() throws Exception {
    TagMap map = new TagMap.Builder()
        .add(extensionA, 1.0)
        .add(extensionA, 2.0)
        .removeAll(extensionA.getTag())
        .build();

    TagMap expected = new TagMap.Builder().build();

    assertThat(map).isEqualTo(expected);
    assertThat(map.size()).isEqualTo(0);
  }

  @Test public void removeAllRangeShift() throws Exception {
    TagMap map = new TagMap.Builder()
        .add(extensionA, 1.0)
        .add(extensionB, 2.0)
        .add(extensionB, 3.0)
        .add(extensionC, 4.0)
        .removeAll(extensionB.getTag())
        .build();

    TagMap expected = new TagMap.Builder()
        .add(extensionA, 1.0)
        .add(extensionC, 4.0)
        .build();

    assertThat(map).isEqualTo(expected);
    assertThat(map.size()).isEqualTo(2);
  }

  @Test public void removeAllMultipleRanges() throws Exception {
    TagMap map = new TagMap.Builder()
        .add(extensionA, 1.0)
        .add(extensionB, 2.0)
        .add(extensionB, 3.0)
        .add(extensionB, 4.0)
        .add(extensionC, 5.0)
        .add(extensionB, 6.0)
        .add(extensionD, 7.0)
        .removeAll(extensionB.getTag())
        .build();

    TagMap expected = new TagMap.Builder()
        .add(extensionA, 1.0)
        .add(extensionC, 5.0)
        .add(extensionD, 7.0)
        .build();

    assertThat(map).isEqualTo(expected);
    assertThat(map.size()).isEqualTo(3);
  }

  @Test public void encodeRepeatedExtension() throws IOException {
    Extension<FileOptions, List<Integer>> extension = Extension.get(FileOptions.class,
        WireField.Label.REPEATED, "a", 90,
        "com.squareup.wire.ProtoAdapter#INT32");

    TagMap map = new TagMap.Builder()
        .add(extension, 601)
        .add(extension, 701)
        .build();

    Buffer buffer = new Buffer();
    ProtoWriter protoWriter = new ProtoWriter(buffer);
    map.encode(protoWriter);
    assertThat(buffer.readByteString()).isEqualTo(ByteString.decodeHex("d005d904d005bd05"));
  }

  @Test public void encodePackedExtension() throws IOException {
    Extension<FileOptions, List<Integer>> extension = Extension.get(FileOptions.class,
        WireField.Label.PACKED, "a", 90,
        "com.squareup.wire.ProtoAdapter#INT32");

    TagMap map = new TagMap.Builder()
        .add(extension, 601)
        .add(extension, 701)
        .build();

    Buffer buffer = new Buffer();
    ProtoWriter protoWriter = new ProtoWriter(buffer);
    map.encode(protoWriter);
    assertThat(buffer.readByteString()).isEqualTo(ByteString.decodeHex("d20504d904bd05"));
  }

  @Test public void encodeUnknownEncodesAsRepeated() throws IOException {
    Extension<FileOptions, Object> unknown
        = Extension.unknown(FileOptions.class, 90, FieldEncoding.VARINT);
    TagMap map = new TagMap.Builder()
        .add(unknown, 601L)
        .add(unknown, 701L)
        .build();

    Buffer buffer = new Buffer();
    ProtoWriter protoWriter = new ProtoWriter(buffer);
    map.encode(protoWriter);
    assertThat(buffer.readByteString()).isEqualTo(ByteString.decodeHex("d005d904d005bd05"));
  }

  /**
   * Though it's unlikely in practice it's programmatically possible to have both extensions and
   * unknown values with the same tag!
   */
  @Test public void encodeMixOfPackedAndUnknown() throws IOException {
    Extension<FileOptions, List<Integer>> extension = Extension.get(FileOptions.class,
        WireField.Label.PACKED, "a", 90,
        "com.squareup.wire.ProtoAdapter#INT32");
    Extension<FileOptions, Object> unknown = Extension.unknown(FileOptions.class,
        90, FieldEncoding.VARINT);

    TagMap map = new TagMap.Builder()
        .add(extension, 601)
        .add(extension, 602)
        .add(unknown, 701L)
        .add(unknown, 702L)
        .build();

    Buffer buffer = new Buffer();
    ProtoWriter protoWriter = new ProtoWriter(buffer);
    map.encode(protoWriter);
    assertThat(buffer.readByteString())
        .isEqualTo(ByteString.decodeHex("d20504d904da04d005bd05d005be05"));
  }

  @Test public void transcodeUnknownValueToPackedList() throws Exception {
    Extension<FileOptions, List<Integer>> extension = Extension.get(FileOptions.class,
        WireField.Label.PACKED, "a", 90,
        "com.squareup.wire.ProtoAdapter#INT32");
    Extension<FileOptions, Object> unknown = Extension.unknown(
        FileOptions.class, 90, FieldEncoding.LENGTH_DELIMITED);

    TagMap map = new TagMap.Builder()
        .add(unknown, ByteString.decodeHex("d904da04"))
        .build();

    assertThat(map.get(extension)).isEqualTo(Arrays.asList(601, 602));
  }

  /** We had a bug where the capacity was being included in {@link TagMap#equals}. */
  @Test public void removeAllAndEquals() throws Exception {
    TagMap.Builder builder = new TagMap.Builder();
    builder.add(extensionA, 1.0);
    for (int i = 0; i < TagMap.Builder.INITIAL_CAPACITY; i++) {
      builder.add(extensionB, 1.0);
    }
    builder.removeAll(extensionB.getTag());
    TagMap map = builder.build();

    TagMap expected = new TagMap.Builder()
        .add(extensionA, 1.0)
        .build();

    assertThat(map).isEqualTo(expected);
    assertThat(map.size()).isEqualTo(1);
  }
}
