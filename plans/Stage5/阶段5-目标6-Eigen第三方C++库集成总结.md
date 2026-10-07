# Stage 5 目标 6：使用 FetchContent 集成 Eigen 总结

## 1. 本节目标

这一节完成了一个真正的第三方 C++ 库集成实践：

- 在 Android Native 模块中引入 Eigen；
- 不把 Eigen 源码直接提交进项目仓库；
- 使用 CMake `FetchContent` 自动获取指定版本的 Eigen；
- 使用 Eigen 官方提供的 `Eigen3::Eigen` CMake target；
- 在 `native-lib.cpp` 中实际调用 Eigen；
- 通过 JNI 把 Eigen 的计算结果返回 Kotlin；
- 最终在 `MainActivity` 中调用，并在 Logcat 中确认结果。

最终已经跑通：

```text
Kotlin
    ↓
NativeBridge.eigenSum()
    ↓
JNI
    ↓
Eigen
    ↓
C++ 计算
    ↓
JNI
    ↓
Kotlin
```

实际运行结果：

```text
eigenSum result = 10
```

---

## 2. 为什么选择 FetchContent

最开始可以想到两种比较直接的方式：

### 方式一：把 Eigen 源码直接放进项目

例如：

```text
app/src/main/cpp/third_party/eigen/
```

然后手动配置 include path。

这种方式能工作，但 Eigen 源码量比较大，会让项目仓库膨胀，而且需要自己维护第三方源码。

### 方式二：在项目外安装 Eigen

例如安装到：

```text
C:\Libraries\eigen-install
```

然后让：

```cmake
find_package(Eigen3 ...)
```

去寻找安装好的 Eigen。

这种方式也正常，但会让项目依赖本机环境，不够适合当前这个学习项目。

### 最终采用：CMake FetchContent

由 CMake 自己下载依赖：

```text
项目
  ↓
CMake
  ↓
FetchContent
  ↓
下载指定版本 Eigen
  ↓
加入当前 CMake 构建
```

这样：

- Eigen 不需要手动安装；
- Eigen 不需要提交进当前 Git 仓库；
- 依赖版本可以直接写在 `CMakeLists.txt` 中；
- 项目依赖关系更加清楚。

---

## 3. Eigen 的特点

Eigen 是一个以头文件模板为主的 C++ 数学库。

因此它不像普通动态库那样需要：

```text
libeigen.so
```

也不是：

```text
libnative-lib.so
    ↓
libeigen.so
```

更接近：

```text
native-lib.cpp
    +
Eigen 头文件 / 模板代码
        ↓
NDK / Clang 编译
        ↓
机器码进入 libnative-lib.so
```

因此 Eigen 虽然通过：

```cmake
target_link_libraries(...)
```

接入，但这里并不意味着链接了一个 `libEigen.so`。

---

## 4. CMake 配置

当前 Native 工程入口：

```text
app/src/main/cpp/CMakeLists.txt
```

最终加入 Eigen 后的核心配置：

```cmake
cmake_minimum_required(VERSION 3.22.1)

project("learnandroidfromai")

include(FetchContent)

FetchContent_Declare(
        eigen
        GIT_REPOSITORY https://gitlab.com/libeigen/eigen.git
        GIT_TAG 5.0.1
        GIT_SHALLOW TRUE
)

FetchContent_MakeAvailable(eigen)

add_library(
        native-lib
        SHARED
        native-lib.cpp
)

target_link_libraries(
        native-lib
        PRIVATE
        Eigen3::Eigen
)
```

---

## 5. `FetchContent_Declare`

这一段：

```cmake
FetchContent_Declare(
        eigen
        GIT_REPOSITORY https://gitlab.com/libeigen/eigen.git
        GIT_TAG 5.0.1
        GIT_SHALLOW TRUE
)
```

是在声明一个外部依赖。

其中：

```cmake
GIT_REPOSITORY
```

指定 Eigen 源码仓库。

```cmake
GIT_TAG 5.0.1
```

固定使用指定版本，而不是永远跟着仓库最新提交变化。

这样构建结果更可重复。

```cmake
GIT_SHALLOW TRUE
```

表示进行浅克隆，不需要把整个 Git 历史下载下来。

---

## 6. `FetchContent_MakeAvailable`

```cmake
FetchContent_MakeAvailable(eigen)
```

真正让 Eigen 可用于当前 CMake 工程。

概念上：

```text
FetchContent_Declare
        ↓
描述依赖从哪里来

FetchContent_MakeAvailable
        ↓
获取依赖
        ↓
处理 Eigen 自己的 CMakeLists.txt
        ↓
Eigen 提供自己的 CMake target
```

其中我们真正使用的是：

```text
Eigen3::Eigen
```

---

## 7. `Eigen3::Eigen` 是什么

这里：

```cmake
target_link_libraries(
        native-lib
        PRIVATE
        Eigen3::Eigen
)
```

容易产生一个误解：

```text
Eigen3::Eigen = 一个 .so
```

实际上不是。

`Eigen3::Eigen` 是 Eigen 提供的一个 CMake target。

它携带 Eigen 的使用信息，例如：

```text
Eigen3::Eigen
    ↓
include 路径
    ↓
编译要求
    ↓
传给 native-lib
```

所以：

```cmake
target_link_libraries(
        native-lib
        PRIVATE
        Eigen3::Eigen
)
```

更准确地理解为：

> `native-lib` 依赖 Eigen，并继承 Eigen 这个 CMake target 所声明的使用要求。

于是我们不需要自己手写：

```cmake
target_include_directories(...)
```

去维护 Eigen 的实际源码路径。

---

## 8. Eigen 源码下载到哪里

使用 `FetchContent` 后，Eigen 不会出现在：

```text
app/src/main/cpp/
```

它通常会进入 CMake 的构建目录中。

Android Studio / AGP 的 Native 构建通常位于：

```text
app/.cxx/...
```

其中可能看到类似：

```text
_deps/
├── eigen-src/
└── eigen-build/
```

这些属于构建过程产生的内容，不需要手动维护，也不需要提交进 Git。

---

## 9. 第一次验证：确认 Eigen 能参与编译

在：

```text
app/src/main/cpp/native-lib.cpp
```

中加入：

```cpp
#include <Eigen/Dense>
```

并写一个简单测试：

```cpp
static int eigenSmokeTest() {
    Eigen::Matrix2i matrix;

    matrix << 1, 2,
              3, 4;

    return matrix.sum();
}
```

其中：

```cpp
Eigen::Matrix2i
```

表示一个：

```text
2 × 2
```

的整数矩阵。

内容：

```text
1  2
3  4
```

因此：

```cpp
matrix.sum()
```

结果是：

```text
10
```

项目能够正常编译，说明：

```text
FetchContent
    ↓
Eigen 已获取

Eigen3::Eigen
    ↓
使用信息传给 native-lib

#include <Eigen/Dense>
    ↓
NDK / Clang 成功找到并编译 Eigen
```

---

## 10. 第二次验证：真正通过 JNI 使用 Eigen

之后不再只做静态 smoke test，而是增加一个真正的 JNI 函数：

```cpp
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_learnandroidfromai_NativeBridge_eigenSum(
        JNIEnv* env,
        jobject thiz
) {
    Eigen::Matrix2i matrix;

    matrix << 1, 2,
              3, 4;

    return matrix.sum();
}
```

这里真正执行了 Eigen 代码。

调用关系：

```text
NativeBridge.eigenSum()
        ↓
JNI 函数
        ↓
Eigen::Matrix2i
        ↓
matrix.sum()
        ↓
10
```

---

## 11. Kotlin NativeBridge

在：

```text
app/src/main/java/com/example/learnandroidfromai/NativeBridge.kt
```

中增加：

```kotlin
external fun eigenSum(): Int
```

NativeBridge 已经通过：

```kotlin
companion object {
    init {
        System.loadLibrary("native-lib")
    }
}
```

加载：

```text
libnative-lib.so
```

因此 Kotlin 可以通过：

```kotlin
nativeBridge.eigenSum()
```

进入 JNI。

---

## 12. MainActivity 中实际调用

在：

```text
app/src/main/java/com/example/learnandroidfromai/MainActivity.kt
```

中，已有：

```kotlin
val nativeBridge = NativeBridge()
```

然后增加：

```kotlin
val eigenSum = nativeBridge.eigenSum()
println("eigenSum result = $eigenSum")
```

运行应用后，Logcat 中成功看到：

```text
eigenSum result = 10
```

因此完整链路已经验证成功。

---

# 13. 本节最终链路

现在整个过程可以整理成：

```text
Gradle / AGP
    ↓
启动 Android CMake 构建
    ↓
CMakeLists.txt
    ↓
FetchContent
    ↓
从 Eigen Git 仓库获取 5.0.1
    ↓
FetchContent_MakeAvailable(eigen)
    ↓
处理 Eigen CMake 工程
    ↓
得到 Eigen3::Eigen
    ↓
native-lib 依赖 Eigen3::Eigen
    ↓
native-lib.cpp
#include <Eigen/Dense>
    ↓
NDK / Clang 编译
    ↓
Eigen 模板代码进入 libnative-lib.so
    ↓
System.loadLibrary("native-lib")
    ↓
Kotlin 调用 NativeBridge.eigenSum()
    ↓
JNI
    ↓
Eigen 计算
    ↓
返回 10
    ↓
Logcat
```

---

# 14. 和目标 5 的知识连接起来

目标 5 已经理解了：

```text
Gradle / AGP
    ↓
CMake
    ↓
NDK toolchain
    ↓
C++
    ↓
不同 ABI 的 libnative-lib.so
    ↓
APK
```

这一节只是在这条链上增加了一层：

```text
CMake
    ↓
第三方 C++ 依赖 Eigen
    ↓
native-lib
```

因为 Eigen 是 header-only，所以最终不会额外得到：

```text
libeigen.so
```

而是：

```text
Eigen 相关模板代码
    ↓ 编译
libnative-lib.so
```

因此运行时依然只需要加载：

```text
libnative-lib.so
```

---

# 15. 这一节真正需要记住的几点

1. **第三方 C++ 库不一定意味着额外的 `.so`。**

   Eigen 是 header-only，代码主要在编译期进入自己的 Native 库。

2. **`FetchContent` 可以让 CMake 自己获取并管理第三方源码。**

   不需要把大型第三方库直接提交进自己的仓库，也不需要手工在本机安装。

3. **现代 CMake 更倾向于依赖 target，而不是手工管理头文件路径。**

   这里使用：

   ```cmake
   Eigen3::Eigen
   ```

   而不是自己硬编码 Eigen 的 include 目录。

4. **`target_link_libraries()` 不一定真的意味着链接一个二进制库。**

   `Eigen3::Eigen` 是 CMake target，它主要传播 Eigen 的使用要求。

5. **现在已经真正完成了 Android → JNI → 第三方 C++ 库 → Android 的调用闭环。**

---

# 16. 当前 Stage 5 进度

到目前为止，Stage 5 已经完成：

```text
目标 1
理解 C++ 在 Android 中的定位

目标 2
建立 Native 模块

目标 3
理解 JNI 调用边界与基础数据交互

目标 4
普通对象与集合传递

目标 5
理解 Native 构建、NDK、ABI、.so 与 Android 打包

目标 6
使用 FetchContent 集成真实第三方 C++ 库 Eigen
```

现在已经从：

```text
自己写一点 C++
```

推进到了：

```text
Android 工程
    +
CMake
    +
NDK
    +
JNI
    +
真实第三方 C++ 生态
```

下一阶段可以继续利用 Eigen 做一个更具有 Native 特点的计算密集型功能，而不再只是简单的 `2 × 2` 矩阵求和。
