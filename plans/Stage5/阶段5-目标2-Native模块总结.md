# Stage 5 · 目标 2 总结：在 Android 项目中建立并维护 Native 模块

## 目标

> 能够在 Android 项目中建立并维护 Native 模块。

这一目标的重点不是 Kotlin 如何调用 C++，而是先把 Android 项目中的 Native 构建链真正建立起来。

---

## 1. 最终建立起来的工程结构

```text
app/
├─ build.gradle.kts
└─ src/main/
   ├─ java/
   ├─ res/
   ├─ AndroidManifest.xml
   └─ cpp/
      ├─ CMakeLists.txt
      └─ native-lib.cpp
```

这里的 C++ 仍然属于同一个 `:app` Gradle Module，只是给这个 Module 增加了一条 Native 构建支线。

---

## 2. Gradle Module 和 C++ 库不是同一个概念

C++ 放在 `:app` 内，并不意味着只能有一个 C++ 库。

Gradle 层和 CMake 层是两个不同维度：

```text
Gradle 层
:app
:native
:data
...

CMake / C++ 层
libmath.so
libimage.so
libcore.a
...
```

一个 Gradle Module 内可以通过 CMake 构建多个 C++ target。

如果某套 Native 能力以后需要被多个 Gradle Module 复用，可以再考虑把它包装成独立的 Android Library Module。

---

## 3. CMake 和 NDK 分别负责什么

### CMake

CMake 描述：

> 哪些 C++ 文件要参与构建，以及要生成什么目标。

例如：

```cmake
add_library(
    native-lib
    SHARED
    native-lib.cpp
)
```

表示：

```text
native-lib.cpp
      ↓
    CMake
      ↓
libnative-lib.so
```

### NDK

NDK 提供 Android Native 开发所需的 C/C++ 编译工具链。

整体关系：

```text
Gradle
  ↓
CMake
  ↓
NDK Toolchain
  ↓
C++ 编译 + 链接
  ↓
.so
```

---

## 4. `CMakeLists.txt`

本次最小配置：

```cmake
cmake_minimum_required(VERSION 3.22.1)

project("learnandroidfromai")

add_library(
    native-lib
    SHARED
    native-lib.cpp
)
```

### `cmake_minimum_required`

```cmake
cmake_minimum_required(VERSION 3.22.1)
```

表示：

> 构建这份项目至少需要 CMake 3.22.1。

它不是“当前必须使用 3.22.1”。

例如 CMake 4.1.2 也满足：

```text
4.1.2 >= 3.22.1
```

### `add_library`

```cmake
add_library(
    native-lib
    SHARED
    native-lib.cpp
)
```

表示：

- target 名：`native-lib`
- 类型：动态库 `SHARED`
- 源文件：`native-lib.cpp`
- 最终生成：`libnative-lib.so`

---

## 5. Gradle 如何连接到 CMake

在 `app/build.gradle.kts` 的 `android {}` 中加入：

```kotlin
externalNativeBuild {
    cmake {
        path = file("src/main/cpp/CMakeLists.txt")
    }
}
```

这一步把 Android Gradle 构建系统和 CMake 接起来：

```text
app/build.gradle.kts
        ↓
src/main/cpp/CMakeLists.txt
        ↓
native-lib.cpp
```

Gradle 不会直接解析 `.cpp` 文件。

它只知道：

> 这个 Module 有一套外部 Native 构建系统，入口是这份 `CMakeLists.txt`。

之后再由 CMake 决定具体编译哪些 C++ 文件。

---

## 6. CMake 版本选择

Android SDK 可以同时安装多个版本的 CMake，例如：

```text
Android/Sdk/cmake/
├─ 3.22.1/
└─ 4.1.2/
```

SDK Manager 决定：

> 哪些 CMake 版本已经安装、可以使用。

而项目可以在 Gradle 中明确指定版本：

```kotlin
externalNativeBuild {
    cmake {
        path = file("src/main/cpp/CMakeLists.txt")
        version = "4.1.2"
    }
}
```

如果不写 `version`，Gradle 会按照 Android Gradle Plugin 的默认规则选择可用版本，并不保证选择最新版本。

本次实际观察到：

```text
CMAKE_COMMAND:INTERNAL=
C:/Users/mdrs/AppData/Local/Android/Sdk/cmake/3.22.1/bin/cmake.exe
```

说明没有明确指定版本时，当前项目实际使用的是 SDK 中的 CMake 3.22.1。

---

## 7. `.cxx` 目录是什么

Gradle / CMake 配置完成后，项目中生成了：

```text
app/.cxx/
```

这是 Android Native 构建过程中生成的中间目录。

其中可以找到：

```text
CMakeCache.txt
```

通过搜索：

```text
CMAKE_COMMAND
```

可以确认本次 Native 构建真正调用的是哪一个 `cmake.exe`。

`.cxx` 目录里的哈希目录名，例如：

```text
232zn465
y1h454o4
```

属于 Android Gradle Plugin 生成的内部构建配置目录。

当 CMake 版本、构建参数等 Native 配置发生变化时，可能生成另一套目录。

---

## 8. ABI

Native 代码需要针对不同 CPU 架构分别编译。

本次构建中可以看到：

```text
arm64-v8a
armeabi-v7a
x86
x86_64
```

这些就是 Android ABI。

同一个：

```text
native-lib.cpp
```

会针对不同 ABI 生成各自的：

```text
libnative-lib.so
```

例如：

```text
arm64-v8a/libnative-lib.so
x86_64/libnative-lib.so
```

所以 Native 构建和 Kotlin/JVM 字节码有一个明显区别：

```text
Kotlin / JVM 字节码
更偏向跨 CPU 架构

C++ Native 二进制
需要针对具体 ABI 编译
```

---

## 9. 本次真正跑通的构建链

最终已经实际验证：

```text
app/build.gradle.kts
        ↓
CMakeLists.txt
        ↓
native-lib.cpp
        ↓
CMake
        ↓
NDK
        ↓
按 ABI 编译和链接
        ↓
libnative-lib.so
```

并且已经在 `.cxx` 构建目录中实际找到生成的：

```text
libnative-lib.so
```

所以目标 2 已经完成，不只是“配置写好了”，而是 Native 模块已经真正参与 Android 构建。

---

## 10. 目标 2 和目标 3 的边界

到目标 2 为止，我们解决的是：

> 如何让 Android 工程认识、构建并维护一块 C++ Native 代码。

目前 Kotlin 还不能直接调用：

```cpp
int add(int a, int b) {
    return a + b;
}
```

这部分属于下一目标：

> **目标 3：理解 Kotlin 与 C++ 之间的调用边界和数据交互方式。**

也就是接下来开始进入 JNI。
