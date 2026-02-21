/*
 * Copyright 2026 The Android Open Source Project
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

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/**
 * 为 Android 项目配置 Spotless 代码格式检查
 *
 * Spotless 是一个代码格式检查工具，支持多种格式化规则：
 * - Kotlin: ktlint
 * - XML: XML 格式化
 * - KTS: Kotlin 脚本格式化
 *
 * 此配置包括：
 * - Kotlin 代码格式检查
 * - XML 文件格式检查
 * - 许可证头检查
 */
internal fun Project.configureSpotlessForAndroid() {
    configureSpotlessCommon() // 配置通用 Spotless 规则
    extensions.configure<SpotlessExtension> {
        // 配置 XML 格式化规则
        format("xml") {
            target("src/**/*.xml") // 格式化所有 XML 文件
            // 查找第一个非注释 XML 标签或 XML 声明
            licenseHeaderFile(rootDir.resolve("spotless/copyright.xml"), "(<[^!?])")
            endWithNewline() // 文件末尾添加换行符
        }
    }
}

/**
 * 为 JVM 项目配置 Spotless 代码格式检查
 */
internal fun Project.configureSpotlessForJvm() {
    configureSpotlessCommon() // 配置通用 Spotless 规则
}

/**
 * 为根项目配置 Spotless 代码格式检查
 *
 * 根项目的配置略有不同：
 * - 使用特定的 build-logic 目录作为目标
 * - 为 Kotlin 和 KTS 分别配置不同的规则
 */
internal fun Project.configureSpotlessForRootProject() {
    apply(plugin = "com.diffplug.spotless")
    extensions.configure<SpotlessExtension> {
        // 配置 Kotlin 格式化规则
        kotlin {
            target("build-logic/convention/src/**/*.kt")
            // 使用 ktlint，并启用 Android 特定的规则
            ktlint(libs.findVersion("ktlint").get().requiredVersion).editorConfigOverride(
                mapOf("android" to "true"),
            )
            // 许可证头文件
            licenseHeaderFile(rootDir.resolve("spotless/copyright.kt"))
            endWithNewline()
        }
        // 配置 Kotlin 脚本格式化规则
        format("kts") {
            target("*.kts")
            target("build-logic/*.kts")
            target("build-logic/convention/*.kts")
            // 查找第一个不是块注释的行（假定为许可证）
            licenseHeaderFile(rootDir.resolve("spotless/copyright.kts"), "(^(?![\\/ ]\\*).*$)")
            endWithNewline()
        }
    }
}

/**
 * 配置通用的 Spotless 规则
 *
 * 适用于大多数 Android 和 JVM 项目
 */
private fun Project.configureSpotlessCommon() {
    apply(plugin = "com.diffplug.spotless")
    extensions.configure<SpotlessExtension> {
        // 配置 Kotlin 格式化规则
        kotlin {
            target("src/**/*.kt")
            // 使用 ktlint，并启用 Android 特定的规则
            ktlint(libs.findVersion("ktlint").get().requiredVersion).editorConfigOverride(
                mapOf("android" to "true"),
            )
            // 许可证头文件
            licenseHeaderFile(rootDir.resolve("spotless/copyright.kt"))
            endWithNewline()
        }
        // 配置 Kotlin 脚本格式化规则
        format("kts") {
            target("*.kts")
            // 查找第一个不是块注释的行（假定为许可证）
            licenseHeaderFile(rootDir.resolve("spotless/copyright.kts"), "(^(?![\\/ ]\\*).*$)")
            endWithNewline()
        }
    }
}
