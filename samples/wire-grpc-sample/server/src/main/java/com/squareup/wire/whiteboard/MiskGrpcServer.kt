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

import misk.MiskApplication
import misk.MiskRealServiceModule
import misk.config.ConfigModule
import misk.config.MiskConfig
import misk.environment.DeploymentModule
import misk.web.MiskWebModule
import wisp.deployment.getDeploymentFromEnvironmentVariable

fun main(args: Array<String>) {
  val deployment = getDeploymentFromEnvironmentVariable()
  val config = MiskConfig.load<WhiteboardConfig>("whiteboard", deployment)

  MiskApplication(
    MiskRealServiceModule(),
    MiskWebModule(config.web),
    WhiteboardGrpcModule(),
    ConfigModule.create("whiteboard", config),
    DeploymentModule(deployment),
  ).run(args)
}
