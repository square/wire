/*
 * Copyright 2014 Square Inc.
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

import com.squareup.wire.protos.redacted.Redacted;
import com.squareup.wire.protos.unknownfields.VersionOne;
import com.squareup.wire.protos.unknownfields.VersionTwo;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

public class RedactedTest {

  @Test
  public void testRedacted() throws IOException {
    assertEquals("Redacted{b=b, c=c}", new Redacted.Builder().a("a").b("b").c("c").build().toString());
  }
}
