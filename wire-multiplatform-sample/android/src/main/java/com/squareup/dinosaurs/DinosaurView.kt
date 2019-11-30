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
package com.squareup.dinosaurs

import android.content.Context
import android.widget.TextView
import com.squareup.contour.ContourLayout

class DinosaurView(context: Context) : ContourLayout(context) {
  private val nameView = TextView(context).apply {
    textSize = 14.dip.toFloat()
    applyLayout(
        x = leftTo { parent.left() + 32.dip },
        y = topTo { parent.top() + 32.dip }
    )
  }
  private val periodView = TextView(context).apply {
    textSize = 14.dip.toFloat()
    applyLayout(
        x = leftTo { parent.left() + 32.dip },
        y = topTo { nameView.bottom() + 16.dip }
    )
  }
  private val lengthView = TextView(context).apply {
    textSize = 14.dip.toFloat()
    applyLayout(
        x = leftTo { parent.left() + 32.dip },
        y = topTo { periodView.bottom() + 16.dip }
    )
  }
  private val massView = TextView(context).apply {
    textSize = 14.dip.toFloat()
    applyLayout(
        x = leftTo { parent.left() + 32.dip },
        y = topTo { lengthView.bottom() + 16.dip }
    )
  }

  var dinosaur: Dinosaur? = null
    set(dinosaur) {
      requireNotNull(dinosaur)
      nameView.text = resources.getString(R.string.name_template, dinosaur.name)
      periodView.text = resources.getString(R.string.period_template, dinosaur.period!!.name)
      lengthView.text = resources.getString(R.string.length_template, dinosaur.length_meters)
      massView.text = resources.getString(R.string.mass_template, dinosaur.mass_kilograms)
    }
}
