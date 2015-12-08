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


import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public final class LocationTest {

  @Test public void get() throws Exception {
    compareLocations(Location.get("", ""), Location.get(""));
    compareLocations(Location.get("", "path\\to\\location"), Location.get("path\\to\\location"));
    compareLocations(Location.get("main", "path\\to\\location"), Location.get("main\\path\\to\\location"));
    compareLocations(Location.get("main\\dir", "path\\to\\location"), Location.get("main\\dir\\path\\to\\location"));
  }

  @Test(expected = NullPointerException.class) public void getNullBase() throws Exception {
    Location.get(null, "path\\to\\location");
    fail("Location should throw NullPointerException when tries to get location with null base");
  }

  @Test(expected = NullPointerException.class) public void getNullPath() throws Exception {
    Location.get(null);
    fail("Location should throw NullPointerException when tries to get location with null base");
  }

  @Test(expected = NullPointerException.class) public void getNullPathWithBase() throws Exception {
    Location.get("main\\dir", null);
    fail("Location should throw NullPointerException when tries to get location with null path");
  }

  @Test public void defaultLineAndColumn() throws Exception {
    Location location = Location.get("path\\to\\location");

    assertThat(location.line()).isEqualTo(-1);
    assertThat(location.column()).isEqualTo(-1);
  }

  @Test public void at() throws Exception {
    Location location = Location.get("path\\to\\location").at(11, 21);

    assertThat(location.line()).isEqualTo(11);
    assertThat(location.column()).isEqualTo(21);
  }

  @Test public void withoutBase() throws Exception {
    compareLocations(Location.get("path\\to\\location").withoutBase(), Location.get("path\\to\\location"));
    compareLocations(Location.get("main\\dir", "path\\to\\location").withoutBase(), Location.get("path\\to\\location"));

    Location location = Location.get("main\\dir", "path\\to\\location").at(11, 21);
    Location withoutBaseLocation = location.withoutBase();

    assertThat(location.line()).isEqualTo(withoutBaseLocation.line());
    assertThat(location.column()).isEqualTo(withoutBaseLocation.column());
  }

  @Test public void testToString() throws Exception {
    assertThat(Location.get("", "").toString()).isEqualTo(Location.get("").toString());
    assertThat(Location.get("", "path\\to\\location").toString()).isEqualTo(Location.get("path\\to\\location").toString());
    assertThat(Location.get("main", "path\\to\\location").toString()).isEqualTo(Location.get("main\\path\\to\\location").toString());
    assertThat(Location.get("main\\dir", "path\\to\\location").toString()).isEqualTo(Location.get("main\\dir\\path\\to\\location").toString());

    assertThat(Location.get("main\\dir", "path\\to\\location").toString()).isNotEqualTo(Location.get("main\\dir\\path\\to\\location").at(11, 12).toString());
    assertThat(Location.get("main\\dir", "path\\to\\location").at(5, 6).toString()).isNotEqualTo(Location.get("main\\dir\\path\\to\\location").at(11, 12).toString());
    assertThat(Location.get("main\\dir", "path\\to\\location").at(11, 12).toString()).isEqualTo(Location.get("main\\dir\\path\\to\\location").at(11, 12).toString());
    assertThat(Location.get("main\\dir", "path\\to\\location").at(10, -1).toString()).isEqualTo(Location.get("main\\dir", "path\\to\\location").at(10,-1).toString());
    assertThat(Location.get("main\\dir", "path\\to\\location").at(-1, -1).toString()).isEqualTo(Location.get("main\\dir", "path\\to\\location").toString());
    assertThat(Location.get("main\\dir", "path\\to\\location").at(-1, 10).toString()).isEqualTo(Location.get("main\\dir", "path\\to\\location").at(-1,10).toString());
    assertThat(Location.get("main\\dir", "path\\to\\location").at(-1, 10).toString()).isEqualTo(Location.get("main\\dir", "path\\to\\location").toString());
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
