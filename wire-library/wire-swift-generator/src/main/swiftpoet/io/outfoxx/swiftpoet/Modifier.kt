/*
 * Copyright 2018 Outfox, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.outfoxx.swiftpoet

enum class Modifier(
  internal val keyword: String,
  private vararg val targets: Target
) {

  OPEN("open", Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  PUBLIC("public", Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  PRIVATE("private", Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  FILEPRIVATE("fileprivate", Target.CLASS, Target.FUNCTION, Target.PROPERTY),
  INTERNAL("internal", Target.CLASS, Target.FUNCTION, Target.PROPERTY),

  CLASS("class", Target.FUNCTION, Target.PROPERTY),
  STATIC("static", Target.FUNCTION, Target.PROPERTY),

  MUTATING("mutating", Target.FUNCTION, Target.PROPERTY),
  NONMUTATING("nonmutating", Target.FUNCTION, Target.PROPERTY),

  FINAL("final", Target.CLASS, Target.FUNCTION),
  OVERRIDE("override", Target.FUNCTION, Target.PROPERTY),

  REQUIRED("required", Target.FUNCTION),

  INOUT("inout", Target.PARAMETER);

  internal enum class Target {
    CLASS,
    PARAMETER,
    FUNCTION,
    PROPERTY,
  }

  internal fun checkTarget(target: Target) {
    require(targets.contains(target)) { "unexpected modifier $this for $target" }
  }
}
