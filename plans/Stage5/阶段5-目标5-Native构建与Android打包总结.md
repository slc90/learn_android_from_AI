# Stage 5 目标 5：Native 构建、ABI 与 Android 打包总结

## 1. 这一目标真正需要理解什么

这一节不再重复普通 C++ 的编译、链接和 CMake 基础，而是只看 **Android 在 Native 构建链路上额外增加的部分**：

- Android NDK 如何进入 CMake 构建；
- ABI 为什么会让同一份 C++ 生成多份 `.so`；
- Gradle / Android Gradle Plugin（AGP）如何驱动 Native 构建；
- `.so` 如何进入 APK；
- Android 运行时如何选择和加载匹配当前设备架构的 Native 库；
- Android 系统库、C++ runtime 和第三方 Native 库分别怎样参与链接与运行。

本节暂不展开：

- Debug / Release 对 Native 优化、调试符号和 strip 的影响；
- Native 构建问题的系统化分类与排查。

---

## 2. CMakeLists.txt 并不会直接写 Android toolchain

当前项目中的 `CMakeLists.txt` 很简单：

```cmake
cmake_minimum_required(VERSION 3.22.1)

project("learnandroidfromai")

add_library(
        native-lib
        SHARED
        native-lib.cpp
)
```

它主要负责描述：

- 要构建什么 target；
- target 类型是什么；
- 源码有哪些；
- target 之间如何链接。

它本身没有直接写：

```text
Android
NDK
arm64-v8a
x86_64
API level
```

这些 Android 特有的信息主要由 **AGP 在启动 CMake 时从外部传入**。

概念上类似：

```text
Gradle / AGP
    ↓
发现 externalNativeBuild
    ↓
启动 CMake
    ↓
传入 Android toolchain / ABI / API level / variant 等参数
    ↓
CMake 再读取 CMakeLists.txt
```

所以同一份：

```cmake
add_library(native-lib SHARED native-lib.cpp)
```

在普通桌面环境中可以生成桌面平台库；

在 Android Gradle Plugin 驱动下，则会生成 Android 对应 ABI 的 `.so`。

---

## 3. AGP 如何接入 CMake

当前项目在 `app/build.gradle.kts` 中通过：

```kotlin
externalNativeBuild {
    cmake {
        path = file("src/main/cpp/CMakeLists.txt")
    }
}
```

把 CMake 工程纳入 Android 构建。

最低限度只需要告诉 AGP：

> CMake 工程的入口 `CMakeLists.txt` 在哪里。

AGP 不需要你再重复声明：

- 哪些 `.cpp` 文件参与构建；
- target 叫什么；
- target 之间怎样链接。

这些继续由 CMake 自己负责。

而 Android 相关上下文，例如：

```text
NDK 在哪里
当前 build variant
当前 ABI
minSdk / Android API level
Android toolchain
```

由 AGP 和 Android 构建环境掌握。

因此职责可以简单理解成：

```text
AGP
负责 Android 构建环境
    ↓
CMake
负责 C++ 工程结构
```

---

## 4. NDK 的作用

NDK 可以理解为：

> Android 官方提供的 C/C++ 开发工具链和 Native 平台环境。

它不只是一个编译器，而是包含：

- Clang；
- Android 对应的 linker 工具；
- Android 平台头文件；
- Native API；
- 针对不同 ABI 的工具链配置。

因此 Android Native 构建不是简单地：

```text
普通 C++ 编译器
    ↓
生成一个 Linux .so
```

而是：

```text
C++
    ↓
Android NDK toolchain
    ↓
针对 Android 目标环境生成 .so
```

---

## 5. ABI：为什么同一份 C++ 会有多份 `.so`

Android Native 代码已经是特定 CPU 架构的机器码。

当前常见 ABI 包括：

```text
arm64-v8a
armeabi-v7a
x86
x86_64
```

其中两种 ARM ABI 可以简单记成：

```text
armeabi-v7a
= 32 位 ARM

arm64-v8a
= 64 位 ARM
```

同一份：

```cpp
int add(int a, int b);
```

针对 ARM64 和 x86_64 编译后，机器码完全不同。

因此 Native 构建实际更像：

```text
native-lib.cpp
    ├─→ arm64-v8a/libnative-lib.so
    ├─→ armeabi-v7a/libnative-lib.so
    ├─→ x86/libnative-lib.so
    └─→ x86_64/libnative-lib.so
```

这些 `.so` 文件名字可以一样，但内容不是同一份机器码。

---

## 6. `.cxx` 中按 ABI 分目录的原因

项目构建后，可以看到类似：

```text
.cxx/Debug/<hash>/
├── arm64-v8a
├── armeabi-v7a
├── x86
└── x86_64
```

这表示 AGP / CMake 会针对不同 ABI 分别维护 Native 构建状态。

它不是：

> 一个 CMake 构建目录里放了四份结果。

而更接近：

```text
同一个 CMakeLists.txt
    ↓
针对不同 ABI 分别配置 / 构建
```

每个 ABI 对应的：

- 编译目标；
- CMake cache；
- 工具链参数；
- 中间文件；
- 最终 `.so`

都可能不同，所以它们必须分开。

---

## 7. Android build variant 与 Native 构建

Native 构建也属于 Android build variant 的一部分。

例如：

```text
Debug
Release
```

AGP 在构建某个 Android variant 时，会同时驱动对应的 Native 构建。

概念上：

```text
assembleDebug
   │
   ├─ Kotlin / Java 编译
   ├─ Android 资源处理
   ├─ Native 构建
   └─ APK 打包
```

因此 CMake 在 Android 项目里只是整个 Gradle / AGP 构建流程中的一个子构建系统。

---

## 8. minSdk 也会影响 Native 世界

当前项目有：

```kotlin
minSdk = 24
```

这不只影响 Kotlin / Java Framework API。

它还会影响 Native 侧可以安全依赖的 Android API level。

概念上：

```text
minSdk = 24
    ↓
AGP / NDK / CMake
    ↓
Native 目标平台约束在 Android 24 这一层
```

也就是 Native 代码不能无条件依赖只在更高 Android API level 才存在的系统符号或 Native API。

所以 `minSdk` 对 Native 的意义不仅是“设备能不能安装”，还会影响：

> `.so` 可以假设系统提供哪些 Native 能力。

---

## 9. `.so` 如何进入 APK

CMake / NDK 产生各 ABI 的 `.so` 以后，AGP 会在 packaging 阶段收集 Native 库。

典型 APK 结构可以是：

```text
APK
├── classes.dex
├── res/
├── AndroidManifest.xml
└── lib/
    ├── arm64-v8a/
    │   └── libnative-lib.so
    ├── armeabi-v7a/
    │   └── libnative-lib.so
    ├── x86/
    │   └── libnative-lib.so
    └── x86_64/
        └── libnative-lib.so
```

不过：

> `.cxx` 里构建了哪些 ABI，不等于最终 APK 一定包含哪些 ABI。

实际观察中，当前模拟器生成的 APK 里只包含了：

```text
lib/x86_64/...
```

这说明 **构建产物集合** 和 **最终打包集合** 是两个不同阶段。

可以理解成：

```text
Native 构建
    ↓
得到多个 ABI 的 .so
    ↓
Packaging
    ↓
根据当前构建 / 部署策略选择最终放进 APK 的 ABI
```

---

## 10. Universal APK、设备定向 APK 与 App Bundle

最终 APK 是否包含所有 ABI，取决于打包方式。

可能有：

### 一个 APK 包含所有 ABI

```text
APK
├── arm64-v8a
├── armeabi-v7a
├── x86
└── x86_64
```

这种 APK 体积会更大。

### 按 ABI 拆 APK

例如：

```text
app-arm64-v8a.apk
app-x86_64.apk
```

每份只带自己需要的 Native 库。

### Android App Bundle

上传 `.aab` 后，由分发系统根据用户设备生成适合当前设备的安装内容。

因此要区分：

```text
构建阶段
    ↓
可能为多个 ABI 产生 .so

分发 / 打包阶段
    ↓
决定某台设备最终拿到哪些 ABI
```

---

## 11. 运行时只加载当前 ABI 对应的 Native 库

Kotlin 中：

```kotlin
System.loadLibrary("native-lib")
```

逻辑名称：

```text
native-lib
```

会对应：

```text
libnative-lib.so
```

但真正加载哪一份取决于当前进程 ABI。

例如当前模拟器是：

```text
x86_64
```

那么加载的就是：

```text
lib/x86_64/libnative-lib.so
```

如果以后在常见 ARM64 真机上：

```text
arm64-v8a
```

那么就会使用：

```text
lib/arm64-v8a/libnative-lib.so
```

所以：

```text
构建阶段：
可以准备多份不同 ABI 的 .so

运行阶段：
当前进程只使用匹配自己的那一套
```

---

## 12. 模拟器中的 Android 系统库也是 x86_64

Native 进程内部的机器码 ABI 必须匹配。

因此在 x86_64 模拟器中：

```text
libnative-lib.so   → x86_64
liblog.so          → x86_64
libandroid.so      → x86_64
libc.so            → x86_64
```

到了 ARM64 真机：

```text
libnative-lib.so   → ARM64
Android 系统库     → ARM64
```

不会出现：

```text
x86_64 的应用 .so
    ↓
链接 ARM64 的系统 .so
```

这种 ABI 混搭无法正常运行。

---

## 13. Native 依赖可以分成三类

### Android 系统库

例如：

```cmake
target_link_libraries(
    native-lib
    log
    android
)
```

这里的：

```text
liblog.so
libandroid.so
```

由 Android 系统提供。

应用自己的 APK 通常不需要再带一份。

---

### C++ runtime

C++ 标准库实现通常来自 NDK 的 libc++。

如果采用 shared runtime，可以出现：

```text
libnative-lib.so
    ↓
依赖 libc++_shared.so
```

这种情况下，APK 里可能需要包含：

```text
lib/<abi>/libc++_shared.so
```

它和 Android 系统库不是同一种关系。

---

### 第三方 C++ 库

如果第三方库是：

```text
libfoo.a
```

它主要在链接阶段参与，代码通常直接进入最终的：

```text
libnative-lib.so
```

运行时不再单独加载 `.a`。

如果第三方库是：

```text
libfoo.so
```

那么：

```text
libnative-lib.so
    ↓
依赖 libfoo.so
```

运行时 Android dynamic linker 会继续加载并解析这个共享库。

---

## 14. C++ 一般不需要手动调用函数加载依赖 `.so`

典型 C++ 开发方式依然是：

```cpp
#include <foo/foo.h>

foo_do_something();
```

再在 CMake 中配置链接关系：

```cmake
target_link_libraries(
    native-lib
    foo
)
```

构建时：

```text
头文件
    ↓
让编译器知道声明

链接配置
    ↓
让 linker 知道符号来自哪个库
```

运行时：

```text
Android dynamic linker
    ↓
加载 libnative-lib.so
    ↓
发现它依赖 libfoo.so
    ↓
继续加载 libfoo.so
```

通常不需要自己写：

```cpp
dlopen(...)
dlsym(...)
```

只有需要运行时动态发现或可选插件式加载时，才会主动这样做。

---

## 15. `System.loadLibrary()` 与普通 C++ 共享库依赖的区别

Kotlin：

```kotlin
System.loadLibrary("native-lib")
```

是在 Java / Kotlin 世界主动把顶层 Native 库加载进当前进程。

之后：

```text
libnative-lib.so
    ↓
依赖其他 .so
```

这些依赖通常由 Android 的动态链接器自动继续处理。

所以两层关系是：

```text
Kotlin / Java
    ↓ System.loadLibrary()
libnative-lib.so
    ↓ ELF 动态依赖
libfoo.so / libc++_shared.so / 系统库
```

JNI 主要负责：

```text
Kotlin / Java ↔ Native
```

而 `.so ↔ .so` 的依赖解析属于系统动态链接机制，不需要 JNI 参与。

---

## 16. Eigen 为什么比较特殊

后续目标 6 会集成 Eigen。

Eigen 主要是 header-only C++ 库。

因此它通常不会表现成：

```text
libeigen.so
```

也不需要额外在运行时加载一个 Eigen 动态库。

更接近：

```text
#include Eigen 头文件
    ↓
模板代码在编译时实例化
    ↓
机器码进入自己的 libnative-lib.so
```

因此 Eigen 很适合用来作为 Android 第三方 C++ 库集成的第一个实践对象。

---

# 17. 本节最终模型

这一目标的 Android Native 构建链可以整理成：

```text
build.gradle.kts
    ↓
externalNativeBuild
    ↓
AGP
    ↓
为当前 variant / ABI 配置 CMake
    ↓
NDK toolchain
    ↓
编译并链接 C++
    ↓
产生不同 ABI 的 .so
    ↓
AGP packaging
    ↓
APK / App Bundle
    ↓
安装到具体设备
    ↓
选择与当前进程 ABI 匹配的 Native 库
    ↓
System.loadLibrary()
    ↓
Android dynamic linker 继续解析 .so 依赖
    ↓
JNI 开始连接 Kotlin / Java 与 C++
```

真正需要记住的几点：

1. **CMakeLists.txt 只描述 C++ 工程；Android toolchain、ABI、variant 等环境主要由 AGP 从外部注入。**
2. **同一份 C++ 源码会针对不同 ABI 生成不同机器码的 `.so`。**
3. **构建出多个 ABI 的 `.so`，不代表最终 APK 一定包含全部 ABI。**
4. **当前 Android 进程只能加载和自己 ABI 匹配的 Native 库及其依赖。**
5. **Android 系统库由系统提供；第三方 `.so` 则通常需要和 App 一起分发。**
6. **JNI 负责 Kotlin / Java 与 C++ 的边界，不负责普通 `.so` 之间的动态链接。**
