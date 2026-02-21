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

package com.google.samples.apps.nowinandroid.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import com.google.samples.apps.nowinandroid.PACKAGE_NAME
import com.google.samples.apps.nowinandroid.startActivityAndAllowNotifications
import org.junit.Rule
import org.junit.Test

/**
 * 应用启动的基线配置文件
 *
 * 此配置文件还通过 `includeInStartupProfile` 参数启用了
 * [Dex 布局优化](https://developer.android.com/topic/performance/baselineprofiles/dex-layout-optimizations)
 */
class StartupBaselineProfile {
    @get:Rule val baselineProfileRule = BaselineProfileRule()

    /**
     * 生成启动基线配置文件
     *
     * 收集应用启动时的跟踪数据，用于优化首次启动性能
     */
    @Test
    fun generate() = baselineProfileRule.collect(
        PACKAGE_NAME,
        includeInStartupProfile = true, // 包含在启动配置文件中
        profileBlock = MacrobenchmarkScope::startActivityAndAllowNotifications,
    )
}
