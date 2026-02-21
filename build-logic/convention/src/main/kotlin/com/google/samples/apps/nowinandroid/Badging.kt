/*
 * Copyright 2023 The Android Open Source Project
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

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.Aapt2
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.google.common.truth.Truth.assertWithMessage
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.process.ExecOperations
import javax.inject.Inject

/**
 * 生成 APK Badging 信息的任务
 *
 * 使用 aapt2 工具从 APK 中提取 badging 信息（包名、版本、权限等）
 * 结果会保存到指定的输出文件中
 */
@CacheableTask
abstract class GenerateBadgingTask : DefaultTask() {

    // 输出文件：badging 信息
    @get:OutputFile
    abstract val badging: RegularFileProperty

    // 输入文件：APK
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val apk: RegularFileProperty

    // 输入文件：aapt2 可执行文件
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val aapt2Executable: RegularFileProperty

    // 执行操作的注入
    @get:Inject
    abstract val execOperations: ExecOperations

    // 任务执行入口
    @TaskAction
    fun taskAction() {
        execOperations.exec {
            commandLine(
                aapt2Executable.get().asFile.absolutePath, // aapt2 工具路径
                "dump",                                   // dump 命令
                "badging",                                // 提取 badging 信息
                apk.get().asFile.absolutePath,            // APK 路径
            )
            // 将输出写入文件
            standardOutput = badging.asFile.get().outputStream()
        }
    }
}

/**
 * 检查 APK Badging 信息的任务
 *
 * 比较生成的 badging 信息与"golden"文件（预期的正确值）
 * 用于确保 APK 的元信息没有意外变化
 */
@CacheableTask
abstract class CheckBadgingTask : DefaultTask() {

    // 输出目录：任务输出目录（必须有输出才能被 Gradle 缓存）
    // 即使不使用，也必须声明输出，否则任务将始终运行
    @get:OutputDirectory
    abstract val output: DirectoryProperty

    // 输入文件：预期的 badging 信息（golden 文件）
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val goldenBadging: RegularFileProperty

    // 输入文件：生成的 badging 信息
    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val generatedBadging: RegularFileProperty

    // 输入属性：更新 badging 任务的名称
    @get:Input
    abstract val updateBadgingTaskName: Property<String>

    // 设置任务组
    override fun getGroup(): String = LifecycleBasePlugin.VERIFICATION_GROUP

    // 任务执行入口
    @TaskAction
    fun taskAction() {
        assertWithMessage(
            "生成的 badging 与 golden badging 不同！ " +
                "如果此更改是有意的，请运行 ./gradlew ${updateBadgingTaskName.get()}",
        )
            .that(generatedBadging.get().asFile.readText())
            .isEqualTo(goldenBadging.get().asFile.readText())
    }
}

/**
 * 首字母大写辅助函数
 */
private fun String.capitalized() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}

/**
 * 配置 Badging 任务
 *
 * 为每个构建变体创建：
 * 1. generateBadging 任务 - 生成 APK 的 badging 信息
 * 2. updateBadging 任务 - 更新 golden badging 文件
 * 3. checkBadging 任务 - 验证 badging 信息是否匹配
 */
fun Project.configureBadgingTasks(
    componentsExtension: ApplicationAndroidComponentsExtension,
) {
    // 注册回调，当新的变体配置完成时调用
    componentsExtension.onVariants { variant ->
        // 首字母大写的变体名称
        val capitalizedVariantName = variant.name.capitalized()

        // 生成 badging 任务名称
        val generateBadgingTaskName = "generate${capitalizedVariantName}Badging"
        // 注册生成 badging 任务
        val generateBadging =
            tasks.register<GenerateBadgingTask>(generateBadgingTaskName) {
                apk = variant.artifacts.get(SingleArtifact.APK_FROM_BUNDLE) // 从 Bundle 获取 APK
                aapt2Executable = componentsExtension.sdkComponents.aapt2.flatMap(Aapt2::executable) // aapt2 工具
                badging = project.layout.buildDirectory.file(
                    "outputs/apk_from_bundle/${variant.name}/${variant.name}-badging.txt",
                )
            }

        // 更新 badging 任务名称
        val updateBadgingTaskName = "update${capitalizedVariantName}Badging"
        // 注册更新任务：将生成的 badging 复制到项目目录作为 golden 文件
        tasks.register<Copy>(updateBadgingTaskName) {
            from(generateBadging.map(GenerateBadgingTask::badging))
            into(project.layout.projectDirectory)
        }

        // 检查 badging 任务名称
        val checkBadgingTaskName = "check${capitalizedVariantName}Badging"
        // 注册检查任务：验证 badging 信息
        tasks.register<CheckBadgingTask>(checkBadgingTaskName) {
            goldenBadging = project.layout.projectDirectory.file("${variant.name}-badging.txt")

            generatedBadging.set(generateBadging.flatMap(GenerateBadgingTask::badging))

            this.updateBadgingTaskName = updateBadgingTaskName

            output = project.layout.buildDirectory.dir("intermediates/$checkBadgingTaskName")
        }
    }
}
