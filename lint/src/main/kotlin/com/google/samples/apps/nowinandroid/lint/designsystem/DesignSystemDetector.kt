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

package com.google.samples.apps.nowinandroid.lint.designsystem

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UQualifiedReferenceExpression

/**
 * 设计系统检测器
 *
 * 检查错误使用 Compose Material API 的情况，提倡使用 Now in Android 设计系统模块中的等价组件
 */
class DesignSystemDetector : Detector(), Detector.UastScanner {

    // 获取适用的 UAST 类型列表
    override fun getApplicableUastTypes(): List<Class<out UElement>> = listOf(
        UCallExpression::class.java,
        UQualifiedReferenceExpression::class.java,
    )

    // 创建 UAST 处理器
    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            // 处理函数调用表达式
            override fun visitCallExpression(node: UCallExpression) {
                val name = node.methodName ?: return
                val preferredName = METHOD_NAMES[name] ?: return
                reportIssue(context, node, name, preferredName)
            }

            // 处理限定引用表达式
            override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
                val name = node.receiver.asRenderString()
                val preferredName = RECEIVER_NAMES[name] ?: return
                reportIssue(context, node, name, preferredName)
            }
        }

    companion object {
        /**
         * 设计系统问题定义
         */
        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "DesignSystem",
            briefDescription = "设计系统",
            explanation = "此检查突出显示代码中使用 Compose Material " +
                "composables 而非 Now in Android 设计系统模块中等价组件的调用。",
            category = Category.CUSTOM_LINT_CHECKS,
            priority = 7,
            severity = Severity.ERROR,
            implementation = Implementation(
                DesignSystemDetector::class.java,
                Scope.JAVA_FILE_SCOPE,
            ),
        )

        // 不幸的是，:lint 是一个 Java 模块，因此无法依赖 :core-designsystem
        // Android 模块，所以我们不能使用 composable 函数引用（例如 ::Button.name）
        // 而是需要使用硬编码的名称。
        /**
         * 方法名称映射表：将 Material 组件映射到 Nia 组件
         */
        val METHOD_NAMES = mapOf(
            "MaterialTheme" to "NiaTheme",
            "Button" to "NiaButton",
            "OutlinedButton" to "NiaOutlinedButton",
            "TextButton" to "NiaTextButton",
            "FilterChip" to "NiaFilterChip",
            "ElevatedFilterChip" to "NiaFilterChip",
            "NavigationBar" to "NiaNavigationBar",
            "NavigationBarItem" to "NiaNavigationBarItem",
            "NavigationRail" to "NiaNavigationRail",
            "NavigationRailItem" to "NiaNavigationRailItem",
            "TabRow" to "NiaTabRow",
            "Tab" to "NiaTab",
            "IconToggleButton" to "NiaIconToggleButton",
            "FilledIconToggleButton" to "NiaIconToggleButton",
            "FilledTonalIconToggleButton" to "NiaIconToggleButton",
            "OutlinedIconToggleButton" to "NiaIconToggleButton",
            "CenterAlignedTopAppBar" to "NiaTopAppBar",
            "SmallTopAppBar" to "NiaTopAppBar",
            "MediumTopAppBar" to "NiaTopAppBar",
            "LargeTopAppBar" to "NiaTopAppBar",
        )

        /**
         * 接收者名称映射表：将 Material Icons 映射到 NiaIcons
         */
        val RECEIVER_NAMES = mapOf(
            "Icons" to "NiaIcons",
        )

        /**
         * 报告问题
         */
        fun reportIssue(
            context: JavaContext,
            node: UElement,
            name: String,
            preferredName: String,
        ) {
            context.report(
                ISSUE,
                node,
                context.getLocation(node),
                "使用 $name 而非 $preferredName",
            )
        }
    }
}
