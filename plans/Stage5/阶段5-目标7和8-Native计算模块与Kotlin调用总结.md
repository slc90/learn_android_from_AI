# Stage 5 · 目标 7 + 8 总结：Native 计算模块与 Kotlin 调用

## 目标

### 目标 7

> 使用 C++ 实现一个具有明显 Native 特点的计算密集型模块。

### 目标 8

> 从 Kotlin 层调用该模块，并把结果返回给 Android 应用使用。

这两个目标最终连成了一条完整链路：

```text
Android / Kotlin
    ↓
协程后台线程
    ↓
JNI
    ↓
C++ 计算模块
    ↓
Eigen
    ↓
重计算
    ↓
结果返回 Kotlin
```

---

# 1. 为什么这一节选择矩阵乘法

前面的 `eigenSum()` 只能证明：

```text
Eigen 已经成功集成
```

但它的计算量太小，并不能体现 Native 层真正适合承担的工作。

这一节改成较大的矩阵乘法：

```text
A × B = C
```

让一次 JNI 调用进入 C++ 后执行大量连续计算。

这里真正想体现的是：

> C++ 更适合承担计算量较大、能够在 Native 层连续执行一段时间的任务，而不是为了“用了 C++”就把简单逻辑搬进 Native。

如果只是几个整数相加、简单字符串处理、少量业务判断，JNI 调用、数据转换、构建和维护成本可能比计算本身还重。

---

# 2. 把计算逻辑从 JNI 中拆出去

原来的 Native 代码主要集中在：

```text
app/src/main/cpp/native-lib.cpp
```

目标 7 增加了独立计算模块：

```text
app/src/main/cpp/
├─ CMakeLists.txt
├─ native-lib.cpp
├─ matrix_compute.h
└─ matrix_compute.cpp
```

职责变成：

```text
native-lib.cpp
    ↓
负责 JNI 边界

matrix_compute.cpp
    ↓
负责真正的 C++ / Eigen 计算
```

这样 JNI 层不会逐渐堆满业务和算法代码。

---

# 3. `matrix_compute.h`

```cpp
#pragma once

double runMatrixMultiply(int size);
```

接口很小：

```text
输入：矩阵规模 size
输出：一个 double
```

这里没有直接返回整个大矩阵，而是把结果压缩成一个轻量值，避免一开始就把：

```text
计算成本
JNI 数据传输成本
大型数组转换
```

混在一起。

这一节主要验证：

> Native 模块是否真的在做重计算。

---

# 4. `matrix_compute.cpp`

实现使用 Eigen：

```cpp
#include "matrix_compute.h"

#include <Eigen/Dense>
#include <chrono>
#include <android/log.h>

double runMatrixMultiply(int size) {
    auto start = std::chrono::steady_clock::now();

    Eigen::MatrixXd a =
            Eigen::MatrixXd::Random(size, size);

    Eigen::MatrixXd b =
            Eigen::MatrixXd::Random(size, size);

    Eigen::MatrixXd c = a * b;

    double result = c.sum();

    auto end = std::chrono::steady_clock::now();

    auto duration =
            std::chrono::duration_cast<std::chrono::milliseconds>(
                    end - start
            ).count();

    __android_log_print(
            ANDROID_LOG_INFO,
            "MatrixCompute",
            "size=%d, duration=%lld ms, result=%f",
            size,
            static_cast<long long>(duration),
            result
    );

    return result;
}
```

这里：

```cpp
Eigen::MatrixXd
```

表示运行时动态决定尺寸的 `double` 矩阵。

真正的重计算发生在：

```cpp
Eigen::MatrixXd c = a * b;
```

最后：

```cpp
return c.sum();
```

只是把大矩阵结果压缩成一个简单值，方便验证和跨 JNI 返回。

---

# 5. CMake 中把多个 `.cpp` 编进同一个 `.so`

`CMakeLists.txt` 中：

```cmake
add_library(
        native-lib
        SHARED
        native-lib.cpp
        matrix_compute.cpp
)
```

这里需要建立一个重要认识：

```text
native-lib.cpp
matrix_compute.cpp
```

并不会各自生成一个 `.so`。

它们共同属于同一个 CMake target：

```text
native-lib
```

最终一起被编译、链接进：

```text
libnative-lib.so
```

关系可以画成：

```text
native-lib.cpp ───────┐
                      ├──→ libnative-lib.so
matrix_compute.cpp ───┘
        ↑
      Eigen
```

所以：

> 一个 Native 动态库内部完全可以由多个 C++ 源文件组成。

---

# 6. Native Logcat

为了观察真正的计算耗时，在 C++ 中加入：

```cpp
#include <android/log.h>
```

并使用：

```cpp
__android_log_print(...)
```

因此需要在 CMake 中链接 Android 的 `log` 库：

```cmake
find_library(
        log-lib
        log
)

target_link_libraries(
        native-lib
        PRIVATE
        Eigen3::Eigen
        ${log-lib}
)
```

运行后可以在 Logcat 搜索：

```text
MatrixCompute
```

观察类似：

```text
size=500, duration=1417 ms, result=...
```

实际测试中，`500 × 500` 在当前模拟器环境下已经出现约 1.4 秒的耗时。

这也说明：

> “体感好像很快”不能替代真实测量。

---

# 7. JNI 层只负责转发

在 `native-lib.cpp` 中引入：

```cpp
#include "matrix_compute.h"
```

然后增加 JNI 函数：

```cpp
extern "C"
JNIEXPORT jdouble JNICALL
Java_com_example_learnandroidfromai_NativeBridge_runMatrixMultiply(
        JNIEnv* env,
        jobject thiz,
        jint size
) {
    return runMatrixMultiply(size);
}
```

这层几乎没有业务逻辑。

结构是：

```text
Kotlin
    ↓
JNI
    ↓
runMatrixMultiply(size)
    ↓
matrix_compute.cpp
    ↓
Eigen
```

这是比“把整个算法直接写在 JNI 函数里”更清楚的组织方式。

---

# 8. Kotlin NativeBridge

在：

```text
app/src/main/java/com/example/learnandroidfromai/NativeBridge.kt
```

增加：

```kotlin
external fun runMatrixMultiply(size: Int): Double
```

于是 Kotlin 可以直接：

```kotlin
nativeBridge.runMatrixMultiply(500)
```

进入 Native 层。

到这里，目标 8 的基本调用闭环已经形成：

```text
Kotlin
    ↓
JNI
    ↓
C++
    ↓
Eigen
    ↓
double
    ↓
Kotlin
```

---

# 9. 一个非常重要的问题：C++ 不会自动跑到后台线程

最开始调用写在：

```kotlin
nativeBridge.runMatrixMultiply(500)
```

如果这句直接从 `MainActivity.onCreate()` 的主线程执行，那么实际线程关系是：

```text
Main Thread
    ↓
JNI
    ↓
C++
    ↓
Eigen
```

也就是说：

> JNI 调用不会自动把 Native 工作切到后台线程。

C++ 在哪个线程执行，取决于是谁调用它。

如果主线程调用 JNI，那么 C++ 也会占用这个主线程，直到 Native 函数返回。

---

# 10. 把 Native 重计算移出主线程

项目已经有 Lifecycle KTX，因此直接使用：

```kotlin
lifecycleScope.launch {
    val result = withContext(Dispatchers.Default) {
        nativeBridge.runMatrixMultiply(500)
    }

    println("matrix result = $result")
}
```

对应 import：

```kotlin
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
```

线程关系变成：

```text
Main Thread
    ↓
lifecycleScope.launch
    ↓
withContext(Dispatchers.Default)
    ↓
后台工作线程
    ↓
JNI
    ↓
C++ / Eigen
    ↓
结果返回
    ↓
Main Thread
```

这里使用：

```kotlin
Dispatchers.Default
```

是因为这个任务属于：

```text
CPU 密集型计算
```

而不是文件、网络等主要处于等待状态的 IO 任务。

---

# 11. 为什么要用按钮测试，而不是只看 App 启动

如果重计算发生在 `onCreate()`，模拟器启动、界面初始化、Compose 首次绘制和 Native 计算混在一起，很难凭体感判断 UI 是否真的被阻塞。

因此后来改成：

> 等界面已经显示后，再通过按钮手动触发 Native 计算。

测试中把矩阵规模继续放大后：

```text
点击按钮
    ↓
Native 计算开始
    ↓
大约 10 秒后才输出结果
```

但这 10 秒期间：

```text
UI 上其他按钮仍然可以继续点击
```

这个现象非常关键。

它说明：

```text
Native 重计算
```

已经没有阻塞 Android 主线程。

这比单纯看到一个正确计算结果更能证明线程处理是正确的。

---

# 12. “UI 不阻塞”不等于“重计算对 UI 完全没有影响”

即使 Native 工作已经运行在：

```text
Dispatchers.Default
```

后台 CPU 线程上，如果计算把设备 CPU 使用率拉得很高，UI 仍然可能受到资源竞争影响。

所以要区分：

```text
主线程被阻塞
```

和：

```text
系统 CPU 很忙
```

前者是线程使用错误。

后者可能只是重计算本身真的很重。

这一节验证的是：

> Android 主线程仍然能够继续执行和响应用户交互。

---

# 13. 协程取消和 Native 计算

当前写法中：

```kotlin
withContext(Dispatchers.Default) {
    nativeBridge.runMatrixMultiply(...)
}
```

一旦已经进入普通的 C++ 阻塞计算，协程取消并不能自动把正在运行的矩阵乘法强行停下来。

也就是说：

```text
协程取消
≠
自动中断正在执行的 C++ 函数
```

如果以后真的需要支持长时间 Native 任务取消，需要在 C++ 模块本身设计：

```text
取消标志
分段计算
中断检查
```

当前阶段不需要继续展开。

---

# 14. 目标 7 最终完成的内容

目标 7：

> 使用 C++ 实现一个具有明显 Native 特点的计算密集型模块。

已经完成：

```text
matrix_compute.h
        ↓
定义计算接口

matrix_compute.cpp
        ↓
Eigen
        ↓
较大矩阵乘法
        ↓
真实 CPU 密集计算
```

并且通过 Native Logcat 实际观察到了明显耗时。

这已经不再是前面的：

```text
2 × 2 矩阵求和
```

而是真正具有计算密集特征的 Native 模块。

---

# 15. 目标 8 最终完成的内容

目标 8：

> 从 Kotlin 层调用该模块，并把结果返回给 Android 应用使用。

已经完成：

```text
Kotlin
    ↓
NativeBridge
    ↓
JNI
    ↓
matrix_compute
    ↓
Eigen
    ↓
计算结果
    ↓
JNI
    ↓
Double
    ↓
Kotlin
```

同时进一步处理了线程问题：

```text
Main Thread
    ↓
Dispatchers.Default
    ↓
JNI / C++
```

最终实测：

```text
Native 计算持续约 10 秒
```

期间 UI 仍然可以继续操作。

因此不仅“调用成功”，也完成了更合理的 Android 集成方式。

---

# 16. 目标 7 + 8 最终心智模型

现在可以把整个过程压缩成：

```text
Android UI
    ↓
主线程只负责界面和交互
    ↓
Dispatchers.Default
    ↓
JNI
    ↓
C++ Native 计算模块
    ↓
Eigen
    ↓
执行重计算
    ↓
轻量结果
    ↓
Kotlin
    ↓
Android 使用结果
```

这一节最重要的几个结论：

1. **C++ 应该承担真正适合 Native 的工作，而不是为了使用 C++ 而使用 C++。**

2. **JNI 最好只做边界和转发，真正的计算逻辑应该放在独立 C++ 模块中。**

3. **多个 `.cpp` 可以共同编译进同一个 `libnative-lib.so`。**

4. **JNI 不负责线程切换。谁调用 JNI，C++ 就在谁的线程上执行。**

5. **CPU 密集型 Native 工作应该避免占用 Android 主线程。**

6. **`Dispatchers.Default` 可以让 Kotlin 在后台 CPU 线程调用同步 Native 函数。**

7. **“计算完成”只是第一步；在 Android 中，还必须考虑它是否会影响主线程和 UI 响应。**

---

# 17. 当前 Stage 5 进度

目前 Stage 5 已完成：

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

目标 7
使用 C++ / Eigen 实现计算密集型模块

目标 8
从 Kotlin 调用 Native 计算模块，并把结果返回 Android
```

下一目标：

```text
目标 9
能够调试 Native 代码，并定位基础的崩溃、构建和链接问题
```
