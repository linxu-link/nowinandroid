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

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.google.samples.apps.nowinandroid.configureBadgingTasks
import com.google.samples.apps.nowinandroid.configureGradleManagedDevices
import com.google.samples.apps.nowinandroid.configureKotlinAndroid
import com.google.samples.apps.nowinandroid.configurePrintApksTask
import com.google.samples.apps.nowinandroid.configureSpotlessForAndroid
import com.google.samples.apps.nowinandroid.isIsolatedProjectsEnabled
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.configuration.BuildFeatures
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import javax.inject.Inject

/**
 * Android 应用Convention插件
 *
 * 此插件用于配置 Android 应用模块，主要负责：
 * 1. 应用 com.android.application 插件
 * 2. 配置 Kotlin Android 扩展
 * 3. 设置目标 SDK 和测试选项
 * 4. 配置 Gradle 管理的设备用于测试
 * 5. 配置代码格式检查（Spotless）
 */
abstract class AndroidApplicationConventionPlugin : Plugin<Project> {
    @get:Inject abstract val buildFeatures: BuildFeatures
    override fun apply(target: Project) {
        with(target) {
            // 应用 Android 应用插件
            apply(plugin = "com.android.application")
            // 应用自定义 Lint 插件
            apply(plugin = "nowinandroid.android.lint")
            // 应用 Dropbox 依赖Guard插件（用于追踪依赖）
            apply(plugin = "com.dropbox.dependency-guard")

            // 配置应用扩展
            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this) // 配置 Kotlin Android 扩展
                defaultConfig.targetSdk = 36 // 设置目标 SDK 版本
                testOptions.animationsDisabled = true // 禁用测试动画以加快测试速度
                configureGradleManagedDevices(this) // 配置 Gradle 管理的测试设备
            }

            // 配置 Android 组件扩展
            extensions.configure<ApplicationAndroidComponentsExtension> {
                configurePrintApksTask(this) // 配置 APK 输出任务
                configureBadgingTasks(this) // 配置应用信息任务
            }

            // 如果未启用隔离项目模式，则配置 Spotless 代码格式检查
            if (!buildFeatures.isIsolatedProjectsEnabled()) {
                configureSpotlessForAndroid()
            }
        }
    }
}
