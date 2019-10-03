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
package com.squareup.wire.whiteboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import com.squareup.wire.whiteboard.WhiteboardCommand.AddPoint
import com.squareup.wire.whiteboard.WhiteboardCommand.ClearBoard
import com.squareup.wire.whiteboard.WhiteboardUpdate.InitialiseBoard
import com.squareup.wire.whiteboard.WhiteboardUpdate.UpdatePoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), OnBoardEventListener {
  private lateinit var sendCommandChannel: SendChannel<WhiteboardCommand>
  private lateinit var receiveUpdateChannel: ReceiveChannel<WhiteboardUpdate>

  private lateinit var whiteboardView: WhiteboardView
  private lateinit var clearButton: Button
  private val uiScope = CoroutineScope(Dispatchers.Main)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.main_activity)
    whiteboardView = findViewById(R.id.whiteboard)
    whiteboardView.onBoardEventListener = this
    clearButton = findViewById(R.id.clear)

    uiScope.launch {
      withContext(Dispatchers.IO) {
        GrpcClientProvider.grpcClient.create(WhiteboardClient::class)
            .Whiteboard()
            .execute()
            .let { (sendChannel, receiveChannel) ->
              sendCommandChannel = sendChannel
              receiveUpdateChannel = receiveChannel
            }
      }
      try {
        for (update in receiveUpdateChannel) {
          when {
            update.initialise_board != null -> initialiseBoard(update.initialise_board!!)
            update.update_points != null -> updatePoints(update.update_points!!)
          }
        }
      } catch (e: IOException) {
        e.printStackTrace()
        AlertDialog.Builder(this@MainActivity)
            .setTitle("Ouch!")
            .setMessage("IoException:\n${e.localizedMessage}")
            .setPositiveButton("That hurts.") { _, _ ->
              this@MainActivity.finish()
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setCancelable(false)
            .show()
      }
    }

    clearButton.setOnClickListener { onClear() }
  }

  override fun onPoint(point: Point) {
    tryAndCatch {
      sendCommandChannel.offer(WhiteboardCommand(add_point = AddPoint(point)))
    }
  }

  override fun onClear() {
    tryAndCatch {
      sendCommandChannel.offer(WhiteboardCommand(clear_board = ClearBoard()))
    }
  }

  private fun initialiseBoard(initialiseBoard: InitialiseBoard) {
    whiteboardView.points = emptyList()
    whiteboardView.abstractWidth = initialiseBoard.width
    whiteboardView.abstractHeight = initialiseBoard.height
    whiteboardView.color = initialiseBoard.color
    whiteboardView.invalidate()
  }

  private fun updatePoints(updatePoints: UpdatePoints) {
    whiteboardView.points = updatePoints.points
    whiteboardView.invalidate()
  }

  class WhiteboardView(
    context: Context,
    attrs: AttributeSet?
  ) : View(context, attrs) {
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
            Paint().apply { color = point.color }
        )
      }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
      when (event.action) {
        MotionEvent.ACTION_DOWN,
        MotionEvent.ACTION_MOVE -> {
          val (abstractX, abstractY) = convertToAbstract(event.x, event.y)
          onBoardEventListener?.onPoint(Point(abstractX, abstractY, color))
        }
        else -> return false
      }

      return true
    }

    private fun convertToReal(
      x: Int,
      y: Int
    ) = Pair(
        x * width / abstractWidth,
        y * height / abstractHeight
    )

    private fun convertToAbstract(
      x: Float,
      y: Float
    ) = Pair(
        (x / width * abstractWidth).roundToInt(),
        (y / height * abstractHeight).roundToInt()
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
          paint
      )
    }
  }

  private fun tryAndCatch(block: () -> Unit) {
    try {
      block()
    } catch (e: IOException) {
      e.printStackTrace()
      AlertDialog.Builder(this@MainActivity)
          .setTitle("Ouch!")
          .setMessage("IoException:\n${e.localizedMessage}")
          .setPositiveButton("That hurts.") { _, _ ->
            this@MainActivity.finish()
          }
          .setIcon(android.R.drawable.ic_dialog_alert)
          .setCancelable(false)
          .show()
    }
  }
}

interface OnBoardEventListener {
  fun onPoint(point: Point)
  fun onClear()
}
