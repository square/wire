/*
 * Copyright 2019 Square Inc.
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
package com.squareup.dinosaurs;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import com.squareup.geology.Period;
import java.io.IOException;
import java.util.Arrays;
import okio.ByteString;

public final class SampleActivity extends Activity {

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.demo);

    TextView stegosaurusView = findViewById(R.id.stegosaurus);
    TextView tyrannosaurusView = findViewById(R.id.tyrannosaurus);

    // Create an immutable value object with the Builder API.
    Dinosaur stegosaurus = new Dinosaur.Builder()
        .name("Stegosaurus")
        .period(Period.JURASSIC)
        .length_meters(9.0)
        .mass_kilograms(5_000.0)
        .picture_urls(Arrays.asList("http://goo.gl/LD5KY5", "http://goo.gl/VYRM67"))
        .build();

    try {
      // Encode that value to bytes, and print that as base64.
      byte[] stegosaurusEncoded = Dinosaur.ADAPTER.encode(stegosaurus);
      Log.d("WireDemo", ByteString.of(stegosaurusEncoded).base64());

      // Decode base64 bytes, and decode those bytes as a dinosaur.
      ByteString tyrannosaurusEncoded =
          ByteString.decodeBase64("Cg1UeXJhbm5vc2F1cnVzEmhodHRwOi8vdmln"
              + "bmV0dGUxLndpa2lhLm5vY29va2llLm5ldC9qdXJhc3NpY3BhcmsvaW1hZ2VzLzYvNmEvTGVnbzUuanBnL3Jldmlz"
              + "aW9uL2xhdGVzdD9jYj0yMDE1MDMxOTAxMTIyMRJtaHR0cDovL3ZpZ25ldHRlMy53aWtpYS5ub2Nvb2tpZS5uZXQv"
              + "anVyYXNzaWNwYXJrL2ltYWdlcy81LzUwL1JleHlfcHJlcGFyaW5nX2Zvcl9iYXR0bGVfd2l0aF9JbmRvbWludXNf"
              + "cmV4LmpwZxmamZmZmZkoQCEAAAAAAJC6QCgB");
      Dinosaur tyrannosaurus = Dinosaur.ADAPTER.decode(tyrannosaurusEncoded.toByteArray());

      // Print both of our dinosaurs.
      stegosaurusView.setText(
          stegosaurus.name + " is " + stegosaurus.length_meters + " meters long!");
      tyrannosaurusView.setText(
          tyrannosaurus.name + " weighs " + tyrannosaurus.mass_kilograms + " kilos!");
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
}
