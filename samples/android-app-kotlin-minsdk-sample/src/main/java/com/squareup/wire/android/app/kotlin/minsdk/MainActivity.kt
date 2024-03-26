package com.squareup.wire.android.app.kotlin.minsdk

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter
import com.squareup.wire.WireJsonAdapterFactory

class MainActivity : AppCompatActivity() {

  @OptIn(ExperimentalStdlibApi::class)
  private val jsonAdapter = Moshi.Builder()
    .add(WireJsonAdapterFactory())
    .build()
    .adapter<SomeText>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val someText = checkNotNull(jsonAdapter.fromJson("""{"value": "Hi"}"""))
    findViewById<TextView>(R.id.text_view).text = someText.value_
  }
}
