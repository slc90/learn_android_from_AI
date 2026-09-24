# Stage 2 - 目标 9：Android 版本与设备环境差异总结

## 目标

理解 Android 应用为什么会因为：

- Android 系统版本不同
- App 自身的 SDK 配置不同
- 设备能力不同
- 厂商与系统实现不同

而出现不同运行结果，并形成一个基本判断框架：

> 同一份 App 并不是运行在一个固定环境里，而是运行在一组会变化的 Android 环境中。

---

## 1. Android 版本与 API Level

Android 平台版本通常对应一个 API Level。

例如：

```text
Android 13 → API 33
Android 14 → API 34
Android 15 → API 35
```

开发时，很多版本判断实际上是判断 API Level：

```kotlin
if (Build.VERSION.SDK_INT >= 33) {
    // 使用较新的 API 或规则
} else {
    // 旧版本处理
}
```

可以先把 API Level 理解成：

> 当前设备实际运行的是哪一代 Android 平台。

---

## 2. 四个容易混淆的版本概念

Android 项目里经常同时出现：

```text
设备 API Level
minSdk
TargetSdk
compileSdk
```

它们不是同一件事。

### 设备 API Level

表示：

> App 当前实际运行在哪一代 Android 平台上。

例如：

```text
Android 15
→ API 35
```

---

### minSdk

表示：

> App 最低允许运行在哪个 API Level 上。

例如：

```text
minSdk = 26
```

意味着：

```text
API 25 → 不支持
API 26 → 支持范围开始
API 27+ → 可以进入支持范围
```

注意：

> 进入支持范围，不等于已经证明一定不会出问题。

如果代码没有正确处理某个旧系统场景，依然可能出现崩溃、功能失效或行为异常。

---

### targetSdk

表示：

> App 声明自己已经面向哪一代 Android 的行为规则进行适配。

例如：

```text
targetSdk = 35
```

它不是：

```text
“App 最高只能运行到 API 35”
```

也不是：

```text
“API 35 及以下已经全部验证兼容”
```

它主要影响 Android 系统在遇到某些新行为规则时，应该按哪一代兼容策略来对待这个 App。

因此：

```text
设备 API = 35
targetSdk = 34
```

仍然可以运行。

反过来：

```text
设备 API = 35
targetSdk = 38
```

也不代表一定不能运行。

API 35 系统只能执行自己已经知道的规则；它不可能执行未来 API 36～38 才出现的系统行为。

---

### compileSdk

表示：

> 编译代码时，开发者可以看到并使用哪一代 Android SDK 的 API。

例如：

```text
compileSdk = 35
minSdk = 26
targetSdk = 35
```

表示：

```text
使用 API 35 SDK 编译
最低支持 API 26
面向 API 35 行为规则
```

`compileSdk = 35` 并不意味着 App 只能运行在 API 35。

例如它仍然可以运行在 API 29，只要运行时没有错误执行 API 29 上不存在的接口。

---

## 3. “代码里出现新 API”不等于旧设备必崩

真正危险的是：

> 在旧设备上实际执行到了这个设备不存在的 API。

例如某个 API 从 API 33 才存在：

```kotlin
if (Build.VERSION.SDK_INT >= 33) {
    newApi()
} else {
    oldWay()
}
```

即使项目使用了新 API，API 29 设备也不会进入新分支，因此可以正常工作。

所以版本兼容的实际逻辑是：

```text
编译时可以看到新 API
        ↓
运行时检查设备实际 API Level
        ↓
决定走新实现还是旧实现
```

---

## 4. 为什么 Android 要设计这些兼容机制

Android 同时需要满足几件互相拉扯的事情：

```text
系统要继续升级
+
旧 App 不能系统一升级就全部坏掉
+
旧设备不能因为开发者用了新 SDK 就立刻被淘汰
+
开发者还需要使用新系统能力
```

如果没有兼容机制：

```text
系统升级
→ 行为立即全部变化
→ 大量旧 App 没按新规则写
→ 旧 App 集体出问题
```

因此 Android 会尽量为旧 targetSdk 的 App 保留一部分兼容行为，让平台可以逐步推进新的安全、隐私和后台运行规则。

这套机制并不代表系统可以自动证明 App 一定兼容。

很多问题只有真正运行到某条代码路径时才能暴露出来。

---

## 5. 声明支持不等于实际兼容

例如：

```text
minSdk = 26
targetSdk = 38
```

并不意味着：

```text
API 26～38 全部已经正确适配
```

实际可能出现：

```text
API 26 → 正常
API 27 → 正常
API 28 → 某个特殊场景失败
API 29 → 正常
...
API 38 → 正常
```

因此可以把这些 SDK 配置理解为：

```text
minSdk
→ 支持范围下边界

targetSdk
→ 面向哪代 Android 行为规则

compileSdk
→ 编译时能使用哪代 SDK API

设备 API Level
→ 运行时真实的平台环境
```

而真正兼容与否，最终仍取决于：

```text
当前设备实际有什么
+
代码实际执行了哪条路径
+
开发者有没有正确处理这个环境
```

---

## 6. 同一个 Android 版本，设备也可能不同

即使两台设备都是：

```text
Android 15 / API 35
```

实际环境仍然可能不同。

主要差异包括：

```text
设备硬件能力不同
+
屏幕和设备形态不同
+
厂商系统实现与策略不同
```

例如：

```text
设备 A → 有 NFC
设备 B → 没有 NFC
```

这时候问题不是 API Level，而是设备能力本身不同。

---

## 7. 优先检测能力，不要先猜设备身份

如果真正关心的是：

```text
“这台设备有没有 NFC？”
```

应该直接检测 NFC 能力。

不要先写成：

```text
“这是某个品牌，所以应该有 NFC。”
```

基本原则是：

> 对能力和状态编程，而不是对设备身份编程。

例如：

```text
需要某个硬件能力
→ 检测该 feature 是否存在

需要 API 33 才存在的接口
→ 判断 API Level

需要某项权限
→ 检查权限状态
```

只有在：

```text
标准 API 用法正确
+
设备能力也存在
+
仍然只有某厂商 / 某系统版本异常
```

时，才考虑针对特定厂商或设备做 workaround。

---

## 8. 实际代码中的兼容判断通常不会到处都是

正常项目里，版本判断和能力判断一般只集中在少数边界位置：

- 新旧 API 分支
- 硬件能力检查
- 权限检查
- 某些特殊系统功能

很多常见兼容问题也会由 AndroidX / Jetpack 帮助封装。

更合理的结构是：

```text
业务代码
   ↓
兼容封装层
   ↓
版本判断 / 能力判断
```

这样上层业务只需要问：

> 这个功能现在能不能用？

而不是让每个页面自己处理设备型号和 API Level。

---

## 9. 兼容最终还是要测试

`minSdk`、`targetSdk`、版本判断和能力判断，都只能描述设计意图。

真正兼容与否仍然要靠运行验证。

但并不需要把所有 Android 设备组合全部测一遍。

更现实的做法是选择有代表性的边界环境。

例如：

```text
minSdk = 26
targetSdk = 35
```

可以重点测试：

```text
API 26
→ 最低支持边界

API 35
→ 当前目标行为

中间某个常见版本
→ 普通运行环境
```

设备配置也可以挑：

```text
普通手机
+
较大屏幕 / 不同方向
+
某项关键能力“有”和“没有”的设备
```

目标不是穷举所有组合，而是优先验证最容易出问题的边界。

---

## 10. 如果产品只部署在固定设备上

如果实际项目明确规定用户使用某一款或少数几款指定设备，那么兼容问题会简单很多。

因为：

```text
设备型号固定
Android 版本范围可控
硬件能力明确
厂商行为基本固定
```

这种开发更接近：

> 面向已知平台开发。

而不是：

> 面向整个 Android 生态开发。

不过仍然需要注意系统 OTA 升级。

即使设备没有更换，Android 系统版本变化后，权限、后台限制、系统行为等仍然可能发生改变。

---

## 11. 最终判断地图

```text
Android 环境差异
│
├─ 系统版本差异
│   ├─ API Level
│   ├─ minSdk
│   ├─ targetSdk
│   └─ compileSdk
│
├─ 设备差异
│   ├─ 硬件 / feature
│   ├─ 屏幕 / 设备形态
│   └─ 厂商与系统实现差异
│
└─ 开发策略
    ├─ 版本差异 → 判断 API Level
    ├─ 能力差异 → 检测实际能力
    ├─ 尽量避免厂商 / 型号硬编码
    ├─ 把兼容逻辑集中封装
    └─ 用代表性环境测试边界
```

---

## 12. 一句话压缩

可以把这一目标压缩成：

> Android App 面对的不是一个固定运行环境，而是一组由系统版本、SDK 配置、设备能力和具体系统实现共同组成的环境；开发时要明确支持范围，在运行时判断真正需要的条件，并在关键边界上验证。

Stage 2 的这一目标不要求记住每一代 Android 的具体变化。

真正需要建立的是：

> 看到“同一 App 在不同设备上表现不同”时，知道应该从系统版本、SDK 配置、设备能力和厂商实现这几个方向判断，而不是把 Android 当成一个完全统一、永远不变的平台。

---

## 参考资料

- Android Studio 官方介绍  
  https://developer.android.com/studio/intro?hl=zh-cn
- Kotlin 官方文档  
  https://kotlinlang.org/docs/home.html
- Jetpack Compose 官方文档  
  https://developer.android.com/develop/ui/compose/documentation?hl=zh-cn
- Android App compatibility  
  https://developer.android.com/guide/app-compatibility
- `<uses-sdk>` / SDK 版本配置  
  https://developer.android.com/guide/topics/manifest/uses-sdk-element
