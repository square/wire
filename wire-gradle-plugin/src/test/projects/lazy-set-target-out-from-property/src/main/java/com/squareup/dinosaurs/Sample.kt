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
package com.squareup.dinosaurs

import com.squareup.geology.Period
import java.io.IOException
import okio.ByteString.Companion.toByteString

class Sample {
  @Throws(IOException::class)
  fun run() {
    // Create an immutable value object with the Builder API.
    val stegosaurus = Dinosaur(
      name = "Stegosaurus",
      period = Period.JURASSIC,
      length_meters = 9.0,
      mass_kilograms = 5_000.0,
      picture_urls = listOf("http://goo.gl/LD5KY5", "http://goo.gl/VYRM67"),
    )
    // Encode that value to bytes, and print that as base64.
    val stegosaurusEncoded = Dinosaur.ADAPTER.encode(stegosaurus)
    println(stegosaurusEncoded.toByteString().base64())
  }

  companion object {
    @Throws(IOException::class)
    @JvmStatic
    fun main(args: Array<String>) {
      Sample().run()
    }
  }
}
