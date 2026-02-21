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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.google.samples.apps.nowinandroid.R
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaBackground
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaGradientBackground
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaNavigationSuiteScaffold
import com.google.samples.apps.nowinandroid.core.designsystem.component.NiaTopAppBar
import com.google.samples.apps.nowinandroid.core.designsystem.icon.NiaIcons
import com.google.samples.apps.nowinandroid.core.designsystem.theme.GradientColors
import com.google.samples.apps.nowinandroid.core.designsystem.theme.LocalGradientColors
import com.google.samples.apps.nowinandroid.core.navigation.Navigator
import com.google.samples.apps.nowinandroid.core.navigation.toEntries
import com.google.samples.apps.nowinandroid.feature.bookmarks.impl.navigation.LocalSnackbarHostState
import com.google.samples.apps.nowinandroid.feature.bookmarks.impl.navigation.bookmarksEntry
import com.google.samples.apps.nowinandroid.feature.foryou.api.navigation.ForYouNavKey
import com.google.samples.apps.nowinandroid.feature.foryou.impl.navigation.forYouEntry
import com.google.samples.apps.nowinandroid.feature.interests.impl.navigation.interestsEntry
import com.google.samples.apps.nowinandroid.feature.search.api.navigation.SearchNavKey
import com.google.samples.apps.nowinandroid.feature.search.impl.navigation.searchEntry
import com.google.samples.apps.nowinandroid.feature.settings.impl.SettingsDialog
import com.google.samples.apps.nowinandroid.feature.topic.impl.navigation.topicEntry
import com.google.samples.apps.nowinandroid.navigation.TOP_LEVEL_NAV_ITEMS
import com.google.samples.apps.nowinandroid.feature.settings.impl.R as settingsR

/**
 * NiaApp 主界面组件 - 外层包装
 *
 * 负责处理：
 * 1. 背景颜色/渐变显示
 * 2. 网络状态提示（离线时显示 Snackbar）
 * 3. 设置对话框的显示
 *
 * @param appState 应用状态，包含导航、网络监控等信息
 * @param windowAdaptiveInfo 窗口自适应信息，用于适配不同屏幕尺寸
 */
@Composable
fun NiaApp(
    appState: NiaAppState,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    // 判断是否显示渐变背景（仅在"为你推荐"页面显示）
    val shouldShowGradientBackground = appState.navigationState.currentTopLevelKey == ForYouNavKey
    // 设置对话框显示状态
    var showSettingsDialog by rememberSaveable { mutableStateOf(false) }

    // 基础背景层
    NiaBackground(modifier = modifier) {
        // 渐变背景层（根据是否在"为你推荐"页面显示不同背景）
        NiaGradientBackground(
            gradientColors = if (shouldShowGradientBackground) {
                LocalGradientColors.current // 使用渐变色
            } else {
                GradientColors() // 使用普通颜色
            },
        ) {
            // 创建 Snackbar 状态管理器
            val snackbarHostState = remember { SnackbarHostState() }

            // 收集网络状态
            val isOffline by appState.isOffline.collectAsStateWithLifecycle()

            // 如果用户未连接到互联网，显示 Snackbar 通知
            val notConnectedMessage = stringResource(R.string.not_connected)
            LaunchedEffect(isOffline) {
                if (isOffline) {
                    snackbarHostState.showSnackbar(
                        message = notConnectedMessage,
                        duration = Indefinite, // 持续显示直到用户操作
                    )
                }
            }

            // 提供 Snackbar 状态给子组件使用
            CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
                // 渲染内部应用界面
                NiaApp(
                    appState = appState,

                    // TODO: 设置应该是一个对话框屏幕
                    showSettingsDialog = showSettingsDialog,
                    onSettingsDismissed = { showSettingsDialog = false },
                    onTopAppBarActionClick = { showSettingsDialog = true },
                    windowAdaptiveInfo = windowAdaptiveInfo,
                )
            }
        }
    }
}

/**
 * NiaApp 主界面组件 - 内部实现
 *
 * 负责处理：
 * 1. 顶部应用栏（TopAppBar）
 * 2. 底部导航栏（NavigationSuite）
 * 3. 内容区域（NavHost）
 * 4. 系统Insets处理（状态栏、导航栏、IME）
 *
 * @param appState 应用状态
 * @param showSettingsDialog 是否显示设置对话框
 * @param onSettingsDismissed 关闭设置对话框的回调
 * @param onTopAppBarActionClick 顶部栏操作按钮点击回调
 * @param windowAdaptiveInfo 窗口自适应信息
 */
@Composable
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class,
    ExperimentalMaterial3AdaptiveApi::class,
)
internal fun NiaApp(
    appState: NiaAppState,
    showSettingsDialog: Boolean,
    onSettingsDismissed: () -> Unit,
    onTopAppBarActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
) {
    // 收集有未读资源的一级导航项
    val unreadNavKeys by appState.topLevelNavKeysWithUnreadResources
        .collectAsStateWithLifecycle()

    // 如果显示设置对话框
    if (showSettingsDialog) {
        SettingsDialog(
            onDismiss = { onSettingsDismissed() },
        )
    }

    // 获取 Snackbar 状态
    val snackbarHostState = LocalSnackbarHostState.current

    // 创建导航器，用于页面跳转
    val navigator = remember { Navigator(appState.navigationState) }

    /**
     * 导航脚手架（Scaffold）
     * 根据窗口大小自适应显示底部或侧边导航栏
     */
    NiaNavigationSuiteScaffold(
        navigationSuiteItems = {
            // 遍历所有一级导航项
            TOP_LEVEL_NAV_ITEMS.forEach { (navKey, navItem) ->
                val hasUnread = unreadNavKeys.contains(navKey) // 是否有未读
                val selected = navKey == appState.navigationState.currentTopLevelKey // 是否选中
                item(
                    selected = selected,
                    onClick = { navigator.navigate(navKey) },
                    icon = {
                        Icon(
                            imageVector = navItem.unselectedIcon,
                            contentDescription = null,
                        )
                    },
                    selectedIcon = {
                        Icon(
                            imageVector = navItem.selectedIcon,
                            contentDescription = null,
                        )
                    },
                    label = { Text(stringResource(navItem.iconTextId)) },
                    modifier = Modifier
                        .testTag("NiaNavItem")
                        .then(if (hasUnread) Modifier.notificationDot() else Modifier), // 未读红点
                )
            }
        },
        windowAdaptiveInfo = windowAdaptiveInfo,
    ) {
        /**
         * 主 Scaffold
         * 注意：contentWindowInsets 设置为 0，因为我们在内部手动处理了 Insets
         * containerColor = Color.Transparent 允许背景渐变透过显示
         */
        Scaffold(
            modifier = modifier.semantics {
                testTagsAsResourceId = true
            },
            containerColor = Color.Transparent, // 背景透明，显示渐变背景
            contentColor = MaterialTheme.colorScheme.onBackground,
            contentWindowInsets = WindowInsets(0, 0, 0, 0), // 手动处理 Insets
            snackbarHost = {
                // Snackbar 容器，添加 IME 排除的 Insets 填充
                // 这样当键盘弹出时，Snackbar 不会跟随键盘上移
                SnackbarHost(
                    snackbarHostState,
                    modifier = Modifier.windowInsetsPadding(
                        WindowInsets.safeDrawing.exclude(
                            WindowInsets.ime, // 排除键盘区域
                        ),
                    ),
                )
            },
        ) { padding ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .consumeWindowInsets(padding) // 消费 Scaffold 提供的 padding
                    .windowInsetsPadding(
                        // 仅在水平方向添加安全绘制区域填充
                        // 这样可以避免内容与屏幕边缘重叠
                        WindowInsets.safeDrawing.only(
                            WindowInsetsSides.Horizontal,
                        ),
                    ),
            ) {
                // 仅在一级导航页面显示顶部应用栏
                var shouldShowTopAppBar = false

                if (appState.navigationState.currentKey in appState.navigationState.topLevelKeys) {
                    shouldShowTopAppBar = true

                    // 获取当前导航项信息
                    val destination = TOP_LEVEL_NAV_ITEMS[appState.navigationState.currentTopLevelKey]
                        ?: error("Top level nav item not found for ${appState.navigationState.currentTopLevelKey}")

                    // 渲染顶部应用栏
                    NiaTopAppBar(
                        titleRes = destination.titleTextId,
                        navigationIcon = NiaIcons.Search,
                        navigationIconContentDescription = stringResource(
                            id = settingsR.string.feature_settings_impl_top_app_bar_navigation_icon_description,
                        ),
                        actionIcon = NiaIcons.Settings,
                        actionIconContentDescription = stringResource(
                            id = settingsR.string.feature_settings_impl_top_app_bar_action_icon_description,
                        ),
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent, // 透明背景
                        ),
                        onActionClick = { onTopAppBarActionClick() },
                        onNavigationClick = { navigator.navigate(SearchNavKey) },
                    )
                }

                // 内容区域
                Box(
                    // 解决 Issue: https://issuetracker.google.com/338478720
                    // 消费顶部安全绘制区域，确保内容不被状态栏遮挡
                    modifier = Modifier.consumeWindowInsets(
                        if (shouldShowTopAppBar) {
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
                        } else {
                            WindowInsets(0, 0, 0, 0)
                        },
                    ),
                ) {
                    // 使用列表详情策略（在大屏幕设备上支持分屏显示）
                    val listDetailStrategy = rememberListDetailSceneStrategy<NavKey>()

                    // 创建导航入口提供器
                    val entryProvider = entryProvider {
                        forYouEntry(navigator)
                        bookmarksEntry(navigator)
                        interestsEntry(navigator)
                        topicEntry(navigator)
                        searchEntry(navigator)
                    }

                    // 导航显示组件
                    NavDisplay(
                        entries = appState.navigationState.toEntries(entryProvider),
                        sceneStrategy = listDetailStrategy,
                        onBack = { navigator.goBack() },
                    )
                }

                // TODO: 当 Snackbar 显示时，可能需要添加内边距或间隔符，
                // 以防止内容显示在它后面
            }
        }
    }
}

/**
 * 未读消息红点修饰器
 *
 * 在导航项图标上绘制一个小型指示器圆点，表示有未读内容
 * 该圆点的位置基于 NavigationBar 的 "indicator pill" 尺寸
 */
private fun Modifier.notificationDot(): Modifier =
    composed {
        val tertiaryColor = MaterialTheme.colorScheme.tertiary
        drawWithContent {
            drawContent()
            // 绘制圆形指示器
            drawCircle(
                tertiaryColor,
                radius = 5.dp.toPx(),
                // 基于 NavigationBar 的 "indicator pill" 尺寸计算位置
                // (NavigationBarTokens.ActiveIndicatorWidth = 64.dp)
                // 由于这些参数是私有的，我们只能隐式依赖它们
                center = center + Offset(
                    64.dp.toPx() * .45f,  // 水平偏移
                    32.dp.toPx() * -.45f - 6.dp.toPx(), // 垂直偏移
                ),
            )
        }
    }
