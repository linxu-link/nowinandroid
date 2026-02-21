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

package com.google.samples.apps.nowinandroid.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation3.runtime.NavKey
import com.google.samples.apps.nowinandroid.core.data.repository.UserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.data.util.NetworkMonitor
import com.google.samples.apps.nowinandroid.core.data.util.TimeZoneMonitor
import com.google.samples.apps.nowinandroid.core.navigation.NavigationState
import com.google.samples.apps.nowinandroid.core.navigation.rememberNavigationState
import com.google.samples.apps.nowinandroid.core.ui.TrackDisposableJank
import com.google.samples.apps.nowinandroid.feature.bookmarks.api.navigation.BookmarksNavKey
import com.google.samples.apps.nowinandroid.feature.foryou.api.navigation.ForYouNavKey
import com.google.samples.apps.nowinandroid.navigation.TOP_LEVEL_NAV_ITEMS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone

/**
 * 创建并记住 [NiaAppState] 的实例
 *
 * 这是创建应用状态的入口点，使用 remember 确保状态在重组时保持一致
 *
 * @param networkMonitor 网络监控器
 * @param userNewsResourceRepository 用户新闻资源仓库
 * @param timeZoneMonitor 时区监控器
 * @param coroutineScope 协程作用域，默认为 rememberCoroutineScope
 * @return NiaAppState 实例
 */
@Composable
fun rememberNiaAppState(
    networkMonitor: NetworkMonitor,
    userNewsResourceRepository: UserNewsResourceRepository,
    timeZoneMonitor: TimeZoneMonitor,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): NiaAppState {
    // 创建导航状态，使用 ForYouNavKey 作为起始导航项
    val navigationState = rememberNavigationState(ForYouNavKey, TOP_LEVEL_NAV_ITEMS.keys)

    // 启动导航追踪副作用
    NavigationTrackingSideEffect(navigationState)

    // 记住并返回 NiaAppState 实例
    return remember(
        navigationState,
        coroutineScope,
        networkMonitor,
        userNewsResourceRepository,
        timeZoneMonitor,
    ) {
        NiaAppState(
            navigationState = navigationState,
            coroutineScope = coroutineScope,
            networkMonitor = networkMonitor,
            userNewsResourceRepository = userNewsResourceRepository,
            timeZoneMonitor = timeZoneMonitor,
        )
    }
}

/**
 * 应用状态管理器
 *
 * 负责管理：
 * 1. 导航状态
 * 2. 网络连接状态
 * 3. 未读资源追踪
 * 4. 时区信息
 *
 * 使用 @Stable 注解告诉 Compose 此类的属性不会频繁变化，有助于优化重组
 */
@Stable
class NiaAppState(
    val navigationState: NavigationState,
    coroutineScope: CoroutineScope,
    networkMonitor: NetworkMonitor,
    userNewsResourceRepository: UserNewsResourceRepository,
    timeZoneMonitor: TimeZoneMonitor,
) {
    /**
     * 网络是否离线
     * 通过 map(Boolean::not) 将在线状态反转
     * 使用 WhileSubscribed(5_000) 表示当没有订阅者时，5秒后停止收集
     * 初始值为 false（假设在线）
     */
    val isOffline = networkMonitor.isOnline
        .map(Boolean::not)
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false,
        )

    /**
     * 包含未读新闻资源的一级导航键集合
     *
     * 逻辑说明：
     * 1. 观察所有已关注话题的新闻资源
     * 2. 观察所有已收藏的新闻资源
     * 3. 如果 ForYou 中有任何未读资源，添加 ForYouNavKey
     * 4. 如果收藏中有任何未读资源，添加 BookmarksNavKey
     */
    val topLevelNavKeysWithUnreadResources: StateFlow<Set<NavKey>> =
        userNewsResourceRepository.observeAllForFollowedTopics()
            .combine(userNewsResourceRepository.observeAllBookmarked()) { forYouNewsResources, bookmarkedNewsResources ->
                setOfNotNull(
                    // 如果有未读的新闻资源，则包含 ForYouNavKey
                    ForYouNavKey.takeIf { forYouNewsResources.any { !it.hasBeenViewed } },
                    // 如果有未读的收藏资源，则包含 BookmarksNavKey
                    BookmarksNavKey.takeIf { bookmarkedNewsResources.any { !it.hasBeenViewed } },
                )
            }
            .stateIn(
                coroutineScope,
                SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet(),
            )

    /**
     * 当前时区
     * 默认使用系统当前时区
     */
    val currentTimeZone = timeZoneMonitor.currentTimeZone
        .stateIn(
            coroutineScope,
            SharingStarted.WhileSubscribed(5_000),
            TimeZone.currentSystemDefault(),
        )
}

/**
 * 存储导航事件信息，用于 JankStats 性能追踪
 *
 * 这是一个副作用组件，用于在导航状态变化时更新 JankStats
 * 使性能监控工具能够正确追踪导航相关的卡顿
 */
@Composable
private fun NavigationTrackingSideEffect(navigationState: NavigationState) {
    TrackDisposableJank(navigationState.currentKey) { metricsHolder ->
        // 将当前导航键存储到 JankStats 状态中
        metricsHolder.state?.putState("Navigation", navigationState.currentKey.toString())
        onDispose {} // 清理函数，这里为空
    }
}
