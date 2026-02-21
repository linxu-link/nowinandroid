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

import androidx.benchmark.macro.junit4.BaselineProfileRule
import com.google.samples.apps.nowinandroid.PACKAGE_NAME
import com.google.samples.apps.nowinandroid.foryou.forYouScrollFeedDownUp
import com.google.samples.apps.nowinandroid.foryou.forYouSelectTopics
import com.google.samples.apps.nowinandroid.foryou.forYouWaitForContent
import com.google.samples.apps.nowinandroid.startActivityAndAllowNotifications
import org.junit.Rule
import org.junit.Test

/**
 * "为你推荐"页面的基线配置文件生成
 *
 * 用于生成优化应用启动和运行时性能的基线配置文件
 */
class ForYouBaselineProfile {
    @get:Rule val baselineProfileRule = BaselineProfileRule()

    /**
     * 生成基线配置文件
     *
     * 收集典型用户旅程的跟踪数据，用于生成优化的基线配置文件
     */
    @Test
    fun generate() =
        baselineProfileRule.collect(PACKAGE_NAME) {
            startActivityAndAllowNotifications()

            // 滚动订阅源的关键用户旅程
            forYouWaitForContent()
            forYouSelectTopics(true)
            forYouScrollFeedDownUp()
        }
}
