# Stage 2 · 目标 2：Activity、应用进程和 Task 的关系

## 1. 三个概念先彻底分开

Android 中最容易混在一起的是：

```text
Activity
Task
Process
```

它们分别回答三个不同的问题：

```text
Activity
→ 当前有哪些界面组件

Task
→ 用户经历过怎样的 Activity 导航路径

Process
→ 这些 Activity 的代码实际在哪里运行
```

最重要的关系可以画成：

```text
          Task
           ↑
           │ 导航归属
           │
        Activity
           │
           │ 运行归属
           ↓
         Process
```

也就是说：

> **Task 管导航，Process 管运行，Activity 同时参与这两个维度。**

---

## 2. Activity 不等于 App，也不等于 Process

一个应用进程里可以同时存在多个 Activity：

```text
App Process
│
├── Application
├── MainActivity instance
├── SettingsActivity instance
└── DetailActivity instance
```

因此：

```text
Activity ≠ App
Activity ≠ Process
```

Activity 是 Android 的一个应用组件。

真正的 Activity 对象实例运行在某个应用进程中。

---

## 3. 什么是 Task

Task 可以理解为：

> **Android 系统为一次连续的用户操作维护的一组 Activity 导航记录。**

其中 Activity 按打开顺序形成：

```text
Back Stack
```

例如用户依次进入：

```text
MainActivity
    ↓
SettingsActivity
    ↓
DetailActivity
```

Task 可以表示为：

```text
Task
│
├── DetailActivity      ← top
├── SettingsActivity
└── MainActivity        ← root
```

---

## 4. Task 是 Android 系统维护的

Task 不是：

```text
App Process
└── Task
```

也不是应用自己写了一个：

```kotlin
Stack<Activity>
```

更接近：

```text
Android 系统
│
├── Task A
│   └── Back Stack
│       ├── Activity C
│       ├── Activity B
│       └── Activity A
│
└── App Process
    ├── Activity A instance
    ├── Activity B instance
    └── Activity C instance
```

因此：

> **Task 以及 Task 内 Activity 的导航关系，本身就是 Android 系统管理的。**

Activity 本身并不需要保存：

```text
previousActivity
nextActivity
```

这种引用。

---

## 5. Task 不是 Process

这是本目标最重要的认识之一。

可能存在：

```text
Task A
├── Activity B
└── Activity A
```

同时：

```text
Process
├── Activity A instance
└── Activity B instance
```

看起来一一对应，但这只是最简单情况。

实际上：

```text
Task ≠ Process
```

因为它们是两个维度。

### Task

关注：

```text
用户从哪里来
现在在哪里
Back 后去哪里
```

### Process

关注：

```text
代码在哪里执行
对象存在于哪块内存
主线程在哪里
Application 在哪里
```

---

## 6. 一个 Task 可以涉及多个 App / Process

例如：

```text
Task A
│
├── App B / Activity B
└── App A / Activity A
```

运行上却可能是：

```text
Process A
└── Activity A instance

Process B
└── Activity B instance
```

所以：

```text
一个 Task
≠ 一个 App
≠ 一个 Process
```

Task 更关心用户操作是否属于同一段导航过程，而不是这些 Activity 来自哪个 APK。

---

## 7. 一个 App 也可以参与多个 Task

反过来也成立。

例如同一个 App：

```text
Task A
└── Activity A

Task B
└── Activity B
```

而两个 Activity 又可能都运行在：

```text
同一个 App Process
```

因此不能建立：

```text
一个 App
→ 一个 Process
→ 一个 Task
```

这种俄罗斯套娃式理解。

---

## 8. Recents 里看到的主要是 Task，不是 Process

手机上的：

```text
最近任务
Recents
```

里面一张张 App 卡片，可以大致理解成：

```text
Task A
Task B
Task C
...
```

用户看到的是：

```text
应用名称
图标
界面预览
```

而不是：

```text
taskId = 42
```

因此平时所谓的：

> “后台进程界面”

这个说法其实并不准确。

更接近：

> **最近 Task 界面。**

某张 Recents 卡片还存在：

```text
≠
对应的 App Process 一定还活着
```

---

## 9. Task 可以存在，而 Process 已经死亡

例如原来：

```text
Task
├── DetailActivity
├── SettingsActivity
└── MainActivity
```

以及：

```text
App Process
├── DetailActivity instance
├── SettingsActivity instance
└── MainActivity instance
```

后来系统为了回收内存杀掉进程：

```text
App Process
×
```

Activity 对象、Application、线程以及普通内存数据都会消失。

但是：

```text
Task 的导航状态
```

仍可能由系统保留。

因此：

```text
Process 死亡
≠ Task 必然消失
```

---

## 10. 用户重新进入 Task 时是按需恢复

假设一个 Task 中甚至有来自不同 App 的 Activity：

```text
Task
├── Activity C → Process C
├── Activity B → Process B
└── Activity A → Process A
```

三个进程后来都死了。

用户重新进入这个 Task 时，Android 不需要：

```text
一次性重建 Process A
一次性重建 Process B
一次性重建 Process C
```

而是当前首先需要显示：

```text
Activity C
```

于是大致：

```text
需要 Activity C
        ↓
Process C 不存在
        ↓
启动 Process C
        ↓
重新创建 Activity C
```

如果之后导航回 Activity B，才需要再启动它所属的 Process。

所以可以理解为：

> **Task 的恢复是按当前导航需要进行的，而不是把历史中所有 Process 一次性复活。**

---

## 11. Back 和 Home 不一样

### Back

普通情况下：

```text
Task
├── DetailActivity
├── SettingsActivity
└── MainActivity
```

执行 Back：

```text
DetailActivity finish
        ↓

Task
├── SettingsActivity
└── MainActivity
```

也就是：

> **在当前导航历史中往回走。**

---

### Home

Home 更像：

```text
整个 Task
→ 移到后台
```

通常不会因为 Home 就把：

```text
DetailActivity
SettingsActivity
MainActivity
```

一个个移出 back stack。

所以：

```text
Back
→ 主要操作导航历史

Home
→ 主要改变整个 Task 的前后台状态
```

现代 Android 中，根 Activity 上的 Back 行为还有版本差异，但当前阶段先不展开。

---

## 12. Back 也不是 Activity 自己寻找“上一个 Activity”

例如：

```text
Activity B
```

执行 Back 时，并不是：

```text
Activity B
→ previousActivity = Activity A
→ 手动返回 Activity A
```

Activity B 可以完全不知道 Activity A 是什么。

真正的模型是：

```text
Android 系统
    ↓
知道当前 Activity / Task 导航状态
    ↓
处理 Back
    ↓
决定接下来应该显示什么
```

所以：

> **导航关系属于 Android 系统，而不是 Activity 对象彼此直接维护。**

---

## 13. 默认 launchMode：standard

Activity 默认是：

```text
standard
```

不写 `android:launchMode` 时就是它。

假设：

```text
Task
├── DetailActivity
├── SettingsActivity #1
└── MainActivity
```

再次启动：

```text
SettingsActivity
```

通常会创建新实例：

```text
Task
├── SettingsActivity #2
├── DetailActivity
├── SettingsActivity #1
└── MainActivity
```

因此同一个 Activity 类型可以有多个实例。

---

## 14. singleTop

例如：

```xml
android:launchMode="singleTop"
```

它只关心：

> **Task 顶部是不是已经存在这个 Activity 类型的实例。**

如果：

```text
Task
├── SettingsActivity
└── MainActivity
```

再次启动 `SettingsActivity`：

```text
不创建新实例
        ↓
现有 SettingsActivity.onNewIntent()
```

但是如果：

```text
Task
├── DetailActivity
├── SettingsActivity
└── MainActivity
```

由于 `SettingsActivity` 不在顶部，再启动它仍然可以创建：

```text
SettingsActivity #2
DetailActivity
SettingsActivity #1
MainActivity
```

所以：

```text
singleTop
≠ 整个 Task 只能存在一个实例
```

它只是：

```text
顶部已经是我
→ 不连续重复创建我
```

---

## 15. singleTask

Manifest 可以声明：

```xml
<activity
    android:name=".MainActivity"
    android:launchMode="singleTask" />
```

如果相应的 `singleTask` Activity 已经存在，Android 可以找到已有实例：

```text
Task
├── DetailActivity
├── SettingsActivity
└── MainActivity    ← singleTask
```

再次启动 `MainActivity`：

```text
DetailActivity      ×
SettingsActivity    ×

Task
└── MainActivity
```

原来的 `MainActivity` 被复用，新 Intent 通过：

```kotlin
onNewIntent()
```

交给它。

如果它所在的 Task 原本在后台，该 Task 还可能被带到前台。

所以 `singleTask` 开始明显涉及：

```text
Activity
+
整个 Task
```

之间的关系。

---

## 16. singleInstance

例如：

```xml
<activity
    android:name=".PlayerActivity"
    android:launchMode="singleInstance" />
```

它与 `singleTask` 类似，但多了一条很强的限制：

> **它所在的 Task 只能有它自己一个 Activity。**

因此：

```text
Task A
└── PlayerActivity
```

如果 `PlayerActivity` 再启动：

```text
DetailActivity
```

`DetailActivity` 不能进入 Task A，而会进入其他 Task：

```text
Task A
└── PlayerActivity

Task B
└── DetailActivity
```

但这并不表示：

```text
PlayerActivity
→ 独立 Process
```

所以一定要记：

```text
Task 隔离
≠ Process 隔离
```

---

## 17. 跨 Task 的 Back 仍然由 Android 管理

假设：

```text
Task A
└── PlayerActivity

Task B
└── DetailActivity
```

`DetailActivity` 不需要知道：

```text
“我的前一个 Activity 是 PlayerActivity”
```

Android 系统维护整个启动和导航过程。

因此跨 Task 的情况下，Back 仍然可以把用户带回之前的 Activity。

可以把系统导航理解成两层：

```text
Android 系统
│
├── 管理各个 Task 内部的 back stack
│
└── 管理多个 Task 之间的导航和前后台关系
```

---

## 18. launchMode 控制的不是 Process

`launchMode` 主要回答：

```text
Activity 启动时
→ 应该怎样加入 / 复用 / 影响 Task
```

而不是：

```text
Activity 应该运行在哪个 Process
```

所以不能理解成：

```text
singleTask
→ 单独一个 Process        ×

singleInstance
→ 单独一个 Process        ×
```

它们讨论的是 **Task / Activity 导航组织方式**。

---

## 19. 最终整体模型

现在可以把 Activity、Task、Process 放到一张图里：

```text
                     Android 系统
                          │
             ┌────────────┴────────────┐
             │                         │
       导航 / Task 世界            运行 / Process 世界
             │                         │
          Task A                  Process A
             │                         │
       Activity A  ───────────── Activity A instance
       Activity B  ───────────── Activity B instance
             │
          Task B
             │
       Activity C  ───────────── Activity C instance
                                      │
                                  Process B
```

注意：

```text
Task 中的归属
和
Process 中的归属
```

完全可以交叉。

因此：

```text
一个 Task
可以包含多个 App / Process 的 Activity

一个 Process
也可以包含属于不同 Task 的 Activity 实例
```

---

## 20. 本目标最重要的认知

最终不用死背所有 `launchMode` 细节。

真正应该留下的是：

```text
Activity
→ Android 的界面组件

Task
→ Android 系统维护的用户导航结构

Back Stack
→ 一个 Task 内 Activity 的导航顺序

Process
→ Activity 等组件真正执行代码的运行环境
```

以及几个非常重要的“不等于”：

```text
Activity ≠ App

Activity ≠ Process

Task ≠ App

Task ≠ Process

Recents 卡片 ≠ Process

Task 还存在 ≠ Process 还活着

Task 隔离 ≠ Process 隔离
```

最后把整个目标压缩成一句：

> **Task 描述用户“走过哪些界面、接下来往哪退”，Process 描述代码“在哪里运行”，而 Activity 正好是这两套体系的交汇点。**

到这里，**Stage 2 · 目标 2：理解 Activity、应用进程和任务之间的大致关系**，完成。
