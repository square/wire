package com.squareup.wire.android.app.kotlin.minsdk

import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.wire.WireJsonAdapterFactory
import kotlin.test.assertEquals
import org.junit.Test

@OptIn(ExperimentalStdlibApi::class)
class DeserializeTest {

  @Test
  fun deserialize() {
    val jsonAdapter = Moshi.Builder()
      .add(WireJsonAdapterFactory())
      .build()
      .adapter<SomeText>()
    assertEquals(SomeText(value_ = "Hi"), jsonAdapter.fromJson("""{"value": "Hi"}"""))
  }
}
