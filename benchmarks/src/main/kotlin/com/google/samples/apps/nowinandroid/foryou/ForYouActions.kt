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

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.untilHasChildren
import com.google.samples.apps.nowinandroid.flingElementDownUp
import com.google.samples.apps.nowinandroid.waitAndFindObject
import com.google.samples.apps.nowinandroid.waitForObjectOnTopAppBar
import org.junit.Assert.fail

/**
 * 等待"为你推荐"页面内容加载完成
 */
fun MacrobenchmarkScope.forYouWaitForContent() {
    // 等待内容加载完成（通过检查加载指示器是否消失）
    device.wait(Until.gone(By.res("loadingWheel")), 5_000)
    // 有时候加载指示器消失了，但内容还没加载完成
    // 所以我们在这里等待主题加载完成以确保
    val obj = device.waitAndFindObject(By.res("forYou:topicSelection"), 10_000)
    // 这里的超时时间比较大，因为有时候数据加载需要很长时间！
    obj.wait(untilHasChildren(), 60_000)
}

/**
 * 选择一些话题，这将显示它们的订阅源内容
 *
 * @param recheckTopicsIfChecked 话题可能已经从上一次迭代中选中了，是否重新检查
 */
fun MacrobenchmarkScope.forYouSelectTopics(recheckTopicsIfChecked: Boolean = false) {
    val topics = device.findObject(By.res("forYou:topicSelection"))

    // 设置手势边距，避免触发系统手势导航
    val horizontalMargin = 10 * topics.visibleBounds.width() / 100
    topics.setGestureMargins(horizontalMargin, 0, horizontalMargin, 0)

    // 选择一些话题以显示订阅源内容
    var index = 0
    var visited = 0

    while (visited < 3) {
        if (topics.childCount == 0) {
            fail("No topics found, can't generate profile for ForYou page.")
        }
        // 选择话题，这将填充订阅源中的项目
        val topic = topics.children[index % topics.childCount]
        // 找到可勾选的元素以确定它是否被选中
        val topicCheckIcon = topic.findObject(By.checkable(true))
        // 如果话题图标不在屏幕可见范围内，可能为 null
        // 如果是这种情况，我们尝试另一个索引
        if (topicCheckIcon == null) {
            index++
            continue
        }

        when {
            // 话题未被选中，执行选中
            !topicCheckIcon.isChecked -> {
                topic.click()
                device.waitForIdle()
            }

            // 话题已经被选中且我们想要重新检查，执行两次点击
            recheckTopicsIfChecked -> {
                repeat(2) {
                    topic.click()
                    device.waitForIdle()
                }
            }

            else -> {
                // 话题已被选中，但我们不重新检查
            }
        }

        index++
        visited++
    }
}

/**
 * 在"为你推荐"订阅源中向下向上滚动
 */
fun MacrobenchmarkScope.forYouScrollFeedDownUp() {
    val feedList = device.findObject(By.res("forYou:feed"))
    device.flingElementDownUp(feedList)
}

/**
 * 设置应用主题
 *
 * @param isDark 是否为深色主题
 */
fun MacrobenchmarkScope.setAppTheme(isDark: Boolean) {
    when (isDark) {
        true -> device.findObject(By.text("Dark")).click()   // 点击深色模式
        false -> device.findObject(By.text("Light")).click() // 点击浅色模式
    }
    device.waitForIdle()
    device.findObject(By.text("OK")).click() // 确认选择

    // 等待顶部应用栏在屏幕上可见
    waitForObjectOnTopAppBar(By.text("Now in Android"))
}
