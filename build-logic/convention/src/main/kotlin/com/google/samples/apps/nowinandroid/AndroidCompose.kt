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

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * 配置 Compose 特定的选项
 *
 * 此函数负责：
 * 1. 启用 Compose 构建特性
 * 2. 配置 Compose BOM 依赖
 * 3. 配置 Compose 编译器指标和报告
 * 4. 配置稳定性配置文件
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension,
) {
    commonExtension.apply {
        // 启用 Compose 支持
        buildFeatures.apply {
            compose = true
        }

        // 配置 Compose 依赖
        dependencies {
            // 获取 Compose BOM (Bill of Materials) 用于版本管理
            val bom = libs.findLibrary("androidx-compose-bom").get()
            "implementation"(platform(bom)) // 应用 BOM 到项目
            "androidTestImplementation"(platform(bom)) // Android 测试使用 BOM
            "implementation"(libs.findLibrary("androidx-compose-ui-tooling-preview").get()) // Compose UI 预览
            "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get()) // 调试工具
        }
    }

    // 配置 Compose 编译器扩展
    extensions.configure<ComposeCompilerGradlePluginExtension> {
        // 仅当属性值为 true 时才启用
        fun Provider<String>.onlyIfTrue() = flatMap { provider { it.takeIf(String::toBoolean) } }
        // 获取相对于根项目的目录
        fun Provider<*>.relativeToRootProject(dir: String) = map {
            @Suppress("UnstableApiUsage")
            isolated.rootProject.projectDirectory
                .dir("build")
                .dir(projectDir.toRelativeString(rootDir))
        }.map { it.dir(dir) }

        // 配置 Compose 编译器指标输出目录
        // 使用方法：./gradlew assembleRelease -PenableComposeCompilerMetrics=true
        project.providers.gradleProperty("enableComposeCompilerMetrics").onlyIfTrue()
            .relativeToRootProject("compose-metrics")
            .let(metricsDestination::set)

        // 配置 Compose 编译器报告输出目录
        // 使用方法：./gradlew assembleRelease -PenableComposeCompilerReports=true
        project.providers.gradleProperty("enableComposeCompilerReports").onlyIfTrue()
            .relativeToRootProject("compose-reports")
            .let(reportsDestination::set)

        // 添加稳定性配置文件
        // 用于告诉编译器哪些类是稳定的，从而优化重组
        @Suppress("UnstableApiUsage")
        stabilityConfigurationFiles
            .add(isolated.rootProject.projectDirectory.file("compose_compiler_config.conf"))
    }
}
