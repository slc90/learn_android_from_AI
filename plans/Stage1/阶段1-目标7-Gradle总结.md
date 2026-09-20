# Stage 1 · 目标 7：对 Gradle 在 Android 项目中的作用形成基本认识

## 目标

形成对 Gradle 的基本认识，能够理解它在 Android 项目中的位置，以及项目中的几个主要 Gradle 配置文件分别负责什么。

---

## 1. Gradle 是什么

Gradle 是一个通用构建系统，不是 Android 专属工具，也不是 Android Studio 本身的一部分。

可以先把它理解成：

> **Android 项目的构建总管。**

它负责把项目中的：

```text
Kotlin / Java 代码
Android 资源
Manifest
依赖库
构建配置
```

组织起来，经过一系列构建步骤，最终得到可安装的 Android 应用。

和 CMake 类比时，可以先理解为：

```text
CMake
→ 主要负责描述和组织 C/C++ 构建

Gradle
→ 更通用的项目构建与任务调度系统
```

在 Android 项目里，Gradle 管的范围更大。

---

## 2. Android Studio、Gradle、AGP 的关系

Gradle 本身并不懂 Android。

它本身不知道：

```text
APK
AndroidManifest.xml
compileSdk
minSdk
Android resources
DEX
```

Android 专属构建能力主要由：

```text
Android Gradle Plugin
```

简称：

```text
AGP
```

提供。

所以可以先记成：

```text
Android Studio
    ↓ 调用 / 集成
Gradle
    ↓ 加载
Android Gradle Plugin
    ↓
构建 Android App
```

其中：

```text
Gradle
→ 通用构建引擎

AGP
→ 给 Gradle 增加 Android 构建能力
```

因此：

```text
升级 Gradle
```

和：

```text
升级 AGP
```

是两件不同的事。

---

## 3. 当前项目中的几个关键 Gradle 文件

当前项目大致有：

```text
learn_android_from_AI/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
│   ├── libs.versions.toml
│   └── wrapper/
│       └── gradle-wrapper.properties
└── app/
    └── build.gradle.kts
```

它们可以先这样理解。

### `settings.gradle.kts`

主要负责：

> 这个 Gradle 工程叫什么，里面有哪些模块。

当前项目中：

```kotlin
rootProject.name = "learn_android_from_AI"
include(":app")
```

可以读成：

```text
工程名：learn_android_from_AI
模块：:app
```

---

### 根目录 `build.gradle.kts`

主要负责工程级的构建配置。

当前项目中主要声明了插件：

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

这里可以理解为：

> 先在工程层声明可能使用的构建插件。

`apply false` 表示：

> 在根工程这里先不直接启用。

---

### `app/build.gradle.kts`

这是当前最重要的模块级构建配置。

这里定义：

```text
这个 app 模块是什么
使用什么 SDK
启用什么功能
依赖哪些库
如何构建
```

例如：

```kotlin
android {
    defaultConfig {
        minSdk = 24
        targetSdk = 37
    }

    buildFeatures {
        compose = true
    }
}
```

以及：

```kotlin
dependencies {
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
}
```

---

### `gradle/libs.versions.toml`

主要用于集中管理：

```text
依赖库
插件
版本号
```

其中：

```toml
[libraries]
```

描述的是：

> 工程依赖。

例如：

```text
Compose
AndroidX
JUnit
Espresso
```

而：

```toml
[plugins]
```

描述的是：

> 给 Gradle 增加构建能力的插件。

因此比“libraries 给 Kotlin 用”更准确的理解是：

```text
libraries
→ 工程依赖

plugins
→ 构建插件
```

---

## 4. Module 与 Task

Gradle 真正执行构建时，会把工作拆成很多：

```text
Task
```

Task 可以理解成：

> 一个具体的构建步骤。

例如：

```text
编译 Kotlin
处理 Android 资源
处理 Manifest
生成 DEX
打包 APK
```

一个模块中会有很多 Task。

例如：

```text
:app:compileDebugKotlin
```

可以读成：

```text
app 模块里的 compileDebugKotlin Task
```

所以：

> **Module 是组织单位，Task 才是实际构建执行单位。**

---

## 5. Task 之间如何决定执行顺序

Gradle 不会简单地按文件顺序执行。

它会根据 Task 之间的依赖关系，形成：

```text
Task Dependency Graph
```

即任务依赖图。

例如：

```text
编译代码 ──────┐
               │
处理资源 ──────┼→ 打包 APK
               │
处理 Manifest ─┘
```

多模块项目中也是一样。

如果：

```text
:app
```

依赖：

```text
:core
```

那么可能出现：

```text
:core:compile...
        ↓
:app:compile...
        ↓
:app:package...
```

重要的是：

> **Gradle 决定顺序的基本单位是 Task，不是 Module。**

两个没有依赖关系的 Task 还可能并行执行。

---

## 6. 一次 `assembleDebug` 背后发生什么

本阶段实际执行过：

```bat
gradlew.bat :app:assembleDebug --dry-run
```

其中：

```text
:app
→ app 模块

assembleDebug
→ 构建 Debug 版本

--dry-run
→ 只显示计划执行的 Task，不真正执行
```

实际看到的 Task 包括：

```text
:app:preBuild
:app:generateDebugResources
:app:mergeDebugResources
:app:compileDebugKotlin
:app:compileDebugJavaWithJavac
:app:processDebugManifest
:app:dexBuilderDebug
:app:mergeProjectDexDebug
:app:mergeDebugNativeLibs
:app:validateSigningDebug
:app:packageDebug
:app:assembleDebug
```

可以压缩理解为：

```text
准备
↓
资源处理
↓
Kotlin / Java 编译
↓
Manifest 处理
↓
DEX 处理
↓
Native 库处理
↓
签名
↓
打包 APK
↓
assembleDebug
```

所以：

```text
assembleDebug
```

更像是最终构建目标。

Gradle 会根据它自动展开出完成这个目标所需要的一整套 Task。

---

## 7. 为什么第一次 Build 慢，之后通常更快

Gradle 会尽量避免重复工作。

它会判断：

```text
Task 的输入有没有变化
之前的输出还能不能复用
缓存结果是否仍然有效
```

因此整体思路是：

```text
能复用 → 复用
能跳过 → 跳过
必须重做 → 才重做
```

这就是为什么：

```text
第一次 Build
```

通常比较慢，而后续构建往往更快。

这类机制可以先理解为：

```text
增量构建 + 缓存
```

因此平时没有必要频繁：

```text
Clean
```

因为 Clean 会清掉一部分之前已经生成的构建结果。

---

## 8. Gradle 与 CMake / C++ 的关系

如果以后 Android 项目中加入 C++，通常不是 Gradle 自己直接编译 C++。

更接近：

```text
Gradle
  ↓
某个 Native 相关 Task
  ↓
调用 CMake / NDK
  ↓
CMake 组织 Native 构建
  ↓
Ninja 执行编译 / 链接
  ↓
生成 .so
  ↓
Gradle / AGP 再把 .so 打进 APK
```

可以先记成：

> **Gradle 是整个 Android 工程的总调度，CMake 负责 Native 构建这一部分。**

---

## 9. Gradle Wrapper

项目中的：

```text
gradlew
gradlew.bat
gradle/wrapper/gradle-wrapper.properties
```

属于 Gradle Wrapper。

它的作用可以先理解为：

> 让项目自己指定和使用所需的 Gradle 版本，而不是依赖开发者电脑里手动安装的某个 Gradle 版本。

当前项目通过：

```text
gradle-wrapper.properties
```

指定 Gradle 版本。

所以平时在项目里执行：

```bat
gradlew.bat ...
```

就是通过项目自己的 Wrapper 来运行 Gradle。

---

## 10. 最终形成的心智模型

可以把整个 Android 构建关系压缩成：

```text
Android Studio
        ↓
      Gradle
        ↓
       AGP
        ↓
  生成并组织 Task 图
        ↓
 ┌──────┼────────┐
 │      │        │
代码   资源    Manifest
 │      │        │
 └──────┴────────┘
        ↓
       DEX
        ↓
  Native libraries
        ↓
      签名
        ↓
      APK
```

如果以后加入 C++：

```text
Gradle Task
    ↓
CMake / NDK
    ↓
.so
    ↓
重新进入 Android 打包流程
```

---

# Stage 1 · 目标 7 完成标准

目前已经能够：

- [x] 知道 Gradle 是通用构建系统，不是 Android 专属工具
- [x] 理解 Android Studio、Gradle、AGP 的关系
- [x] 理解 `settings.gradle.kts` 的基本作用
- [x] 理解根 `build.gradle.kts` 与模块 `build.gradle.kts` 的区别
- [x] 理解 `libs.versions.toml` 中 `libraries` 与 `plugins` 的区别
- [x] 理解 Module 与 Task 的关系
- [x] 理解 Task Dependency Graph 的基本概念
- [x] 实际查看 `assembleDebug --dry-run` 的 Task 列表
- [x] 理解增量构建和缓存为什么能加快后续 Build
- [x] 初步理解 Gradle 与 CMake / NDK 的关系
- [x] 理解 Gradle Wrapper 的基本作用

到这里已经达到本目标：

> **对 Gradle 在 Android 项目中的作用形成基本认识。**
