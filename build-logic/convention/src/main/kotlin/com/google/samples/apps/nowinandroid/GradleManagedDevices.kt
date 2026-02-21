/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "AS IS" BASIS),
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.nowinandroid

import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.ManagedVirtualDevice
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.invoke

/**
 * 配置 Gradle 管理的设备
 *
 * 此函数用于配置自动化测试设备，允许 Gradle 自动管理虚拟设备的生命周期
 *
 * 支持的设备：
 * - Pixel 4 (API 30, aosp-atd)
 * - Pixel 6 (API 31, aosp)
 * - Pixel C (API 30, aosp-atd)
 *
 * 设备分组：
 * - allDevices: 所有配置的设备
 * - ciDevices: 用于 CI/CD 的设备（Pixel 4 和 Pixel C）
 */
internal fun configureGradleManagedDevices(
    commonExtension: CommonExtension,
) {
    // 定义设备配置
    val pixel4 = DeviceConfig("Pixel 4", 30, "aosp-atd")   // Pixel 4 设备
    val pixel6 = DeviceConfig("Pixel 6", 31, "aosp")       // Pixel 6 设备
    val pixelC = DeviceConfig("Pixel C", 30, "aosp-atd")   // Pixel C 设备

    // 所有设备列表
    val allDevices = listOf(pixel4, pixel6, pixelC)
    // CI 设备列表（用于持续集成）
    val ciDevices = listOf(pixel4, pixelC)

    // 配置测试选项
    commonExtension.testOptions.apply {
        @Suppress("UnstableApiUsage")
        // 配置 Gradle 管理的虚拟设备
        managedDevices {
            // 配置所有设备
            allDevices {
                allDevices.forEach { deviceConfig ->
                    maybeCreate(deviceConfig.taskName, ManagedVirtualDevice::class.java).apply {
                        device = deviceConfig.device
                        apiLevel = deviceConfig.apiLevel
                        systemImageSource = deviceConfig.systemImageSource
                    }
                }
            }
            // 配置设备分组
            groups {
                maybeCreate("ci").apply {
                    ciDevices.forEach { deviceConfig ->
                        targetDevices.add(localDevices[deviceConfig.taskName])
                    }
                }
            }
        }
    }
}

/**
 * 设备配置数据类
 *
 * @param device 设备名称
 * @param apiLevel API 级别
 * @param systemImageSource 系统镜像来源
 */
private data class DeviceConfig(
    val device: String,
    val apiLevel: Int,
    val systemImageSource: String,
) {
    /**
     * 生成 Gradle 任务名称
     *
     * 例如：Pixel 4 API 30 aosp-atd -> pixel4api30aospatd
     */
    val taskName = buildString {
        append(device.lowercase().replace(" ", "")) // 设备名称转小写并移除空格
        append("api")                              // 添加 api 后缀
        append(apiLevel.toString())                // 添加 API 级别
        append(systemImageSource.replace("-", "")) // 移除系统镜像的连字符
    }
}
