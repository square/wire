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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public final class LoaderTest {
  @Rule public final TemporaryFolder tempFolder1 = new TemporaryFolder();
  @Rule public final TemporaryFolder tempFolder2 = new TemporaryFolder();
  @Rule public final ExpectedException exception = ExpectedException.none();

  @Test public void locateInMultiplePaths() throws IOException {
    File file1 = tempFolder1.newFile();
    File file2 = tempFolder2.newFile();

    Loader loader = Loader.forSearchPaths(
        Arrays.asList(tempFolder1.getRoot().getPath(), tempFolder2.getRoot().getPath()));
    loader.load(Arrays.asList(file1.getName(), file2.getName()));
  }

  @Test public void failLocate() throws IOException {
    File file = tempFolder2.newFile();

    Loader loader = Loader.forSearchPaths(Arrays.asList(tempFolder1.getRoot().getPath()));

    exception.expect(IOException.class);
    loader.load(Arrays.asList(file.getName()));
  }
}
