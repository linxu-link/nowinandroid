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
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * 配置 Android 项目的 Kotlin 选项
 *
 * 此函数负责：
 * 1. 设置编译 SDK 和最小 SDK 版本
 * 2. 配置 Java 兼容性和脱糖支持
 * 3. 配置 Kotlin 编译器选项
 */
internal fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {
    commonExtension.apply {
        // 设置编译 SDK 版本
        compileSdk = 36

        // 配置默认最小 SDK 版本
        defaultConfig.apply {
            minSdk = 23
        }

        // 配置编译选项
        compileOptions.apply {
            // 启用 Java 11 API 支持（通过脱糖实现）
            // 允许在旧版 Android 上使用 Java 11 API
            // https://developer.android.com/studio/write/java11-minimal-support-table
            sourceCompatibility = JavaVersion.VERSION_11
            targetCompatibility = JavaVersion.VERSION_11
            isCoreLibraryDesugaringEnabled = true // 启用核心库脱糖
        }
    }

    // 配置 Kotlin 扩展
    configureKotlin<KotlinAndroidProjectExtension>()

    // 添加核心库脱糖依赖
    dependencies {
        "coreLibraryDesugaring"(libs.findLibrary("android.desugarJdkLibs").get())
    }
}

/**
 * 配置 JVM（非 Android）项目的 Kotlin 选项
 *
 * 此函数负责：
 * 1. 设置 Java 兼容性版本
 * 2. 配置 Kotlin 编译器选项
 */
internal fun Project.configureKotlinJvm() {
    extensions.configure<JavaPluginExtension> {
        // 启用 Java 11 API 支持（通过脱糖实现）
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    configureKotlin<KotlinJvmProjectExtension>()
}

/**
 * 配置基础 Kotlin 选项
 *
 * 此函数负责：
 * 1. 设置 JVM 目标版本
 * 2. 配置警告为错误处理
 * 3. 添加实验性 API 的 opt-in 参数
 * 4. 配置数据类 copy 方法的可见性
 */
private inline fun <reified T : KotlinBaseExtension> Project.configureKotlin() = configure<T> {
    // 将所有 Kotlin 警告视为错误（默认禁用）
    // 可以在 ~/.gradle/gradle.properties 中设置 warningsAsErrors=true 来启用
    val warningsAsErrors = providers.gradleProperty("warningsAsErrors").map {
        it.toBoolean()
    }.orElse(false)

    // 根据项目类型获取编译器选项
    when (this) {
        is KotlinAndroidProjectExtension -> compilerOptions
        is KotlinJvmProjectExtension -> compilerOptions
        else -> TODO("Unsupported project extension $this ${T::class}")
    }.apply {
        // 设置 JVM 目标版本
        jvmTarget = JvmTarget.JVM_11

        // 是否将所有警告视为错误
        allWarningsAsErrors = warningsAsErrors

        // 添加自由编译器参数
        freeCompilerArgs.add(
            // 启用实验性协程 API，包括 Flow
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
        )

        freeCompilerArgs.add(
            /**
             * 移除此参数在 Phase 3 之后。
             * https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-consistent-copy-visibility/#deprecation-timeline
             *
             * 弃用时间线
             * Phase 3（预计 Kotlin 2.2 或 Kotlin 2.3）。
             * 默认值会发生变化。
             * 除非使用 ExposedCopyVisibility，否则生成的 'copy' 方法具有与主构造函数相同的可见性。
             * 二进制签名会发生变化。声明上的错误将不再报告。
             * '-Xconsistent-data-class-copy-visibility' 编译器标志和 ConsistentCopyVisibility 注解变得不必要。
             */
            "-Xconsistent-data-class-copy-visibility",
        )
    }
}
