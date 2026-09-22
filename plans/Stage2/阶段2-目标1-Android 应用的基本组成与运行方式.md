# Stage 2 · 目标 1：Android 应用的基本组成与运行方式

## 1. 核心认识

Android App 和普通从 `main()` 开始执行的程序不同。

普通程序更像：

```text
main()
  ↓
程序代码自己控制后续执行
```

Android App 更像：

```text
Android 系统
    ↓
根据需要启动应用组件
    ↓
必要时创建应用进程
    ↓
调用我们的代码
```

所以 Android 开发中很重要的一点是：

> **应用不是自己从某个固定入口一路运行下去，而是由 Android 系统管理进程、组件和生命周期，并在合适的时候调用我们的代码。**

---

# 2. Android 应用的核心组件

Android App 中有四类主要应用组件：

```text
Android App
│
├── Activity
│   └── 主要负责和用户交互的界面入口
│
├── Service
│   └── 没有直接 UI 的工作或能力
│
├── BroadcastReceiver
│   └── 接收系统或其他应用发送的广播事件
│
└── ContentProvider
    └── 向其他组件或应用提供结构化数据访问
```

这些组件不是 App 进程本身，而是运行在应用进程中的 Android 组件。

例如：

```text
应用进程
│
├── MainActivity
├── SettingsActivity
├── 某个 Service
├── 某个 Receiver
└── 某个 Provider
```

默认情况下，一个 App 的这些组件通常运行在同一个应用进程里。

---

# 3. Activity 不是 App，也不是进程

例如：

```kotlin
class MainActivity : ComponentActivity()
```

`MainActivity` 是一个 Activity 类型。

它不是：

```text
整个 App
```

也不是：

```text
一个独立进程
```

例如：

```text
应用进程
│
├── MainActivity
└── SettingsActivity
```

这是非常正常的结构。

因此：

```text
Activity ≠ App
Activity ≠ 进程
```

---

# 4. AndroidManifest.xml 的作用

`AndroidManifest.xml` 可以理解成：

> **App 向 Android 系统提供的一份声明信息。**

例如里面会声明：

```xml
<activity>
<service>
<receiver>
<provider>
```

它告诉 Android：

```text
这个 App 有哪些组件
哪些组件可以被启动
哪些组件可以响应哪些 Intent
应用需要哪些系统能力
……
```

Manifest 本身不是程序入口，也不会像 `main()` 一样执行。

---

# 5. MAIN + LAUNCHER

当前项目的 `MainActivity` 中有：

```xml
<intent-filter>
    <action android:name="android.intent.action.MAIN" />
    <category android:name="android.intent.category.LAUNCHER" />
</intent-filter>
```

可以理解为：

> 这个 Activity 可以作为 Launcher 中的应用启动入口。

因此：

```text
MAIN + LAUNCHER
        ↓
Launcher 可以发现这个 Activity
        ↓
为它建立应用启动入口
```

如果两个 Activity 都声明：

```text
MAIN + LAUNCHER
```

那么 Launcher 可以发现两个启动入口。

每个入口内部会对应具体的 Activity，因此用户点击某个图标时，Launcher 已经知道应该启动哪个 Activity，并不会临时在两个 Activity 之间随机选择。

另外：

```text
MainActivity
```

这个名字本身没有特殊意义。

真正让它成为桌面启动入口的是：

```text
MAIN + LAUNCHER
```

而不是它叫 `MainActivity`。

---

# 6. Intent

Intent 可以先理解成：

> **交给 Android 系统的一份“请求”。**

例如：

```kotlin
val intent = Intent(this, SettingsActivity::class.java)
startActivity(intent)
```

不是：

```text
MainActivity 自己 new SettingsActivity
```

而是：

```text
MainActivity
    ↓
构造 Intent
    ↓
调用 startActivity()
    ↓
向 Android 系统提出请求
    ↓
Android 创建 / 启动 SettingsActivity
```

---

# 7. 显式 Intent 与隐式 Intent

## 显式 Intent

直接指定目标组件：

```kotlin
Intent(this, SettingsActivity::class.java)
```

意思近似：

```text
就启动 SettingsActivity
```

---

## 隐式 Intent

不指定具体组件，而是描述：

```text
我要做什么
```

例如：

```kotlin
Intent(
    Intent.ACTION_VIEW,
    Uri.parse("https://developer.android.com")
)
```

意思近似：

```text
我要查看这个网址
```

Android 再去寻找能够处理这个 Intent 的组件。

---

# 8. Intent 与 intent-filter 的关系

可以简单理解成：

```text
Intent
= 我提出什么请求

intent-filter
= 我声明自己能处理什么请求
```

例如：

```text
隐式 Intent
      ↓
Android 查找匹配的 intent-filter
      ↓
找到能够处理它的组件
```

---

# 9. Intent 可以携带额外数据

Intent 不仅可以指定“去哪里 / 做什么”，还可以携带额外数据：

```kotlin
intent.putExtra("userId", 123)
```

目标 Activity 可以读取：

```kotlin
intent.getIntExtra("userId", -1)
```

因此可以理解成：

```text
Intent
│
├── 目标 / action
│
└── extras
       ↓
    附带数据
```

`extra` 和 `intent-filter` 不是同一类东西。

```text
extra
→ 实际随 Intent 携带的数据

intent-filter
→ 组件声明自己能够接受什么 Intent
```

---

# 10. `SettingsActivity::class.java`

```kotlin
SettingsActivity::class
```

表示：

> 获取 `SettingsActivity` 这个类型本身的信息。

它得到的是 Kotlin 的类型对象 `KClass`。

而：

```kotlin
SettingsActivity::class.java
```

表示：

> 把 Kotlin 的 `KClass` 转换成 Java 的 `Class` 对象。

它不是：

```kotlin
SettingsActivity()
```

后者才是在创建普通对象实例。

因此：

```text
SettingsActivity()
→ 创建对象

SettingsActivity::class
→ 获取类本身的信息

SettingsActivity::class.java
→ 获取 Java Class 对象
```

Activity 的真正实例仍然由 Android 系统创建。

---

# 11. Android 应用进程

Android 中的“进程”和桌面 Windows / Linux 中的进程，本质上是同一个操作系统概念。

可以理解成：

> 操作系统为运行程序建立的独立运行环境。

通常一个 Android App 默认运行在一个应用进程里。

例如：

```text
应用进程
│
├── Application
├── MainActivity
├── SettingsActivity
├── Service
└── Receiver
```

Activity 被销毁并不代表进程一定立即退出。

例如：

```text
MainActivity 被关闭
        ↓
Activity 对象销毁
        ↓
应用进程可能仍然存在
```

Android 系统会根据资源情况决定进程什么时候被回收。

---

# 12. App 已安装、进程存在、界面显示是三回事

可能存在：

```text
状态 1：
App 已安装
进程不存在
```

```text
状态 2：
App 已安装
进程存在
但 Activity 没在前台
```

```text
状态 3：
App 已安装
进程存在
Activity 正在显示
```

所以：

```text
App 已安装
≠ App 进程一定存在
≠ App 一定正在显示
```

---

# 13. Application

Android 创建应用进程后，会创建一个 `Application` 对象。

可以简单理解成：

> **应用进程级别的对象。**

结构大致：

```text
应用进程
│
├── Application
├── MainActivity
├── SettingsActivity
├── Service
└── Receiver
```

通常一个普通单进程 App 有一个 `Application` 实例。

即使开发者没有自定义：

```kotlin
class MyApplication : Application()
```

Android 仍然会创建默认的 `Application` 对象。

---

## Application 常见作用

### 全局初始化

例如：

```text
日志系统
数据库
某些 SDK
进程级依赖
```

可以在：

```kotlin
Application.onCreate()
```

附近完成初始化。

---

### 提供 Application Context

例如：

```kotlin
applicationContext
```

它属于应用级 Context，不依赖某一个 Activity。

---

# 14. Context

Context 可以理解成：

> **进入 Android 系统能力的一种“环境入口”。**

常见的两个：

```text
Activity Context
Application Context
```

---

## Activity Context

和某个具体 Activity 绑定。

它知道：

```text
当前 Activity
当前主题
当前 Window
当前界面环境
```

所以需要具体界面能力时通常使用 Activity Context。

例如：

```text
Dialog
窗口操作
依赖 Activity Theme 的 UI
```

---

## Application Context

和整个应用进程绑定。

它知道：

```text
当前属于哪个 App
应用资源
文件
数据库
部分系统服务
……
```

但它并不知道具体某个 Activity 的：

```text
Window
界面状态
Activity Theme
```

所以：

```text
生命周期更长
≠ 能力更多
```

Activity Context 和 Application Context 是不同用途的 Context。

---

# 15. 主线程

Android 应用进程中最重要的线程是：

```text
Main Thread
```

主线程主要负责：

```text
Activity 生命周期回调
用户输入
点击事件
UI 更新
界面绘制
很多系统回调
```

这和 WPF 中的 UI Thread 思路非常接近。

因此耗时任务不能长时间阻塞主线程，否则：

```text
主线程被占住
    ↓
点击事件无法及时处理
UI 无法及时刷新
动画卡住
    ↓
严重时可能发生 ANR
```

---

# 16. Looper、MessageQueue、Handler

Android 主线程采用消息循环模型。

## MessageQueue

负责：

> 排队保存等待处理的消息 / 任务。

---

## Looper

负责：

> 不断从 MessageQueue 中取出消息并进行分发。

---

## Handler

负责：

> 向某个 Looper 对应的 MessageQueue 投递消息 / 任务，并接收对应消息的分发。

关系可以理解成：

```text
Handler
   ↓
发送任务
   ↓
MessageQueue
   ↓
Looper 取出消息
   ↓
根据 Message.target 分发
   ↓
对应 Handler / callback
   ↓
在线程上执行
```

同一个线程可以有：

```text
一个 Looper
一个 MessageQueue
多个 Handler
```

例如：

```text
Main Thread
│
├── Looper
├── MessageQueue
├── Handler A
├── Handler B
└── Handler C
```

每条 Message 会记录目标 Handler。

---

# 17. Handler 最终在哪个线程执行

决定 Handler 工作线程的不是：

```text
Handler 在哪里创建
```

而是：

```text
Handler 绑定哪个 Looper
```

例如：

```kotlin
Handler(Looper.getMainLooper())
```

无论在哪里创建，这个 Handler 最终都把任务送到：

```text
主线程 MessageQueue
```

因此任务最终在主线程执行。

Handler 本身不会自动创建新线程。

---

# 18. ActivityThread

应用进程启动后，Android Framework 内部有一个核心类：

```text
ActivityThread
```

可以先理解为：

> **Android Framework 在 App 进程中的核心调度对象。**

它负责协助系统：

```text
建立主线程运行环境
创建 Application
创建 Activity
调度组件生命周期
接收 Android 系统的组件调度
```

普通 Android App 开发不会直接控制 `ActivityThread`。

它属于 Android Framework 内部机制。

---

# 19. AOSP

AOSP：

```text
Android Open Source Project
```

即：

```text
Android 开源项目
```

可以理解成：

> Android 系统和 Android Framework 的开源源码工程。

普通应用开发主要面对：

```text
Android SDK
Jetpack
Compose
```

一般不需要直接接触 AOSP。

只有研究系统底层实现、Framework 行为或系统开发时，才会更频繁查看 AOSP。

---

# 20. Android App 冷启动的大致过程

把目前所有东西串起来：

```text
用户点击桌面入口
        ↓
Launcher
        ↓
请求 Android 系统启动目标 Activity
        ↓
Android 发现 App 进程不存在
        ↓
创建应用进程
        ↓
建立主线程运行环境
        ↓
ActivityThread
        ↓
Looper / MessageQueue
        ↓
创建 Application
        ↓
Application.onCreate()
        ↓
创建 MainActivity
        ↓
MainActivity.onCreate()
        ↓
setContent { ... }
        ↓
Compose UI 显示
```

---

# 21. App 进程被回收

用户离开 App 后：

```text
Activity 不在前台
        ↓
进程可能仍然保留
```

但 Android 系统可能因为资源需要而回收整个进程。

一旦进程被终止：

```text
Application 消失
Activity 对象消失
普通内存对象消失
内存变量消失
主线程消失
Looper / MessageQueue 消失
```

因此：

```text
只存在内存里的状态
→ 进程死亡后丢失
```

而：

```text
数据库
文件
SharedPreferences
```

这类持久化数据不会因为进程死亡而自动消失。

---

# 22. Cold / Warm / Hot Start

## Cold Start

```text
应用进程不存在
    ↓
创建新进程
    ↓
重新创建 Application
    ↓
重新创建 Activity
```

---

## Warm Start

大致处于中间状态：

```text
进程可能仍然存在
但 Activity / 界面需要重新创建
```

---

## Hot Start

```text
应用进程仍存在
Activity / UI 基本仍存在
    ↓
快速回到前台
```

所以可以粗略记成：

```text
Cold → 进程都需要重新创建
Warm → 进程可能还在，但部分界面需要重建
Hot  → 进程和界面基本都还在
```

---

# 23. 最终运行模型

现在可以把 Android App 的整体结构整理成：

```text
                Android 系统
                     │
          按需启动某个应用组件
                     │
            App 进程存在吗？
              │         │
             否         是
              ↓         ↓
           创建进程   使用现有进程
              │
         建立主线程环境
              │
     Looper + MessageQueue
              │
        创建 Application
              │
       创建 / 调度组件
              │
 ┌────────────┼────────────┐
 ↓            ↓            ↓
Activity    Service      Receiver
                         Provider
```

应用运行期间：

```text
组件可以创建 / 销毁
        ↓
应用进程可能继续存在
        ↓
Android 也可能回收整个进程
        ↓
下次需要时重新创建
```

---

# 24. 本目标最重要的认知

这一目标最终要建立的不是 API 记忆，而是一张 Android App 的运行地图：

```text
Android 系统掌握组件和进程的调度权

App 不是一个从 main() 一直跑到底的程序

Activity / Service / Receiver / Provider
是系统可以调度的应用组件

Activity 不等于 App
Activity 不等于进程

Application 是进程级对象

Context 表示 Android 环境入口

主线程负责 UI 和大量组件回调

Looper + MessageQueue 构成线程的消息循环

进程可能随时被 Android 系统回收

下次运行时可能重新创建整个运行环境
```

到这里，Stage 2 · 目标 1：

> **理解 Android 应用的基本组成与运行方式**

完成。
