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
 * distributed under the License is the "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.nowinandroid.MainActivityUiState.Loading
import com.google.samples.apps.nowinandroid.MainActivityUiState.Success
import com.google.samples.apps.nowinandroid.core.data.repository.UserDataRepository
import com.google.samples.apps.nowinandroid.core.model.data.DarkThemeConfig
import com.google.samples.apps.nowinandroid.core.model.data.ThemeBrand
import com.google.samples.apps.nowinandroid.core.model.data.UserData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * MainActivity 的 ViewModel
 *
 * 负责管理主界面的 UI 状态，特别是用户数据相关的主题设置
 * 使用 Hilt 进行依赖注入
 */
@HiltViewModel
class MainActivityViewModel @Inject constructor(
    userDataRepository: UserDataRepository,
) : ViewModel() {
    /**
     * 主界面 UI 状态流
     *
     * 从用户数据仓库获取用户数据，并转换为 UI 状态
     * 初始状态为 Loading，加载完成后转为 Success
     *
     * 使用 WhileSubscribed(5_000) 表示：
     * - 当有订阅者时开始收集数据
     * - 当最后一个订阅者取消后，5秒后停止收集
     * - 这有助于在配置变更（如屏幕旋转）时保持数据
     */
    val uiState: StateFlow<MainActivityUiState> = userDataRepository.userData.map {
        Success(it)
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading, // 初始状态为加载中
        started = SharingStarted.WhileSubscribed(5_000),
    )
}

/**
 * MainActivity 的 UI 状态密封接口
 *
 * 用于表示应用加载状态和用户数据
 */
sealed interface MainActivityUiState {
    /**
     * 加载状态
     * 表示用户数据正在加载中
     */
    data object Loading : MainActivityUiState

    /**
     * 成功状态，包含用户数据
     *
     * @property userData 用户数据对象，包含主题设置等信息
     */
    data class Success(val userData: UserData) : MainActivityUiState {
        /**
         * 是否应该禁用动态颜色（Material You）
         * 根据用户设置中的 useDynamicColor 决定
         */
        override val shouldDisableDynamicTheming = !userData.useDynamicColor

        /**
         * 是否应该使用 Android 品牌主题
         * 根据用户的主题品牌设置决定
         */
        override val shouldUseAndroidTheme: Boolean = when (userData.themeBrand) {
            ThemeBrand.DEFAULT -> false // 默认主题
            ThemeBrand.ANDROID -> true  // Android 品牌主题
        }

        /**
         * 是否应该使用深色主题
         *
         * @param isSystemDarkTheme 系统深色主题设置
         * @return 是否使用深色主题
         */
        override fun shouldUseDarkTheme(isSystemDarkTheme: Boolean) =
            when (userData.darkThemeConfig) {
                DarkThemeConfig.FOLLOW_SYSTEM -> isSystemDarkTheme // 跟随系统
                DarkThemeConfig.LIGHT -> false                    // 始终浅色
                DarkThemeConfig.DARK -> true                      // 始终深色
            }
    }

    /**
     * 返回 true 如果状态尚未加载完成，应该保持显示启动屏幕
     */
    fun shouldKeepSplashScreen() = this is Loading

    /**
     * 返回 true 如果动态颜色（Material You）被禁用
     * 默认返回 true（Loading 状态下禁用动态颜色）
     */
    val shouldDisableDynamicTheming: Boolean get() = true

    /**
     * 返回 true 如果应该使用 Android 品牌主题
     * 默认返回 false
     */
    val shouldUseAndroidTheme: Boolean get() = false

    /**
     * 返回 true 如果应该使用深色主题
     *
     * @param isSystemDarkTheme 系统深色主题设置
     * @return 默认跟随系统设置
     */
    fun shouldUseDarkTheme(isSystemDarkTheme: Boolean) = isSystemDarkTheme
}
