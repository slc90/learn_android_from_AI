# Stage 1 · 目标 4：熟练使用 Android 模拟器

## 目标

能够比较熟练地创建、启动、管理和使用 Android 模拟器，并能在模拟器出现常见问题时做基础排查。

---

## 1. AVD、Emulator 和 Device Manager

需要先分清三个概念：

```text
Device Manager
    ↓ 创建 / 管理
AVD
    ↓ 启动
Android Emulator
    ↓ 运行
Android 系统和 App
```

- **AVD（Android Virtual Device）**：一台虚拟 Android 设备的配置。
- **Android Emulator**：真正把 AVD 运行起来的模拟器程序。
- **Device Manager**：Android Studio 中用于创建、编辑和管理 AVD 的工具。

一个 AVD 大体可以理解为：

```text
Hardware Profile
      +
System Image
      +
Additional settings
      ↓
     AVD
```

其中：

- Hardware Profile 决定设备外形、屏幕、摄像头等硬件特征。
- System Image 决定虚拟设备运行哪个 Android 系统版本。
- Additional settings 可以继续配置存储、启动方式、图形渲染等参数。

本次实际使用过的 AVD 包括：

- `Medium Phone`
- `Pixel 10a`

两台 AVD 可以同时运行，而且彼此完全独立。

---

## 2. 模拟器不是“App 预览器”

模拟器更接近一台真正的 Android 设备。

可以直接在其中进行：

- Home
- Back
- 屏幕旋转
- 打开系统 App
- 安装和卸载 App
- 修改电池状态
- 修改位置
- 截图和录屏

所以应该形成这个直觉：

```text
Android Emulator
≠ 只显示 App 的预览窗口

Android Emulator
= 在电脑上模拟一台 Android 设备
```

---

## 3. Run 到模拟器时发生了什么

在 Android Studio 中选择一个运行设备并点击 `Run` 后，大致过程是：

```text
项目代码
   ↓ build
生成 APK
   ↓ install
安装到指定 AVD
   ↓ launch
启动 App
```

所以 `Run` 并不是“临时显示一下 App”。

App 会真正安装到虚拟设备里。按 Home 回到桌面后，可以像普通 App 一样重新从应用列表中打开。

不同 AVD 的 App 安装状态互不共享：

```text
Medium Phone
└── 自己安装的 App 和数据

Pixel 10a
└── 自己安装的 App 和数据
```

同一份 Android 项目也可以分别部署到不同 AVD 上进行测试。

---

## 4. Quick Boot 和 Cold Boot

### Quick Boot

普通情况下，在 Device Manager 中直接点击启动按钮，就是 Quick Boot。

它会尽量恢复虚拟设备上次关闭时保存的状态。

```text
上次运行状态
    ↓ 保存
下次快速恢复
```

特点：

- 启动快
- 日常开发最常用

### Cold Boot

Cold Boot 不恢复之前保存的运行状态，而是让 Android 系统重新进行一次完整启动。

```text
Cold Boot
= 不恢复快照
= 重新完整启动 Android
```

实际可以看到较完整的 Android / Google 开机动画。

可以简单类比：

```text
Quick Boot ≈ 恢复现场
Cold Boot  ≈ 手机重新开机
```

当模拟器状态异常时，可以优先尝试 Cold Boot。

---

## 5. Wipe Data

`Wipe Data` 和 Cold Boot 完全不同。

```text
Cold Boot
= 重新开机
= 原有 App 和数据仍然保留

Wipe Data
= 把 AVD 恢复到接近初始状态
= 已安装 App、设置和用户数据都会被清除
```

可以类比真机：

```text
Cold Boot ≈ 重启手机
Wipe Data ≈ 恢复出厂设置
```

本次实际执行 Wipe Data 后观察到：

- 第一次重新启动明显更慢。
- 之前通过 Android Studio `Run` 安装进去的 APK 消失了。

所以 Wipe Data 一般只在虚拟设备环境本身已经比较混乱时使用。

---

## 6. 多台模拟器可以同时运行

多个 AVD 可以同时启动，例如：

```text
Medium Phone   → Running
Pixel 10a      → Running
```

但：

```text
“模拟器正在运行”
≠
“这次 Run 会部署到它”
```

每次点击 Android Studio 的 `Run` 时，仍然要选择具体的目标设备。

同时运行多台模拟器会消耗更多：

- CPU
- 内存
- 图形资源

如果电脑资源足够，多设备并行测试很方便。

---

## 7. 不同虚拟设备的屏幕差异

不同 Hardware Profile 会有不同的：

- 屏幕尺寸
- 长宽比例
- 分辨率
- dpi
- 设备外形

同一份 App 在不同设备上运行时，可见区域可能不同。

本次实际观察到：

- `Pixel 10a` 的显示区域比 `Medium Phone` 更长。

这说明以后测试 UI 时不能只看一台设备。

---

## 8. Extended Controls

模拟器的 Extended Controls 可以人为制造很多设备状态。

本次实际体验了：

### Battery

可以修改：

- 电量
- 充电状态等

当电量直接调到 `0%` 时，虚拟 Android 设备甚至会按真实设备逻辑自动关机。

这说明：

```text
修改模拟器状态
→ Android 系统会真的按照这个状态运行
```

### Location

可以人为给模拟器提供 GPS 位置。

```text
真实手机
GPS → Android

模拟器
手动设置位置 → Android
```

测试定位功能时，不需要真的移动电脑。

---

## 9. 截图和录屏

### Screenshot

模拟器截图只截虚拟 Android 设备的屏幕，而不是整个 Android Studio。

适合：

- 记录 UI
- 保存错误画面
- 比较不同设备显示效果

### Screen Record

录屏用于记录一段设备操作过程。

适合：

- UI 动画异常
- 点击后的错误流程
- 横竖屏切换问题
- 难以用单张截图描述的问题

可以简单记成：

```text
Screenshot
= 一个瞬间

Screen Record
= 一段操作过程
```

---

## 10. 模拟器显示缩放

Android Studio 的 Running Devices 中可以调整模拟器显示比例。

例如：

```text
69%
100%
1:1
```

这里改变的只是：

> 虚拟设备在电脑屏幕上显示得多大。

不会改变虚拟设备本身的：

- 分辨率
- dpi
- 屏幕尺寸
- App 实际可用布局空间

本次界面中的 `69%` 是当前窗口下完整显示设备时的 fit-to-window 比例，因此不能继续缩小；放大后才可以再使用减号缩回去。

---

## 11. 本次模拟器崩溃排查

实际遇到过：

```text
Android Emulator closed unexpectedly
```

这表示崩溃的是 Android Emulator 进程本身，而不是 App。

排查过程中确认：

### Emulator 版本

Android Emulator：

```text
37.1.11
```

### Windows 虚拟化

在 Android Studio Terminal 中执行：

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -accel-check
```

得到：

```text
accel:
0
WHPX(10.0.26200) is installed and usable.
accel
```

说明 WHPX 硬件虚拟化正常可用。

### Quick Boot 排查

Cold Boot 同样出现崩溃，因此问题不只是 Quick Boot / snapshot 状态。

### Graphics 排查

把 AVD 的 Graphics 从硬件/自动方式改为：

```text
Software
```

之后连续 4 次 Cold Boot 都正常启动。

因此这次问题基本可以定位到：

```text
Emulator
   ↓
硬件图形渲染路径
   ↓
GPU / 驱动 / Emulator 图形后端兼容性
```

而不是：

- Android 项目代码
- APK
- WHPX 虚拟化
- 单纯的 Quick Boot snapshot

当前可以暂时保持 Software Graphics 使用。

代价是：

- 动画可能不够流畅
- 滚动可能更容易掉帧
- CPU 占用可能更高
- 多模拟器同时运行时性能差异可能更明显

但对当前学习阶段已经足够。

---

## 12. 一个实用的模拟器问题排查顺序

以后遇到模拟器问题，可以先按这个顺序判断：

```text
出现异常
   ↓
先判断：
App 崩了？
还是 Emulator 自己崩了？
   ↓
Cold Boot
   ↓
检查虚拟化加速
   ↓
尝试 Software Graphics
   ↓
再进一步看 Emulator 日志
```

不要一开始就 Wipe Data。

---

## 目标 4 完成情况

目前已经实际完成：

- [x] 理解 AVD、Emulator、Device Manager 的关系
- [x] 创建和编辑 AVD
- [x] 启动和关闭模拟器
- [x] 使用 Quick Boot
- [x] 使用 Cold Boot
- [x] 理解并实际体验 Wipe Data
- [x] 同时运行多个 AVD
- [x] 将同一项目部署到不同 AVD
- [x] 使用 Home、Back 和屏幕旋转
- [x] 使用 Battery 模拟设备状态
- [x] 使用 Location 模拟 GPS
- [x] 使用截图
- [x] 使用录屏
- [x] 理解模拟器显示缩放
- [x] 对 Emulator 崩溃做基础定位
- [x] 验证 WHPX 状态
- [x] 使用 Software Graphics 规避当前图形兼容问题

## 当前形成的核心认识

```text
AVD 是虚拟设备配置
Emulator 是运行这台设备的程序
Run 会把 App 真正安装到指定设备

虚拟设备彼此独立
可以同时运行多台

Quick Boot 用于快速恢复
Cold Boot 用于完整重启
Wipe Data 用于重置整台虚拟设备

模拟器可以主动制造设备状态
也可以用于多设备、多屏幕环境测试

遇到问题时，要先区分：
是 App 的问题，还是 Emulator 本身的问题
```

至此，Stage 1 · 目标 4「能够熟练使用 Android 模拟器」完成。
