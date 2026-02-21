# Convention 插件

`build-logic` 文件夹定义了项目特定的 Convention 插件，用于维护通用模块配置的单一事实来源。

这种做法主要基于：
- [https://developer.squareup.com/blog/herding-elephants/](https://developer.squareup.com/blog/herding-elephants/)
- [https://github.com/jjohannes/idiomatic-gradle](https://github.com/jjohannes/idiomatic-gradle)

通过在 `build-logic` 中设置 Convention 插件，我们可以避免重复的构建脚本设置和混乱的 `subproject` 配置，同时避免 `buildSrc` 目录的缺点。

`build-logic` 是一个 included build，配置在根目录的 [`settings.gradle.kts`](../settings.gradle.kts) 中。

在 `build-logic` 内部有一个 `convention` 模块，它定义了一组插件，所有普通模块都可以使用这些插件来配置自己。

`build-logic` 还包括一组 `Kotlin` 文件，用于在插件之间共享逻辑，这对于使用共享代码配置 Android 组件（库与应用）非常有用。

这些插件是**可添加的**和**可组合的**，并且只尝试完成单一职责。模块可以选择它们需要的配置。如果某个模块有一次性的逻辑且没有共享代码，最好直接在模块的 `build.gradle` 中定义，而不是创建具有模块特定设置的 Convention 插件。

当前 Convention 插件列表：

- [`nowinandroid.android.application`](convention/src/main/kotlin/AndroidApplicationConventionPlugin.kt)、
  [`nowinandroid.android.library`](convention/src/main/kotlin/AndroidLibraryConventionPlugin.kt)、
  [`nowinandroid.android.test`](convention/src/main/kotlin/AndroidTestConventionPlugin.kt)：
  配置通用的 Android 和 Kotlin 选项。
- [`nowinandroid.android.application.compose`](convention/src/main/kotlin/AndroidApplicationComposeConventionPlugin.kt)、
  [`nowinandroid.android.library.compose`](convention/src/main/kotlin/AndroidLibraryComposeConventionPlugin.kt)：
  配置 Jetpack Compose 选项。
