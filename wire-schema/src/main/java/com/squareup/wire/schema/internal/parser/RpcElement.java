/*
 * Copyright (C) 2014 Square, Inc.
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
package com.squareup.wire.schema.internal.parser;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.squareup.wire.schema.Location;

@AutoValue
public abstract class RpcElement {
  static Builder builder(Location location) {
    return new AutoValue_RpcElement.Builder()
        .documentation("")
        .location(location)
        .requestStreaming(false)
        .responseStreaming(false)
        .options(ImmutableList.<OptionElement>of());
  }

  public abstract Location location();
  public abstract String name();
  public abstract String documentation();
  public abstract String requestType();
  public abstract String responseType();
  public abstract boolean requestStreaming();
  public abstract boolean responseStreaming();
  public abstract ImmutableList<OptionElement> options();

  @AutoValue.Builder
  interface Builder {
    Builder location(Location location);
    Builder name(String name);
    Builder documentation(String documentation);
    Builder requestType(String requestType);
    Builder responseType(String responseType);
    Builder requestStreaming(boolean requestStreaming);
    Builder responseStreaming(boolean responseStreaming);
    Builder options(ImmutableList<OptionElement> options);
    RpcElement build();
  }
}
