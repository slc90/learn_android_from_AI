# Stage 5 目标 4：JNI 普通对象与集合传递

## 1. 核心认识

这一节学习的是：

> Kotlin/Java 中的普通对象，以及 `List<Person>` 这类对象集合，如何通过 JNI 在 Kotlin 与 C++ 之间双向传递。

需要先建立一个关键认识：

```text
Kotlin Person
    ↓ JNI
jobject
```

JNI 不会自动把 Kotlin 对象转换成 C++ `struct` 或 C++ class。

例如：

```kotlin
data class Person(
    val name: String,
    val age: Int
)
```

到了 C++ 一侧，首先只是：

```cpp
jobject person
```

C++ 如果想读取 `name`、`age`，需要通过 JNI 提供的运行时接口找到类、字段，再读取字段值。

因此这一过程更接近：

> C++ 通过 JVM 的运行时元信息，手动完成对象映射。

它和“序列化 / 反序列化”在思路上有些相似，但 JNI 通常不会先生成 JSON、字节流之类的中间格式，而是直接操作 JVM 中的对象引用。

---

## 2. 本节使用的 Kotlin 对象

```kotlin
package com.example.learnandroidfromai.model

data class Person(
    val name: String,
    val age: Int
)
```

Native 桥接类中增加了类似方法：

```kotlin
external fun describePerson(person: Person): String

external fun createPerson(): Person

external fun describePeople(people: List<Person>): String

external fun createPeople(): List<Person>
```

这四个方法分别对应：

```text
Person           Kotlin → C++
Person           C++ → Kotlin
List<Person>     Kotlin → C++
List<Person>     C++ → Kotlin
```

---

## 3. Kotlin 普通对象进入 C++：`jobject`

Kotlin：

```kotlin
val person = Person("mdrs", 18)
val result = nativeBridge.describePerson(person)
```

对应 Native 方法：

```cpp
extern "C"
JNIEXPORT jstring JNICALL
Java_com_example_learnandroidfromai_NativeBridge_describePerson(
        JNIEnv* env,
        jobject thiz,
        jobject person
) {
    // ...
}
```

这里：

```cpp
jobject person
```

表示 JVM 中那个 `Person` 实例的引用。

C++ 此时并不知道它内部有什么字段。

---

## 4. 从对象找到它的类

已经有具体对象时，可以使用：

```cpp
jclass personClass = env->GetObjectClass(person);
```

关系可以理解为：

```text
jobject person
      ↓ GetObjectClass
jclass personClass
```

其中：

```text
jobject
```

代表某个具体对象；

```text
jclass
```

代表它对应的 JVM 类信息。

这和之前 C++ 回调 Kotlin 方法时的流程非常接近：

```text
对象
 ↓
拿到 class
 ↓
按名字 + 签名找到成员
 ↓
通过成员 ID 操作具体对象
```

也可以类比 C# 反射中的：

```csharp
var type = obj.GetType();
var prop = type.GetProperty("Name");
var value = prop.GetValue(obj);
```

JNI 更底层，需要手动提供名字、签名和正确的调用 API。

---

## 5. 找字段：`GetFieldID`

要读取：

```kotlin
val name: String
```

先找到字段：

```cpp
jfieldID nameField = env->GetFieldID(
        personClass,
        "name",
        "Ljava/lang/String;"
);
```

`jfieldID` 可以理解为：

> JVM 中某个字段的定位信息 / 句柄。

它不是字段值本身。

三个参数分别是：

```text
personClass                 在哪个类中找
"name"                      字段名
"Ljava/lang/String;"        字段类型签名
```

本节继续沿用前面已经学过的 JNI 类型签名规则：

```text
L...;       对象类型
/           替代包名中的 .
I           Int
V           void
```

---

## 6. 读取对象字段

对象类型字段使用：

```cpp
jstring name = (jstring) env->GetObjectField(
        person,
        nameField
);
```

`Int` 字段使用：

```cpp
jfieldID ageField = env->GetFieldID(
        personClass,
        "age",
        "I"
);

jint age = env->GetIntField(
        person,
        ageField
);
```

因此可以看到一个规律：

```text
对象字段      GetObjectField
Int 字段      GetIntField
Boolean       GetBooleanField
Long          GetLongField
...
```

读取 `Person` 的完整核心过程：

```cpp
jclass personClass = env->GetObjectClass(person);

jfieldID nameField = env->GetFieldID(
        personClass,
        "name",
        "Ljava/lang/String;"
);

jfieldID ageField = env->GetFieldID(
        personClass,
        "age",
        "I"
);

jstring name = (jstring) env->GetObjectField(
        person,
        nameField
);

jint age = env->GetIntField(
        person,
        ageField
);
```

整个链路是：

```text
Person("mdrs", 18)
       ↓
jobject
       ↓
GetObjectClass
       ↓
jclass
       ↓
GetFieldID("name")
GetFieldID("age")
       ↓
GetObjectField / GetIntField
       ↓
name = "mdrs"
age  = 18
```

---

## 7. 字段访问与方法调用本质相同

前面学习 C++ 调 Kotlin 方法时使用：

```cpp
GetMethodID(...)
CallVoidMethod(...)
CallObjectMethod(...)
CallIntMethod(...)
```

这一节访问字段时使用：

```cpp
GetFieldID(...)
GetObjectField(...)
GetIntField(...)
```

两套 API 的结构几乎完全平行：

```text
方法：
class → GetMethodID → CallXXXMethod

字段：
class → GetFieldID  → GetXXXField
```

因此可以把 JNI 对 JVM 对象成员的操作统一理解为：

> 先通过 JVM 的运行时元信息找到成员，再通过得到的 ID / 句柄访问它。

---

## 8. C++ 创建 Kotlin `Person`

反方向时，C++ 一开始没有 `Person` 对象，因此不能使用：

```cpp
GetObjectClass(...)
```

而是通过完整类名找到类：

```cpp
jclass personClass = env->FindClass(
        "com/example/learnandroidfromai/model/Person"
);
```

区别是：

```text
已经有对象：
object → GetObjectClass → class

还没有对象：
完整类名 → FindClass → class
```

---

## 9. 构造函数也是方法：`<init>`

JVM 中构造函数使用特殊名字：

```text
<init>
```

`Person(String, Int)` 的构造函数可以这样找到：

```cpp
jmethodID constructor = env->GetMethodID(
        personClass,
        "<init>",
        "(Ljava/lang/String;I)V"
);
```

签名：

```text
(Ljava/lang/String;I)V
```

表示：

```text
参数：String, Int
返回：void
```

也就是 Kotlin：

```kotlin
Person(name: String, age: Int)
```

---

## 10. `NewObject` 创建 JVM 对象

先准备参数：

```cpp
jstring name = env->NewStringUTF("Native");
```

然后：

```cpp
jobject person = env->NewObject(
        personClass,
        constructor,
        name,
        42
);
```

可以直接类比 Kotlin：

```kotlin
Person("Native", 42)
```

最终返回：

```cpp
return person;
```

Kotlin 端可以直接得到：

```text
Person(name=Native, age=42)
```

完整流程：

```text
C++
 ↓
FindClass(Person)
 ↓
GetMethodID(<init>)
 ↓
NewObject(...)
 ↓
jobject
 ↓
Kotlin Person
```

---

# `List<Person>`

## 11. `List<Person>` 进入 JNI 后仍然只是对象

Kotlin：

```kotlin
val people = listOf(
    Person("Alice", 20),
    Person("Bob", 30)
)
```

Native：

```cpp
jobject people
```

JNI 不会自动把它转换为：

```cpp
std::vector<Person>
```

它仍然只是 JVM 中一个集合对象的引用。

因此处理方式是：

```text
List<Person>
    ↓
先操作 List
    ↓
逐个取出元素
    ↓
每个元素再按 Person 处理
```

---

## 12. 通过 `size()` 与 `get()` 拆 List

先拿到 List 的类：

```cpp
jclass listClass = env->GetObjectClass(people);
```

找到 `size()`：

```cpp
jmethodID sizeMethod = env->GetMethodID(
        listClass,
        "size",
        "()I"
);
```

找到 `get(index)`：

```cpp
jmethodID getMethod = env->GetMethodID(
        listClass,
        "get",
        "(I)Ljava/lang/Object;"
);
```

调用：

```cpp
jint size = env->CallIntMethod(
        people,
        sizeMethod
);

jobject person = env->CallObjectMethod(
        people,
        getMethod,
        0
);
```

这里又回到了前面学过的：

```text
GetMethodID
+
CallXXXMethod
```

所以 `List<Person>` 并没有引入新的 JNI 机制，只是把“调用集合方法”和“拆普通对象”组合起来。

---

## 13. 泛型擦除

`List.get()` 在 JNI 中使用的签名是：

```text
(I)Ljava/lang/Object;
```

而不是：

```text
(I)Lcom/example/learnandroidfromai/model/Person;
```

这是因为 JVM 运行时的 Java/Kotlin 泛型存在类型擦除。

所以：

```kotlin
List<Person>
List<String>
List<Something>
```

在 JNI 这一层并不会靠泛型参数自动区分元素类型。

`get()` 取得的元素首先只是：

```cpp
jobject
```

然后 Native 代码自己按照 `Person` 的结构去访问它。

---

## 14. 遍历整个 `List<Person>`

核心结构：

```cpp
jint size = env->CallIntMethod(
        people,
        sizeMethod
);

for (jint i = 0; i < size; ++i) {
    jobject person = env->CallObjectMethod(
            people,
            getMethod,
            i
    );

    jstring name = (jstring) env->GetObjectField(
            person,
            nameField
    );

    jint age = env->GetIntField(
            person,
            ageField
    );

    // 使用 name / age
}
```

从结构上看就是：

```text
外层：List
 ├─ size()
 └─ get(i)

内层：Person
 ├─ name
 └─ age
```

也就是把两套已经学过的操作嵌套起来。

---

## 15. 循环中的局部引用

遍历时会不断得到：

```cpp
jobject person
jstring name
```

它们都是 JNI 局部引用。

循环中可以在使用完成后主动释放：

```cpp
env->DeleteLocalRef(name);
env->DeleteLocalRef(person);
```

这样可以避免长循环中局部引用不断积累。

本阶段先记住：

> JNI 循环里如果持续创建局部对象引用，用完及时 `DeleteLocalRef` 是一个好习惯。

---

# C++ 创建 `List<Person>`

## 16. 创建 `ArrayList`

C++ 需要先创建一个 JVM 集合对象。

找到类：

```cpp
jclass listClass = env->FindClass("java/util/ArrayList");
```

找到无参构造：

```cpp
jmethodID listConstructor = env->GetMethodID(
        listClass,
        "<init>",
        "()V"
);
```

创建实例：

```cpp
jobject list = env->NewObject(
        listClass,
        listConstructor
);
```

相当于 Kotlin：

```kotlin
val list = ArrayList<Person>()
```

---

## 17. 找到 `ArrayList.add()`

```cpp
jmethodID addMethod = env->GetMethodID(
        listClass,
        "add",
        "(Ljava/lang/Object;)Z"
);
```

签名表示：

```text
参数：Object
返回：boolean
```

然后创建 `Person`：

```cpp
jobject person = env->NewObject(
        personClass,
        personConstructor,
        name,
        21
);
```

加入列表：

```cpp
env->CallBooleanMethod(
        list,
        addMethod,
        person
);
```

C++ 整体做的事情，本质上等价于：

```kotlin
val list = ArrayList<Person>()

list.add(Person("Native Alice", 21))
list.add(Person("Native Bob", 31))

return list
```

---

## 18. Native 层并不会维护 `List<Person>` 的泛型约束

`ArrayList.add()` 在 JNI 中是：

```text
(Ljava/lang/Object;)Z
```

因此 Native 层理论上可以往同一个 `ArrayList` 中加入不同类型的 JVM 对象：

```text
Person
String
Integer
其他 Object
```

也就是说，绕过 Kotlin / Java 编译器以后：

> JVM 底层的 `ArrayList` 主要保存的是对象引用，Native 层并不会替 Kotlin 保证 `Person` 泛型约束。

如果 Kotlin 端把返回值声明为：

```kotlin
List<Person>
```

但 Native 实际偷偷塞入了其他类型，那么问题通常会在 Kotlin 真正把元素当 `Person` 使用时出现，例如触发：

```text
ClassCastException
```

因此泛型擦除意味着：

```text
绕过编译器
    ↓
获得更直接的底层操作能力
    ↓
同时失去一部分类型安全
```

---

## 19. 本节四条完整链路

### Kotlin `Person` → C++

```text
Person
 ↓
jobject
 ↓
GetObjectClass
 ↓
GetFieldID
 ↓
GetObjectField / GetIntField
```

### C++ → Kotlin `Person`

```text
FindClass
 ↓
GetMethodID("<init>")
 ↓
NewObject
 ↓
jobject Person
```

### Kotlin `List<Person>` → C++

```text
jobject List
 ↓
size()
 ↓
get(i)
 ↓
jobject Person
 ↓
拆字段
```

### C++ → Kotlin `List<Person>`

```text
FindClass(ArrayList)
 ↓
NewObject(ArrayList)
 ↓
NewObject(Person)
 ↓
add(Person)
 ↓
返回 List
```

---

## 20. 最终理解

这一节最重要的不是记住每一个 JNI API，而是看清它们背后的统一结构：

```text
对象
 ↓
类信息
 ↓
成员 ID
 ↓
读取 / 调用 / 构造
```

具体 API 只是这个结构的不同版本：

```text
GetObjectClass
FindClass

GetFieldID
GetMethodID

GetObjectField
GetIntField

CallObjectMethod
CallIntMethod
CallBooleanMethod

NewObject
```

所以普通 Kotlin 对象跨 JNI 的本质可以概括为：

> JNI 不会自动理解业务对象；C++ 需要通过 JVM 的运行时元信息找到类、字段、方法和构造函数，然后手动完成对象映射。

而 `List<Person>` 也没有额外的神秘机制：

> 它只是“一个 JVM 集合对象 + 多个 JVM 普通对象”的组合。

当这个模型建立以后，更复杂的 DTO、对象数组、嵌套对象、集合对象，本质上都只是同一套机制继续组合。
