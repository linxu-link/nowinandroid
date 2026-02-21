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

package com.google.samples.apps.nowinandroid.util

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.core.util.Consumer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 检查深色模式是否启用的便捷包装属性
 *
 * 通过检查配置的 uiMode 与 UI_MODE_NIGHT_MASK 的比较来确定
 *
 * @return true 如果系统处于深色模式，false 否则
 */
val Configuration.isSystemInDarkTheme
    get() = (uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

/**
 * 注册配置变化监听器，以获取系统是否处于深色模式
 *
 * 这是一个协程 Flow，返回系统深色模式状态的变化
 *
 * 工作原理：
 * 1. 立即发送当前值（订阅时）
 * 2. 注册配置变化监听器
 * 3. 当配置变化时，发送新的深色模式值
 * 4. 当流关闭时，移除监听器
 *
 * 使用示例：
 * ```
 * isSystemInDarkTheme().collect { isDark ->
 *     // 处理深色模式变化
 * }
 * ```
 *
 * @return Flow<Boolean> 深色模式状态流
 */
fun ComponentActivity.isSystemInDarkTheme() = callbackFlow {
    // 立即发送当前值
    channel.trySend(resources.configuration.isSystemInDarkTheme)

    // 创建配置变化监听器
    val listener = Consumer<Configuration> {
        channel.trySend(it.isSystemInDarkTheme)
    }

    // 注册监听器
    addOnConfigurationChangedListener(listener)

    // 当流关闭时，移除监听器
    awaitClose { removeOnConfigurationChangedListener(listener) }
}
    .distinctUntilChanged() // 过滤重复值
    .conflate() // 合并快速发送的值，避免积压
