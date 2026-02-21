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

import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import com.google.samples.apps.nowinandroid.configureFlavors
import com.google.samples.apps.nowinandroid.configureGradleManagedDevices
import com.google.samples.apps.nowinandroid.configureKotlinAndroid
import com.google.samples.apps.nowinandroid.configurePrintApksTask
import com.google.samples.apps.nowinandroid.configureSpotlessForAndroid
import com.google.samples.apps.nowinandroid.disableUnnecessaryAndroidTests
import com.google.samples.apps.nowinandroid.isIsolatedProjectsEnabled
import com.google.samples.apps.nowinandroid.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.configuration.BuildFeatures
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import javax.inject.Inject

/**
 * Android 库Convention插件
 *
 * 此插件用于配置 Android 库模块，主要负责：
 * 1. 应用 com.android.library 插件
 * 2. 配置 Kotlin Android 扩展
 * 3. 设置目标 SDK 和 Lint 选项
 * 4. 配置产品风味（Flavors）
 * 5. 配置 Gradle 管理的设备
 * 6. 设置资源前缀（用于多模块项目）
 */
abstract class AndroidLibraryConventionPlugin : Plugin<Project> {
    @get:Inject abstract val buildFeatures: BuildFeatures
    override fun apply(target: Project) {
        with(target) {
            // 应用 Android 库插件
            apply(plugin = "com.android.library")
            // 应用自定义 Lint 插件
            apply(plugin = "nowinandroid.android.lint")

            // 配置库扩展
            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this) // 配置 Kotlin Android 扩展
                testOptions.targetSdk = 36 // 测试目标 SDK 版本
                lint.targetSdk = 36 // Lint 检查目标 SDK 版本
                // 设置默认测试 Instrumentation 运行器
                defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                testOptions.animationsDisabled = true // 禁用测试动画
                configureFlavors(this) // 配置产品风味
                configureGradleManagedDevices(this) // 配置 Gradle 管理的设备

                /**
                 * 资源前缀从模块名称派生，
                 * 因此 ":core:module1" 中的资源必须以 "core_module1_" 为前缀
                 * 例如：R.string.xxx -> R.string.core_module1_xxx
                 */
                resourcePrefix =
                    path.split("""\W""".toRegex()).drop(1).distinct().joinToString(separator = "_")
                        .lowercase() + "_"
            }

            // 配置 Android 组件扩展
            extensions.configure<LibraryAndroidComponentsExtension> {
                configurePrintApksTask(this) // 配置 APK 输出任务
                disableUnnecessaryAndroidTests(target) // 禁用不必要的 Android 测试
            }

            // 如果未启用隔离项目模式，则配置 Spotless 代码格式检查
            if (!buildFeatures.isIsolatedProjectsEnabled()) {
                configureSpotlessForAndroid()
            }

            // 配置公共依赖
            dependencies {
                // Android 测试依赖
                "androidTestImplementation"(libs.findLibrary("kotlin.test").get())
                // 本地测试依赖
                "testImplementation"(libs.findLibrary("kotlin.test").get())
                "testImplementation"(libs.findLibrary("junit").get())

                // 核心依赖
                "implementation"(libs.findLibrary("androidx.tracing.ktx").get())
            }
        }
    }
}
