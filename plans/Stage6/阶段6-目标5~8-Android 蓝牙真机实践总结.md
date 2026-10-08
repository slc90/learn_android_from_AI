# 阶段 6｜目标 5～8：Android 蓝牙真机实践总结

> 项目：`learn_android_from_AI`  
> 学习环境：Android Studio、Kotlin、Jetpack Compose、Android 真机  
> 实际测试设备：`vivo TWS Air3` 耳机  
> 范围：学习规划第 6 阶段的目标 **5、6、7、8**  
> 记录说明：根据本次对话中的实际动手过程整理；**已验证**和**仅讨论、尚未实践**分开记录。

## 一、这四个目标分别学了什么

| 目标 | 原计划内容 | 本次学习结果 |
| --- | --- | --- |
| **5** | 以蓝牙为例，学习 Android 如何访问设备硬件能力 | **已实践**：硬件特性检测、获取蓝牙适配器、读取开关状态、广播监听 |
| **6** | 理解蓝牙权限、设备发现、连接和数据交互 | **部分实践**：权限、BLE 扫描、已配对设备查询、A2DP 连接查询；**未实际执行** GATT 连接和数据收发 |
| **7** | 将一个简单蓝牙功能接入已有 Android App | **已实践**：新建 Compose 蓝牙界面，整合权限、扫描、设备列表及查询功能 |
| **8** | 观察并处理真机上才较容易遇到的问题 | **已实践**：系统蓝牙开关、权限差异、匿名广播、重复结果、经典蓝牙与 BLE 发现差异、耳机连接状态变化 |

**总体结论：**已经完成一条可运行的真机蓝牙探索流程；尚未实现由 App 主动建立 GATT 连接、读取或写入 BLE 特征值。

---

## 二、目标 5：访问 Android 蓝牙硬件

### 1. 先确认手机有没有蓝牙硬件

通过 `PackageManager` 检查设备特性：

- `PackageManager.FEATURE_BLUETOOTH`：是否支持经典蓝牙。
- `PackageManager.FEATURE_BLUETOOTH_LE`：是否支持 BLE（低功耗蓝牙）。
- `BluetoothManager` / `BluetoothAdapter`：获取 Android 的蓝牙管理入口。

清单中为硬件能力声明可选特性（不强制设备必须具备蓝牙能力才能安装）：

```xml
<uses-feature
    android:name="android.hardware.bluetooth"
    android:required="false" />
<uses-feature
    android:name="android.hardware.bluetooth_le"
    android:required="false" />
```

**观察：**关闭手机蓝牙开关后，硬件支持检测依然返回 `true`。

这说明要区分两件事：

- **硬件支持**：手机有没有这个能力。
- **当前启用**：用户有没有打开该能力。

### 2. 蓝牙开关状态

```kotlin
val bluetoothEnabled = bluetoothAdapter?.isEnabled == true
```

单纯在 Compose 函数中读取一次 `isEnabled`，不会自动追踪手机设置里的开关变化。

为实现实时更新，实践了：

- `BluetoothAdapter.ACTION_STATE_CHANGED`：蓝牙开关状态变化广播。
- `BroadcastReceiver`：接收系统广播。
- `DisposableEffect`：随 Compose 生命周期注册和注销接收器。
- `mutableStateOf(...)`：广播到来后更新界面状态。

数据流为：

```text
用户切换蓝牙开关
       ↓
Android 发送状态变化广播
       ↓
BroadcastReceiver 接收
       ↓
更新 Compose State
       ↓
界面自动刷新
```

**实际验证：**切换系统蓝牙开关，App 页面可以同步反映变化。

---

## 三、目标 6：权限、发现、配对与连接

### 1. 蓝牙权限分 Android 版本处理

在本次应用使用的权限配置中，包含：

```xml
<!-- Android 11 及以下：经典蓝牙相关权限 -->
<uses-permission
    android:name="android.permission.BLUETOOTH"
    android:maxSdkVersion="30" />
<uses-permission
    android:name="android.permission.BLUETOOTH_ADMIN"
    android:maxSdkVersion="30" />

<!-- Android 11 及以下：BLE 扫描所需的位置权限 -->
<uses-permission
    android:name="android.permission.ACCESS_FINE_LOCATION"
    android:maxSdkVersion="30" />

<!-- Android 12+：附近蓝牙设备扫描 -->
<uses-permission
    android:name="android.permission.BLUETOOTH_SCAN"
    android:usesPermissionFlags="neverForLocation" />

<!-- Android 12+：已配对设备信息、连接相关操作 -->
<uses-permission
    android:name="android.permission.BLUETOOTH_CONNECT" />
```

> `neverForLocation` 表示应用明确声明不使用蓝牙扫描结果推断物理位置；在部分设备上，这可能使某些 BLE 广播内容被过滤。如果未来需要利用扫描推断位置，应重新评估权限声明。

运行时：

- Android 12（API 31）及以上，根据操作检查和申请 `BLUETOOTH_SCAN`、`BLUETOOTH_CONNECT`。
- 旧版本按需要申请 `ACCESS_FINE_LOCATION` 等运行时权限。
- 使用 `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` 发起权限请求，配合 `ContextCompat.checkSelfPermission(...)` 检查授权状态。

**实际验证：**手机授权“附近设备”后，App 能执行 BLE 扫描及配对设备查询；同一权限组中的授权未必每次都会弹出新对话框。

### 2. BLE 扫描：拿到的是“广播结果”，不是设备总数

主要 API：

```kotlin
bluetoothAdapter?.bluetoothLeScanner?.startScan(scanCallback)

// 结束时
bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
```

用 `ScanCallback.onScanResult(...)` 处理每次扫描回调，并通过 `LaunchedEffect` 在约 **10 秒**后停止扫描。

最初用回调次数计数：

- 一次 10 秒扫描得到约 **157 条扫描回调**。
- 后来一次扫描观察到 **361 条回调、53 个不同地址**。

**核心认识：361 条回调不等于 361 台设备。**同一设备可能重复广播，同一地址会多次出现；而地址也不一定永久对应一台物理设备。

### 3. 去重和设备信息展示

把扫描结果改为按地址缓存：

```kotlin
val uniqueDevices = remember {
    mutableStateMapOf<String, ScannedBleDevice>()
}
```

扫描回调中以 `result.device.address` 作为本轮扫描的去重键，保存：

- `address`：扫描到的蓝牙地址。
- `name`：广播数据中的设备名，可能为空。
- `rssi`：接收信号强度，数值通常为负，越接近 0 通常表示接收信号越强，但不能直接换算出准确距离。
- `serviceUuids`：广播中携带的服务 UUID；没有广播出来不等于设备没有服务。

界面使用 `LazyColumn` 显示去重后的设备列表，并按 RSSI 排序。

**注意：**BLE 设备可能采用随机地址或地址轮换；用地址去重适合一次短扫描的 UI 展示，不能保证长期可靠识别物理设备。

### 4. 配对设备查询：BLE 扫描与系统配对列表不是一回事

尝试寻找 `vivo TWS Air3` 时，BLE 扫描列表里出现大量 `(no name)`，不能据此判断耳机不在附近。

后来让手机在系统蓝牙设置中完成耳机配对，并使用：

```kotlin
val pairedDevices = bluetoothAdapter?.bondedDevices
```

读取系统保存的配对设备。Android 12+ 访问此类信息需要 `BLUETOOTH_CONNECT` 权限。

**实际验证：**App 成功显示已配对的 **`vivo TWS Air3`**。

这里要分清：

- **扫描（Discovery）**：寻找周围设备或接收 BLE 广播。
- **配对 / 绑定（Pairing / Bonding）**：建立安全关系；绑定会保存供后续使用的安全信息。
- **连接（Connection）**：建立某种实际的通信通道。

**已配对 ≠ 当前已连接。**

### 5. A2DP：查询耳机实际音频连接状态

耳机音频连接主要使用经典蓝牙的 **A2DP Profile**。实践用到了：

- `BluetoothProfile.A2DP`
- `BluetoothAdapter.getProfileProxy(...)`
- `BluetoothProfile.ServiceListener`
- `proxy.connectedDevices`
- `BluetoothAdapter.closeProfileProxy(...)`

特别注意：`ServiceListener.onServiceConnected()` 说明 **App 已取得系统 Profile 服务的代理**，不是“耳机刚刚连接成功”。实际设备连接要看 `proxy.connectedDevices`。

**真机实测：**

1. 耳机取出并连接手机：A2DP 已连接列表出现 `vivo TWS Air3`。
2. 耳机放回充电盒并断开：A2DP 已连接列表变为 0 个。
3. 期间 `bondedDevices` 的配对记录仍然保留。

A2DP 列表只表示**这个音频 Profile** 的连接状态；列表为空不等于所有蓝牙连接都已断开。

### 6. 关于“不扫描、不配对，按地址直接连接”的讨论

如果事先知道目标设备地址，Android 可以先获取代表该地址的设备对象：

```kotlin
val device = bluetoothAdapter.getRemoteDevice("AA:BB:CC:DD:EE:FF")
```

**注意：此调用只获取设备对象，不会自动建立连接，也不证明设备存在。**

对于 BLE 硬件，可以进一步通过 `connectGatt()` 尝试连接；是否要求蓝牙配对，取决于外设对于连接及具体数据访问的安全要求。

- 简单、公开的传感器数据：设备可能允许不配对直接连接和读取。
- 涉及隐私、控制权限的设备：应通过 BLE 安全机制或应用层认证保护操作。
- **蓝牙地址不是密码**，知道地址不代表获得控制权限。
- BLE 私有随机地址可能变化，因此长期把地址写死不总是可靠。

**本节属于概念讨论，未实际调用 `connectGatt()`，也没有测试数据读写。**

### 7. 传统蓝牙和 BLE 的区别

| 维度 | 传统蓝牙（Bluetooth Classic） | 低功耗蓝牙（BLE） |
| --- | --- | --- |
| 设计侧重点 | 较适合连续通信 | 较适合低功耗、间歇性、小数据交互 |
| 常见应用 | 传统耳机音频、经典蓝牙串口 | 温度计、心率计、传感器、物联网控制 |
| 本次涉及 API | A2DP / `BluetoothProfile` | `BluetoothLeScanner`、`ScanCallback` |
| 进一步通信 | 根据 Profile 或 Socket 等机制 | GATT 服务与特征值读写 |

两者不是简单的“旧版 vs 新版”。BLE 也能建立持续连接；现代 **LE Audio** 还能用低功耗蓝牙技术传输音频。

本次耳机实验主要验证了传统 A2DP 音频连接；BLE 扫描与 A2DP 连接查询来自不同机制，因此观察到的设备及名称可能不一致。

---

## 四、目标 7：将蓝牙功能接入已有 App

本次没有新建独立工程，而是在原有 Android 学习项目里集成蓝牙页面。

主要改动（以**本地已运行版本**为准）：

```text
app/src/main/
├── AndroidManifest.xml
└── java/com/example/learnandroidfromai/
    ├── MainActivity.kt
    └── ui/bluetooth/BluetoothScreen.kt   ← 新增
```

`MainActivity.kt` 的 Compose 界面中调用 `BluetoothScreen()`，蓝牙逻辑集中在新文件中。

页面逐步集成了：

1. 是否支持经典蓝牙、BLE 的硬件能力检查。
2. 当前蓝牙开关状态与广播同步更新。
3. 扫描权限请求和授权状态反馈。
4. BLE 扫描开始 / 停止、10 秒自动结束。
5. 扫描回调计数、按地址去重、名称 / RSSI / UUID 展示。
6. `BLUETOOTH_CONNECT` 授权与已配对设备列表查询。
7. A2DP 已连接音频设备的手动查询。

使用到的 Compose 工具：

- `remember`、`mutableStateOf`、`mutableStateMapOf`：保存 UI 可观察状态。
- `rememberLauncherForActivityResult`：请求运行时权限。
- `DisposableEffect`：负责注册和销毁广播接收器。
- `LaunchedEffect`：处理扫描超时等随状态启动的任务。
- `LazyColumn`：展示扫描结果。

这相当于把 Android 平台 API、真机权限和 Compose 状态管理串成了一个可交互的实验页面。

---

## 五、目标 8：真机上遇到的现象和经验

| 实际现象 | 学到的原因或处理方式 |
| --- | --- |
| 关闭蓝牙后，“硬件支持”仍然显示 `true` | 设备支持和开关启用状态是两个独立概念 |
| 手动读取 `isEnabled` 后界面不会立刻刷新 | 外部系统状态要通过广播等机制更新 Compose State |
| 扫描前必须处理授权 | 不同 Android 版本的蓝牙运行时权限不同 |
| 扫描回调非常多 | 一个设备可重复广播；回调次数不是设备数量 |
| 大量扫描结果名称为 `(no name)` | BLE 广播可能不携带名称，不能由此判断设备身份 |
| 耳机可在系统设置中识别，但 BLE 列表找不到名称 | 经典蓝牙配对和 BLE 广播发现是不同机制 |
| 耳机无法立刻在手机设置中出现 | 需确认耳机进入可发现 / 配对模式，并排查其他设备的占用或连接 |
| 耳机放回充电盒后不再显示 A2DP 已连接 | 连接状态变化不代表配对记录消失 |
| 面对不认识的 BLE 设备 | 不贸然连接、写入数据，先确认属于自己的设备 |

### 哪些实验主动跳过了？

- **A2DP 连接状态变化广播实时监听：**已经掌握相同的 `BroadcastReceiver + DisposableEffect` 模式，决定不重复实践。可用 `BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED` 作为以后扩展方向。
- **实际 BLE GATT 连接与数据读写：**尚未有明确且合适的可控 BLE 外设，因此没有随意连接附近的匿名设备；可以在以后使用自己的 BLE 开发板时补做。

---

## 六、最重要的知识关系图

```text
Android 蓝牙功能
    │
    ├── 硬件能力（是否支持 Classic / BLE）
    │
    ├── 系统开关（BluetoothAdapter.isEnabled）
    │
    ├── 运行时权限（SCAN / CONNECT 等）
    │
    ├── 设备发现
    │   └── BLE ScanCallback：广播、RSSI、可能为空的名称
    │
    ├── 系统已配对设备
    │   └── BluetoothAdapter.bondedDevices
    │
    └── 通信连接
        ├── 经典蓝牙音频：A2DP 已连接设备查询【已验证】
        └── BLE：connectGatt → 服务发现 → 特征值读写【未实践】
```

**一句话复盘：**我们已经学会如何让 Android App 在真机上识别蓝牙硬件、处理权限、发现 BLE 广播、识别已配对设备，并区分耳机的配对与 A2DP 连接状态；还没有做到应用自己建立 GATT 连接并读写数据。

## 七、后续待办

- [x] 目标 5：访问蓝牙硬件、检测开关、监听系统状态。
- [x] 目标 6：完成权限、BLE 设备发现和系统配对 / A2DP 连接状态的观察。
- [ ] 目标 6 延伸：拥有合适 BLE 外设后，补做 GATT 连接和读写。
- [x] 目标 7：将蓝牙实验界面集成到现有 Compose App。
- [x] 目标 8：记录并理解真机蓝牙行为差异。
- [ ] **下一条主线：阶段 6 目标 9 —— Release 构建、签名和 APK。**

---

## 八、参考与说明

- 项目仓库：<https://github.com/slc90/learn_android_from_AI>
- 规划来源：`plans/Android开发学习规划.md` 中的“阶段 6：真机运行与设备能力”。
- API 关键词：`BluetoothManager`、`BluetoothAdapter`、`BluetoothLeScanner`、`ScanCallback`、`BluetoothDevice`、`BluetoothProfile`、`BluetoothA2dp`、`BluetoothGatt`。
- 本文记录的是**本次对话中的学习实践**，不是对本地完整工程文件的逐行审核；本地未提交的新文件和改动可能还没有同步到 GitHub。
