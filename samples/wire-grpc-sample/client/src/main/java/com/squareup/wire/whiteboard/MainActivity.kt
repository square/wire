/*
 * Copyright (C) 2019 Square, Inc.
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
package com.squareup.wire.whiteboard

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.squareup.wire.whiteboard.WhiteboardCommand.AddPoint
import com.squareup.wire.whiteboard.WhiteboardCommand.ClearBoard
import com.squareup.wire.whiteboard.WhiteboardUpdate.InitialiseBoard
import com.squareup.wire.whiteboard.WhiteboardUpdate.UpdatePoints
import com.squareup.wire.whiteboard.ui.MainContentView
import com.squareup.wire.whiteboard.ui.WhiteboardView
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity(), OnBoardEventListener {
  private lateinit var sendCommandChannel: SendChannel<WhiteboardCommand>
  private lateinit var receiveUpdateChannel: ReceiveChannel<WhiteboardUpdate>
  private lateinit var whiteboardView: WhiteboardView
  private val uiScope = CoroutineScope(Dispatchers.Main)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val mainContentView = MainContentView(this)
    whiteboardView = mainContentView.whiteboardView
    setContentView(mainContentView)
    whiteboardView.onBoardEventListener = this

    val streamingCoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    streamingCoroutineScope.launch {
      GrpcClientProvider.grpcClient.create(WhiteboardClient::class)
        .Whiteboard()
        .executeIn(this)
        .let { (sendChannel, receiveChannel) ->
          sendCommandChannel = sendChannel
          receiveUpdateChannel = receiveChannel
        }
      try {
        for (update in receiveUpdateChannel) {
          withContext(Dispatchers.Main) {
            when {
              update.initialise_board != null -> initialiseBoard(
                update.initialise_board as InitialiseBoard,
              )

              update.update_points != null -> updatePoints(update.update_points as UpdatePoints)
            }
          }
        }
      } catch (e: IOException) {
        withContext(Dispatchers.Main) {
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

    mainContentView.button.setOnClickListener { onClear() }
  }

  override fun onPoint(point: Point) {
    tryAndCatch {
      require(sendCommandChannel.trySend(WhiteboardCommand(add_point = AddPoint(point))).isSuccess)
    }
  }

  override fun onClear() {
    tryAndCatch {
      require(sendCommandChannel.trySend(WhiteboardCommand(clear_board = ClearBoard())).isSuccess)
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
