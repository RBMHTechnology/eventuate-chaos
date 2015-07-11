/*
 * Copyright (C) 2015 Red Bull Media House GmbH <http://www.redbullmediahouse.com> - all rights reserved.
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

package com.rbmhtechnology.eventuate.chaos

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config

class ChaosSettings(config: Config) {
  val delayStartMinMillis: Long =
    config.getDuration("delay.start.min", TimeUnit.MILLISECONDS)

  val delayStartMaxMillis: Long =
    config.getDuration("delay.start.max", TimeUnit.MILLISECONDS)

  val delayStopMinMillis: Long =
    config.getDuration("delay.stop.min", TimeUnit.MILLISECONDS)

  val delayStopMaxMillis: Long =
    config.getDuration("delay.stop.max", TimeUnit.MILLISECONDS)

  val nodesDownMax: Int =
    config.getInt("nodes.down.max")

  val nodesTotal: Int =
    config.getInt("nodes.total")

  assert(nodesTotal > nodesDownMax, "num.modes must be > down.max")
}
