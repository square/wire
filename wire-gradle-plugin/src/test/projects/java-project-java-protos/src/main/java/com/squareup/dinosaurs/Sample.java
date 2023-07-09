/*
 * Copyright (C) 2023 Square, Inc.
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
package com.squareup.dinosaurs;

import com.squareup.geology.Period;
import java.io.IOException;
import java.util.Arrays;
import okio.ByteString;

public final class Sample {
  public void run() throws IOException {
    // Create an immutable value object with the Builder API.
    Dinosaur stegosaurus =
        new Dinosaur.Builder()
            .name("Stegosaurus")
            .period(Period.JURASSIC)
            .length_meters(9.0)
            .mass_kilograms(5_000.0)
            .picture_urls(Arrays.asList("http://goo.gl/LD5KY5", "http://goo.gl/VYRM67"))
            .build();

    // Encode that value to bytes, and print that as base64.
    byte[] stegosaurusEncoded = Dinosaur.ADAPTER.encode(stegosaurus);
    System.out.println(ByteString.of(stegosaurusEncoded).base64());
  }

  public static void main(String[] args) throws IOException {
    new Sample().run();
  }
}
