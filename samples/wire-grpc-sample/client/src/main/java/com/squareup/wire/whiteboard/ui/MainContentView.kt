/*
 * Copyright (C) 2022 Square, Inc.
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
package com.squareup.wire.whiteboard.ui

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.widget.Button
import com.squareup.contour.ContourLayout
import com.squareup.contour.SizeMode.AtMost

class MainContentView(context: Context) : ContourLayout(context) {
  internal val whiteboardView = WhiteboardView(context)
  internal val button = Button(context).apply {
    text = "Clear !"
    setTextSize(
      TypedValue.COMPLEX_UNIT_PX,
      TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        30f,
        resources.displayMetrics,
      ),
    )
  }

  init {
    background = GradientDrawable().also {
      it.shape = GradientDrawable.RECTANGLE
      it.setColor(0xff2b2b2b.toInt())
    }
    contourWidthMatchParent()
    contourHeightMatchParent()

    whiteboardView.layoutBy(
      x = matchParentX(),
      y = topTo { parent.top() }.bottomTo { button.top() - 24.dip },
    )
    button.layoutBy(
      x = centerHorizontallyTo { parent.centerX() }.widthOf(AtMost) { parent.width() - 48.dip },
      y = bottomTo { parent.bottom() - 24.dip },
    )
  }
}
