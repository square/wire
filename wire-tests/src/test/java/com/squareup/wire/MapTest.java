/*
 * Copyright 2016 Square Inc.
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

import com.google.common.collect.ImmutableMap;
import com.squareup.wire.map.Mappy;
import com.squareup.wire.map.Thing;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import okio.ByteString;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public final class MapTest {
  private static final ByteString BYTES = ByteString.decodeHex(
      "0a0c0a036f6e6512050a034f6e650a0c0a0374776f12050a0354776f0a100a05746872656512070a055468726565");
  private static final Mappy EMPTY = new Mappy.Builder().build();
  private static final Mappy THREE = new Mappy.Builder() //
      .things(ImmutableMap.of( //
          "one", new Thing("One"), //
          "two", new Thing("Two"), //
          "three", new Thing("Three") //
      )) //
      .build();

  @Parameters(name = "{0}")
  public static List<Object[]> parameters() {
    return Arrays.asList(
        new Object[] { "Generated", Mappy.ADAPTER },
        new Object[] { "Runtime", RuntimeMessageAdapter.create(Mappy.class) }
    );
  }

  @Parameter(0)
  public String name;
  @Parameter(1)
  public ProtoAdapter<Mappy> adapter;

  @Test public void serialize() {
    assertThat(ByteString.of(adapter.encode(THREE))).isEqualTo(BYTES);

    assertThat(adapter.encode(EMPTY)).hasSize(0);
  }

  @Test public void deserialize() throws IOException {
    assertThat(adapter.decode(BYTES)).isEqualTo(THREE);

    Mappy empty = adapter.decode(new byte[0]);
    assertThat(empty.things).isNotNull();
  }
}
