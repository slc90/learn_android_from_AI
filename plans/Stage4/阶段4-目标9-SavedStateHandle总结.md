# Stage 4 · 目标 9：SavedStateHandle 总结

## 1. SavedStateHandle 是什么

`SavedStateHandle` 可以理解为：

> 给 `ViewModel` 使用的小型可恢复状态容器。

普通 `ViewModel` 可以跨配置变化继续存活，例如屏幕旋转导致 Activity 重建时，原来的 ViewModel 通常还在。

但如果整个应用进程被系统回收：

```text
Process 死亡
    ↓
ViewModel 也死亡
```

这时，仅靠 ViewModel 无法恢复其中的内存状态。

`SavedStateHandle` 就是用来补这个缺口的。

---

## 2. 它解决的是什么问题

典型场景：

```text
用户正在搜索 "android"
    ↓
App 进入后台
    ↓
系统因为内存压力杀死进程
    ↓
旧 ViewModel 消失
    ↓
用户重新回到原来的 Task
    ↓
创建新的 ViewModel
    ↓
通过 SavedStateHandle 恢复 query = "android"
```

它恢复的不是旧对象。

旧进程已经死亡以后：

```text
旧 ViewModel
旧 SavedStateHandle
旧 Activity
```

全部已经不存在。

真正发生的是：

```text
旧 SavedStateHandle 中的状态
        ↓
交给 Android Saved State 系统保存

进程死亡

新的 ViewModel
        ↓
新的 SavedStateHandle
        ↓
恢复之前保存的值
```

---

## 3. SavedStateHandle 和 savedInstanceState 的关系

它和我们之前学过的 `savedInstanceState` 属于同一类机制。

可以粗略理解为：

```text
savedInstanceState
→ Activity 视角的 Saved State API

SavedStateHandle
→ ViewModel 视角的 Saved State API
```

底层仍然依赖 Android 的 Saved State 机制，而不是 `SavedStateHandle` 自己把数据永久写进某个数据库。

因此：

```text
SavedStateHandle
≠ 数据库
≠ DataStore
≠ 永久持久化
```

---

## 4. SavedStateHandle 能“活”多久

要区分两件事。

### SavedStateHandle 对象本身

对象跟着 ViewModel 和当前进程存在。

```text
进程死亡
→ SavedStateHandle 对象也死亡
```

### SavedStateHandle 中可恢复的状态

这些状态可以在系统杀死进程以后继续被 Android 保存，并在用户回到原 Task、页面被重新创建时恢复到新的 SavedStateHandle 中。

所以它不是“保存多久”的问题，而是：

> 只要 Android 仍然认为当前 Task / 页面具有恢复资格，这份 saved state 就有机会被恢复。

它适合：

```text
系统回收进程
配置变化
Activity 重建
```

但不能把它当成新的 App 会话之间的永久存储。

例如：

```text
用户明确结束 Task
Force Stop
设备重启
```

都不应该依赖 `SavedStateHandle` 来保留重要业务数据。

---

## 5. 基本使用方式

最直接的写法：

```kotlin
class SearchViewModel(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    var query: String
        get() = savedStateHandle["query"] ?: ""
        set(value) {
            savedStateHandle["query"] = value
        }
}
```

当：

```kotlin
query = "android"
```

本质上就是：

```text
SavedStateHandle
└── "query" → "android"
```

---

## 6. 和 StateFlow 配合

现在更常见的写法是直接让某个 saved-state key 暴露成 `StateFlow`。

### getStateFlow()

```kotlin
val query: StateFlow<String> =
    savedStateHandle.getStateFlow("query", "")

fun setQuery(value: String) {
    savedStateHandle["query"] = value
}
```

`getStateFlow()` 是只读的。

修改同一个 key：

```kotlin
savedStateHandle["query"] = "abc"
```

对应的 `StateFlow` 会发出新值。

---

### getMutableStateFlow()

如果使用较新的 Lifecycle 版本，可以直接：

```kotlin
private val _query =
    savedStateHandle.getMutableStateFlow("query", "")

val query: StateFlow<String> =
    _query.asStateFlow()

fun setQuery(value: String) {
    _query.value = value
}
```

这样它的使用体验就和普通：

```kotlin
MutableStateFlow
```

非常接近。

区别在于：

```text
普通 MutableStateFlow
→ ViewModel 死亡以后状态消失

SavedStateHandle.getMutableStateFlow(...)
→ 状态可以参与 Saved State 恢复
```

---

## 7. 一个典型数据流

假设 Todo 页面允许按关键字搜索：

```text
SavedStateHandle
      ↓
query: StateFlow<String>
      ↓
Repository.search(query)
      ↓
查询结果
      ↓
UiState
      ↓
Compose
```

如果进程死亡，不需要保存整个查询结果。

只需要恢复：

```text
query = "android"
```

然后：

```text
恢复 query
    ↓
重新执行 Repository 查询
    ↓
重新生成结果
    ↓
重新生成 UiState
```

---

## 8. 什么适合放进 SavedStateHandle

核心原则：

> 保存“恢复页面所需的最小输入”。

比较合适的例子：

```text
query = "android"
selectedTaskId = 17
filter = "unfinished"
currentTab = 2
```

这些状态通常：

- 很小；
- 很容易保存；
- 丢失后会明显破坏用户当前操作现场；
- 可以用来重新生成完整页面状态。

---

## 9. 什么不适合放进去

例如：

```text
完整 Todo 列表
整个 UiState
大型对象
Repository
Database
大量网络响应
复杂业务对象
```

这些不应该因为“技术上能序列化”就塞进去。

更合理的方式是：

```text
SavedStateHandle
└── filter = Unfinished

Room
└── Todo 数据

恢复后：
filter + Room
    ↓
重新生成 TodoUiState
```

---

## 10. “原因”和“结果”的判断法

判断一个状态是否值得保存，可以问：

> 它是页面状态的“原因”，还是业务执行后的“结果”？

例如：

```text
搜索词
筛选条件
选中的 ID
```

这些更像输入条件，也就是“原因”。

它们很适合保存。

而：

```text
搜索结果
加载状态
网络错误
数据库查询结果
```

通常都是“结果”。

这些状态更适合重新产生，而不是恢复。

例如：

```text
query = "android"
        ↓
Repository
        ↓
Loading
        ↓
Result / Error
```

进程恢复后：

```text
恢复 query
        ↓
重新触发业务逻辑
        ↓
重新生成 Loading / Result / Error
```

---

## 11. 为什么不应该保存 isLoading

比如：

```text
isLoading = true
```

如果进程死亡：

```text
旧网络请求已经不存在
```

这时候新进程恢复：

```text
isLoading = true
```

反而可能造成错误状态，因为实际上并没有对应的旧请求继续运行。

所以：

```text
业务过程状态
业务结果状态
```

通常应该重新计算。

---

## 12. 和 rememberSaveable 的区别

两者都可以参与 Saved State 恢复，但状态归属不同。

```text
rememberSaveable
→ Compose UI 自己拥有的轻量 UI 状态

SavedStateHandle
→ ViewModel / 业务逻辑拥有的轻量可恢复状态
```

例如：

```text
某个局部 UI 控件是否展开
→ rememberSaveable

搜索关键字 query
而 ViewModel 要根据 query 查询数据
→ SavedStateHandle
```

---

## 13. 和 Room / DataStore 的区别

可以这样理解：

```text
SavedStateHandle
→ “帮我恢复刚才做到哪了”

Room / DataStore
→ “这是 App 真正拥有的数据，下次启动还得有”
```

例如：

```text
query = "android"
→ SavedStateHandle

用户设置
→ DataStore

Todo 列表
→ Room
```

---

## 14. 最终心智模型

`SavedStateHandle` 最适合保存：

```text
重新生成页面所需的
最小、轻量、临时输入状态
```

而不是：

```text
整个页面
整个 UiState
完整业务数据
完整网络结果
```

最后压缩成一句：

> `ViewModel` 解决 Activity 重建时的状态连续性，`SavedStateHandle` 再补上系统杀进程后的轻量状态恢复；真正长期存在的数据仍然属于 Room、DataStore 等持久化层。
