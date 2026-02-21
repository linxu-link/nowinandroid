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

package com.google.samples.apps.nowinandroid

/**
 * 构建类型枚举
 *
 * 在 :app 和 :benchmarks 模块之间共享，用于提供配置的类型安全性
 *
 * - DEBUG: 调试构建，包含调试信息，启用调试功能
 * - RELEASE: 发布构建，经过优化，用于发布到应用商店
 *
 * @param applicationIdSuffix 应用ID后缀，用于区分调试和发布版本
 */
enum class NiaBuildType(val applicationIdSuffix: String? = null) {
    DEBUG(".debug"), // 调试构建，应用ID后缀为 .debug
    RELEASE,        // 发布构建，无后缀
}
