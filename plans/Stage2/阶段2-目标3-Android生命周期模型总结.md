# Stage 2 · 目标 3：Android 生命周期模型总结

> 实验环境：Android 17 / API 37.1

## 1. 核心认识

Activity 生命周期不是一条必须完整执行的固定流水线，而是 Android 系统根据 Activity 当前状态变化，调用相应 callback 通知应用。

```text
Android 系统
    ↓
Activity 状态变化
    ↓
如果应用进程仍然活着
    ↓
调用对应生命周期 callback
```

因此生命周期图应该理解成“状态转换图”，而不是：

```text
onCreate → onStart → onResume → onPause → onStop → onDestroy
```

这条一定完整执行的程序流程。

---

## 2. 主要生命周期回调

正常启动：

```text
onCreate
   ↓
onStart
   ↓
onResume
```

离开前台时可能经历：

```text
onPause
   ↓
onStop
   ↓
onDestroy
```

重新从 `STOPPED` 状态回来：

```text
onRestart
   ↓
onStart
   ↓
onResume
```

但只要进程直接死亡，后续 callback 就不再有保证。

---

## 3. 两个最重要的生命周期范围

### 可见生命周期

```text
onStart
   ↓
Activity 可见
   ↓
onStop
```

只要 Activity 对用户仍然可见，就可能处于这个范围。

### 前台交互生命周期

```text
onResume
   ↓
Activity 位于前台并承担主要用户交互
   ↓
onPause
```

因此：

```text
STARTED
→ Activity 可以仍然可见

RESUMED
→ Activity 可见，并且当前处于前台交互状态
```

---

## 4. “可见”和“当前可交互”不是一回事

普通全屏 `SecondActivity` 覆盖 `MainActivity` 时，我们观察到：

```text
MainActivity onPause

SecondActivity onCreate
SecondActivity onStart
SecondActivity onResume

MainActivity onStop
```

因为 MainActivity 最终完全不可见。

而把 `SecondActivity` 改成半透明窗口后：

```text
MainActivity onPause

SecondActivity onCreate
SecondActivity onStart
SecondActivity onResume
```

这时 `MainActivity onStop` 没有出现。

说明：

```text
MainActivity
→ 仍然可见
→ 保持 STARTED

但已经失去前台交互位置
→ 不再 RESUMED
```

所以：

```text
可见
≠
当前前台交互
```

---

## 5. Activity 内部 UI Mask 不等于生命周期变化

如果只是同一个 Activity 内部的 Compose UI 加一个灰色 Mask：

```text
MainActivity
└── Compose
    ├── 页面内容
    └── 灰色 Mask
```

即使 Mask 把点击全部拦截，MainActivity 在 Activity 生命周期层面仍然可能保持：

```text
RESUMED
```

因为：

```text
UI 是否允许点击
```

和：

```text
Activity 在系统中的生命周期状态
```

是两个不同层次的问题。

---

## 6. 两个 Activity 的生命周期会交错

MainActivity 启动 SecondActivity 时：

```text
MainActivity onPause

SecondActivity onCreate
SecondActivity onStart
SecondActivity onResume

MainActivity onStop
```

Android 并不是先把旧 Activity 完全停掉，再启动新 Activity。

这也意味着：

> `onPause()` 应该快速完成，不适合塞入耗时同步工作。

---

## 7. Back 返回时发生了什么

从普通全屏 `SecondActivity` Back：

```text
SecondActivity onPause

MainActivity onRestart
MainActivity onStart
MainActivity onResume

SecondActivity onStop
SecondActivity onDestroy
```

MainActivity 之前处于 `STOPPED`，因此回来需要：

```text
onRestart → onStart → onResume
```

而半透明 SecondActivity 覆盖 MainActivity 时，MainActivity 从未进入 `STOPPED`。

所以 Back 后 MainActivity 可以直接：

```text
onResume
```

---

## 8. Home 和 Back 不是同一种场景

按 Home：

```text
Activity
→ 离开前台
→ Task 进入后台
```

实验观察：

```text
onPause
onStop
```

回来：

```text
onRestart
onStart
onResume
```

而 Back 通常表示当前 Activity 的导航结束，例如 SecondActivity 最终进入：

```text
onDestroy
```

在 Android 17 / API 37.1 的实验里，对根 Launcher Activity 按 Back 时，没有简单表现为“立刻 onDestroy”，因此不能把：

```text
Back = Activity 一定 onDestroy
```

当成通用规则。

---

## 9. Configuration Change 会重建 Activity

用旋转屏幕做实验时：

```text
旧 Activity：
onPause
onStop
onDestroy

新 Activity：
onCreate
onStart
onResume
```

因此：

> 用户感觉还是同一个页面，但 Activity 对象已经换了。

要区分：

```text
Activity 类
≠
Activity 对象实例
≠
用户眼中的“这个页面”
```

---

## 10. 普通成员变量会随 Activity 实例消失

实验中：

```kotlin
private var counter = 0
```

点击到：

```text
counter = 3
```

旋转后 Activity 被重建，新实例重新初始化：

```text
counter = 0
```

说明普通成员变量属于当前 Activity 对象实例。

Activity 实例销毁后，这些普通内存状态也会消失。

---

## 11. savedInstanceState 与 Bundle

加入：

```kotlin
override fun onSaveInstanceState(outState: Bundle) {
    outState.putInt("counter", counter)
    super.onSaveInstanceState(outState)
}
```

再在 `onCreate()` 中恢复：

```kotlin
counter = savedInstanceState?.getInt("counter") ?: 0
```

旋转后：

```text
旧 Activity counter = 3
        ↓
旧实例销毁
        ↓
新 Activity 创建
        ↓
savedInstanceState 恢复 counter = 3
```

`Bundle` 可以先理解成 Android 常用的小型键值容器：

```text
Bundle
├── "counter" → 3
├── "name"    → "abc"
└── ...
```

它适合保存少量页面重建状态，不是数据库、文件或长期持久化存储。

---

## 12. onSaveInstanceState 不是每次离开都调用

我们给 SecondActivity 加入：

```kotlin
override fun onSaveInstanceState(outState: Bundle) {
    logLifecycle("onSaveInstanceState")
    super.onSaveInstanceState(outState)
}
```

然后：

```text
MainActivity
→ SecondActivity
→ Back
```

实验中 SecondActivity 没有调用 `onSaveInstanceState()`。

因为 Back 明确结束了这个 Activity，系统通常没有以后再恢复它的需要。

所以不能把：

```text
onSaveInstanceState
```

理解成“Activity 离开前必定调用的保存函数”。

---

## 13. savedInstanceState 不是重要业务数据的持久化方案

应该分开理解：

```text
savedInstanceState
→ Activity 重建时恢复少量临时 UI 状态

持久化存储
→ App / Process 死亡后也不能丢的重要数据
```

重要订单、用户输入、业务数据等，不应该等 `onDestroy()` 或 `onSaveInstanceState()` 才保存。

---

## 14. Activity Recreation 和 Process Death 不一样

### Activity 重建

例如旋转：

```text
Process 仍然存在

旧 Activity instance
        ↓
销毁

新 Activity instance
        ↓
创建
```

即：

```text
Activity 换了
Process 没换
```

### Process Death

进程被杀：

```text
整个 App Process
        ↓
直接消失
```

其中的：

```text
Application
Activity 对象
普通 Kotlin / Java 对象
线程
内存变量
```

都会一起消失。

---

## 15. PID、Activity 实例与 identityHashCode

实验使用：

```kotlin
Process.myPid()
```

旧进程：

```text
pid = 15205
```

执行：

```bash
adb shell am kill com.example.learnandroidfromai
```

重新进入后：

```text
pid = 15269
```

PID 改变说明旧进程已经死亡，新进程已经创建。

最初用：

```kotlin
System.identityHashCode(this)
```

区分 Activity 实例时，两个不同进程里碰巧出现了相同数字。

这并不表示对象相同，因为 `identityHashCode` 不是：

```text
内存地址
全局唯一 ID
跨进程唯一 ID
UUID
```

后来改用：

```kotlin
UUID.randomUUID().toString()
```

更直观地区分 Activity 实例。

---

## 16. Process Death + 状态恢复实验

完整实验：

```text
启动 MainActivity
counter 点击到 3

↓ Home

onPause
onStop
onSaveInstanceState(counter = 3)

↓ adb shell am kill

旧进程死亡
没有 onDestroy

↓ 从 Recents 返回

新进程
新 Activity

onCreate(savedInstanceState)
counter 恢复为 3
```

这个实验同时证明：

```text
PID 改变
→ Process 换了

Activity UUID 改变
→ Activity instance 换了

counter 仍然是 3
→ savedInstanceState 恢复成功
```

---

## 17. onDestroy 不是可靠的“最后回调”

Process Death 实验中没有出现：

```text
onDestroy
```

因为：

```text
进程已经不存在
→ Activity 对象也不存在
→ 没有代码继续执行 callback
```

所以：

> `onDestroy()` 不是可靠的最后保存机会。

---

## 18. 不只是 onDestroy，任何后续回调都可能缺席

只要进程直接死亡，后续 callback 链就会立刻终止。

例如理论上可能：

```text
onCreate
↓
进程死亡
```

于是：

```text
onStart
onResume
```

都不会继续发生。

也可能：

```text
onPause
↓
进程死亡
```

于是：

```text
onStop
onDestroy
```

都不会发生。

因此最准确的心智模型是：

> **进程还活着时，Android 根据状态变化调用相应 callback；进程一旦死亡，后续 callback 不再有保证。**

---

## 19. Activity 生命周期和 Process 生命周期必须分开

最重要的关系：

```text
Activity lifecycle
≠
Process lifecycle
```

可能出现：

```text
Activity 被销毁
但 App Process 仍然存在
```

也可能：

```text
App Process 直接死亡
↓
其中所有 Activity / Application / 内存对象一起消失
```

所以 Activity 是 Process 中的对象，而不是 Process 本身。

---

## 20. 生命周期回调在开发中的判断方式

不要先问：

```text
“这段代码应该背下来放在哪个生命周期函数？”
```

应该先问：

```text
“这个东西应该活到 Activity 的哪个状态？”
```

### 只要页面可见就需要

围绕：

```text
onStart / onStop
```

考虑。

### 只有页面处于前台交互时才需要

围绕：

```text
onResume / onPause
```

考虑。

### Activity 重建后需要恢复少量临时 UI 状态

使用：

```text
savedInstanceState
```

### App / Process 死亡后也不能丢

使用真正的：

```text
持久化
```

---

## 21. 最终生命周期模型

```text
Activity instance 创建
        ↓
     onCreate
        ↓
     onStart
        ↓
     onResume
        ↓
┌─────────────────┐
│ RESUMED          │
│ 可见 + 前台交互 │
└─────────────────┘
        ↓
     onPause
        ↓
┌─────────────────┐
│ STARTED          │
│ 仍可能可见       │
│ 但不再前台交互   │
└─────────────────┘
        ↓
     onStop
        ↓
┌─────────────────┐
│ STOPPED          │
│ 不可见           │
└─────────────────┘
        │
        ├── 回来
        │     ↓
        │  onRestart
        │     ↓
        │  onStart
        │     ↓
        │  onResume
        │
        └── Activity 结束
              ↓
           onDestroy
```

这张图旁边必须永远补一条：

```text
任意时刻
   ↓
Process Death
   ↓
整个进程直接消失
   ↓
后续生命周期 callback 不保证出现
```

---

## 22. 本目标最重要的认知

最终需要留下的是：

```text
生命周期是状态变化模型
不是固定执行脚本

STARTED
→ Activity 仍然可以可见

RESUMED
→ Activity 当前处于前台交互状态

Activity 可以被销毁并重新创建
→ 普通成员变量不能假设永远存在

savedInstanceState
→ 用于 Activity 重建时恢复少量临时状态

savedInstanceState
≠ 业务数据持久化

Activity lifecycle
≠ Process lifecycle

Process 一旦死亡
→ 后续生命周期 callback 都不再有保证

onDestroy
≠ 可靠的最后保存机会
```

最后压缩成一句：

> **Android 生命周期描述的是系统当前允许这个 Activity 处于什么状态，而不是保证 Activity 会完整经历一套固定回调；真正写代码时，要同时考虑可见性、交互状态、Activity 重建和 Process Death。**

到这里，**Stage 2 · 目标 3：理解 Android 的生命周期模型**，完成。
