# 阶段 1 · 目标 2：理解 Android 项目的大体结构

## 目标

完成阶段 1 的第二个目标：

> 能够理解 Android 项目的大体结构。

本目标不是要求记住所有目录和配置，而是建立一张基本地图：知道一个 Android App 由哪些部分组成、这些部分为什么分开，以及它们怎样在构建和运行时连接起来。

---

## 当前项目的整体结构

当前仓库可以先粗略看成：

```text
learn_android_from_AI/
├── app/                     Android 应用模块
│   ├── build.gradle.kts     app 模块的构建配置
│   └── src/
│       ├── main/            正式 App 内容
│       ├── test/            本地单元测试
│       └── androidTest/     Android 环境中的测试
├── gradle/                  Gradle Wrapper、版本目录等
├── plans/                   学习规划和学习记录
├── build.gradle.kts         工程级 Gradle 配置
├── settings.gradle.kts      工程与模块定义
├── gradle.properties        Gradle 属性
├── gradlew
└── gradlew.bat
```

当前真正的 Android 应用模块是：

```text
app/
```

一个 Gradle 工程可以包含多个模块，目前这里只有一个主要 Android 模块。

---

## `settings.gradle.kts` 与模块

当前配置中有：

```kotlin
rootProject.name = "learn_android_from_AI"
include(":app")
```

其中：

```text
:app
```

是 Gradle 的项目路径。

最前面的 `:` 表示从根项目开始，类似文件系统中的根路径。

例如：

```text
Gradle 项目层级：
根项目
└── feature
    └── login
```

可以表示为：

```kotlin
include(":feature:login")
```

所以 `:app` 中真正的模块名仍然是 `app`，冒号只是 Gradle 的项目路径分隔符。

---

## `app/src` 与 Source Set

`app/src` 下目前主要有：

```text
src/
├── main/
├── test/
└── androidTest/
```

这些可以理解为不同的 **source set（源集）**：按照用途区分的代码和资源集合。

```text
main
→ 正式 App 的代码和资源

test
→ 普通 JVM 环境中的本地单元测试

androidTest
→ 需要 Android 设备或模拟器环境运行的测试
```

当前学习主线主要关注：

```text
app/src/main/
```

---

## `src/main` 的核心结构

当前主要结构是：

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/example/learnandroidfromai/
│   ├── MainActivity.kt
│   └── ui/theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── res/
    ├── drawable/
    ├── mipmap-*/
    ├── values/
    └── xml/
```

可以先压缩成三部分：

```text
AndroidManifest.xml
→ App 提供给 Android 系统的声明信息

Kotlin 代码
→ App 运行时执行的程序逻辑和 Compose UI

res/
→ Android 资源，例如图标、字符串、样式等
```

---

## AndroidManifest.xml

Manifest 可以理解为 App 给 Android 系统的一份“声明清单”。

当前项目中有：

```xml
<activity
    android:name=".MainActivity"
    ...>

    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

</activity>
```

这里声明了：

- 应用中存在 `MainActivity`；
- `MainActivity` 可以作为 Launcher 启动入口。

Manifest 的职责更接近：

```text
“这个 App 有什么、需要什么、Android 系统应该怎样看待它。”
```

而 Kotlin 代码更接近：

```text
“App 真正运行以后具体做什么。”
```

Android App 因此不是普通 Kotlin 程序那种简单从 `fun main()` 开始运行的模型，而是由 Android 系统管理和启动各个应用组件。

---

## `<application>` 与 `<activity>` 的配置关系

Manifest 中可以同时出现：

```xml
<application
    android:label="..."
    android:theme="...">

    <activity
        android:label="..."
        android:theme="..." />

</application>
```

可以理解为：

```text
Application
→ 整个应用的默认配置

Activity
→ 某个 Activity 自己的配置，可以覆盖默认值
```

如果 Activity 没有单独写对应属性，就可以使用 Application 层的默认配置。

因此当两边的 `label`、`theme` 完全相同时，Android Studio 的 Lint 可能提示 Activity 上的配置是 redundant（冗余）的。

模板生成它们和 Lint 提示冗余并不矛盾：模板倾向于明确生成可修改的配置，而 Lint 判断的是当前具体值是否产生额外效果。

---

## Merged Manifest

构建时使用的最终 Manifest 不一定只来自：

```text
app/src/main/AndroidManifest.xml
```

还可能来自：

- App 自己的 Manifest；
- 第三方依赖库的 Manifest；
- Gradle 构建配置注入的信息；
- 不同构建场景产生的 Manifest 内容。

概念上：

```text
App Manifest
    +
依赖库 Manifest
    +
构建配置注入
    ↓
Merged Manifest
```

Android Studio 的 **Merged Manifest** 页面可以查看最终合并结果以及每段内容来自哪里。

例如项目中的：

```kotlin
minSdk = 24
targetSdk = 37
```

定义在 `app/build.gradle.kts` 中，但在 Merged Manifest 中也能看到对应结果。

这个页面很适合排查：

> “我没有在自己的 Manifest 里写这项配置，它到底是从哪里来的？”

---

## MainActivity 与 Compose

当前主要执行关系可以压缩为：

```text
MainActivity
    ↓
onCreate()
    ↓
setContent { ... }
    ↓
Compose UI
    ↓
Greeting()
    ↓
Text("Hello Android!")
```

当前 `MainActivity.kt` 中：

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Learn_android_from_AITheme {
                Scaffold(...) {
                    Greeting(...)
                }
            }
        }
    }
}
```

其中 `setContent { ... }` 可以先理解为：

> 这个 Activity 的界面使用 Jetpack Compose 描述。

Compose 部分继续逐层调用：

```text
setContent
    ↓
Theme
    ↓
Scaffold
    ↓
Greeting
    ↓
Text
```

最终形成屏幕上的 UI。

---

## 从点击 App 图标到显示界面

运行阶段可以理解为：

```text
App 已安装
    ↓
Android 已登记 Manifest 信息
    ↓
用户点击 Launcher 图标
    ↓
系统找到 MainActivity
    ↓
创建 MainActivity
    ↓
调用 onCreate()
    ↓
setContent { ... }
    ↓
Compose 构建 UI
    ↓
显示 Hello Android!
```

注意：并不是每次点击图标时，Android 才现场读取源码目录中的 `AndroidManifest.xml`。

Manifest 信息在构建、安装过程中已经被处理并登记。

---

## `res/`：Android 资源

`res/` 保存 Android 资源，例如：

```text
res/
├── drawable/       可绘制资源
├── mipmap-*/       App 图标等资源
├── values/         字符串、主题、颜色等
└── xml/            其他 XML 配置资源
```

Manifest 中可以通过：

```xml
android:icon="@mipmap/ic_launcher"
android:label="@string/app_name"
android:theme="@style/Theme.Learn_android_from_AI"
```

引用这些资源。

代码或 Manifest 引用的是“逻辑资源名”，Android 可以根据设备环境选择合适的具体资源。

例如：

```text
mipmap-mdpi/
mipmap-hdpi/
mipmap-xhdpi/
mipmap-xxhdpi/
mipmap-xxxhdpi/
```

表示针对不同屏幕密度准备的资源版本。

这种根据设备条件区分资源版本的规则属于 Android 的 **resource qualifier（资源限定符）** 机制。

---

## 为什么项目要拆成这些部分

项目结构不是单纯为了把目录分得好看，而是不同部分承担不同职责：

```text
build.gradle.kts
→ 怎么构建 App

AndroidManifest.xml
→ Android 系统需要知道什么

Kotlin / Compose
→ App 运行时具体做什么

res/
→ Android 资源
```

它们在构建时甚至会经过不同的处理流程，所以分开存放很自然。

面对以后不认识的文件，可以先判断它大致属于哪一类：

```text
构建配置？
系统声明？
运行逻辑？
资源？
测试？
```

通常就能先定位它的作用。

---

## `app/build.gradle.kts`

`app/build.gradle.kts` 描述 `app` 模块怎样构建，例如：

```kotlin
android {
    namespace = "com.example.learnandroidfromai"

    defaultConfig {
        applicationId = "com.example.learnandroidfromai"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}
```

它还负责声明插件和依赖。

---

## Gradle 插件与 App 依赖库

这两类东西需要分清。

### Gradle 插件

例如 `libs.versions.toml` 中：

```toml
android-application = {
    id = "com.android.application",
    version.ref = "agp"
}
```

`android-application` 只是当前 version catalog 中定义的别名，真正的插件 ID 是：

```text
com.android.application
```

它属于 **Android Gradle Plugin（AGP）** 的应用插件。

作用是告诉 Gradle：

> 这个模块是 Android Application，请按照 Android App 的规则构建。

它属于构建期工具，不是 App 的普通运行时库。

它会参与：

- Android 构建流程；
- Manifest 处理；
- `res` 资源处理；
- DEX 生成；
- APK 打包等。

### App 依赖库

例如：

```kotlin
implementation(libs.androidx.activity.compose)
implementation(libs.androidx.compose.material3)
implementation(libs.androidx.compose.ui)
```

它们是 App 代码依赖的 AndroidX / Compose 库。

当前可以这样理解：

```text
activity-compose
→ Android Activity 与 Compose 的连接层

compose-ui
→ Compose UI 的基础能力

material3
→ 基于 Compose 的 Material 3 UI 组件
```

当前 `MainActivity.kt` 中就使用了它们提供的内容，例如：

```kotlin
ComponentActivity
setContent
Modifier
Scaffold
Text
```

所以：

```text
Gradle 插件
→ 决定“怎么构建 App”

App 依赖库
→ 决定“App 代码能够使用什么能力”
```

---

## `dependencies` 中为什么还有不同类型

Android / Gradle 会进一步区分一个依赖在哪种场景下使用。

当前项目中主要有：

```kotlin
implementation(...)
testImplementation(...)
androidTestImplementation(...)
debugImplementation(...)
```

可以理解为不同的依赖作用域：

```text
implementation
→ 正常 App 代码使用

testImplementation
→ src/test 的本地单元测试使用

androidTestImplementation
→ src/androidTest 的 Android 测试使用

debugImplementation
→ 仅 Debug 构建使用，Release 不需要
```

这和一些生态中的 `dependencies / devDependencies` 思路相似，但 Android / Gradle 划分得更细，因为还要区分测试类型和构建类型。

例如：

```kotlin
debugImplementation(libs.androidx.compose.ui.tooling)
```

表示 Compose 调试工具只在 Debug 构建中加入。

---

## 从源码到 APK

按下 Android Studio 的 Run 之前，工程中的源码还不是 Android 可以直接安装的 App。

大致构建过程是：

```text
settings.gradle.kts
    ↓
确定 :app 模块
    ↓
app/build.gradle.kts
    ↓
加载 Android 插件、SDK 配置和依赖
    ↓
处理 app/src/main
```

`src/main` 中不同内容走不同路线：

```text
AndroidManifest.xml
        ↓
Manifest 合并

res/
        ↓
AAPT2 处理资源

Kotlin / Compose 源码
        ↓
Kotlin / Compose 编译
        ↓
JVM 字节码
        ↓
D8
        ↓
DEX
```

其中：

- **AAPT2**：Android 的资源处理工具；
- **D8**：把编译后的 JVM 字节码转换成 Android 使用的 DEX 格式。

最后大致汇合为：

```text
最终 Manifest
      +
编译后的资源
      +
DEX 程序代码
      +
依赖和其他必要内容
      ↓
     APK
```

APK 中安装的是构建后的程序和资源，不是原始的 `MainActivity.kt` 源文件。

---

## 构建阶段与运行阶段

把整个过程连起来：

```text
源码与配置
    ↓
Gradle / Android 构建
    ↓
APK
    ↓
安装到模拟器
    ↓
Android 根据 Manifest 启动 MainActivity
    ↓
onCreate()
    ↓
setContent
    ↓
Compose UI
```

也就是说，可以把 Android App 理解成两个阶段：

```text
构建阶段
源码 → APK

运行阶段
APK → Android 系统 → Activity → UI
```

---

## 本目标最重要的项目地图

最终只需要把下面这条主线真正串起来：

```text
settings.gradle.kts
        ↓
      :app
        ↓
app/build.gradle.kts
        ↓
   app/src/main
        ↓
┌───────────────┬──────────────┬────────────┐
│               │              │            │
Manifest     Kotlin 代码       res        依赖
│               │              │            │
└───────────────┴──────────────┴────────────┘
                ↓
             Gradle 构建
                ↓
               APK
                ↓
          Android 安装 / 启动
                ↓
           MainActivity
                ↓
             Compose UI
```

---

## 当前阶段需要记住的内容

1. **工程和模块不是同一个概念。** 当前 Gradle 工程中主要有一个 Android 模块 `:app`。
2. **`src/main` 是正式 App 的主体。** Manifest、Kotlin 代码和资源各有不同职责，最终在构建时汇合。
3. **Gradle 插件和 App 库依赖不是一类东西。** 插件负责构建，依赖库提供 App 代码实际使用的能力。
4. **最终运行的是构建后的 APK。** Android 根据 Manifest 中的信息管理和启动应用组件，而不是直接从 Kotlin 的 `main()` 开始运行。

---

## 目标完成标准

现在看到一个 Android 项目时，能够大致判断：

- 哪些文件负责 Gradle / 构建；
- 哪个目录是 Android App 模块；
- `src/main`、`test`、`androidTest` 分别用于什么；
- Manifest、Kotlin 代码和 `res` 的职责有什么区别；
- App 从源码经过构建形成 APK，再由 Android 系统启动的大致流程；
- Gradle 插件与普通 App 依赖库的区别。

做到这些，就已经完成了阶段 1 的目标 2。更细的 Activity 生命周期、Android 进程与任务、权限等机制放到后面的 Android 平台基础阶段继续学习。
