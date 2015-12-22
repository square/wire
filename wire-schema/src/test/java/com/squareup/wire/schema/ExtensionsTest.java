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


import com.squareup.wire.schema.internal.parser.ExtensionsElement;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class ExtensionsTest {
  Location location = Location.get("file.proto");

  @Test public void constructor() throws Exception {
    ExtensionsElement element = ExtensionsElement.create(location, 300, 500, "Test Documentation");

    Extensions item = new Extensions(element);

    assertThat(item.documentation()).isEqualTo(element.documentation());
    assertThat(item.start()).isEqualTo(element.start());
    assertThat(item.end()).isEqualTo(element.end());
    assertThat(item.location()).isEqualTo(element.location());
  }
}
