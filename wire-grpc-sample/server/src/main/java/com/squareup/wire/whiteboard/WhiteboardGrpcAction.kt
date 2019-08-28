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

import com.squareup.wire.MessageSink
import com.squareup.wire.MessageSource
import com.squareup.wire.whiteboard.WhiteboardUpdate.InitialiseBoard
import com.squareup.wire.whiteboard.WhiteboardUpdate.UpdatePoints
import misk.web.actions.WebAction
import javax.inject.Singleton

@Singleton
class WhiteboardGrpcAction : WebAction, WhiteboardWhiteboardBlockingServer {
  val clients = mutableListOf<Client>()
  val points = mutableListOf<Point>()

  override fun Whiteboard(
    request: MessageSource<WhiteboardCommand>,
    response: MessageSink<WhiteboardUpdate>
  ) {
    val client = Client(request, response)
    clients += client
    client.process()
  }

  inner class Client(
    private val commands: MessageSource<WhiteboardCommand>,
    private val updates: MessageSink<WhiteboardUpdate>
  ) {
    fun process() {
      updates.use {
        val initialiseBoard = InitialiseBoard(
            BOARD_WIDTH,
            BOARD_HEIGHT,
            COLORS[clients.indexOf(this) % COLORS.size]
        )
        updates.write(WhiteboardUpdate(initialise_board = initialiseBoard))
        updates.write(WhiteboardUpdate(update_points = UpdatePoints(points)))

        commands.consumeEachAndClose { command ->
          when {
            command.add_point != null -> points += command.add_point!!.point
            command.clear_board != null -> points.clear()
            else -> throw IllegalArgumentException("Unexpected command $command")
          }

          for (client in clients) {
            synchronized(WhiteboardGrpcAction::class.java) {
              client.updates.write(WhiteboardUpdate(update_points = UpdatePoints(points)))
            }
          }
        }
      }
    }
  }

  companion object {
    private const val BOARD_WIDTH = 60
    private const val BOARD_HEIGHT = 80

    private val COLORS = arrayOf(
        0xff00c8fa.toInt(), // Blue
        0xfff46e38.toInt(), // Red
        0xff0bb634.toInt(), // Green
        0xff333333.toInt() // Black
    )
  }
}
