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

package com.google.samples.apps.nowinandroid.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.client.api.Vendor
import com.android.tools.lint.detector.api.CURRENT_API
import com.google.samples.apps.nowinandroid.lint.designsystem.DesignSystemDetector

/**
 * Now in Android 的自定义 Lint 问题注册表
 *
 * 注册项目中使用的自定义 Lint 检查规则
 */
class NiaIssueRegistry : IssueRegistry() {

    /**
     * 获取所有注册的问题列表
     */
    override val issues = listOf(
        DesignSystemDetector.ISSUE, // 设计系统使用检查
        TestMethodNameDetector.FORMAT, // 测试方法格式检查
        TestMethodNameDetector.PREFIX, // 测试方法前缀检查
    )

    // 当前 API 版本
    override val api: Int = CURRENT_API

    // 最低支持的 API 级别
    override val minApi: Int = 12

    // 供应商信息
    override val vendor: Vendor = Vendor(
        vendorName = "Now in Android",
        feedbackUrl = "https://github.com/android/nowinandroid/issues",
        contact = "https://github.com/android/nowinandroid",
    )
}
