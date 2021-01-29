/*
 * Copyright 2021 Square Inc.
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

import io.grpc.stub.StreamObserver

class WhiteboardImpl : WhiteboardWireGrpc.WhiteboardImplBase() {
  override fun Whiteboard(response: StreamObserver<WhiteboardUpdate>): StreamObserver<WhiteboardCommand> {
    return object : StreamObserver<WhiteboardCommand> {
      override fun onNext(value: WhiteboardCommand?) {
        response.onNext(
          WhiteboardUpdate(
            update_points = WhiteboardUpdate.UpdatePoints(
              listOf(
                Point(0, 0, 0)
              )
            )
          )
        )
      }

      override fun onError(t: Throwable?) {
        response.onCompleted()
      }

      override fun onCompleted() {
        response.onCompleted()
      }
    }
  }
}
