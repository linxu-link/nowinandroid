/*
 * Copyright 2023 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid.interests

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import com.google.samples.apps.nowinandroid.PACKAGE_NAME
import com.google.samples.apps.nowinandroid.startActivityAndAllowNotifications
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 话题列表滚动基准测试
 *
 * 测试在不同编译模式下滚动话题列表的性能
 */
@RunWith(AndroidJUnit4::class)
class ScrollTopicListBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * 使用基线配置文件测试状态变化性能
     */
    @Test
    fun benchmarkStateChangeCompilationBaselineProfile() =
        benchmarkStateChange(CompilationMode.Partial())

    /**
     * 执行状态变化基准测试
     */
    private fun benchmarkStateChange(compilationMode: CompilationMode) =
        benchmarkRule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(FrameTimingMetric()), // 帧时间指标
            compilationMode = compilationMode,
            iterations = 10, // 迭代次数
            startupMode = StartupMode.WARM, // 温启动模式
            setupBlock = {
                // 启动应用
                pressHome()
                startActivityAndAllowNotifications()
                // 导航到兴趣页面
                device.findObject(By.text("Interests")).click()
                device.waitForIdle()
            },
        ) {
            interestsWaitForTopics()
            repeat(3) {
                interestsScrollTopicsDownUp()
            }
        }
}
