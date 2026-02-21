# 架构学习之旅

在这个学习之旅中，您将了解 Now in Android 应用的架构：它的层级、关键类以及它们之间的交互。

## 目标和需求

应用架构的目标是：

*   尽可能遵循[官方架构指南](https://developer.android.com/jetpack/guide)。
*   易于开发者理解，不过于实验性。
*   支持多个开发者在同一代码库上工作。
*   便于本地和仪器测试，无论是在开发者的机器上还是使用持续集成（CI）。
*   最小化构建时间。

## 架构概述

应用架构包含三个层级：[数据层](https://developer.android.com/jetpack/guide/data-layer)、[领域层](https://developer.android.com/jetpack/guide/domain-layer) 和 [UI 层](https://developer.android.com/jetpack/guide/ui-layer)。

<center>
<img src="images/architecture-1-overall.png" width="600px" alt="Diagram showing overall app architecture" />
</center>

> [!NOTE]
> 官方 Android 架构与其他架构（如"清洁架构"）不同。来自其他架构的概念可能不适用于此处，或以不同方式应用。[更多讨论见此处](https://github.com/android/nowinandroid/discussions/1273)。

该架构遵循响应式编程模型，采用[单向数据流](https://developer.android.com/jetpack/guide/ui-layer#udf)。数据层位于底部，关键概念如下：

*   上层对下层的变化作出反应。
*   事件向下流动。
*   数据向上流动。

数据流使用流实现，采用 [Kotlin Flows](https://developer.android.com/kotlin/flow)。

### 示例：在"为你推荐"屏幕上显示新闻

首次运行应用时，它会尝试从远程服务器加载新闻资源列表（当选择 `prod` 构建风味时，`demo` 构建将使用本地数据）。加载后，根据用户选择的兴趣向用户展示这些内容。

下图显示了发生的事件以及数据如何从相关对象流动以实现此功能。

![Diagram showing how news resources are displayed on the For You screen](images/architecture-2-example.png "Diagram showing how news resources are displayed on the For You screen")

以下是每个步骤中发生的情况。找到关联代码的最简单方法是将项目加载到 Android Studio 并搜索 Code 列中的文本（快捷方式：按 <kbd>⇧ SHIFT</kbd> 两次）。

<table>
  <tr>
   <td><strong>步骤</strong>
   </td>
   <td><strong>描述</strong>
   </td>
   <td><strong>代码</strong>
   </td>
  </tr>
  <tr>
   <td>1
   </td>
   <td>应用启动时，一个 <a href="https://developer.android.com/topic/libraries/architecture/workmanager">WorkManager</a> 任务被加入队列，用于同步所有仓库。
   </td>
   <td><code>Sync.initialize</code>
   </td>
  </tr>
  <tr>
   <td>2
   </td>
   <td><code>ForYouViewModel</code> 调用 <code>GetUserNewsResourcesUseCase</code> 获取带有收藏/保存状态的新闻资源流。在用户和新闻仓库都发出数据之前，该流中不会发出任何项目。等待期间，订阅源状态设置为 <code>Loading</code>。
   </td>
   <td>搜索 <code>NewsFeedUiState.Loading</code> 的用法
   </td>
  </tr>
  <tr>
   <td>3
   </td>
   <td>用户数据仓库从由 Proto DataStore 支持的本地数据源获取 <code>UserData</code> 对象流。
   </td>
   <td><code>NiaPreferencesDataSource.userData</code>
   </td>
  </tr>
  <tr>
   <td>4
   </td>
   <td>WorkManager 执行同步任务，调用 <code>OfflineFirstNewsRepository</code> 开始与远程数据源同步数据。
   </td>
   <td><code>SyncWorker.doWork</code>
   </td>
  </tr>
  <tr>
   <td>5
   </td>
   <td><code>OfflineFirstNewsRepository</code> 调用 <code>RetrofitNiaNetwork</code> 使用 <a href="https://square.github.io/retrofit/">Retrofit</a> 执行实际的 API 请求。
   </td>
   <td><code>OfflineFirstNewsRepository.syncWith</code>
   </td>
  </tr>
  <tr>
   <td>6
   </td>
   <td><code>RetrofitNiaNetwork</code> 调用远程服务器上的 REST API。
   </td>
   <td><code>RetrofitNiaNetwork.getNewsResources</code>
   </td>
  </tr>
  <tr>
   <td>7
   </td>
   <td><code>RetrofitNiaNetwork</code> 从远程服务器接收网络响应。
   </td>
   <td><code>RetrofitNiaNetwork.getNewsResources</code>
   </td>
  </tr>
  <tr>
   <td>8
   </td>
   <td><code>OfflineFirstNewsRepository</code> 通过在本地 <a href="https://developer.android.com/training/data-storage/room">Room 数据库</a>中插入、更新或删除数据，将远程数据与 <code>NewsResourceDAO</code> 同步。
   </td>
   <td><code>OfflineFirstNewsRepository.syncWith</code>
   </td>
  </tr>
  <tr>
   <td>9
   </td>
   <td>当 <code>NewsResourceDAO</code> 中的数据发生变化时，它会被发送到新闻资源数据流（这是一个 <a href="https://developer.android.com/kotlin/flow">Flow</a>）。
   </td>
   <td><code>NewsResourceDAO.getNewsResources</code>
   </td>
  </tr>
  <tr>
   <td>10
   </td>
   <td><code>OfflineFirstNewsRepository</code> 作为该流上的<a href="https://developer.android.com/kotlin/flow#modify">中间操作符</a>，将传入的 <code>PopulatedNewsResource</code>（数据库模型，数据层内部使用）转换为其他层使用的公共 <code>NewsResource</code> 模型。
   </td>
   <td><code>OfflineFirstNewsRepository.getNewsResources</code>
   </td>
  </tr>
  <tr>
   <td>11
   </td>
   <td><code>GetUserNewsResourcesUseCase</code> 将新闻资源列表与用户数据结合，发出 <code>UserNewsResource</code> 列表。
   </td>
   <td><code>GetUserNewsResourcesUseCase.invoke</code>
   </td>
  </tr>
  <tr>
   <td>12
   </td>
   <td>当 <code>ForYouViewModel</code> 收到可保存的新闻资源时，它将订阅源状态更新为 <code>Success</code>。

  <code>ForYouScreen</code> 然后使用状态中的可保存新闻资源来渲染屏幕。
   </td>
   <td>搜索 <code>NewsFeedUiState.Success</code> 的实例
   </td>
  </tr>
</table>



## 数据层

数据层实现为应用数据和业务逻辑的离线优先数据源。它是应用中所有数据的真实来源。

![Diagram showing the data layer architecture](images/architecture-3-data-layer.png "Diagram showing the data layer architecture")

每个仓库都有自己的模型。例如，`TopicsRepository` 有 `Topic` 模型，`NewsRepository` 有 `NewsResource` 模型。

仓库是其他层的公共 API，它们提供访问应用数据的_唯一_方式。仓库通常提供一个或多个读写数据的方法。

### 读取数据

数据作为数据流公开。这意味着仓库的每个客户端都必须准备好响应数据变化。数据不作为快照公开（例如 `getModel`），因为无法保证到使用时它仍然有效。

从本地存储作为真实来源进行读取，因此从 `Repository` 实例读取时不会发生错误。但是，在尝试将本地存储与远程源协调数据时可能会发生错误。有关错误协调的更多信息，请参阅下面的数据同步部分。

_示例：读取主题列表_

可以通过订阅 `TopicsRepository::getTopics` 流来获取主题列表，该流会发出 `List<Topic>`。

每当主题列表发生变化（例如添加新主题时），更新的 `List<Topic>` 会被发送到流中。

### 写入数据

要写入数据，仓库提供挂起函数。由调用者确保其执行范围适当。

_示例：关注主题_

只需使用用户希望关注的主题 ID 调用 `UserDataRepository.toggleFollowedTopicId`，并将 `followed=true` 表示应该关注该主题（使用 `false` 取消关注主题）。

### 数据源

一个仓库可能依赖一个或多个数据源。例如，`OfflineFirstTopicsRepository` 依赖以下数据源：


<table>
  <tr>
   <td><strong>名称</strong>
   </td>
   <td><strong>后端</strong>
   </td>
   <td><strong>用途</strong>
   </td>
  </tr>
  <tr>
   <td>TopicsDao
   </td>
   <td><a href="https://developer.android.com/training/data-storage/room">Room/SQLite</a>
   </td>
   <td>与主题关联的持久化关系数据
   </td>
  </tr>
  <tr>
   <td>NiaPreferencesDataSource
   </td>
   <td><a href="https://developer.android.com/topic/libraries/architecture/datastore">Proto DataStore</a>
   </td>
   <td>与用户偏好关联的持久化非结构化数据，特别是用户感兴趣的主题。这使用 .proto 文件定义和建模，使用 protobuf 语法。
   </td>
  </tr>
  <tr>
   <td>NiaNetworkDataSource
   </td>
   <td>使用 Retrofit 访问的远程 API
   </td>
   <td>主题数据，通过 REST API 端点以 JSON 形式提供。
   </td>
  </tr>
</table>



### 数据同步

仓库负责将本地存储与远程源协调数据。一旦从远程数据源获取数据，它会立即写入本地存储。更新后的数据从本地存储（Room）发送到相关数据流，并被任何监听客户端接收。

这种方法确保了应用的读写关注点分离且互不干扰。

如果在数据同步过程中发生错误，将采用指数退避策略。这通过 `SyncWorker`（`Synchronizer` 接口的实现）委托给 `WorkManager`。

有关数据同步的示例，请参阅 `OfflineFirstNewsRepository.syncWith`。

## 领域层

[领域层](https://developer.android.com/topic/architecture/domain-layer) 包含用例。这些是具有单一可调用方法（`operator fun invoke`）的类，包含业务逻辑。

这些用例用于简化和消除 ViewModel 中的重复逻辑。它们通常组合和转换来自仓库的数据。

例如，`GetUserNewsResourcesUseCase` 将来自 `NewsRepository` 的 `NewsResource` 流（使用 `Flow` 实现）与来自 `UserDataRepository` 的 `UserData` 对象流组合，创建 `UserNewsResource` 流。这个流被各种 ViewModel 用于在屏幕上显示带有收藏状态的新闻资源。

值得注意的是，Now in Android 中的领域层（目前）不包含事件处理用例。事件由 UI 层直接调用仓库方法来处理。

## UI 层

[UI 层](https://developer.android.com/topic/architecture/ui-layer) 包括：

*   使用 [Jetpack Compose](https://developer.android.com/jetpack/compose) 构建的 UI 元素
*   [Android ViewModels](https://developer.android.com/topic/libraries/architecture/viewmodel)

ViewModel 从用例和仓库接收数据流，并将其转换为 UI 状态。UI 元素反映此状态，并提供用户与应用交互的方式。这些交互作为事件传递给 ViewModel 进行处理。

![Diagram showing the UI layer architecture](images/architecture-4-ui-layer.png "Diagram showing the UI layer architecture")

### 建模 UI 状态

UI 状态使用接口和不可变数据类建模为密封层级。状态对象只能通过数据流的转换发出。这种方法确保了：

*   UI 状态始终代表底层应用数据——应用数据是真实来源。
*   UI 元素处理所有可能的状态。

**示例："为你推荐"屏幕上的新闻订阅源**

"为你推荐"屏幕上的新闻资源列表使用 `NewsFeedUiState` 建模。这是一个密封接口，创建两个可能状态的层级：

*   `Loading` 表示数据正在加载
*   `Success` 表示数据加载成功。Success 状态包含新闻资源列表。

`feedState` 被传递给 `ForYouScreen` 可组合函数，该函数处理这两种状态。

### 将流转换为 UI 状态

ViewModel 接收来自一个或多个用例或仓库的冷 [flows](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-flow/index.html) 数据流。这些被[组合](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/combine.html)在一起，或者简单[映射](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/map.html)，以产生单一的 UI 状态流。然后使用 [stateIn](https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/state-in.html) 将此单一流转换为热流。转换为状态流使 UI 元素能够从流中读取最后已知状态。

**示例：显示已关注的主题**

`InterestsViewModel` 将 `uiState` 公开为 `StateFlow<InterestsUiState>`。这个热流是通过获取 `GetFollowableTopicsUseCase` 提供的冷 `List<FollowableTopic>` 流创建的。每次发出新列表时，它都会转换为 `InterestsUiState.Interests` 状态并暴露给 UI。

### 处理用户交互

用户操作使用常规方法调用从 UI 元素传达给 ViewModel。这些方法作为 lambda 表达式传递给 UI 元素。

**示例：关注主题**

`InterestsScreen` 接受一个名为 `followTopic` 的 lambda 表达式，它来自 `InterestsViewModel.followTopic`。每次用户点击要关注的主题时都会调用此方法。然后 ViewModel 通过通知用户数据仓库来处理此操作。

## 进一步阅读

[应用架构指南](https://developer.android.com/topic/architecture)

[Jetpack Compose](https://developer.android.com/jetpack/compose)
