# 阶段 1 · 目标 1：创建并运行第一个 Android 项目

## 目标

完成阶段 1 的第一个目标：

> 能够独立创建一个 Android 项目。

本次项目使用：

- Android Studio
- Kotlin
- Jetpack Compose
- Kotlin DSL
- Android 模拟器
- Git

---

## 项目创建配置

本次使用 `Empty Activity` 模板。

```text
Template: Empty Activity
Name: learn_android_from_AI
Package name: com.example.learnandroidfromai
Minimum SDK: API 24 (Android 7.0)
Build configuration language: Kotlin DSL
```

### 为什么选择 Empty Activity

`Empty Activity` 会生成一个基础的 Jetpack Compose Android 项目，适合作为当前学习路线的起点。

### Minimum SDK

本次选择：

```text
API 24 (Android 7.0)
```

`minSdk = 24` 表示应用最低支持 Android 7.0。

它不表示：

- 项目只能使用 Android 7.0 的功能；
- 模拟器必须运行 Android 7.0；
- 项目的编译版本或目标版本就是 Android 7.0。

这些概念之后再单独学习。

---

## 已有 Git 仓库与 Android Studio

仓库原本已经存在：

```text
learn_android_from_AI/
├── .git/
└── plans/
```

最开始直接用 Android Studio 打开仓库时，IDE 自动生成了 `.idea/`。

这时仓库还不是 Android 项目，`.idea/` 只是 Android Studio / IntelliJ 为当前目录创建的 IDE 配置。

之后尝试直接将 `New Project` 的保存位置指定为这个已有且非空的仓库根目录。

Android Studio 显示警告：

```text
already exists at the specified Save location and it is not empty
```

本次实际操作中，点击 `Finish` 后并没有生成 `app/`、Gradle 配置等 Android 项目内容。

最终采用：

```text
先在新的空目录中创建 Android 项目
        ↓
确认项目正常生成
        ↓
将生成的 Android 项目内容复制到原 Git 仓库根目录
        ↓
保留原来的 .git/ 和 plans/
        ↓
重新用 Android Studio 打开仓库并完成 Gradle Sync
```

最终目录大致为：

```text
learn_android_from_AI/
├── .git/
├── .gradle/
├── .idea/
├── app/
├── gradle/
├── plans/
├── .gitignore
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── local.properties
└── settings.gradle.kts
```

其中：

- `.git/`：原 Git 仓库数据；
- `plans/`：学习规划与学习记录；
- `.idea/`：Android Studio / IntelliJ 项目配置；
- `.gradle/`：Gradle 的本地工作数据与缓存；
- `app/`：当前 Android 应用模块；
- `gradle/`：Gradle Wrapper、version catalog 等 Gradle 项目文件；
- 根目录的 Gradle 文件：负责项目构建配置。

---

## `.gitignore` 与版本控制

项目里出现了多份 `.gitignore`，它们的作用范围不同。

### 根目录 `.gitignore`

根目录：

```text
learn_android_from_AI/.gitignore
```

负责整个仓库范围的忽略规则。

当前模板已经会忽略一部分明显不应提交的本地内容，例如：

```text
*.iml
.gradle
/local.properties
/.idea/caches
/.idea/libraries
/.idea/modules.xml
/.idea/workspace.xml
/build
```

其中比较重要的几类是：

- `.gradle/`：本地 Gradle 工作数据与缓存；
- `local.properties`：包含本机环境信息，例如 Android SDK 路径；
- `build/`、`app/build/`：构建输出；
- `workspace.xml`：个人 IDE 工作区状态。

这些不进入 Git。

### `app/.gitignore`

`app/.gitignore` 当前主要用于忽略：

```text
/build
```

因为它位于 `app/` 下，所以这里的 `/build` 指的是：

```text
app/build/
```

### `.idea/.gitignore`

`.idea/.gitignore` 当前包含：

```gitignore
/shelf/
/workspace.xml
```

其中：

- `workspace.xml`：个人工作区状态；
- `shelf/`：JetBrains IDE 的 Shelve Changes 功能产生的本地临时改动数据。

这两个都不应提交。

---

## `.idea/` 的团队协作策略

`.idea/` 不能简单理解成“全是本机垃圾”。

它里面同时包含：

```text
个人 / 本机状态
+
可能有团队价值的项目级 IDE 配置
```

因此本项目不采用：

```gitignore
/.idea/
```

这种整目录忽略方式，而是采用 **选择性提交**。

原则是：

> 自动生成、可重建、与个人机器或当前 IDE 状态有关的配置忽略；只有团队明确希望共享的 IDE 配置才进入 Git。

当前这次项目创建后出现的 `.idea` 文件中，决定如下：

```text
.idea/.gitignore                    提交
.idea/AndroidProjectSystem.xml      忽略
.idea/compiler.xml                  忽略
.idea/deploymentTargetSelector.xml  忽略
.idea/gradle.xml                    忽略
.idea/markdown.xml                  忽略
.idea/misc.xml                      忽略
.idea/runConfigurations.xml         忽略
.idea/vcs.xml                       忽略
.idea/inspectionProfiles/Project_Default.xml
                                      忽略
```

因此在根 `.gitignore` 中补充：

```gitignore
# Android Studio generated/local project files
/.idea/AndroidProjectSystem.xml
/.idea/compiler.xml
/.idea/deploymentTargetSelector.xml
/.idea/gradle.xml
/.idea/markdown.xml
/.idea/misc.xml
/.idea/runConfigurations.xml
/.idea/vcs.xml
/.idea/inspectionProfiles/Project_Default.xml
```

处理后，当前 `.idea/` 中仍需要进入 Git 的只有：

```text
.idea/.gitignore
```

### 为什么不直接忽略整个 `inspectionProfiles/`

以后团队如果主动制定统一的 inspection 规则，可以把真正需要共享的配置提交到 Git。

因此当前只忽略自动生成的：

```text
.idea/inspectionProfiles/Project_Default.xml
```

而不是忽略整个目录。

### `runConfigurations.xml` 与共享 Run Configuration

当前出现的：

```text
.idea/runConfigurations.xml
```

属于 IDE 自动生成的配置，因此忽略。

它和以后团队主动创建的共享 Run Configuration 不是一回事。

如果以后明确创建团队共享的运行配置，可以将对应的独立项目配置文件纳入 Git；因此当前也不把整个运行配置目录一刀切忽略。

---

## 第一次提交时哪些内容进入 Git

经过 `.gitignore` 整理后，下面这些属于项目资产，应该提交：

```text
app/src/...
app/build.gradle.kts
app/.gitignore

build.gradle.kts
settings.gradle.kts
gradle.properties

gradlew
gradlew.bat

gradle/wrapper/gradle-wrapper.jar
gradle/wrapper/gradle-wrapper.properties
gradle/libs.versions.toml
其他 Gradle 项目配置

.idea/.gitignore

plans/...
```

特别注意：

```text
gradle-wrapper.jar
```

虽然是一个 `.jar` 文件，但它属于 Gradle Wrapper 的项目组成部分，需要进入版本库。

以下内容不应出现在本次提交中：

```text
local.properties
.gradle/
build/
app/build/
.idea/workspace.xml
以及前面明确忽略的 .idea 自动生成配置
```

本次整理完成后，Commit 面板中的 `.idea` 未版本控制文件只剩：

```text
.idea/.gitignore
```

这说明选择性忽略规则已经按预期生效。

---

## Android Studio 的 Project 与 Android 视图

Android Studio 左侧项目窗口可以切换不同视图。

### Project

`Project` 更接近磁盘上的真实目录结构。

适合查看：

- `plans/`
- `.gitignore`
- Gradle Wrapper
- 项目的真实文件层级

### Android

`Android` 是针对 Android 开发重新整理后的逻辑视图。

适合日常查看：

- Manifest
- Kotlin / Java 代码
- Android 资源
- Gradle 配置

因此在 `Android` 视图里看不到 `plans/` 是正常的。

`plans/` 并没有消失，只是它不属于 Android 模块的主要开发内容。

---

## Project 与 Module

当前整个：

```text
learn_android_from_AI/
```

可以看作一个 Gradle 项目。

其中：

```text
app/
```

是一个 Android 应用模块。

目前可以先理解为：

```text
项目
└── app 模块
    └── Android 应用代码和资源
```

当前只有一个 `app` 模块。

---

## `app` 模块的基本结构

在 Android 视图中，`app` 主要可以看到：

```text
app
├── manifests
├── kotlin+java
└── res
```

### manifests

核心文件是：

```text
AndroidManifest.xml
```

当前先理解为：

> 应用向 Android 系统声明自身信息和所需能力的地方。

### kotlin+java

主要放 Kotlin / Java 源代码。

本项目使用 Kotlin，默认生成的：

```text
MainActivity.kt
```

就在这里。

### res

主要存放 Android 资源，例如：

```text
res/
├── drawable/
├── mipmap/
└── values/
```

粗略理解：

```text
manifests   → 应用级声明
kotlin+java → 程序代码
res         → 非代码资源
```

---

## `MainActivity.kt`

模板生成的核心结构可以简化为：

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            // Compose UI
        }
    }
}
```

当前阶段只需要理解：

```text
Android 启动 MainActivity
        ↓
onCreate()
        ↓
setContent { ... }
        ↓
Compose 生成界面
```

具体 Activity 生命周期和 Compose UI 会在后续阶段展开。

---

## `@Preview`

Compose 模板中还会看到：

```kotlin
@Preview
@Composable
fun GreetingPreview() {
    Greeting("Android")
}
```

`@Preview` 主要用于 Android Studio 的设计时预览。

它和真正运行应用是两件事：

```text
@Preview          → Android Studio 中的设计时预览
setContent { ... } → App 真正运行时显示的界面
```

可以定义多个 `@Preview`，分别展示同一个 Composable 在不同输入或条件下的样子。

例如：

```kotlin
@Preview(name = "Android")
@Composable
fun PreviewAndroid() {
    Greeting("Android")
}

@Preview(name = "World")
@Composable
fun PreviewWorld() {
    Greeting("World")
}
```

Android Studio 会分别显示两份预览。

---

## 第一次运行

项目完成 Gradle Sync 后，使用顶部的运行配置：

```text
Device: Medium Phone
Run configuration: app
```

点击绿色运行按钮后，大致经历：

```text
Gradle 构建
→ 生成应用安装包
→ 启动模拟器
→ 安装应用
→ 启动 MainActivity
```

最终在模拟器中成功显示：

```text
Hello Android!
```

这说明以下链路已经正常工作：

```text
项目生成
→ Gradle Sync
→ 构建
→ 安装
→ 启动
```

---

## 本目标完成状态

阶段 1 · 目标 1 已完成。

目前已经能够：

- 使用 `Empty Activity` 创建基础 Compose Android 项目；
- 在已有 Git 仓库中正确放置 Android 项目；
- 理解项目与 `app` 模块的基本关系；
- 区分 Android Studio 的 `Project` 和 `Android` 视图；
- 认识 `manifests`、`kotlin+java`、`res` 三类主要内容；
- 知道 `MainActivity` 是当前模板的主 Activity；
- 区分 Compose 的 `@Preview` 与真正的运行界面；
- 完成 Gradle Sync；
- 将应用构建、安装并运行到 Android 模拟器；
- 确认默认 `Hello Android!` 界面成功运行；
- 理解 Android Studio / Gradle 产生的本地文件与项目资产的区别；
- 按团队协作思路选择性管理 `.idea/`；
- 明确第一次提交中哪些 Android / Gradle 文件应进入 Git，哪些本地文件应忽略。
