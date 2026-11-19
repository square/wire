/*
 * Copyright (C) 2014 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.wire;

import static com.google.common.truth.Truth.assertThat;

import com.squareup.wire.protos.edgecases.MergeChoice;
import com.squareup.wire.protos.edgecases.MergeLeaf;
import java.util.Collections;
import okio.Buffer;
import org.junit.Test;

/** Repeated message fields are merged, and a oneof keeps only its last member. */
public final class MergeChoiceTest {
  private static final int A = 1;
  private static final int B = 2;
  private static final int C = 3;

  private final ProtoAdapter<MergeChoice> compactAdapter =
      ProtoAdapter.newMessageAdapter(MergeChoice.class);

  private static byte[] encode(int[]... occurrences) throws Exception {
    Buffer buffer = new Buffer();
    for (int[] occurrence : occurrences) {
      MergeLeaf leaf =
          new MergeLeaf.Builder().values(Collections.singletonList(occurrence[1])).build();
      MergeLeaf.ADAPTER.encodeWithTag(new ProtoWriter(buffer), occurrence[0], leaf);
    }
    return buffer.readByteArray();
  }

  private static int[] of(int tag, int value) {
    return new int[] {tag, value};
  }

  private static String describe(MergeChoice message) {
    return "a="
        + (message.a != null ? message.a.values : null)
        + " b="
        + (message.b != null ? message.b.values : null)
        + " c="
        + (message.c != null ? message.c.values : null);
  }

  /** Decodes with the generated adapter, which is what protobuf-java semantics are checked on. */
  private void assertDecodes(String expected, int[]... occurrences) throws Exception {
    byte[] bytes = encode(occurrences);
    assertThat(describe(MergeChoice.ADAPTER.decode(bytes))).isEqualTo(expected);
    assertThat(describe(MergeChoice.ADAPTER.decode(new Buffer().write(bytes)))).isEqualTo(expected);
  }

  /**
   * Decodes with the adapter that reflects on the generated class, which compact code uses. It sets
   * a oneof member with the builder method, so the last member on the wire wins.
   */
  private void assertDecodesCompact(String expected, int[]... occurrences) throws Exception {
    byte[] bytes = encode(occurrences);
    assertThat(describe(compactAdapter.decode(bytes))).isEqualTo(expected);
    assertThat(describe(compactAdapter.decode(new Buffer().write(bytes)))).isEqualTo(expected);
  }

  @Test
  public void memberThenRepeatedUnrelatedField() throws Exception {
    assertDecodes("a=null b=[1] c=[2, 3]", of(B, 1), of(C, 2), of(C, 3));
    assertDecodesCompact("a=null b=[1] c=[2, 3]", of(B, 1), of(C, 2), of(C, 3));
  }

  @Test
  public void sameMemberTwice() throws Exception {
    assertDecodes("a=[1, 2] b=null c=null", of(A, 1), of(A, 2));
    assertDecodesCompact("a=[1, 2] b=null c=null", of(A, 1), of(A, 2));
  }

  @Test
  public void repeatedMemberAmongOtherFields() throws Exception {
    assertDecodes("a=[1, 3] b=null c=[2, 4]", of(A, 1), of(C, 2), of(A, 3), of(C, 4));
    assertDecodesCompact("a=[1, 3] b=null c=[2, 4]", of(A, 1), of(C, 2), of(A, 3), of(C, 4));
  }

  @Test
  public void memberRepeatedThenReplaced() throws Exception {
    assertDecodes("a=null b=[3] c=null", of(A, 1), of(A, 2), of(B, 3));
    assertDecodesCompact("a=null b=[3] c=null", of(A, 1), of(A, 2), of(B, 3));
  }

  @Test
  public void memberReplacedThenSetAgain() throws Exception {
    assertDecodes("a=[3] b=null c=null", of(A, 1), of(B, 2), of(A, 3));
    assertDecodesCompact("a=[3] b=null c=null", of(A, 1), of(B, 2), of(A, 3));
  }

  @Test
  public void repeatedMembersAreMergedAfterBeingSetAgain() throws Exception {
    assertDecodes("a=[4, 5] b=null c=null", of(A, 1), of(A, 2), of(B, 3), of(A, 4), of(A, 5));
    assertDecodesCompact(
        "a=[4, 5] b=null c=null", of(A, 1), of(A, 2), of(B, 3), of(A, 4), of(A, 5));
  }

  @Test
  public void lastMemberWins() throws Exception {
    assertDecodes("a=null b=[5] c=null", of(B, 1), of(B, 2), of(A, 3), of(A, 4), of(B, 5));
    assertDecodesCompact("a=null b=[5] c=null", of(B, 1), of(B, 2), of(A, 3), of(A, 4), of(B, 5));
  }
}
