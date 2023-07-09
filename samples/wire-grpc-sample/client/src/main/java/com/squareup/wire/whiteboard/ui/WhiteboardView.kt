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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import com.squareup.wire.whiteboard.OnBoardEventListener
import com.squareup.wire.whiteboard.Point
import kotlin.math.roundToInt

class WhiteboardView(
  context: Context,
) : View(context, null) {
  var onBoardEventListener: OnBoardEventListener? = null

  internal var points: List<Point> = emptyList()
  internal var abstractWidth = 1
  internal var abstractHeight = 1
  internal var color: Int = 0xff333333.toInt()

  private val realWidth: Int
    get() = width / abstractWidth
  private val realHeight: Int
    get() = height / abstractHeight

  override fun draw(canvas: Canvas) {
    super.draw(canvas)

    if (points.isEmpty()) {
      canvas.showEmptyView()
    }

    points.forEach { point ->
      val (realX, realY) = convertToReal(point.x, point.y)
      canvas.drawRect(
        realX - realWidth / 2f,
        realY - realHeight / 2f,
        realX + realWidth / 2f - 1,
        realY + realHeight / 2f - 1,
        Paint().apply { color = point.color },
      )
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_DOWN,
      MotionEvent.ACTION_MOVE,
      -> {
        val (abstractX, abstractY) = convertToAbstract(event.x, event.y)
        onBoardEventListener!!.onPoint(Point(abstractX, abstractY, color))
      }
      else -> return false
    }

    return true
  }

  private fun convertToReal(
    x: Int,
    y: Int,
  ) = Pair(
    x * width / abstractWidth,
    y * height / abstractHeight,
  )

  private fun convertToAbstract(
    x: Float,
    y: Float,
  ) = Pair(
    (x / width * abstractWidth).roundToInt(),
    (y / height * abstractHeight).roundToInt(),
  )

  private fun Canvas.showEmptyView() {
    val centerX = width / 2
    val centerY = height / 2
    val text = "WIRE ♡ gRPC"
    val paint = Paint().apply {
      color = "#f46e38".toColorInt()
      textSize = 90f
    }

    drawText(
      text,
      (centerX - (paint.measureText(text) / 2).toInt()).toFloat(),
      (centerY - (paint.descent() + paint.ascent()) / 2).toInt().toFloat(),
      paint,
    )
  }
}
