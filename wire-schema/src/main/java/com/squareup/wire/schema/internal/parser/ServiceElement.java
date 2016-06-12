/*
 * Copyright (C) 2013 Square, Inc.
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
public abstract class ServiceElement {
  static Builder builder(Location location) {
    return new AutoValue_ServiceElement.Builder()
        .location(location)
        .documentation("")
        .rpcs(ImmutableList.<RpcElement>of())
        .options(ImmutableList.<OptionElement>of());
  }

  public abstract Location location();
  public abstract String name();
  public abstract String documentation();
  public abstract ImmutableList<RpcElement> rpcs();
  public abstract ImmutableList<OptionElement> options();

  @AutoValue.Builder
  interface Builder {
    Builder location(Location location);
    Builder name(String name);
    Builder documentation(String documentation);
    Builder rpcs(ImmutableList<RpcElement> rpcs);
    Builder options(ImmutableList<OptionElement> options);
    ServiceElement build();
  }
}
