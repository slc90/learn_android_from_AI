# Stage 3：Android 界面开发
## 目标 1：使用 Jetpack Compose 构建常见应用界面

### 目标

能够使用 Jetpack Compose 的基础布局、常用 Material 组件和列表组件，组合出常见 Android 应用界面。

这一目标主要关注：

> 界面由什么组成，以及怎样把这些组件组织起来。

状态管理、重组等内容属于后续目标，不在这里深入。

---

## 1. Composable

Compose 中的 UI 由带有 `@Composable` 的函数描述：

```kotlin
@Composable
fun MyScreen() {
    Text("Hello")
}
```

Composable 可以继续调用其他 Composable，因此复杂界面本质上是组件树：

```text
Screen
├── Header
├── Content
│   ├── Card
│   └── Card
└── Button
```

这和 React 的组件组合思路比较接近。

---

## 2. 基础布局

### Column

子元素纵向排列：

```kotlin
Column {
    Text("A")
    Text("B")
}
```

```text
A
B
```

### Row

子元素横向排列：

```kotlin
Row {
    Button(onClick = {}) { Text("A") }
    Button(onClick = {}) { Text("B") }
}
```

```text
[A] [B]
```

### Box

多个子元素共享同一块区域，可以叠放：

```kotlin
Box(
    contentAlignment = Alignment.Center
) {
    Text("Hello")
}
```

常用于覆盖、定位等场景。

---

## 3. Arrangement 与 Alignment

`Column`：

```text
主轴：竖直
交叉轴：水平
```

常见配置：

```kotlin
Column(
    verticalArrangement = Arrangement.spacedBy(16.dp),
    horizontalAlignment = Alignment.CenterHorizontally
)
```

`Row`：

```text
主轴：水平
交叉轴：竖直
```

常见配置：

```kotlin
Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.CenterVertically
)
```

可以粗略理解为：

- `Arrangement`：沿主轴怎样排列
- `Alignment`：交叉轴怎样对齐

---

## 4. Modifier

`Modifier` 用来给 Composable 添加布局、尺寸、绘制和交互等规则：

```kotlin
Modifier
    .fillMaxWidth()
    .padding(16.dp)
    .background(Color.LightGray)
```

可以粗略理解成一串按顺序包裹 UI 的装饰器。

### 顺序很重要

```kotlin
Modifier
    .background(Color.LightGray)
    .padding(16.dp)
```

和：

```kotlin
Modifier
    .padding(16.dp)
    .background(Color.LightGray)
```

视觉效果不同。

因此 `Modifier` 不是一组无序属性，而是一条有顺序的处理链。

Composable 自己的参数和 Modifier 大致可以这样理解：

```text
Composable 参数
→ 这个组件是什么、显示什么、自身样式是什么

Modifier
→ 它怎样占空间、怎样被包裹、怎样参与布局和交互
```

两者边界并非绝对。

---

## 5. 尺寸与空间分配

### fillMaxWidth

```kotlin
Modifier.fillMaxWidth()
```

表示尽量占满父布局允许的横向空间。

### weight

在 `Row` 或 `Column` 中按比例分配剩余空间：

```kotlin
Row(
    modifier = Modifier.fillMaxWidth()
) {
    Button(
        modifier = Modifier.weight(2f),
        onClick = {}
    ) {
        Text("A")
    }

    Button(
        modifier = Modifier.weight(1f),
        onClick = {}
    ) {
        Text("B")
    }
}
```

两个按钮大致按照：

```text
2 : 1
```

分配剩余宽度。

如果还存在 `spacedBy()` 等固定间距，会先扣除这些空间，再分配剩余部分。

---

## 6. Material 组件

Compose Material3 已经提供了大量常用 UI 组件，例如：

```text
Text
Button
OutlinedButton
OutlinedTextField
Card
TopAppBar
...
```

因此常见界面通常不需要从最底层自己绘制。

### Card

```kotlin
Card(
    modifier = Modifier.fillMaxWidth()
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text("Stage 3")
        Text("Jetpack Compose")
    }
}
```

`Card` 是一个现成的 Material 视觉容器。

---

## 7. MaterialTheme

可以使用 Material Theme 中预定义的文字语义：

```kotlin
Text(
    text = "标题",
    style = MaterialTheme.typography.titleLarge
)

Text(
    text = "正文",
    style = MaterialTheme.typography.bodyMedium
)
```

相比直接指定：

```kotlin
fontSize = 24.sp
```

这种方式表达的是：

```text
这是标题
这是正文
```

而不是单纯指定一个具体字号。

---

## 8. Scaffold

`Scaffold` 是 Material 提供的常见页面骨架，不是 Compose 唯一的布局方式。

它可以组织：

```text
topBar
bottomBar
floatingActionButton
snackbarHost
content
```

例如：

```kotlin
Scaffold(
    topBar = {
        TopAppBar(
            title = {
                Text("Android 学习")
            }
        )
    }
) { innerPadding ->
    MyScreen(
        modifier = Modifier.padding(innerPadding)
    )
}
```

`innerPadding` 表示 Scaffold 已经被顶部栏、系统区域等占用的空间。

内容通过：

```kotlin
Modifier.padding(innerPadding)
```

避开这些区域。

---

## 9. LazyColumn

普通固定内容可以使用 `Column`。

长列表通常使用：

```kotlin
LazyColumn
```

例如：

```kotlin
val topics = listOf(
    "Compose 布局",
    "Material 组件",
    "界面状态"
)

LazyColumn {
    items(topics) { topic ->
        Text(topic)
    }
}
```

`LazyColumn` 可以理解为虚拟化纵向列表：

```text
数据
↓
items(...)
↓
根据需要生成列表项 UI
```

和 WPF 中：

```text
ItemsControl / ListBox
+
虚拟化 Panel
```

的思路比较接近。

### items 是泛型的

并不只能使用 `String`：

```kotlin
data class Topic(
    val title: String,
    val description: String
)
```

可以直接：

```kotlin
items(topics) { topic ->
    TopicItem(topic)
}
```

其中 `topic` 会自动推导成 `Topic`。

使用 `List<T>` 版本的 `items` 时需要：

```kotlin
import androidx.compose.foundation.lazy.items
```

否则可能只匹配到接收 `Int` 数量的另一个 `items` 重载。

---

## 10. 拆分自己的 Composable

实际界面不会全部写在一个函数里。

例如：

```kotlin
@Composable
fun TopicItem(topic: Topic) {
    Card {
        Column {
            Text(topic.title)
            Text(topic.description)
        }
    }
}
```

列表只负责组织：

```kotlin
LazyColumn {
    items(topics) { topic ->
        TopicItem(topic)
    }
}
```

于是界面结构可以自然拆成：

```text
Screen
└── LearningList
    └── LazyColumn
        ├── TopicItem
        ├── TopicItem
        └── TopicItem
```

Compose 构建复杂 UI 的基本方式就是：

> 用小 Composable 不断组合成更大的 Composable。

---

## 11. 事件也是普通参数

例如：

```kotlin
@Composable
fun TopicItem(
    topic: Topic,
    onClick: () -> Unit
)
```

调用：

```kotlin
TopicItem(
    topic = topic,
    onClick = {
        println(topic.title)
    }
)
```

从 Kotlin 语言角度看：

```text
topic   : Topic
onClick : () -> Unit
```

都是普通参数。

`onClick` 只是一个函数值，并不存在特殊的语言级“事件”机制。

---

## 12. 当前形成的 Compose UI 心智模型

目前可以把 Compose 界面理解成：

```text
Composable
    ↓ 组合
页面组件树
    ↓
Column / Row / Box
    ↓
Text / Button / Card / TextField ...
    ↓
Modifier 控制布局、尺寸、绘制、交互
```

一个简单页面可能是：

```text
Scaffold
├── TopAppBar
└── Content
    └── LazyColumn
        ├── TopicItem
        │   └── Card
        │       ├── Text
        │       └── Text
        ├── TopicItem
        └── TopicItem
```

Compose 的核心风格不是寻找一个“万能控件”，而是：

> 使用小型 Composable 和布局容器不断组合出完整界面。

---

## 目标 1 完成情况

目前已经实际接触并运行过：

- `@Composable`
- `Column`
- `Row`
- `Box`
- `Arrangement`
- `Alignment`
- `Modifier`
- `padding`
- `background`
- `fillMaxWidth`
- `weight`
- `Text`
- `Button`
- `Card`
- `MaterialTheme.typography`
- `Scaffold`
- `TopAppBar`
- `LazyColumn`
- `items`
- 自定义 Composable
- Composable 参数与事件回调

已经能够理解并构建一个由：

```text
页面骨架
+
基础布局
+
Material 组件
+
列表
+
自定义组件
```

组成的常见静态 Android 界面。

因此：

**Stage 3 · 目标 1 可以视为完成。**

---

## 下一目标

Stage 3 · 目标 2：

> 理解声明式 UI 的基本思路。

之前为了演示 `OutlinedTextField` 已经短暂接触过：

```kotlin
remember
mutableStateOf
```

以及重组的概念。

这些内容暂时视为目标 2 的预览，目标 1 不继续展开。
