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
        picture_urls = listOf("http://goo.gl/LD5KY5", "http://goo.gl/VYRM67")
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
