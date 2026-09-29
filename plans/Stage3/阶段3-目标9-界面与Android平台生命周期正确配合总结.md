# 阶段 3 - 目标 9：界面与 Android 平台生命周期正确配合

## 目标

理解 Compose 界面、Android Lifecycle 和 ViewModel 之间的生命周期关系，并能够让界面在前后台切换、导航和重组时保持合理行为。

这一节最重要的不是记 API，而是先分清：

```text
Android Lifecycle
Compose Composition
ViewModel
```

它们是三套不同的生命周期边界。

---

## 1. Compose 生命周期不等于 Activity 生命周期

Composable 有自己的生命周期：

```text
进入 Composition
        ↓
发生 0~N 次重组
        ↓
离开 Composition
```

而 Activity 有 Android 平台生命周期：

```text
CREATED
STARTED
RESUMED
STOPPED
DESTROYED
```

二者不能直接画等号。

例如：

```text
Activity 进入后台
    ↓
Activity 进入 STOPPED
```

此时某个 Composable 不一定已经离开 Composition。

所以：

> “Composable 还存在”不等于“用户当前正在使用这个界面”。

---

## 2. ViewModel 又是另一套生命周期

ViewModel 没有 Activity 那样的一整套：

```text
onStart()
onResume()
onPause()
onStop()
```

它大致只有：

```text
ViewModel 创建
    ↓
init { ... }

ViewModel 存活
    ↓

ViewModelStore 被清理
    ↓
onCleared()
```

因此：

```kotlin
class StudyViewModel : ViewModel() {

    init {
        loadArticles()
    }

    override fun onCleared() {
        super.onCleared()
    }
}
```

可以理解为：

- `init`：ViewModel 实例创建时执行。
- `onCleared()`：ViewModel 真正被清理时执行。

ViewModel 能活多久，取决于它所属的：

```text
ViewModelStoreOwner
```

例如：

- Activity
- Fragment
- Navigation entry

---

## 3. Navigation 中每个页面可以有自己的 ViewModel

当前项目使用 Navigation 3：

```kotlin
entryDecorators = listOf(
    rememberSaveableStateHolderNavEntryDecorator(),
    rememberViewModelStoreNavEntryDecorator()
)
```

其中：

```kotlin
rememberViewModelStoreNavEntryDecorator()
```

让导航 entry 可以拥有自己的 `ViewModelStore`。

因此：

```text
NavDisplay
├── SetupRoute
│   └── SetupViewModel
│
└── StudyRoute
    └── StudyViewModel
```

甚至两个不同的：

```text
StudyRoute("Kotlin")
StudyRoute("Compose")
```

如果对应两个不同的 back stack entry，也可以拥有两个不同的 `StudyViewModel`。

所以以后判断 ViewModel 生命周期，最准确的问题不是：

> 它属于哪个 Activity？

而是：

> 它的 `ViewModelStoreOwner` 是谁？

---

## 4. `viewModelScope` 跟 ViewModel 生命周期绑定

ViewModel 中常见：

```kotlin
viewModelScope.launch {
    // 异步工作
}
```

`viewModelScope` 是一个协程作用域。

它并不是“始终有一个协程运行”，而是：

```text
ViewModel 存活
    ↓
viewModelScope 存活
    ↓
可以不断 launch 新协程
```

每个协程执行完成以后，会自己结束：

```kotlin
viewModelScope.launch {
    delay(2000)
    loadData()
}
```

执行完后：

```text
这个协程结束
```

但：

```text
viewModelScope 仍然存在
```

直到：

```text
ViewModel.onCleared()
    ↓
viewModelScope 被取消
    ↓
其中尚未完成的协程一起取消
```

另外，Activity 暂时进入后台并不会自动取消 `viewModelScope` 中的任务。

---

## 5. UI 状态应该保持“外部只读”

当前项目之前使用：

```kotlin
var uiState by mutableStateOf<StudyUiState>(
    StudyUiState.Loading
)
    private set
```

这里：

```kotlin
private set
```

非常重要。

它表示：

```text
Composable
    ↓
可以读取 uiState
    ↓
不能直接修改 uiState
```

状态修改必须由 ViewModel 自己完成：

```kotlin
fun loadArticles() {
    uiState = StudyUiState.Loading
}
```

否则如果不加：

```kotlin
private set
```

外部就可以直接：

```kotlin
viewModel.uiState = StudyUiState.Empty
```

这样 UI 层也能随意修改 ViewModel 状态，边界会变得混乱。

使用 `StateFlow` 时通常写成：

```kotlin
private val _uiState =
    MutableStateFlow<StudyUiState>(StudyUiState.Loading)

val uiState = _uiState.asStateFlow()
```

本质也是同样的设计：

> ViewModel 内部可写，外部只读。

---

## 6. `collectAsStateWithLifecycle()`

如果 ViewModel 使用 `StateFlow`：

```kotlin
class StudyViewModel : ViewModel() {

    private val _uiState =
        MutableStateFlow<StudyUiState>(
            StudyUiState.Loading
        )

    val uiState = _uiState.asStateFlow()
}
```

Compose 中推荐：

```kotlin
val uiState by
    viewModel.uiState.collectAsStateWithLifecycle()
```

它负责把：

```text
StateFlow
+
Android Lifecycle
+
Compose State
```

连接起来。

大致行为：

```text
Lifecycle >= STARTED
        ↓
收集 StateFlow
        ↓
转换成 Compose State
        ↓
状态变化触发重组

Lifecycle < STARTED
        ↓
停止收集
```

需要注意：

> 停止收集，不等于 ViewModel 停止工作。

例如：

```kotlin
viewModelScope.launch {
    delay(2000)
    _uiState.value = StudyUiState.Error(...)
}
```

如果 Activity 此时进入后台：

```text
Compose 暂停收集
```

但 ViewModel 中的任务仍然可能继续执行。

等 Activity 回来以后：

```text
重新开始收集
    ↓
拿到 StateFlow 当前最新值
```

---

## 7. `LaunchedEffect`

`LaunchedEffect` 用于：

> 启动一段和当前 Composition 生命周期绑定的协程工作。

例如：

```kotlin
LaunchedEffect(topic) {
    // 当前页面相关的协程工作
}
```

生命周期：

```text
Composable 进入 Composition
        ↓
启动协程

普通重组，key 没变化
        ↓
不重新启动

key 改变
        ↓
取消旧协程
        ↓
启动新协程

Composable 离开 Composition
        ↓
协程取消
```

因此它与 ViewModel 中：

```kotlin
init { ... }
```

看起来有一点像，都是“开始时做某件事”，但绑定对象完全不同：

```text
ViewModel init
→ 跟 ViewModel 实例创建绑定

LaunchedEffect
→ 跟 Composition + key 绑定
```

例如页面的数据初始化：

```kotlin
init {
    loadArticles()
}
```

通常适合放在 ViewModel。

而：

- 滚动列表
- 控制焦点
- 显示 Snackbar
- 只服务于当前 UI 的协程行为

更适合 `LaunchedEffect`。

---

## 8. `DisposableEffect`

有些外部资源不是协程，而是：

```text
注册
    ↕
注销
```

例如：

```text
addObserver()       ↔ removeObserver()
addListener()       ↔ removeListener()
registerReceiver()  ↔ unregisterReceiver()
```

这种场景适合：

```kotlin
DisposableEffect(lifecycleOwner) {

    val observer = LifecycleEventObserver { _, event ->
        // ...
    }

    lifecycleOwner.lifecycle.addObserver(observer)

    onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
    }
}
```

生命周期：

```text
Composable 进入
    ↓
注册资源

key 变化
    ↓
onDispose()
    ↓
重新注册

Composable 离开
    ↓
onDispose()
```

可以简单区分：

```text
LaunchedEffect
→ Compose 帮你取消协程

DisposableEffect
→ 你需要明确告诉 Compose 如何释放资源
```

---

## 9. `rememberUpdatedState`

`rememberUpdatedState` 解决的是：

> Effect 不想因为某个值变化而重启，但 Effect 真正使用这个值时又必须拿到最新版本。

例如：

```kotlin
@Composable
fun LandingScreen(
    onTimeout: () -> Unit
) {
    val currentOnTimeout by
        rememberUpdatedState(onTimeout)

    LaunchedEffect(Unit) {
        delay(3000)
        currentOnTimeout()
    }
}
```

这里希望：

```text
计时器不要重新开始
```

但是：

```text
3 秒后执行最新的 onTimeout
```

因此不能简单把：

```kotlin
onTimeout
```

放进 `LaunchedEffect` 的 key，否则 callback 一变，整个 Effect 就会重启。

可以这样理解：

```text
rememberUpdatedState
≈ 保存一个稳定容器
  但容器里的值一直更新
```

对于有 React 经验的人，它很像：

```js
const callbackRef = useRef(onTimeout)
callbackRef.current = onTimeout
```

也就是一种：

```text
始终保存最新值的 ref
```

---

## 10. Effect 的 key 怎么判断

可以使用一个简单判断：

```text
Effect 中使用了某个值
        ↓
这个值变化后
整个 Effect 应该重新执行吗？
```

如果：

```text
应该重新执行
    ↓
把它作为 key
```

如果：

```text
不应该重新执行
但以后需要使用它的最新值
    ↓
rememberUpdatedState
```

例如：

```kotlin
DisposableEffect(lifecycleOwner) {
    ...
}
```

`lifecycleOwner` 改变以后，observer 应该重新注册，因此它适合作为 key。

但某个 callback 改变：

```text
只希望 observer 下次调用最新 callback
不希望整个 observer 重新注册
```

就适合 `rememberUpdatedState`。

---

## 11. React `useEffect` 与 Compose Effect 的对应关系

React 中：

```js
useEffect(() => {
    // effect

    return () => {
        // cleanup
    }
}, [key])
```

一个 API 同时负责：

```text
启动副作用
+
清理副作用
```

Compose 把不同用途拆得更明确：

```text
LaunchedEffect
→ 协程型副作用

DisposableEffect
→ 显式注册 / 清理型副作用
```

而：

```kotlin
LaunchedEffect(topic)
DisposableEffect(topic)
```

中的 key，与 React：

```js
useEffect(() => {
}, [topic])
```

的 dependency 思路比较接近。

---

## 12. 最终实践：让 `StudyScreen` 生命周期感知地收集状态

把 `StudyViewModel` 改为：

```kotlin
class StudyViewModel : ViewModel() {

    private val _uiState =
        MutableStateFlow<StudyUiState>(
            StudyUiState.Loading
        )

    val uiState = _uiState.asStateFlow()

    init {
        loadArticles()
    }

    fun loadArticles() {
        viewModelScope.launch {
            _uiState.value = StudyUiState.Loading

            delay(2000)

            _uiState.value =
                StudyUiState.Error(
                    "服务器连接失败"
                )
        }
    }
}
```

然后：

```kotlin
@Composable
fun StudyScreen(
    topic: String,
    onBack: () -> Unit,
    viewModel: StudyViewModel = viewModel()
) {
    val uiState by
        viewModel.uiState.collectAsStateWithLifecycle()

    Column {

        SectionCard(
            header = {
                Text("当前 UI 状态")
            },
            content = {
                when (val state = uiState) {

                    StudyUiState.Loading -> {
                        Text("正在加载...")
                    }

                    StudyUiState.Empty -> {
                        Text("暂无内容")
                    }

                    is StudyUiState.Error -> {
                        Column {
                            Text("错误：${state.message}")

                            Button(
                                onClick = viewModel::loadArticles
                            ) {
                                Text("重试")
                            }
                        }
                    }

                    is StudyUiState.Content -> {
                        Column {
                            state.articles.forEach { article ->
                                Text(article)
                            }
                        }
                    }
                }
            }
        )

        Button(onClick = onBack) {
            Text("返回")
        }
    }
}
```

需要：

```kotlin
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
```

验证方式：

```text
进入 StudyScreen
    ↓
立即按 Home 键进入后台
    ↓
等待超过 2 秒
    ↓
重新进入 App
```

此时可以观察到：

```text
ViewModel 的任务已经完成
        ↓
StateFlow 已经变为 Error
        ↓
UI 恢复收集
        ↓
马上显示最新 Error 状态
```

这正好体现：

```text
ViewModel 的工作生命周期
≠
Compose 当前是否正在收集状态
```

---

## 本节完成标准

这一目标最终需要能够判断：

```text
状态应该由谁保存？
→ ViewModel

Flow 应该什么时候被 UI 收集？
→ collectAsStateWithLifecycle()

一段协程只应该跟当前 Composable 一起存在？
→ LaunchedEffect

注册了外部监听器，需要在离开时注销？
→ DisposableEffect

Effect 不想重启，但需要某个参数最新值？
→ rememberUpdatedState
```

最终形成的基本模型：

```text
ViewModel
    │
    │ StateFlow
    ▼
collectAsStateWithLifecycle()
    │
    │ Android Lifecycle 决定是否收集
    ▼
Compose State
    │
    ▼
UI 重组


Composition
    ├── LaunchedEffect
    │     └── 管理 UI 相关协程
    │
    └── DisposableEffect
          └── 管理注册 / 释放资源
```

这一节真正需要掌握的不是生命周期 API 的数量，而是：

> 数据、UI 和 Android 平台各自有自己的生命周期边界；开发时要让工作归属于正确的生命周期，而不是把所有事情都绑定在同一个地方。
