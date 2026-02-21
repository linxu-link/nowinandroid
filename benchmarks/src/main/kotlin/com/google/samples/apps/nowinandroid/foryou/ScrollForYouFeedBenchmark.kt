/*
 * Copyright 2022 The Android Open Source Project
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

package com.google.samples.apps.nowinandroid.foryou

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.google.samples.apps.nowinandroid.PACKAGE_NAME
import com.google.samples.apps.nowinandroid.startActivityAndAllowNotifications
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * "为你推荐"订阅源滚动基准测试
 *
 * 测试在不同编译模式下滚动"为你推荐"页面的性能
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class ScrollForYouFeedBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * 测试无编译模式下的滚动性能
     */
    @Test
    fun scrollFeedCompilationNone() = scrollFeed(CompilationMode.None())

    /**
     * 测试基线配置文件模式下的滚动性能
     */
    @Test
    fun scrollFeedCompilationBaselineProfile() = scrollFeed(CompilationMode.Partial())

    /**
     * 测试完全编译模式下的滚动性能
     */
    @Test
    fun scrollFeedCompilationFull() = scrollFeed(CompilationMode.Full())

    /**
     * 执行滚动基准测试
     *
     * @param compilationMode 编译模式
     */
    private fun scrollFeed(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()), // 帧时间指标
        compilationMode = compilationMode,
        iterations = 10, // 迭代次数
        startupMode = StartupMode.WARM, // 温启动模式
        setupBlock = {
            // 启动应用
            pressHome()
            startActivityAndAllowNotifications()
        },
    ) {
        // 等待内容加载
        forYouWaitForContent()
        // 选择话题
        forYouSelectTopics()
        // 滚动订阅源
        forYouScrollFeedDownUp()
    }
}
