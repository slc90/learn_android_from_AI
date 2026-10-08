# 阶段 6｜目标 9：正式构建、签名与 APK 安装升级总结

> 项目：`learn_android_from_AI`  
> 学习环境：Windows、Android Studio、Kotlin / Jetpack Compose、C++ Native、Android 真机  
> 目标来源：`plans/Android开发学习规划.md` 的阶段 6 目标 9——**了解正式构建、签名和安装包在真实设备上的作用**  
> 记录范围：本次对话中完成的构建、真机安装、签名冲突、覆盖升级，以及 Gradle 自动签名配置  
> 记录原则：将**已实际验证**与**仅解释、尚未操作**分开；本地修改不等同于已经同步到 GitHub 仓库。

## 一、本次实践结论

**目标 9 已完成。** 我们不仅生成了 Debug 和 Release APK，还在真实设备上验证了应用签名对安装更新的限制，并完成了 Release 1.0 → 1.1 的覆盖升级与 Room 数据保留实验。

| 实验 | 观察结果 | 状态 |
| --- | --- | --- |
| 从 Android Studio 手动生成 Debug APK | 找到 `app-debug.apk`，另有构建元数据 | 已验证 |
| 脱离 Android Studio 安装 Debug APK | 将 APK 复制到手机，安装后能独立运行 | 已验证 |
| 创建签名密钥并生成 Release APK | 生成 `app-release.apk`，另有 Native 调试符号和元数据 | 已验证 |
| 在已安装 Debug 的手机上直接安装 Release | 设备提示“与已安装应用签名不同”，安装失败 | 已验证 |
| 安装 Release 后直接使用 Android Studio 的 Debug Run | Android Studio 提示相同包名但签名不同，无法直接覆盖 | 已验证 |
| 使用原密钥构建 Release 1.1 并覆盖安装 1.0 | 安装成功，之前保存的 Room 记录仍能显示 | 已验证 |
| 通过 `:app:assembleRelease` 自动签名 | Gradle 构建成功，APK 出现在 `build/outputs` 下 | 已验证 |
| 将签名配置迁移到项目本地 | 使用 `signing.properties` 和项目内 `keystore/` 后，命令仍可成功生成 APK | 已验证（按本地操作反馈） |

---

## 二、Debug、Release 和 APK 是什么

### 1. 构建与安装的关系

```text
Android 项目代码、资源和 Native 代码
                 ↓
              Gradle 构建
                 ↓
            APK + 数字签名
                 ↓
         在 Android 设备上安装
```

- **Build（构建）**：编译和打包应用代码、资源与相关依赖；包含 Native 模块时还涉及 CMake / NDK 构建。
- **APK**：Android 应用安装包，能够交给设备安装；设备仍会执行自己的安装、权限和安全检查。
- **Signing（签名）**：用私钥对 APK 进行数字签名，支持系统验证包的完整性和签名身份；它不等于应用代码已经通过安全审计。

### 2. Debug 与 Release

| 对比项 | Debug | Release |
| --- | --- | --- |
| 主要用途 | 开发、运行、调试 | 正式测试、交付、发布 |
| 常见签名 | Android Studio/SDK 使用的调试密钥 | 开发者指定的发布密钥 |
| 本次生成文件 | `app-debug.apk` | `app-release.apk` |
| 是否可手动安装 | 可以（本次已验证） | 可以（签名完成后，本次已验证） |

本次项目 Release 的 `optimization.enable = false`，因此 **Release 并不自动意味着已经启用了代码优化或混淆**；具体以项目的构建配置为准。

`AAB`（Android App Bundle）也用于应用分发流程，但本次没有生成或发布 AAB；实践对象是 **APK**。

---

## 三、实践 1：生成并安装 Debug APK

在 Android Studio 中选择 `debug` 构建变体，并通过 **Build → Generate Bundle(s) / APK(s) → Generate APK(s)**（不同版本菜单名称可能略有区别）生成安装包。

默认输出目录示例：

```text
app/
└── build/
    └── outputs/
        └── apk/
            └── debug/
                ├── app-debug.apk
                └── output-metadata.json
```

- `app-debug.apk`：实际用于安装的文件。
- `output-metadata.json`：描述 APK 构建输出的元数据，不需要拷贝到手机安装。

**实际操作：**把 Debug APK 复制到手机，通过文件管理器安装并打开，App 正常运行。这证实 **APK 可以脱离 Android Studio 的 Run 独立安装**。

手机安装过程中还出现了**小米系统的额外安全验证**。这属于设备厂商的安装流程；通过 Android 签名检查不等于可以免除厂商安全检查。

---

## 四、实践 2：创建 Keystore 并生成签名 Release APK

### 1. Keystore 的结构

```text
Keystore 文件（密钥库）
│
├── Store Password（密钥库密码）
│
└── Key（签名私钥）
    ├── Alias（密钥别名）
    └── Key Password（密钥密码）
```

本次创建了用于发布构建的签名密钥，别名为 `learnandroid`。

- **Keystore**：存放私钥等材料的文件，常见扩展名为 `.jks`，但文件名**不强制必须**有 `.jks` 后缀。本次本地密钥文件最终使用了无 `.jks` 后缀的文件名。
- **Alias**：在同一个密钥库中标识某把密钥。
- **私钥**：用于给 APK 签名，必须保密。
- **证书/公钥**：帮助 Android 验证签名身份；证书有效期不等于 App 本身的使用期限。

**不要重新创建一把不同的密钥来代替旧密钥给后续版本签名**，否则可能无法正常覆盖更新当前已安装的 Release App。

### 2. Android Studio 签名向导

操作路径：**Build → Generate Signed Bundle / APK → APK**。

在向导中指定 Keystore、密钥别名和密码，选择 `release`，生成正式签名的 APK。

本次向导指定的输出目录是 `app/`，实际输出位置为：

```text
app/
└── release/
    ├── app-release.apk
    ├── output-metadata.json
    └── native-debug-symbols.zip
```

其中：

- `app-release.apk`：已签名的 Release 安装包。
- `output-metadata.json`：构建输出与版本等元数据。
- `native-debug-symbols.zip`：Native 调试符号归档，用于辅助分析 C/C++ 崩溃；不是安装包。

### 3. C++ 构建较慢的原因

第一次 Release 构建期间，Android Studio 的构建窗口显示了针对多个 ABI（CPU 架构）的 CMake 配置任务，例如 `arm64-v8a`、`armeabi-v7a`、`x86`。这一步耗时较长，但随后 `arm64-v8a` 等任务陆续完成，构建最终成功。

需要区分：**`configureCMake...` 是 CMake 配置阶段，并不等于所有耗时都发生在真正的 C++ 编译中。** 后续构建在任务输出未变化时可能显示 `UP-TO-DATE`，明显更快。

---

## 五、实践 3：验证签名冲突

项目包名（Application ID）是：

```text
com.example.learnandroidfromai
```

实验的两个安装包具有相同的 Application ID，但使用了不同的签名身份：

| 安装包 | Application ID | 签名 |
| --- | --- | --- |
| Debug APK | `com.example.learnandroidfromai` | 调试密钥 |
| Release APK | `com.example.learnandroidfromai` | `learnandroid` 发布密钥 |

**实际观察 1：** 手机里仍有 Debug App 时，尝试直接安装 Release APK，系统提示：**“与已安装应用签名不同”**，安装失败。

**实际观察 2：** 卸载旧 Debug App、装上 Release App 后，再在 Android Studio 中按 Debug `Run`，IDE 提示设备已有相同包名、不同签名的应用，不能直接覆盖。

理解为：

```text
Application ID 相同
       +
签名身份不兼容
       ↓
无法作为原应用的正常更新包安装
```

这说明 Android 在应用更新时不只看包名，还会校验签名兼容性。卸载再安装不同签名版本虽然可以完成安装，但**卸载通常会删除应用私有数据**，所以不是正常升级手段。

---

## 六、实践 4：版本号与覆盖升级

项目的 `app/build.gradle.kts` 中，版本由 `defaultConfig` 指定：

Release 1.0：

```kotlin
versionCode = 1
versionName = "1.0"
```

Release 1.1：

```kotlin
versionCode = 2
versionName = "1.1"
```

- **`versionCode`**：供 Android 等系统比较版本顺序的整数，正常发布新版本时应递增。
- **`versionName`**：面向用户展示的版本名称；不能代替 `versionCode` 决定版本顺序。

### 实际测试过程

1. 为了留下可验证的旧数据，将先前 Stage 3 的 `Stage3App()` 界面重新作为应用入口展示（替代当时 `MainActivity.kt` 中正在显示的蓝牙实验界面）。
2. 保持 **Release 1.0**（`versionCode = 1`），使用原先 `learnandroid` 密钥重新构建，并在手机上安装。
3. 在旧版本的 **Room 任务**功能中添加一条测试记录，确认可以读取。
4. 把版本号改成 `versionCode = 2`、`versionName = "1.1"`。
5. **不卸载** Release 1.0，使用相同 Keystore 和密钥别名构建 Release 1.1，直接覆盖安装。
6. 打开新版本后，**之前的 Room 记录依然显示**。

**实验结论：**本次成功验证了相同签名身份下的 Release 覆盖升级；Android 在这种正常更新过程中没有自动清除应用私有的 Room 数据。

这不意味着所有升级都天然保证业务数据完全兼容：如果以后修改了 Room 数据库结构，还需要处理数据库迁移等应用自身的问题。本次没有涉及数据库结构变化。

---

## 七、实践 5：让 Gradle 自动签名 Release

最初，每次通过 Android Studio 签名向导都要重复选择 Keystore。后来改为在 Gradle 的 Release 构建类型中引用签名配置，从而可以直接运行：

```powershell
.\gradlew.bat :app:assembleRelease
```

### 1. 使用用户级 Gradle 属性（中间方案）

最初将属性放在用户目录下的：

```text
C:\Users\<用户名>\.gradle\gradle.properties
```

用四个 Gradle 属性指定密钥路径、密钥库密码、别名、密钥密码：

```properties
RELEASE_STORE_FILE=C:/Users/<用户名>/AndroidSigning/learn-android-release
RELEASE_STORE_PASSWORD=<密钥库密码>
RELEASE_KEY_ALIAS=learnandroid
RELEASE_KEY_PASSWORD=<密钥密码>
```

第一次运行时曾因 `RELEASE_STORE_FILE` 指向的目录不正确，导致 Gradle 找不到密钥文件。修正为**实际存在的文件路径**后，构建成功。这次还确认了：**密钥库文件可以没有 `.jks` 后缀，只要路径和文件格式正确即可。**

### 2. 最终调整为项目本地配置（本次最终使用方案）

本次最后将签名配置放在项目目录内，结构示例如下：

```text
learn_android_from_AI/
├── app/
│   └── build.gradle.kts
├── keystore/
│   └── learn-android-release
├── signing.properties
├── gradle.properties
└── .gitignore
```

**注意：这是本地目录组织示例，不代表密钥或密码应该提交到仓库。**

`signing.properties` 存储本地配置，以下均为占位值，不是真实密码：

```properties
RELEASE_STORE_FILE=keystore/learn-android-release
RELEASE_STORE_PASSWORD=<密钥库密码>
RELEASE_KEY_ALIAS=learnandroid
RELEASE_KEY_PASSWORD=<密钥密码>
```

`app/build.gradle.kts` 读取 `signing.properties` 的核心方式：

```kotlin
import java.util.Properties

val signingProperties = Properties().apply {
    rootProject.file("signing.properties")
        .inputStream()
        .use { load(it) }
}

android {
    // ...原有项目配置...

    signingConfigs {
        create("release") {
            storeFile = rootProject.file(
                signingProperties.getProperty("RELEASE_STORE_FILE")
            )
            storePassword =
                signingProperties.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias =
                signingProperties.getProperty("RELEASE_KEY_ALIAS")
            keyPassword =
                signingProperties.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                enable = false
            }
        }
    }
}
```

代码仅展示了**与签名有关的部分**，不应直接替换掉实际工程中完整的 `android` 配置和其他依赖声明。本方案要求本地存在 `signing.properties`；若其他机器或 CI 环境没有该文件，需要另行提供签名信息或调整配置加载方式。

项目根目录 `.gitignore` 增加：

```gitignore
/signing.properties
/keystore/
```

**本次验证结果：**改成项目本地签名配置后，再次运行 `:app:assembleRelease`，仍能正常生成 APK。

### 3. 保护私钥与密码

- `signing.properties` 中的密码为明文，虽然 Git 忽略了它，**本地文件仍需保护**。
- `.gitignore` 不能自动取消已跟踪文件的跟踪，也不能撤销历史提交；提交前可检查 `git status`、`git ls-files signing.properties keystore`。
- 正式签名密钥、对应密码应另外安全备份；项目误删、换电脑或硬盘损坏时仍需要它们。
- **不要因文件名无 `.jks` 后缀就重新生成一把密钥**；应继续使用这次发布 Release 1.0 / 1.1 的原密钥。

---

## 八、两种 Release 构建方式的输出位置为什么不同

### Android Studio 签名向导

签名向导允许选择 `Destination Folder`。本次设为 `app/`，生成结果在：

```text
app/release/app-release.apk
```

### Gradle 的 `assembleRelease`

使用标准 Gradle 构建任务时，APK 默认在：

```text
app/build/outputs/apk/release/app-release.apk
```

两者最终都是 Release APK，但**向导导出目录与标准 Gradle 构建产物目录不同**。

`app/build/` 属于构建产物目录，`clean` 或后续构建可能重建其内容。如果要长期归档某个发布版本，应将 APK 另外复制到安全的归档位置。

### 命令速查

在项目根目录的 Windows PowerShell：

```powershell
# 自动使用已配置的 Release 密钥并生成 APK
.\gradlew.bat :app:assembleRelease

# 如需查看 Gradle 的更多构建日志
.\gradlew.bat :app:assembleRelease --info
```

本次曾出现 `49 up-to-date` 等提示，表示相应任务的输入与输出未发生需要重新执行的变化。首次 Release C++ 构建较慢，后续重复执行可能非常快。

---

## 九、目标 9 的知识关系图

```text
Kotlin / Compose / C++ 工程
            ↓
         Gradle 构建
            ↓
     Debug APK 或 Release APK
            ↓
      Keystore 私钥签名
            ↓
       安装到 Android 真机
            ↓
          更新校验
       ┌────┴────┐
       │         │
  签名不兼容   签名兼容
       │         │
    更新失败   再检查版本等条件
                 │
            可正常覆盖升级
                 │
         应用原有数据通常保留
```

最值得记住的是三件事：

1. **APK 是可以独立安装的交付物**；Android Studio 的 Run 只是开发期的一种安装途径。
2. **签名决定应用更新时的身份兼容性**；包名、版本名相同也不能绕过签名不兼容。
3. **发布密钥要持续保管**；同一签名的 Release 新版本能够正常升级，而升级是否正确保留和使用业务数据，还取决于应用自己的数据兼容处理。

---

## 十、完成状态与未涉及内容

- [x] 构建 Debug APK，并在真机独立安装运行。
- [x] 创建 Keystore 和签名密钥。
- [x] 构建已签名的 Release APK。
- [x] 验证 Debug / Release 不同签名不能直接互相覆盖。
- [x] 调整 `versionCode`、`versionName`，完成 Release 1.0 → 1.1 覆盖升级。
- [x] 验证 Room 测试记录在本次升级后仍然存在。
- [x] 使用 Gradle 的 `assembleRelease` 自动签名构建。
- [x] 将签名配置迁移到项目本地，并成功构建。
- [ ] 本次未实践：AAB / Google Play 发布流程、应用商店签名托管、实际开发者身份注册。
- [ ] 本次未实践：正式发布前的完整测试、自动化 CI 构建、密钥轮换与恢复策略。
- [ ] 本次未实践：对迁移后的 `.gitignore` 做独立的 Git 跟踪状态审计（建议提交前检查）。

## 参考资料与记录来源

- 项目仓库：<https://github.com/slc90/learn_android_from_AI>
- 学习规划：<https://github.com/slc90/learn_android_from_AI/blob/main/plans/Android开发学习规划.md>
- 之前的真机蓝牙学习总结：`plans/Stage6/阶段6-目标5~8-Android 蓝牙真机实践总结.md`
- 本总结根据本次学习对话中的**实际操作反馈、构建截图及所读取的仓库配置**整理。本地项目文件和配置可能尚未同步到远程仓库。
