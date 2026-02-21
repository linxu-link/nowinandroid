# Now in Android 项目

Now in Android 是一个使用 Kotlin 编写的原生 Android 移动应用。它提供关于 Android 开发的定期新闻。用户可以选择关注话题、在新内容可用时收到通知，并收藏内容。

## 架构

该项目是一个遵循 Google 官方架构指南的现代 Android 应用。它是一个响应式、单 Activity 应用，使用以下技术：

-   **UI：** 完全使用 Jetpack Compose 构建，包括 Material 3 组件和用于不同屏幕尺寸的自适应布局。
-   **状态管理：** 使用 Kotlin Coroutines 和 `Flow` 实现单向数据流（UDF）。`ViewModel` 作为状态持有者，将 UI 状态作为数据流暴露。
-   **依赖注入：** 应用中使用 Hilt 进行依赖注入，简化依赖管理并提高可测试性。
-   **导航：** 使用 Jetpack Navigation for Compose 处理导航，提供声明式和类型安全的方式来在屏幕之间导航。
-   **数据：** 数据层使用仓库模式实现。
    -   **本地数据：** 使用 Room 和 DataStore 进行本地数据持久化。
    -   **远程数据：** 使用 Retrofit 和 OkHttp 从网络获取数据。
-   **后台处理：** 使用 WorkManager 进行可延迟的后台任务。

## 模块

主 Android 应用位于 `app/` 文件夹。功能模块位于 `feature/`，核心和共享模块位于 `core/`。

## 构建和测试命令

应用和 Android 库有两种产品风味（flavor）：`demo` 和 `prod`，以及两种构建类型：`debug` 和 `release`。

- 构建：`./gradlew assemble{Variant}`。通常使用 `assembleDemoDebug`。
- 修复代码风格/格式化：`./gradlew spotlessApply`
- 运行本地测试：`./gradlew {variant}Test`
- 运行单个测试：`./gradlew {variant}Test --tests "com.example.myapp.MyTestClass"`
- 运行本地截图测试：`./gradlew verifyRoborazziDemoDebug`

### 仪器测试

- 使用 Gradle 管理的设备运行设备测试：`./gradlew pixel6api31aospDebugAndroidTest`。还有 `pixel4api30aospatdDebugAndroidTest` 和 `pixelcapi30aospatdDebugAndroidTest`。

### 创建测试

#### 仪器测试

- UI 功能的测试应仅使用 `ComposeTestRule` 和 `ComponentActivity`。
- 更大的测试位于 `:app` 模块，它们可以启动如 `MainActivity` 的 Activity。

#### 本地测试

- 大多数断言使用 [kotlinx.coroutines](https://github.com/Kotlin/kotlinx.coroutines)
- 复杂的协程测试使用 [cashapp/turbine](https://github.com/cashapp/turbine)
- 断言使用 [google/truth](https://github.com/google/truth)

## 持续集成

- 工作流程定义在 `.github/workflows/*.yaml` 中，包含各种检查。
- 截图测试由 CI 生成，因此不应从工作站将其提交到仓库。

## 版本控制和代码位置

- 项目使用 git，托管在 https://github.com/android/nowinandroid。
