/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid

import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.TraceSectionMetric

/**
 * 自定义指标，用于测量基线配置文件的有效性
 */
class BaselineProfileMetrics {
    companion object {
        /**
         * 跟踪 JIT 编译时间的 [TraceSectionMetric]
         *
         * 当正确应用基线配置文件后，这个数值应该会下降
         */
        @OptIn(ExperimentalMetricApi::class)
        val jitCompilationMetric = TraceSectionMetric("JIT Compiling %", label = "JIT compilation")

        /**
         * 跟踪类初始化时间的 [TraceSectionMetric]
         *
         * 当正确应用基线配置文件后，这个数值应该会下降
         */
        @OptIn(ExperimentalMetricApi::class)
        val classInitMetric = TraceSectionMetric("L%/%;", label = "ClassInit")

        /**
         * 与启动和基线配置文件有效性测量相关的指标
         */
        @OptIn(ExperimentalMetricApi::class)
        val allMetrics = listOf(StartupTimingMetric(), jitCompilationMetric, classInitMetric)
    }
}
