# Stage 5 目标 3：理解 Kotlin–C++ 调用边界与数据交互

## 1. 核心认识

JNI（Java Native Interface）可以理解为 JVM/ART 与 C/C++ 之间的一套 FFI 机制。

在 Android 中，典型调用链是：

```text
Kotlin / Java
    ↕ JNI
C / C++
```

这一层真正要解决的问题包括：

- Kotlin 如何调用 C++。
- C++ 如何回调 Kotlin。
- 基础数据、字符串、数组如何跨边界。
- JVM 对象引用如何管理生命周期。
- Native 线程如何进入 JVM。
- Kotlin/Java 方法如何被 C++ 定位并调用。
- Native 方法如何完成静态注册或动态注册。

---

## 2. Kotlin 调用 C++

Kotlin 使用 `external` 声明 Native 方法：

```kotlin
class NativeBridge {

    external fun add(a: Int, b: Int): Int

    companion object {
        init {
            System.loadLibrary("native-lib")
        }
    }
}
```

其中：

```kotlin
System.loadLibrary("native-lib")
```

负责把：

```text
libnative-lib.so
```

加载到当前 App 进程。

`external` 只表示：

> 这个 Kotlin 方法的实现位于 Native 层。

加载 `.so` 本身并不会自动完成某个 Kotlin 方法与某个 C++ 函数之间的绑定。

---

## 3. JNI 静态注册

最直接的方式是使用 JNI 约定的 Native 符号名：

```cpp
extern "C"
JNIEXPORT jint JNICALL
Java_com_example_learnandroidfromai_NativeBridge_add(
        JNIEnv* env,
        jobject thiz,
        jint a,
        jint b
) {
    return a + b;
}
```

基本命名规则：

```text
Java_<包名>_<类名>_<方法名>
```

例如：

```text
com.example.learnandroidfromai.NativeBridge.add
```

对应：

```text
Java_com_example_learnandroidfromai_NativeBridge_add
```

### `extern "C"`

C++ 编译器通常会对函数名进行 name mangling。

JNI 静态注册依赖约定好的符号名，因此使用：

```cpp
extern "C"
```

避免 C++ 改写函数符号。

### `thiz` / `clazz`

它们只是惯用变量名，不是 JNI 关键字。

```cpp
jobject thiz;
jclass clazz;
```

之所以写成 `thiz` 和 `clazz`，是因为 C++ 中：

```cpp
this
class
```

都是关键字，不能直接作为普通变量名。

实例 Native 方法的第二个参数通常是：

```cpp
jobject thiz
```

静态 Native 方法则通常是：

```cpp
jclass clazz
```

---

## 4. 基础类型映射

JNI 提供自己的基础类型：

```text
Kotlin / Java    JNI
----------------------
Int              jint
Long             jlong
Float            jfloat
Double           jdouble
Boolean          jboolean
```

例如：

```cpp
jint
```

本质上可以理解为 JNI 定义的 32 位整数类型。

注意：

```cpp
jint
```

是整数类型；

```cpp
jint*
```

才是整数指针。

---

## 5. String 跨 JNI

Kotlin：

```kotlin
external fun greet(name: String): String
```

C++：

```cpp
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_learnandroidfromai_NativeBridge_greet(
        JNIEnv* env,
        jobject thiz,
        jstring name
) {
    const char* nativeName =
            env->GetStringUTFChars(name, nullptr);

    std::string result = "Hello, ";
    result += nativeName;

    env->ReleaseStringUTFChars(name, nativeName);

    return env->NewStringUTF(result.c_str());
}
```

关键点：

```text
jstring
```

仍然是 JVM 管理的对象引用，不是：

```text
std::string
char*
```

读取字符串通常经过：

```cpp
GetStringUTFChars()
```

使用结束后：

```cpp
ReleaseStringUTFChars()
```

这里不是 `free()` 或 `delete`。

它表示：

> Native 侧已经结束对这段 JNI 字符数据的访问。

---

## 6. 数组跨 JNI

Kotlin：

```kotlin
external fun sum(values: IntArray): Int
```

C++：

```cpp
jint* data =
        env->GetIntArrayElements(values, nullptr);
```

之后可以像普通 C/C++ 数组一样访问：

```cpp
data[i]
```

但是：

```cpp
GetIntArrayElements()
```

可能：

- 直接暴露数组数据；
- 也可能创建一份副本。

JNI 不保证是哪一种。

结束时必须：

```cpp
env->ReleaseIntArrayElements(values, data, mode);
```

常见模式：

```text
0
```

必要时把修改写回 JVM 数组，然后释放访问。

```text
JNI_ABORT
```

如果使用的是副本，不把修改写回。

适合只读访问。

### Region API

另一种方式：

```cpp
env->GetIntArrayRegion(...)
```

显式把 JVM 数组的一段数据复制到 C/C++ 自己的 buffer。

例如：

```cpp
std::vector<jint> buffer(length);

env->GetIntArrayRegion(
        values,
        0,
        length,
        buffer.data()
);
```

这种方式的特点：

```text
JVM 数组
   ↓ 显式复制
C++ 自己的 buffer
```

因此不需要 `ReleaseIntArrayElements()`。

对应写回操作：

```cpp
SetIntArrayRegion()
```

---

## 7. `GetPrimitiveArrayCritical`

JNI 还提供：

```cpp
GetPrimitiveArrayCritical()
```

它更偏向于获得直接内存访问，但依然：

> 不保证一定不会复制。

如果 VM 直接暴露数组真实内存，为了保证 C++ 原始指针稳定，VM 可能暂时 pin 住该数组对象。

逻辑是：

```text
C++ 需要稳定的 raw pointer
        ↓
数组不能被 GC 移动
        ↓
VM 可能 pin 住对象
        ↓
GC 自由度下降
```

所以 Critical 区域必须非常短。

不适合在其中：

- 长时间计算；
- 阻塞；
- I/O；
- 大量 JNI 调用。

一般项目优先使用：

```text
Get/SetArrayRegion
GetArrayElements
```

---

## 8. JNI 引用生命周期

JNI 中：

```cpp
jobject
jstring
jclass
```

并不是普通 C++ 对象指针。

### Local Reference

Native 方法参数通常是 Local Reference：

```cpp
jobject thiz
jstring name
```

它们主要用于当前 JNI 调用 / 当前 Local Frame。

不要把一个 Local Reference 保存下来，在之后的调用或其他线程中继续使用。

### Global Reference

如果对象需要跨过当前 JNI 调用继续存在：

```cpp
jobject global =
        env->NewGlobalRef(thiz);
```

结束时：

```cpp
env->DeleteGlobalRef(global);
```

不能：

```cpp
delete global;
free(global);
```

如果要跨线程持有 JVM 对象，也需要 Global Reference。

---

## 9. `JNIEnv*`

`JNIEnv*` 是：

> 当前已附加 JVM 线程使用 JNI 的线程相关接口句柄。

它具有线程亲和性。

不能：

```text
线程 A 的 JNIEnv*
        ↓
拿到线程 B 使用
```

不同线程使用 JNI 时，应使用该线程自己的 `JNIEnv*`。

不要把 `JNIEnv*` 理解成纯粹的一张函数表副本。

更准确地说：

> 它是当前线程进入 JNI 的接口入口，并与该线程的 JVM 运行状态关联。

例如：

- Local Reference 状态；
- pending exception；
- 当前线程的 JNI 调用环境。

---

## 10. `JavaVM*`

`JavaVM*` 表示 VM 级别的接口。

Android 实际使用中，可以建立这个模型：

```text
一个 App 进程
    ↓
一个 ART / JavaVM*

每个已附加线程
    ↓
自己的 JNIEnv*
```

`JavaVM*` 可以跨线程保存和使用。

它常用来：

```cpp
AttachCurrentThread()
DetachCurrentThread()
GetEnv()
```

---

## 11. Native 线程进入 JVM

C++ 自己创建的线程默认不是 JVM 已附加线程。

因此需要：

```cpp
JNIEnv* env = nullptr;

vm->AttachCurrentThread(&env, nullptr);
```

之后该 Native 线程才可以：

```cpp
env->GetObjectClass(...)
env->CallVoidMethod(...)
```

结束时：

```cpp
vm->DetachCurrentThread();
```

核心关系：

```text
Native thread
      ↓
AttachCurrentThread
      ↓
获得当前线程自己的 JNIEnv*
      ↓
可以调用 JNI
```

---

## 12. C++ 回调 Kotlin

Kotlin：

```kotlin
class NativeBridge {

    external fun startWork()

    fun onNativeResult(value: Int) {
        println("Native result = $value")
    }
}
```

Native 侧要回调这个对象时，需要解决两个问题：

1. 对象要跨过当前 JNI 调用继续活着；
2. Native 后台线程需要自己的 `JNIEnv*`。

因此典型流程：

```text
thiz
 ↓
NewGlobalRef()
 ↓
Native thread
 ↓
AttachCurrentThread()
 ↓
GetMethodID()
 ↓
CallVoidMethod()
 ↓
DeleteGlobalRef()
 ↓
DetachCurrentThread()
```

---

## 13. C++ 调用 Kotlin 方法

JNI 不是直接像 C++ 那样通过函数名调用 Kotlin 方法。

通常先获取：

```cpp
jmethodID method =
        env->GetMethodID(
            clazz,
            "onNativeResult",
            "(I)V"
        );
```

然后：

```cpp
env->CallVoidMethod(
        callback,
        method,
        42
);
```

可以理解为：

```text
类 + 方法名 + 方法签名
        ↓
     jmethodID
        ↓
  Call*Method()
```

`jmethodID` 是 VM 内部对方法的句柄。

正常项目中通常会：

> 查找一次，然后缓存 `jmethodID`。

不需要每次调用都重新 `GetMethodID()`。

---

## 14. JNI Method Descriptor

JNI 使用 JVM method descriptor 描述参数和返回值。

格式：

```text
(参数列表)返回值
```

例如：

```text
(I)V
```

表示：

```text
(Int) -> void
```

常见编码：

```text
I  Int
J  Long
F  Float
D  Double
Z  Boolean
V  void
```

对象格式：

```text
L完整类路径;
```

例如：

```text
Ljava/lang/String;
```

表示：

```text
java.lang.String
```

数组使用：

```text
[
```

例如：

```text
[I
```

表示：

```text
IntArray / int[]
```

因此：

```kotlin
fun sum(values: IntArray): Int
```

对应：

```text
([I)I
```

Descriptor 的重要作用之一就是解决方法重载。

例如：

```kotlin
fun foo(value: Int)
fun foo(value: String)
```

虽然方法名都叫 `foo`，但：

```text
(I)V
(Ljava/lang/String;)V
```

不同。

---

## 15. JNI 动态注册

静态注册依赖：

```text
Java_<包名>_<类名>_<方法名>
```

动态注册则使用：

```cpp
RegisterNatives()
```

明确建立映射。

C++ 可以写普通函数名：

```cpp
jint nativeAdd(
        JNIEnv* env,
        jobject thiz,
        jint a,
        jint b
) {
    return a + b;
}
```

注册表：

```cpp
static JNINativeMethod methods[] = {
    {
        "add",
        "(II)I",
        reinterpret_cast<void*>(nativeAdd)
    }
};
```

这里建立的是：

```text
Kotlin 方法名
    +
JVM descriptor
    +
C++ 函数地址
```

例如：

```text
add + (II)I
      ↓
nativeAdd
```

然后：

```cpp
env->RegisterNatives(
        clazz,
        methods,
        sizeof(methods) / sizeof(methods[0])
);
```

### 静态注册 vs 动态注册

静态注册：

```text
绑定关系编码在 C++ 符号名中
```

动态注册：

```text
绑定关系集中放在注册表中
```

动态注册解决了超长 C++ 函数名的问题，也降低了包名、类名与 Native 函数名之间的直接耦合。

不过：

```text
"add"
"(II)I"
```

这种字符串约定仍然存在。

---

## 16. `JNI_OnLoad`

当：

```kotlin
System.loadLibrary("native-lib")
```

加载：

```text
libnative-lib.so
```

时，如果 Native 库导出了：

```cpp
JNI_OnLoad
```

JVM 会调用它。

典型签名：

```cpp
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void* reserved)
```

它可以理解为：

> Native 库的 JNI 初始化钩子。

常用于：

- 获取 `JavaVM*`；
- 获取当前线程 `JNIEnv*`；
- `RegisterNatives()`；
- 进行 Native/JNI 初始化。

例如：

```cpp
JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void*) {

    JNIEnv* env = nullptr;

    if (vm->GetEnv(
            reinterpret_cast<void**>(&env),
            JNI_VERSION_1_6
        ) != JNI_OK) {
        return JNI_ERR;
    }

    // RegisterNatives(...)

    return JNI_VERSION_1_6;
}
```

这里使用：

```cpp
GetEnv()
```

而不是：

```cpp
AttachCurrentThread()
```

因为 `JNI_OnLoad()` 本身就是 JVM 调用进来的，当前线程已经属于 JVM。

成功时：

```cpp
return JNI_VERSION_1_6;
```

初始化失败：

```cpp
return JNI_ERR;
```

完整链路：

```text
System.loadLibrary()
        ↓
加载 .so
        ↓
JNI_OnLoad()
        ↓
GetEnv()
        ↓
RegisterNatives()
        ↓
return JNI_VERSION_1_6
```

---

## 17. 现代 JNI 的现实感受

JNI 的很多接口明显带有历史时期的设计风格：

```text
Java_包名_类名_方法名
(I)V
GetMethodID
CallVoidMethod
jint / jstring
```

它们的优势是：

- ABI 边界明确；
- 跨平台；
- 非常稳定；
- 长期兼容。

缺点是：

- 大量手工约定；
- 字符串 descriptor 容易写错；
- 静态类型信息利用不足；
- glue code 很啰嗦。

现代工程通常会尽量利用：

- 类型系统；
- C++ template；
- 宏；
- code generation；
- JNI binding library；

把手写 JNI 细节隐藏起来。

但底层仍然要解决相同的问题：

```text
类型转换
对象生命周期
GC
线程
方法定位
ABI
```

因此：

> 现代方案通常不是消灭 JNI，而是不让业务代码直接手写大量 JNI。

---

# 18. 本节最重要的心智模型

可以把整个 Kotlin ↔ C++ 边界压缩成下面这张图：

```text
                Android App Process
                       │
                  JavaVM*
                       │
        ┌──────────────┴──────────────┐
        │                             │
   JVM/Kotlin Thread             Native Thread
        │                             │
     JNIEnv*                   AttachCurrentThread
        │                             │
        │                          JNIEnv*
        │                             │
        └────────── JNI ──────────────┘
                       │
                  Native C++
```

对象生命周期：

```text
LocalRef
   ↓
只用于当前 JNI 调用 / Local Frame

需要跨调用 / 跨线程
   ↓
NewGlobalRef()
   ↓
GlobalRef
   ↓
DeleteGlobalRef()
```

方法调用：

```text
Kotlin → C++
external + 静态注册 / RegisterNatives

C++ → Kotlin
GetMethodID → jmethodID → Call*Method
```

---

# 19. 这一目标学到这里就够了

Stage 5 目标 3 的主干已经覆盖：

- Kotlin → C++；
- C++ → Kotlin；
- JNI 基础类型；
- String；
- 数组；
- LocalRef / GlobalRef；
- `JNIEnv*`；
- `JavaVM*`；
- Native thread attach / detach；
- JVM method descriptor；
- 静态注册；
- 动态注册；
- `JNI_OnLoad`。

下一目标可以进入：

> **常见 Kotlin / C++ 数据传递方式与更复杂的数据结构。**
