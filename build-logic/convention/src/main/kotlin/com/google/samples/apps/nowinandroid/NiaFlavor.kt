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

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ProductFlavor
import org.gradle.kotlin.dsl.invoke

/**
 * 风味维度枚举
 *
 * 用于定义产品风味的维度，目前只有一个内容类型维度
 */
@Suppress("EnumEntryName")
enum class FlavorDimension {
    contentType, // 内容类型维度
}

/**
 * 应用内容来源的风味配置
 *
 * 应用的内容可以来自：
 * 1. 本地静态数据 - 用于演示目的
 * 2. 生产后端服务器 - 提供最新的真实内容
 *
 * 这两种产品风味反映了这种行为：
 * - demo: 使用本地静态数据，便于开发和演示
 * - prod: 连接到生产服务器，获取真实数据
 */
@Suppress("EnumEntryName")
enum class NiaFlavor(val dimension: FlavorDimension, val applicationIdSuffix: String? = null) {
    demo(FlavorDimension.contentType, applicationIdSuffix = ".demo"), // 演示风味，应用ID后缀为 .demo
    prod(FlavorDimension.contentType), // 生产风味
}

/**
 * 配置产品风味
 *
 * @param commonExtension 通用扩展
 * @param flavorConfigurationBlock 风味配置回调
 */
fun configureFlavors(
    commonExtension: CommonExtension,
    flavorConfigurationBlock: ProductFlavor.(flavor: NiaFlavor) -> Unit = {},
) {
    commonExtension.apply {
        // 添加所有风味维度
        FlavorDimension.entries.forEach { flavorDimension ->
            flavorDimensions += flavorDimension.name
        }

        // 配置产品风味
        productFlavors {
            NiaFlavor.entries.forEach { niaFlavor ->
                register(niaFlavor.name) {
                    dimension = niaFlavor.dimension.name
                    // 调用配置回调
                    flavorConfigurationBlock(this, niaFlavor)

                    // 如果是应用模块，设置应用ID后缀
                    if (commonExtension is ApplicationExtension && this is ApplicationProductFlavor) {
                        if (niaFlavor.applicationIdSuffix != null) {
                            applicationIdSuffix = niaFlavor.applicationIdSuffix
                        }
                    }
                }
            }
        }
    }
}
