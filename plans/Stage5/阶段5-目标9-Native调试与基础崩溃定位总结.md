# Stage 5 · 目标 9：Native 调试与基础崩溃定位总结

## 本节目标

能够判断 Native 问题属于哪一类，并知道应该从哪里开始排查：

```text
构建失败
JNI / 加载问题
Native 运行时崩溃
```

其中真正需要重点掌握的是第三类：**C++ 已经开始执行，但运行过程中把 App 进程崩掉。**

---

## 1. Native 问题先分类

### 1.1 构建 / 编译问题

例如：

- C++ 语法错误
- `#include` 找不到
- CMake 文件写错
- 源文件没有加入 `add_library`
- 链接阶段缺少符号

这类问题通常 App 根本跑不起来。

主要看：

```text
Build Output
CMake / clang / linker 输出
```

一般 IDE 已经会给出比较明确的位置。

---

### 1.2 JNI / Native 加载问题

例如 Kotlin：

```kotlin
external fun runMatrixMultiply(size: Int): Double
```

和 C++：

```cpp
Java_com_example_learnandroidfromai_NativeBridge_runMatrixMultiply(...)
```

对应不上，或者：

```kotlin
System.loadLibrary("native-lib")
```

加载不到对应的 `.so`。

这类问题可能已经编译成功，但运行到 JNI 调用时才暴露，典型错误包括：

```text
UnsatisfiedLinkError
```

需要注意：

普通 JVM `test/` 不一定真正加载 Android Native 库。

如果要测试 JNI 调用，更可靠的是：

- `androidTest`
- 模拟器 / 真机运行 App

---

## 2. Native fatal crash 会杀掉整个 App

本节人为制造过：

```cpp
double runMatrixMultiply(int size) {
    std::abort();
    return 0.0;
}
```

调用链：

```text
Kotlin
  ↓
JNI
  ↓
C++
  ↓
std::abort()
  ↓
SIGABRT
  ↓
整个 App 进程结束
```

即使 Kotlin 是在协程里调用：

```kotlin
lifecycleScope.launch {
    val result = withContext(Dispatchers.Default) {
        nativeBridge.runMatrixMultiply(1000)
    }
}
```

也不能隔离这种崩溃。

原因是：

```text
协程 / 线程
≠
进程
```

`Dispatchers.Default` 只是把工作放到另一个线程，线程仍然属于同一个 Android App 进程。

所以某个 Native 线程收到致命 signal 时，整个进程都会结束。

---

## 3. Kotlin 的 try/catch 抓不到 Native fatal signal

这种 Kotlin 代码：

```kotlin
try {
    nativeBridge.runMatrixMultiply(1000)
} catch (e: Exception) {
    println("caught")
}
```

抓不到：

```text
SIGSEGV
SIGABRT
SIGBUS
```

因为它们不是 Java / Kotlin 异常，而是进程级的 Native 致命信号。

可以这样理解：

```text
Java/Kotlin Exception
→ JVM 里的异常机制
→ 可以 try/catch

Native fatal signal
→ 操作系统 / Native 层
→ 进程可能直接结束
→ Kotlin 没机会执行 catch
```

---

## 4. C++ 异常可以转换成 Java / Kotlin 异常

C++ 自己的普通异常可以在 Native 层先接住：

```cpp
try {
    // Native 工作
} catch (const std::exception& e) {
    jclass cls = env->FindClass("java/lang/RuntimeException");
    env->ThrowNew(cls, e.what());
}
```

这里不是让 C++ exception 直接穿过 JNI。

实际过程是：

```text
C++ exception
  ↓
C++ catch
  ↓
JNIEnv* 创建 Java exception
  ↓
返回 JVM
  ↓
Kotlin catch
```

一旦调用：

```cpp
env->ThrowNew(...)
```

跨过 JNI 边界的已经是 **Java 异常状态**，和原来的 C++ exception 本身没有直接关系了。

原则：

> 不要让 C++ exception 直接穿过 JNI 边界；应在 Native 侧处理并转换。

---

## 5. Logcat 中怎么看 Native crash

本次实际看到：

```text
Fatal signal 6 (SIGABRT)
```

这告诉我们“怎么死的”。

常见例子：

```text
SIGABRT  主动终止、assert 等
SIGSEGV  非法内存访问
SIGBUS   某些非法地址 / 内存访问问题
```

接下来主要看：

```text
backtrace:
```

例如：

```text
#00 ... libc.so (abort+...)
#01 ... libnative-lib.so (runMatrixMultiply(int)+...)
#02 ... libnative-lib.so
     (Java_com_example_learnandroidfromai_NativeBridge_runMatrixMultiply+...)
```

阅读方式：

```text
JNI 函数
  ↓
runMatrixMultiply(int)
  ↓
abort()
```

系统库里的：

```text
libc.so
abort()
```

通常只是最后执行 signal / abort 的系统代码。

真正应该优先找的是自己的：

```text
libnative-lib.so
```

---

## 6. 函数级符号化和行号级符号化

崩溃日志中可能已经能看到：

```text
runMatrixMultiply(int)+41
```

这说明系统已经知道：

```text
地址
↓
函数名
```

但不一定直接知道：

```text
matrix_compute.cpp:14
```

源码文件和行号需要更完整的调试符号信息，例如 DWARF。

可以理解为：

```text
CPU / crash log
知道：指令地址

调试符号
知道：地址对应哪个函数、哪个源码文件、哪一行
```

---

## 7. 开发阶段优先使用 LLDB

如果崩溃能够稳定复现，最方便的方式不是手动处理地址，而是：

```text
Android Studio Debug 启动
  ↓
正常操作复现
  ↓
Native 收到 SIGABRT / SIGSEGV
  ↓
LLDB 自动暂停
  ↓
看 Call Stack
  ↓
找到自己代码的栈帧
```

本次实际验证中，没有提前设置断点，LLDB 仍然在 `SIGABRT` 时自动暂停，并直接显示：

```text
runMatrixMultiply(int)
matrix_compute.cpp:14
```

同时还能查看现场变量，例如：

```text
size = 1000
```

所以：

```text
已知可疑位置
→ 可以提前打断点

不知道在哪里崩
→ 直接 Debug 运行
→ 等 fatal signal 自动停下
→ 从 Call Stack 反查
```

---

## 8. LLDB Console 是什么

Android Studio 中的：

```text
LLDB
(lldb)
```

是 Native 调试器的命令行控制台。

常见命令：

```text
bt
```

查看当前线程调用栈。

```text
frame variable
```

查看当前栈帧局部变量。

```text
p size
```

计算 / 打印表达式。

```text
frame select 1
```

切换调用栈帧。

Android Studio 的 `Threads & Variables` 等图形界面，本质上也是在使用 LLDB 的这些能力。

平时 GUI 已经够用；需要更细的控制时再使用 LLDB Console。

---

## 9. `ndk-stack` 是事后符号化工具

如果崩溃已经发生，只有这种日志：

```text
#01 pc 000000000002d429 libnative-lib.so
```

可以结合电脑上对应版本、带调试符号的 `.so`，使用 `ndk-stack` 等工具进行符号化：

```text
0x2d429
  ↓
runMatrixMultiply(int)
  ↓
matrix_compute.cpp:14
```

它不是让 Android 以后直接在原始 Logcat 中记录源码行号。

它做的是：

> 把已经产生的地址型崩溃日志，事后翻译成函数名、文件名和行号。

本阶段暂时知道用途即可；真正遇到只能拿到事后 crash log 的情况时再实践。

---

## 10. 能不能让日志自己记录源码行号

自己主动记录的位置可以直接使用：

```cpp
__FILE__
__LINE__
```

例如：

```cpp
__android_log_print(
    ANDROID_LOG_ERROR,
    "Native",
    "error at %s:%d",
    __FILE__,
    __LINE__
);
```

输出可能是：

```text
error at matrix_compute.cpp:42
```

这适合自己检测到错误后主动留下信息。

例如可以配合检查：

```cpp
if (size <= 0) {
    __android_log_print(
        ANDROID_LOG_ERROR,
        "Native",
        "invalid size at %s:%d",
        __FILE__,
        __LINE__
    );
    std::abort();
}
```

但对于突然发生的：

```cpp
int* p = nullptr;
*p = 123;
```

这种 `SIGSEGV`，Android 原始 crash log 通常仍然先记录机器地址，而不是天然输出：

```text
matrix_compute.cpp:42
```

这种情况下还是依赖：

- LLDB：现场定位
- 符号化工具：事后定位

---

# 本节形成的排查模型

以后碰到 Native 问题，先按这个顺序想：

```text
出现问题
  │
  ├─ 编译都过不了？
  │    → Build Output
  │
  ├─ 编译成功，但 JNI / .so 接不上？
  │    → 加载、JNI 名称、签名、UnsatisfiedLinkError
  │
  └─ 已经进入 C++ 后崩溃？
       ↓
     看 signal
       ↓
     看 backtrace
       ↓
     找自己的 libnative-lib.so
       ↓
     能复现 → LLDB
       ↓
     只能拿事后日志 → 符号化
```

---

## 本节最重要的结论

1. **Native fatal crash 的故障边界通常是进程，不是协程或线程。**
2. **Kotlin `try/catch` 无法捕获 `SIGSEGV`、`SIGABRT` 等 Native fatal signal。**
3. **开发阶段可复现的 Native crash，优先直接用 LLDB 捕获 signal 和查看调用栈。**
4. **事后日志里的地址可以结合调试符号还原成函数和源码行，`ndk-stack` 就是这类工具。**
5. **C++ 普通异常如果需要交给 Kotlin，应在 JNI 层转换成 Java exception，而不是直接跨 JNI 边界。**

---

## 当前阶段掌握程度

到这里，Stage 5 目标 9 的主线已经完成。

暂时不展开：

- ASan / HWASan
- 复杂内存踩踏
- core dump
- 深入反汇编
- 线上 crash 符号服务器
- 完整的 `ndk-stack` 实战

这些等真正遇到对应问题时再学更合适。
