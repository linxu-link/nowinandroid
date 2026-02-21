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

package com.google.samples.apps.nowinandroid.startup

import androidx.benchmark.macro.BaselineProfileMode.Disable
import androidx.benchmark.macro.BaselineProfileMode.Require
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.StartupMode.COLD
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import com.google.samples.apps.nowinandroid.BaselineProfileMetrics
import com.google.samples.apps.nowinandroid.PACKAGE_NAME
import com.google.samples.apps.nowinandroid.allowNotifications
import com.google.samples.apps.nowinandroid.foryou.forYouWaitForContent
import com.google.samples.apps.nowinandroid.startActivityAndAllowNotifications
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 启用从各种基线配置文件或 [CompilationMode] 状态的应用程序启动。
 * 从 Studio 运行此基准测试以查看启动测量，以及用于调查应用程序在冷启动状态下性能的捕获系统跟踪。
 */
@RunWith(AndroidJUnit4ClassRunner::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    /**
     * 测试无预编译的启动性能
     */
    @Test
    fun startupWithoutPreCompilation() = startup(CompilationMode.None())

    /**
     * 测试部分编译且禁用基线配置文件的启动性能
     */
    @Test
    fun startupWithPartialCompilationAndDisabledBaselineProfile() = startup(
        CompilationMode.Partial(baselineProfileMode = Disable, warmupIterations = 1),
    )

    /**
     * 测试使用基线配置文件预编译的启动性能
     */
    @Test
    fun startupPrecompiledWithBaselineProfile() =
        startup(CompilationMode.Partial(baselineProfileMode = Require))

    /**
     * 测试完全预编译的启动性能
     */
    @Test
    fun startupFullyPrecompiled() = startup(CompilationMode.Full())

    /**
     * 执行启动基准测试
     *
     * @param compilationMode 编译模式
     */
    private fun startup(compilationMode: CompilationMode) = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = BaselineProfileMetrics.allMetrics,
        compilationMode = compilationMode,
        // 更多的迭代次数会导致更高的统计显著性
        iterations = 20,
        startupMode = COLD, // 冷启动模式
        setupBlock = {
            // 按Home键返回主屏幕
            pressHome()
            // 允许通知权限
            allowNotifications()
        },
    ) {
        // 启动活动并允许通知
        startActivityAndAllowNotifications()
        // 等待内容准备好以捕获 Time To Full Display（完全显示时间）
        forYouWaitForContent()
    }
}
