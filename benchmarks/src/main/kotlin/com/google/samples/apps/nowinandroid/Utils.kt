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

import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.google.samples.apps.nowinandroid.benchmarks.BuildConfig
import java.io.ByteArrayOutputStream

/**
 * 便捷参数，用于根据构建类型和构建风味使用正确的包名
 */
val PACKAGE_NAME = buildString {
    append("com.google.samples.apps.nowinandroid")
    append(BuildConfig.APP_FLAVOR_SUFFIX)
}

/**
 * 对元素执行向下向上快速滑动（fling）操作
 *
 * @param element 要滑动的元素
 */
fun UiDevice.flingElementDownUp(element: UiObject2) {
    // 设置一些边距以防止触发系统导航
    element.setGestureMargin(displayWidth / 5)

    element.fling(Direction.DOWN) // 向下快速滑动
    waitForIdle()
    element.fling(Direction.UP)   // 向上快速滑动
}

/**
 * 等待指定选择器的对象在屏幕上可见，并返回该对象
 *
 * @param selector 用于查找元素的选择器
 * @param timeout 超时时间（毫秒）
 * @return 找到的 UI 对象
 * @throws AssertionError 如果在超时时间内未找到元素
 */
fun UiDevice.waitAndFindObject(selector: BySelector, timeout: Long): UiObject2 {
    if (!wait(Until.hasObject(selector), timeout)) {
        throw AssertionError("Element not found on screen in ${timeout}ms (selector=$selector)")
    }

    return findObject(selector)
}

/**
 * 辅助函数：将窗口层级结构转储为字符串
 *
 * @return 窗口层级结构的字符串表示
 */
fun UiDevice.dumpWindowHierarchy(): String {
    val buffer = ByteArrayOutputStream()
    dumpWindowHierarchy(buffer)
    return buffer.toString()
}
