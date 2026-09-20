# Stage 1 - 目标 6：修改项目内容，并确认修改结果正确生效

## 目标

能够修改 Android 项目中的内容，并确认修改后的结果已经真正进入正在运行的 App。

阶段 1 学习规划中的目标 6：

> 能够修改项目内容，并确认修改结果正确生效。

---

## 这次实际做了什么

在 `MainActivity.kt` 中，把：

```kotlin
Greeting(
    name = "Android",
    modifier = Modifier.padding(innerPadding)
)
```

修改为：

```kotlin
Greeting(
    name = "mdrs",
    modifier = Modifier.padding(innerPadding)
)
```

`Greeting()` 中：

```kotlin
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}
```

因此运行结果从：

```text
Hello Android!
```

变成：

```text
Hello mdrs!
```

并在模拟器中确认修改已经生效。

---

## 最重要的认识

### 1. 源代码修改不等于运行中的 App 已经更新

修改 `.kt` 文件后，只能说明硬盘上的源码发生了变化。

真正确认修改生效，需要看到运行中的 App 出现预期结果。

最基本的验证闭环是：

```text
修改代码
   ↓
构建
   ↓
部署到设备
   ↓
启动 / 更新 App
   ↓
观察运行结果
```

所以：

> “代码改了” 和 “运行中的 App 已经使用新代码” 是两件不同的事。

---

## Build、Deploy 和 Run

### Build

Build 可以先理解为：

> 把 Android 工程中的源码、资源和配置处理成 Android 可以运行的应用。

大致关系：

```text
Android 工程
    ↓
   Build
    ↓
可安装 / 可运行的 Android 应用
```

Build 成功，只表示项目成功构建，并不表示 App 已经在设备上运行。

### Deploy

Deploy 是把构建结果安装或更新到目标设备中。

目标设备可以是：

- Android 模拟器
- Android 真机

### Run

Android Studio 中的 Run 可以粗略理解为：

```text
Build
  +
Deploy
  +
启动 App
```

因此平时点一次 Run，Android Studio 实际替我们完成了多步工作。

---

## 不一定每次都“完整重建”

开发过程中，Android Studio 和 Gradle 会尽量复用已有结果，只重新处理发生变化或受影响的部分。

所以重新 Run 并不意味着每一次都从零重新编译整个工程。

某些较小的代码或 UI 修改，也可能通过更快的更新机制进入正在运行的 App。

现阶段不需要深入区分这些机制，也不要简单把它们都叫作“热加载”。

现在只需要记住：

> 快速更新只是少做了一些不必要的工作。

如果怀疑当前运行结果不是最新的：

```text
重新 Run
```

如果仍然有异常：

```text
停止 App
→ 再重新启动
```

对 Stage 1 来说已经足够。

---

## 真机调试

使用真机时，Android Studio 仍然可以进行正常的调试。

只要真机完成相应调试配置，例如：

- 开启开发者选项
- 开启 USB 调试或无线调试
- 授权当前电脑
- 使用可调试的 App 构建

就可以在 Android Studio 中：

```text
Run
Debug
断点
单步执行
查看变量
Logcat
Attach to Process
```

因此模拟器和真机在开发调试流程上并不是两套完全不同的体系。

---

## ADB

**ADB = Android Debug Bridge**

可以把它理解成：

> 电脑与 Android 设备之间的调试和控制桥梁。

Android Studio 对模拟器或真机执行很多操作时，底层都会用到 ADB，例如：

```text
安装 APK
启动 App
查看 Logcat
连接调试器
Attach 到进程
执行 shell 命令
传输文件
```

关系可以粗略理解为：

```text
Android Studio
     ↓
    ADB
     ↓
模拟器 / 真机
```

虽然名称里有 `Debug`，ADB 并不只负责断点调试，它是一整套与 Android 设备通信和控制的工具。

---

## Stage 1 目标 6 到这里掌握什么就够了

现在能够做到：

- 修改项目代码。
- 重新 Run。
- 在模拟器中确认修改结果。
- 理解源码修改与运行结果之间需要经过构建和部署。
- 知道 Build、Deploy、Run 的基本关系。
- 知道开发工具会尽量复用已有结果，不一定每次完整重建。
- 知道结果不确定时可以直接重新 Run。
- 知道真机同样可以 Debug、看 Logcat、Attach to Process。
- 知道 ADB 是 Android Studio 与 Android 设备之间的重要通信工具。

到这里，**Stage 1 目标 6 可以视为完成**。

继续深入快速更新、进程状态、具体部署机制等内容，当前阶段收益不高；以后真正遇到相关问题时再展开即可。

---

## 当前参考资料

- Android Studio 官方介绍  
  https://developer.android.com/studio/intro?hl=zh-cn
- Kotlin 官方文档  
  https://kotlinlang.org/docs/home.html
- Jetpack Compose 官方文档  
  https://developer.android.com/develop/ui/compose/documentation?hl=zh-cn
