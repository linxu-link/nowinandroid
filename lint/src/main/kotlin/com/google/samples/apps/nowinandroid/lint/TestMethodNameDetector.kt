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

package com.google.samples.apps.nowinandroid.lint

import com.android.tools.lint.detector.api.AnnotationInfo
import com.android.tools.lint.detector.api.AnnotationUsageInfo
import com.android.tools.lint.detector.api.Category.Companion.TESTING
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope.JAVA_FILE
import com.android.tools.lint.detector.api.Scope.TEST_SOURCES
import com.android.tools.lint.detector.api.Severity.WARNING
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat.RAW
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UElement
import java.util.EnumSet
import kotlin.io.path.Path

/**
 * 测试方法名称检测器
 *
 * 检查测试方法命名中的常见模式：
 * - [detectPrefix] 移除所有单元测试中不必要的 "test" 前缀
 * - [detectFormat] 检查 Android 仪器测试的 `given_when_then` 格式（不支持反引号）
 */
class TestMethodNameDetector : Detector(), SourceCodeScanner {

    // 适用的注解列表
    override fun applicableAnnotations() = listOf("org.junit.Test")

    // 访问注解使用的方法
    override fun visitAnnotationUsage(
        context: JavaContext,
        element: UElement,
        annotationInfo: AnnotationInfo,
        usageInfo: AnnotationUsageInfo,
    ) {
        val method = usageInfo.referenced as? PsiMethod ?: return

        method.detectPrefix(context, usageInfo) // 检测前缀
        method.detectFormat(context, usageInfo) // 检测格式
    }

    // 判断是否为 Android 测试
    private fun JavaContext.isAndroidTest() = Path("androidTest") in file.toPath()

    /**
     * 检测并报告不必要的前缀
     */
    private fun PsiMethod.detectPrefix(
        context: JavaContext,
        usageInfo: AnnotationUsageInfo,
    ) {
        if (!name.startsWith("test")) return // 如果不是以 test 开头，则返回
        context.report(
            issue = PREFIX,
            scope = usageInfo.usage,
            location = context.getNameLocation(this),
            message = PREFIX.getBriefDescription(RAW),
            quickfixData = LintFix.create()
                .name("移除前缀")
                .replace().pattern("""test[\s_]*""")
                .with("")
                .autoFix()
                .build(),
        )
    }

    /**
     * 检测并报告不符合格式的方法名
     */
    private fun PsiMethod.detectFormat(
        context: JavaContext,
        usageInfo: AnnotationUsageInfo,
    ) {
        if (!context.isAndroidTest()) return // 如果不是 Android 测试，则返回
        // 检查是否符合 given_when_then 或 when_then 格式
        if ("""[^\W_]+(_[^\W_]+){1,2}""".toRegex().matches(name)) return
        context.report(
            issue = FORMAT,
            scope = usageInfo.usage,
            location = context.getNameLocation(this),
            message = FORMAT.getBriefDescription(RAW),
        )
    }

    companion object {

        /**
         * 创建 Issue 的辅助函数
         */
        private fun issue(
            id: String,
            briefDescription: String,
            explanation: String,
        ): Issue = Issue.create(
            id = id,
            briefDescription = briefDescription,
            explanation = explanation,
            category = TESTING,
            priority = 5,
            severity = WARNING,
            implementation = Implementation(
                TestMethodNameDetector::class.java,
                EnumSet.of(JAVA_FILE, TEST_SOURCES),
            ),
        )

        /**
         * 测试方法前缀问题
         * 检测以 "test" 开头的方法名
         */
        @JvmField
        val PREFIX: Issue = issue(
            id = "TestMethodPrefix",
            briefDescription = "测试方法以 'test' 开头",
            explanation = "测试方法不应以 'test' 开头。",
        )

        /**
         * 测试方法格式问题
         * 检测不符合 given_when_then 或 when_then 格式的方法名
         */
        @JvmField
        val FORMAT: Issue = issue(
            id = "TestMethodFormat",
            briefDescription = "测试方法不符合 'given_when_then' 或 'when_then' 格式",
            explanation = "测试方法应遵循 'given_when_then' 或 'when_then' 格式。",
        )
    }
}
