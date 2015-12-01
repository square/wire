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
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ Linker.class })
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

  @Test public void validate() throws Exception {
    Extensions extensions = new Extensions(ExtensionsElement.create(location, 1, 2, ""));
    Linker linker = spy(new Linker(Collections.EMPTY_LIST));

    extensions.validate(linker);
    Mockito.verify(linker, Mockito.never()).withContext(extensions);
    Mockito.verify(linker, Mockito.never()).addError(Mockito.anyString(), Mockito.anyList());
  }

  @Test public void validateIllegalStart() throws Exception {
    Extensions extensions = new Extensions(ExtensionsElement.create(location, 0, 1, ""));
    Linker linker = spy(new Linker(Collections.EMPTY_LIST));
    when(linker.withContext(extensions)).thenReturn(linker);

    extensions.validate(linker);
    Mockito.verify(linker, Mockito.times(1)).withContext(extensions);
    Mockito.verify(linker, Mockito.times(1)).addError(Mockito.anyString(),
      Mockito.eq(extensions.start()), Mockito.eq(extensions.end()));
  }

  @Test public void validateIllegalEnd() throws Exception {
    Extensions extensions = new Extensions(ExtensionsElement.create(location, 100, 19500, ""));
    Linker linker = spy(new Linker(Collections.EMPTY_LIST));
    when(linker.withContext(extensions)).thenReturn(linker);

    extensions.validate(linker);
    Mockito.verify(linker, Mockito.times(1)).withContext(extensions);
    Mockito.verify(linker, Mockito.times(1)).addError(Mockito.anyString(),
      Mockito.eq(extensions.start()), Mockito.eq(extensions.end()));
  }

  @Test public void validateIllegalStartAndEnd() throws Exception {
    Extensions extensions = new Extensions(ExtensionsElement.create(location, 19999, 19000, ""));
    Linker linker = spy(new Linker(Collections.EMPTY_LIST));
    when(linker.withContext(extensions)).thenReturn(linker);

    extensions.validate(linker);
    Mockito.verify(linker, Mockito.times(1)).withContext(extensions);
    Mockito.verify(linker, Mockito.times(1)).addError(Mockito.anyString(),
      Mockito.eq(extensions.start()), Mockito.eq(extensions.end()));
  }
}
