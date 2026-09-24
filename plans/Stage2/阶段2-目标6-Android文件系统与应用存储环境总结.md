# 阶段 2 - 目标 6：Android 文件系统与应用存储环境总结

## 一、目标

理解 Android 应用中的文件系统与存储环境，能够判断：

- 数据属于 App 还是用户。
- 数据应该长期保存还是可以作为缓存丢弃。
- 应该使用 internal storage、external app-specific storage，还是 shared storage。
- App 被清缓存、清除存储空间或卸载时，不同数据会发生什么。

---

# 二、核心判断方式

Android 存储首先不要按“路径在哪里”来理解，而应该先问：

> **这个数据属于谁？谁需要访问它？丢失后能不能重新生成？**

可以先建立下面这张图：

```text
Android 存储
│
├── App-specific storage
│   └── 属于某个 App 自己的数据
│
└── Shared storage
    └── 属于用户、可能被多个 App 使用的数据
```

---

# 三、App-specific storage

App-specific storage 是某个应用自己的存储空间。

典型内容包括：

- 配置文件
- 数据库
- App 自己使用的普通文件
- 下载给 App 自己使用的资源
- 缓存文件

这些数据通常跟随 App 一起存在。

应用卸载后，App-specific 数据会被系统删除。

---

## 1. Internal storage

Internal storage 是应用最核心的私有数据区域。

常见接口：

```kotlin
context.filesDir
context.cacheDir
```

典型路径类似：

```text
/data/user/0/com.example.app/files/
/data/user/0/com.example.app/cache/
```

在 Android Studio 的 Device Explorer 中，也经常通过下面这个等价入口看到：

```text
/data/data/com.example.app/
```

其中：

```text
files/
cache/
```

分别对应 `filesDir` 和 `cacheDir`。

普通 App 不能直接访问其他 App 的 internal storage。

访问自己 App 的 internal storage 不需要额外申请存储权限。

---

## 2. filesDir

`filesDir` 用来存放 App 自己需要长期保留的数据。

例如：

```kotlin
val file = File(context.filesDir, "settings.json")
file.writeText("hello")
```

适合：

```text
配置文件
应用自己的普通文件
重要的持久数据
```

它的语义是：

> **这是 App 自己的数据，需要保留。**

---

## 3. cacheDir

`cacheDir` 也是 App 的 internal storage，但它表示缓存。

例如：

```kotlin
val file = File(context.cacheDir, "avatar.jpg")
```

缓存的关键特征不是“速度更快”，而是：

> **这个数据被删除后，可以重新生成或重新获取。**

例如：

```text
服务器下载的缩略图
临时计算结果
网络响应缓存
```

适合放在 cache。

而：

```text
用户写的笔记
唯一的一份配置
不能恢复的数据
```

不能只放在 cache。

Android 在设备存储空间紧张时可能删除缓存文件，用户也可以主动执行：

```text
Clear cache
```

因此程序不能假设缓存文件一定存在。

### cacheDir 不比 filesDir 天生更快

`cacheDir` 和 `filesDir` 通常都位于同类 internal storage 上。

二者主要区别是语义：

```text
filesDir
→ 持久数据

cacheDir
→ 可丢弃数据
```

而不是：

```text
filesDir
→ 慢

cacheDir
→ 快
```

这里的 Cache 和 CPU Cache 的“高速缓存”不是同一层面的概念。

---

# 四、External storage 中也存在 App-specific storage

这是 Android 存储体系里最容易误解的一点之一：

> **External ≠ Shared**

External storage 里面也可以存在某个 App 自己的专属目录。

常见接口：

```kotlin
context.getExternalFilesDir(null)
context.externalCacheDir
```

例如：

```kotlin
val file = File(
    context.getExternalFilesDir(null),
    "model.bin"
)
```

典型路径类似：

```text
/storage/emulated/0/Android/data/com.example.app/files/model.bin
```

虽然它位于 external storage 中，但它仍然属于：

```text
App-specific storage
```

也就是说：

- 属于这个 App。
- 普通 App 不能随意访问。
- 不需要传统的存储权限来访问自己的目录。
- App 卸载后会被删除。
- 仍然算作这个 App 的存储占用。

---

## 1. getExternalFilesDir(null) 中的 null

`getExternalFilesDir()` 的参数表示希望使用哪一种标准分类目录。

例如：

```kotlin
getExternalFilesDir(Environment.DIRECTORY_PICTURES)
```

大致对应：

```text
.../files/Pictures/
```

而：

```kotlin
getExternalFilesDir(null)
```

表示：

> **不指定分类，直接使用 App 的 external files 根目录。**

大致对应：

```text
.../Android/data/com.example.app/files/
```

---

## 2. External 不等于物理 SD 卡

`external storage` 这个名字有明显的历史包袱。

早期它更接近：

```text
External
→ 外接 SD 卡
```

现代 Android 中，它已经更接近一种逻辑存储区域。

例如：

```text
/storage/emulated/0/
```

虽然叫 external storage，但它往往实际上仍位于手机自己的内置闪存中。

一台设备也可能同时存在：

```text
内置闪存提供的 emulated external storage
+
真正可拔出的 SD 卡 storage volume
```

因此现在不要把：

```text
internal / external
```

简单理解为：

```text
手机内部 / 手机外部
```

---

# 五、Internal 和 External app-specific 的关系

可以整理成下面这张表：

| | 持久数据 | Cache |
|---|---|---|
| Internal | `filesDir` | `cacheDir` |
| External app-specific | `getExternalFilesDir()` | `externalCacheDir` |

这四类位置本质上都属于：

> **App-specific storage**

差异主要在于：

- 是 internal 还是 external 区域。
- 是持久数据还是可丢弃缓存。

不能简单理解为：

```text
Internal = 小文件
External = 大文件
```

例如一个 800 MB 文件，只要内部存储空间足够，技术上也可以放进 `filesDir`。

只是大型、可重新下载、App 自己使用的数据，通常更适合考虑 external app-specific storage。

---

# 六、Shared storage

Shared storage 面向的是：

> **用户自己的数据，而不是某个 App 的内部数据。**

典型例子：

```text
照片
视频
音频
PDF
txt
下载文件
用户主动导出的内容
```

这些文件通常应该：

- 能被用户看到。
- 能被其他合适的 App 使用。
- 不因为创建它的 App 被卸载而自动消失。

---

# 七、MediaStore

如果 App 需要操作用户的：

```text
图片
视频
音频
```

现代 Android 通常使用：

```text
MediaStore
```

思路不是让 App 随便扫描整个磁盘，而是：

```text
App
 ↓
ContentResolver
 ↓
MediaStore
 ↓
共享媒体
```

这时经常操作的是：

```text
content://...
```

形式的 `Uri`，而不是传统绝对文件路径。

---

# 八、Storage Access Framework（SAF）

如果用户需要：

```text
导出 PDF
保存 txt
选择某个普通文档
选择保存位置
```

可以使用 Storage Access Framework。

例如：

```text
ACTION_CREATE_DOCUMENT
```

基本流程：

```text
App
 ↓
启动系统文件选择器
 ↓
用户决定文件位置
 ↓
系统返回 Uri
 ↓
App 使用 ContentResolver 读写
```

这里拿到的 `Uri` 甚至可能指向：

```text
Downloads
Documents
Google Drive
其他文件提供器
```

因此不应该执着于把每个 `Uri` 转换成一个绝对文件路径。

现代 Android 中更应该习惯：

```text
Uri
+
ContentResolver
```

这一套资源访问方式。

---

# 九、Scoped Storage

早期 Android 的 external storage 比较开放。

App 拿到较宽泛的存储权限以后，可以在 external storage 中访问很大的范围。

这会带来：

```text
隐私问题
文件混乱
App 互相访问文件
卸载后留下大量孤儿文件
```

Android 后来引入 Scoped Storage。

它的核心思想是：

> **限制每个 App 在 external storage 中能够访问的范围。**

现代 Android 更希望：

```text
App 自己的数据
→ App-specific storage

用户媒体
→ MediaStore / Photo Picker

用户普通文档
→ SAF
```

而不是：

```text
申请一个大范围磁盘权限
→ 到处读取文件
```

因此现代 Android 存储模型的重点越来越不是：

> “文件物理上在哪块闪存？”

而是：

> **这个 App 被允许访问什么范围？**

---

# 十、Context 与存储目录

例如：

```kotlin
context.filesDir
```

这里的 `context` 并不是特指 Application Context。

`filesDir`、`cacheDir` 等都是 `Context` 提供的能力。

在 `Activity` 中：

```kotlin
filesDir
```

本质上相当于通过当前 Activity Context 获取当前 App 的目录。

也可以：

```kotlin
applicationContext.filesDir
```

对于当前讨论的 App 私有存储目录，两者最终都指向这个 App 对应的存储区域。

可以这样理解：

```text
Context
  ↓
告诉 Android 当前调用者属于哪个应用环境
  ↓
系统找到对应 App 的 filesDir / cacheDir 等目录
```

---

# 十一、清缓存、清除存储空间与卸载

这三个操作的区别非常重要。

## Clear cache

只清理缓存数据。

例如：

```text
cacheDir 中的文件
→ 删除

filesDir 中的持久数据
→ 保留

external app-specific 持久文件
→ 保留
```

---

## Clear storage

清除 App 的用户数据，但 App 本体仍然安装着。

可以理解为：

> **把 App 恢复到刚安装、还没有使用过的状态。**

通常会清掉：

```text
filesDir
cacheDir
数据库
SharedPreferences / DataStore
external app-specific 数据
```

但：

```text
APK / App 本体
```

仍然存在。

---

## Uninstall

卸载 App 时：

```text
App 本体
+
App-specific 数据
```

一起被移除。

包括：

```text
internal app-specific storage
external app-specific storage
```

而真正属于用户的 shared storage 文件通常不会因为 App 卸载而自动删除。

---

# 十二、实际验证实验

本阶段使用模拟器进行了实际验证。

写入三个文件：

```kotlin
private fun runStorageExperiment() {
    val internalFile = File(filesDir, "internal_test.txt")
    internalFile.writeText("Hello from filesDir")

    val cacheFile = File(cacheDir, "cache_test.txt")
    cacheFile.writeText("Hello from cacheDir")

    val externalDir = getExternalFilesDir(null)
    val externalFile = externalDir?.let {
        File(it, "external_test.txt").apply {
            writeText("Hello from external files dir")
        }
    }
}
```

实际观察到：

```text
/data/user/0/com.example.learnandroidfromai/files/internal_test.txt

/data/user/0/com.example.learnandroidfromai/cache/cache_test.txt

/storage/emulated/0/Android/data/
com.example.learnandroidfromai/files/external_test.txt
```

在 Device Explorer 中：

```text
/data/data/com.example.learnandroidfromai/
```

可以看到：

```text
cache/cache_test.txt
files/internal_test.txt
```

External app-specific 文件位于：

```text
/storage/emulated/0/Android/data/
com.example.learnandroidfromai/files/external_test.txt
```

实验结果：

```text
Clear cache
→ cache_test.txt 消失
→ internal_test.txt 保留
→ external_test.txt 保留
```

```text
Clear storage
→ 三个实验文件全部消失
→ App 本体仍存在
```

```text
Uninstall
→ App 本体被删除
→ internal app-specific 目录被删除
→ external app-specific 目录也被删除
```

这个实验验证了：

> **external app-specific storage 虽然位于 external storage 中，但它依然属于 App，而不是用户共享文件。**

---

# 十三、最终判断模型

遇到“我要保存一个东西”的需求时，可以按下面的顺序判断：

```text
我要保存数据
     │
     ├─ 数据属于 App？
     │      │
     │      ├─ 需要长期保存
     │      │    ├─ internal → filesDir
     │      │    └─ external → getExternalFilesDir()
     │      │
     │      └─ 丢了可以重新获取
     │           ├─ cacheDir
     │           └─ externalCacheDir
     │
     └─ 数据属于用户？
            │
            ├─ 图片 / 视频 / 音频
            │      → MediaStore / Photo Picker
            │
            └─ PDF / txt / 普通文档
                   → SAF
```

最值得记住的三个判断问题：

> **这个数据属于谁？**

> **它要不要长期保存？**

> **谁需要访问它？**

只要这三个问题判断清楚，大多数 Android 存储需求就能找到正确方向。

---

# 十四、阶段结论

完成这一目标后，应形成下面这些认识：

- Android 的存储不是一个供所有 App 随便访问的大文件系统。
- 每个 App 都拥有自己的 app-specific storage。
- `filesDir` 和 `cacheDir` 都属于 internal storage，区别主要在数据语义，而不是速度。
- external storage 中同样可以存在 App 专属目录。
- `External` 不等于 `Shared`，也不等于物理 SD 卡。
- App-specific 数据通常随着 App 卸载而删除。
- Shared storage 更强调“数据属于用户”。
- 现代 Android 通过 Scoped Storage 限制 App 的文件访问范围。
- MediaStore 和 SAF 代表现代 Android 面向共享数据的主要访问方式。
- 对共享资源，应逐渐习惯 `Uri + ContentResolver`，而不是依赖绝对文件路径。

最终应该具备的判断能力是：

> **面对一个 Android 存储需求时，能够根据数据所有权、生命周期和访问范围，判断它应该进入哪一种存储环境。**
