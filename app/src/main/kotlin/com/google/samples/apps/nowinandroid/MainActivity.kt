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

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.metrics.performance.JankStats
import androidx.trace.trace
import com.google.samples.apps.nowinandroid.MainActivityUiState.Loading
import com.google.samples.apps.nowinandroid.core.analytics.AnalyticsHelper
import com.google.samples.apps.nowinandroid.core.analytics.LocalAnalyticsHelper
import com.google.samples.apps.nowinandroid.core.data.repository.UserNewsResourceRepository
import com.google.samples.apps.nowinandroid.core.data.util.NetworkMonitor
import com.google.samples.apps.nowinandroid.core.data.util.TimeZoneMonitor
import com.google.samples.apps.nowinandroid.core.designsystem.theme.NiaTheme
import com.google.samples.apps.nowinandroid.core.ui.LocalTimeZone
import com.google.samples.apps.nowinandroid.ui.NiaApp
import com.google.samples.apps.nowinandroid.ui.rememberNiaAppState
import com.google.samples.apps.nowinandroid.util.isSystemInDarkTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 应用的主 Activity，是整个应用的入口点
 * 负责：
 * 1. 管理应用的主题设置（深色/浅色主题、动态颜色等）
 * 2. 实现 Edge-to-Edge（沉浸式）状态栏和导航栏
 * 3. 处理启动屏幕（Splash Screen）
 * 4. 初始化 JankStats 用于性能监控
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /**
     * 延迟注入 [JankStats]，用于跟踪应用中的卡顿情况
     * JankStats 是一个性能监控工具，用于检测应用界面帧率问题
     */
    @Inject
    lateinit var lazyStats: dagger.Lazy<JankStats>

    /**
     * 网络状态监控器，用于检测设备是否联网
     */
    @Inject
    lateinit var networkMonitor: NetworkMonitor

    /**
     * 时区监控器，用于获取当前时区
     */
    @Inject
    lateinit var timeZoneMonitor: TimeZoneMonitor

    /**
     * 分析助手，用于记录用户行为分析
     */
    @Inject
    lateinit var analyticsHelper: AnalyticsHelper

    /**
     * 用户新闻资源仓库，用于管理用户的新闻数据和收藏
     */
    @Inject
    lateinit var userNewsResourceRepository: UserNewsResourceRepository

    /**
     * 主Activity的ViewModel，负责管理UI状态
     */
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 安装启动屏幕（解决冷启动白屏问题）
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 我们将其保持为可变状态，以便可以在组合中跟踪变化
        // 这允许我们响应深色/浅色模式的变化
        // 主题设置包括：深色主题、Android主题、动态颜色
        var themeSettings by mutableStateOf(
            ThemeSettings(
                darkTheme = resources.configuration.isSystemInDarkTheme,
                androidTheme = Loading.shouldUseAndroidTheme,
                disableDynamicTheming = Loading.shouldDisableDynamicTheming,
            ),
        )

        // 更新 UI 状态
        // 使用 lifecycleScope 在 Activity 生命周期内启动协程
        lifecycleScope.launch {
            // repeatOnLifecycle 确保在 STARTED 状态时运行，并在 STOPPED 时暂停
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    isSystemInDarkTheme(), // 系统深色模式状态流
                    viewModel.uiState, // ViewModel 的 UI 状态流
                ) { systemDark, uiState ->
                    // 根据用户偏好和系统设置组合主题设置
                    ThemeSettings(
                        darkTheme = uiState.shouldUseDarkTheme(systemDark),
                        androidTheme = uiState.shouldUseAndroidTheme,
                        disableDynamicTheming = uiState.shouldDisableDynamicTheming,
                    )
                }
                    .onEach { themeSettings = it } // 每次主题设置变化时更新
                    .map { it.darkTheme } // 只关心深色主题的变化
                    .distinctUntilChanged() // 避免重复的值触发
                    .collect { darkTheme ->
                        // 使用 trace 标记这段代码，便于性能分析
                        trace("niaEdgeToEdge") {
                            /**
                             * Edge-to-Edge（沉浸式）模式配置
                             *
                             * 关键点说明：
                             * 1. 关闭系统装饰窗口适配（decor fitting system windows），允许应用完全自定义绘制区域
                             * 2. 这样可以处理所有系统Insets，包括 IME（输入法）动画
                             * 3. 状态栏使用透明背景，通过 SystemBarStyle.auto 自动根据深色模式选择
                             *    - lightScrim: 浅色模式下的状态栏颜色（透明）
                             *    - darkScrim: 深色模式下的状态栏颜色（透明）
                             *    - 第三个 lambda 根据当前是否为深色模式返回对应的 scrim 颜色
                             * 4. 导航栏同样使用透明背景，深色模式下使用 darker 的背景色
                             *
                             * 与默认 enableEdgeToEdge 的区别：
                             * 这里我们手动根据 uiState（用户设置）来决定是否显示深色主题，
                             * 而不是简单地使用配置文件的深色主题值
                             */
                            enableEdgeToEdge(
                                // 状态栏样式：自动根据深色模式选择透明或深色
                                statusBarStyle = SystemBarStyle.auto(
                                    lightScrim = android.graphics.Color.TRANSPARENT, // 浅色模式：透明
                                    darkScrim = android.graphics.Color.TRANSPARENT, // 深色模式：透明
                                ) { darkTheme },
                                // 导航栏样式：自动根据深色模式选择浅色或深色遮罩
                                navigationBarStyle = SystemBarStyle.auto(
                                    lightScrim = lightScrim, // 浅色模式：白色遮罩 (alpha 0xe6)
                                    darkScrim = darkScrim,   // 深色模式：深灰色遮罩 (alpha 0x80)
                                ) { darkTheme },
                            )
                        }
                    }
            }
        }

        // 保持启动屏幕显示直到 UI 状态加载完成
        // 每次应用需要重绘时都会评估此条件，所以应该快速完成以避免阻塞 UI
        splashScreen.setKeepOnScreenCondition { viewModel.uiState.value.shouldKeepSplashScreen() }

        // 设置 Compose 内容
        setContent {
            // 创建应用状态，包含网络监控、用户数据仓库和时区监控
            val appState = rememberNiaAppState(
                networkMonitor = networkMonitor,
                userNewsResourceRepository = userNewsResourceRepository,
                timeZoneMonitor = timeZoneMonitor,
            )

            // 收集当前时区状态
            val currentTimeZone by appState.currentTimeZone.collectAsStateWithLifecycle()

            // 提供组合本地的依赖
            CompositionLocalProvider(
                LocalAnalyticsHelper provides analyticsHelper,
                LocalTimeZone provides currentTimeZone,
            ) {
                // 应用主题配置
                NiaTheme(
                    darkTheme = themeSettings.darkTheme,
                    androidTheme = themeSettings.androidTheme,
                    disableDynamicTheming = themeSettings.disableDynamicTheming,
                ) {
                    // 渲染主应用界面
                    NiaApp(appState)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 恢复时启用 JankStats 性能追踪
        lazyStats.get().isTrackingEnabled = true
    }

    override fun onPause() {
        super.onPause()
        // 暂停时禁用 JankStats 性能追踪
        lazyStats.get().isTrackingEnabled = false
    }
}

/**
 * 默认的浅色模式遮罩（scrim），由 androidx 和平台定义：
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=35-38;drc=27e7d52e8604a080133e8b842db10c89b4482598
 * 颜色值：0xe6 alpha 值，即白色带 90% 不透明度
 */
private val lightScrim = android.graphics.Color.argb(0xe6, 0xFF, 0xFF, 0xFF)

/**
 * 默认的深色模式遮罩（scrim），由 androidx 和平台定义：
 * https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:activity/activity/src/main/java/androidx/activity/EdgeToEdge.kt;l=40-44;drc=27e7d52e8604a080133e8b842db10c89b4482598
 * 颜色值：0x80 alpha 值，即深灰色(#1b1b1b)带 50% 不透明度
 */
private val darkScrim = android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b)

/**
 * 系统主题设置的封装类
 * 使用这个封装类可以将所有变化组合在一起，防止不必要的重组（recomposition）
 *
 * @param darkTheme 是否使用深色主题
 * @param androidTheme 是否使用 Android 品牌主题
 * @param disableDynamicTheming 是否禁用动态颜色（Material You）
 */
data class ThemeSettings(
    val darkTheme: Boolean,
    val androidTheme: Boolean,
    val disableDynamicTheming: Boolean,
)
