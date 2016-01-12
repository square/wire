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


import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public final class LocationTest {

  private static final String S = File.separator;

  @Test public void get() throws Exception {
    compareLocations(Location.get("", ""), Location.get(""));
    compareLocations(Location.get("", "path" + S + "to" + S + "location"),
      Location.get("path" + S + "to" + S + "location"));
    compareLocations(Location.get("main", "path" + S + "to" + S + "location"),
      Location.get("main" + S + "path" + S + "to" + S + "location"));
    compareLocations(Location.get("main" + S + "dir", "path" + S + "to" + S + "location"),
      Location.get("main" + S + "dir" + S + "path" + S + "to" + S + "location"));
  }

  @Test public void getNullBase() throws Exception {
    try {
      // when
      Location.get(null, "path" + S + "to" + S + "location");

      // then
      fail("Location should throw NullPointerException when tries to get location with null base");
    }
    catch (NullPointerException e) {
      assertThat(e.getMessage()).isNull();
    }
  }

  @Test public void getNullPath() throws Exception {
    try {
      // when
      Location.get(null);

      // then
      fail("Location should throw NullPointerException when tries to get location with null base");
    }
    catch (NullPointerException e) {
      assertThat(e.getMessage()).isNull();
    }
  }

  @Test public void getNullPathWithBase() throws Exception {
    try {
      // when
      Location.get("main" + S + "dir", null);

      // then
      fail("Location should throw NullPointerException when tries to get location with null path");
    }
    catch (NullPointerException e) {
      assertThat(e.getMessage()).isNull();
    }

  }

  @Test public void defaultLineAndColumn() throws Exception {
    Location location = Location.get("path" + S + "to" + S + "location");

    assertThat(location.line()).isEqualTo(-1);
    assertThat(location.column()).isEqualTo(-1);
  }

  @Test public void at() throws Exception {
    Location location = Location.get("path" + S + "to" + S + "location").at(11, 21);

    assertThat(location.line()).isEqualTo(11);
    assertThat(location.column()).isEqualTo(21);
  }

  @Test public void withoutBase() throws Exception {
    compareLocations(Location.get("path" + S + "to" + S + "location").withoutBase(),
      Location.get("path" + S + "to" + S + "location"));
    compareLocations(Location.get("main" + S + "dir", "path" + S + "to" + S + "location").withoutBase(),
      Location.get("path" + S + "to" + S + "location"));

    Location location = Location.get("main" + S + "dir", "path" + S + "to" + S + "location").at(11, 21);
    Location withoutBaseLocation = location.withoutBase();

    assertThat(location.line()).isEqualTo(withoutBaseLocation.line());
    assertThat(location.column()).isEqualTo(withoutBaseLocation.column());
  }

  @Test public void testToString() throws Exception {
    assertThat(Location.get("", "").toString()).isEqualTo(Location.get("").toString());
    assertThat(Location.get("", "path" + S + "to" + S + "location").toString()).isEqualTo(
      Location.get("path" + S + "to" + S + "location").toString());
    assertThat(Location.get("main", "path" + S + "to" + S + "location").toString()).isEqualTo(
      Location.get("main" + S + "path" + S + "to" + S + "location").toString());
    assertThat(Location.get("main" + S + "dir", "path" + S + "to" + S + "location").toString()).isEqualTo(
      Location.get("main" + S + "dir" + S + "path" + S + "to" + S + "location").toString());

    assertThat(Location.get("main" + S + "dir", "path" + S + "to" + S + "location").toString()).isNotEqualTo(
      Location.get("main" + S + "dir" + S + "path" + S + "to" + S + "location").at(11, 12).toString());
    assertThat(Location.get("main" + S + "dir", "path" + S + "to" + S + "location").at(5, 6).toString()).isNotEqualTo
      (Location.get("main" + S + "dir" + S + "path" + S + "to" + S + "location").at(11, 12).toString());
    assertThat(Location.get("main" + S + "dir", "path" + S + "to" + S + "location").at(11, 12).toString()).isEqualTo(
      Location.get("main" + S + "dir" + S + "path" + S + "to" + S + "location").at(11, 12).toString());
    assertThat(Location.get("main" + S + "dir", "path" + S + "to" + S + "location").at(10, -1).toString()).isEqualTo(
      Location.get("main" + S + "dir", "path" + S + "to" + S + "location").at(10,-1).toString());
    assertThat(Location.get("main" + S + "dir", "path" + S + "to" + S + "location").at(-1, -1).toString()).isEqualTo(
      Location.get("main" + S + "dir", "path" + S + "to" + S + "location").toString());
    assertThat(Location.get("main" + S + "dir", "path" + S + "to" + S + "location").at(-1, 10).toString()).isEqualTo(
      Location.get("main" + S + "dir", "path" + S + "to" + S + "location").at(-1,10).toString());
    assertThat(Location.get("main" + S + "dir", "path" + S + "to" + S + "location").at(-1, 10).toString()).isEqualTo(
      Location.get("main" + S + "dir", "path" + S + "to" + S + "location").toString());
  }

  private void compareLocations(Location location1, Location location2)
  {
    assertThat(concat(location1.base(), location1.path())).isEqualTo(concat(location2.base(), location2.path()));
    assertThat(location1.line()).isEqualTo(location2.line());
    assertThat(location1.column()).isEqualTo(location2.column());
  }

  private String concat(String path1, String path2)
  {
    return (path1.isEmpty()) ? path1 + path2 : path1 + File.separator + path2;
  }
}
