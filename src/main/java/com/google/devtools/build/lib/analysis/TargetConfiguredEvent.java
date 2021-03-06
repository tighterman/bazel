// Copyright 2017 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
package com.google.devtools.build.lib.analysis;

import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.analysis.config.BuildConfiguration;
import com.google.devtools.build.lib.analysis.config.BuildEventWithConfiguration;
import com.google.devtools.build.lib.buildeventstream.BuildEventConverters;
import com.google.devtools.build.lib.buildeventstream.BuildEventId;
import com.google.devtools.build.lib.buildeventstream.BuildEventStreamProtos;
import com.google.devtools.build.lib.buildeventstream.GenericBuildEvent;
import com.google.devtools.build.lib.cmdline.Label;
import java.util.Collection;

/** Event reporting about the configurations associated with a given target */
public class TargetConfiguredEvent implements BuildEventWithConfiguration {
  private final Label label;
  private final Collection<BuildConfiguration> configurations;

  TargetConfiguredEvent(Label label, Collection<BuildConfiguration> configurations) {
    this.label = label;
    this.configurations = configurations;
  }

  @Override
  public Collection<BuildConfiguration> getConfigurations() {
    return configurations;
  }

  @Override
  public BuildEventId getEventId() {
    return BuildEventId.targetConfigured(label);
  }

  @Override
  public Collection<BuildEventId> getChildrenEvents() {
    ImmutableList.Builder childrenBuilder = ImmutableList.builder();
    for (BuildConfiguration config : configurations) {
      if (config != null) {
        childrenBuilder.add(BuildEventId.targetCompleted(label, config.getEventId()));
      } else {
        childrenBuilder.add(
            BuildEventId.targetCompleted(label, BuildEventId.nullConfigurationId()));
      }
    }
    return childrenBuilder.build();
  }

  @Override
  public BuildEventStreamProtos.BuildEvent asStreamProto(BuildEventConverters converters) {
    return GenericBuildEvent.protoChaining(this)
        .setConfigured(BuildEventStreamProtos.TargetConfigured.getDefaultInstance())
        .build();
  }
}
