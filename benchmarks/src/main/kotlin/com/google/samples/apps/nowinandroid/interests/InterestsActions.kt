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

package com.google.samples.apps.nowinandroid.interests

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.google.samples.apps.nowinandroid.flingElementDownUp
import com.google.samples.apps.nowinandroid.waitForObjectOnTopAppBar

/**
 * 导航到"兴趣"页面
 */
fun MacrobenchmarkScope.goToInterestsScreen() {
    device.findObject(By.text("Interests")).click() // 点击"兴趣"导航项
    device.waitForIdle()
    // 等待兴趣页面显示在屏幕上
    waitForObjectOnTopAppBar(By.text("Interests"))

    // 等待内容加载完成（通过检查加载指示器是否消失）
    device.wait(Until.gone(By.res("loadingWheel")), 5_000)
}

/**
 * 在话题列表中向下向上滚动
 */
fun MacrobenchmarkScope.interestsScrollTopicsDownUp() {
    device.wait(Until.hasObject(By.res("interests:topics")), 5_000)
    val topicsList = device.findObject(By.res("interests:topics"))
    device.flingElementDownUp(topicsList)
}

/**
 * 等待话题加载完成
 */
fun MacrobenchmarkScope.interestsWaitForTopics() {
    device.wait(Until.hasObject(By.text("Accessibility")), 30_000)
}

/**
 * 切换话题的收藏状态
 */
fun MacrobenchmarkScope.interestsToggleBookmarked() {
    val topicsList = device.findObject(By.res("interests:topics"))
    val checkable = topicsList.findObject(By.checkable(true))
    checkable.click()
    device.waitForIdle()
}
