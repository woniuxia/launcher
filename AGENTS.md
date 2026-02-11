# Repository Guidelines

## 项目结构与模块组织
本仓库是一个基于 Kotlin + Jetpack Compose 的单模块 Android 应用。

- `app/src/main/java/cn/whc/launcher/`：业务代码主目录
- `app/src/main/java/.../ui/`：Compose 界面层（`components`、`screens`、`theme`、`viewmodel`）
- `app/src/main/java/.../data/`：数据层（Room 实体、DAO、数据库、Repository、缓存）
- `app/src/main/java/.../di/`：依赖注入模块（Hilt）
- `app/src/main/res/`：资源文件（`drawable`、`mipmap`、`strings`、`themes`）
- `app/src/test/`：本地 JVM 单元测试
- `app/src/androidTest/`：设备/模拟器上的仪器化与 UI 测试

## 项目架构与核心逻辑
- 架构分层：`Compose UI -> LauncherViewModel -> Repository -> Room/DataStore`，通过 `StateFlow` 驱动界面状态。
- 页面模型：主界面为两页纵向切换（首页 + 应用抽屉），设置页独立路由管理。
- 冷启动策略：优先读取 `HomePageCache` 直接出首屏，再后台执行 `syncInstalledApps()` 同步。
- 排序策略：基于 30 天次数、7 天活跃、最近使用衰减计算评分；仅在页面 `ON_STOP` 触发刷新，避免点击后重排抖动。
- 推荐策略：按“当前时间 ±30 分钟”统计启动记录生成 Top 列表，不足时用最近应用补足。
- 名单机制：黑名单应用隐藏；灰名单应用保留在抽屉，但不进入首页/常用/推荐。
- 系统集成：`AppChangeReceiver` 监听安装卸载更新，`DataCleanupWorker` 每日清理 30 天外统计。

## 预设档位说明（极简 / 均衡 / 专注）
- 本项目新增个人化预设档位：`LITE`（极简）、`BALANCED`（均衡）、`FOCUS`（专注）。
- 预设入口位于设置主页顶部，优先于细粒度开关；若发生冲突，以“最近一次显式修改”生效。
- `LITE`（极简）：
  - 目标：最低复杂度与较低资源占用。
  - 默认行为：`homeDisplayCount=12`、`drawerFrequentCount=4`、`showSearch=true`、`showTimeRecommendation=false`、`blurStrength=12`、`iconSize=54`、`hapticFeedback=false`。
- `BALANCED`（均衡）：
  - 目标：功能与体验平衡，作为默认推荐档位。
  - 默认行为：`homeDisplayCount=16`、`drawerFrequentCount=5`、`showSearch=true`、`showTimeRecommendation=true`、`blurStrength=20`、`iconSize=56`、`hapticFeedback=true`。
- `FOCUS`（专注）：
  - 目标：高效率访问与稳定操作反馈。
  - 默认行为：`homeDisplayCount=20`、`drawerFrequentCount=6`、`showSearch=true`、`showTimeRecommendation=true`、`blurStrength=10`、`iconSize=58`、`hapticFeedback=true`。
- 数据结构约定：
  - `AppSettings` 采用 `core + advanced + legacy` 并行模型；`core` 承载预设主参数，`advanced` 承载低频高级项。
  - `SettingsRepository` 使用 `schema v2` 键空间（`core_*`、`advanced_*`、`preset`），并保持对旧键的读取兼容。
  - 迁移入口：应用启动后由 `LauncherViewModel` 调用 `ensureSchemaV2()`，完成旧数据到 v2 的惰性迁移。

## 构建、测试与开发命令
请在仓库根目录（`E:\Projects\launcher`）执行以下命令。

- `./gradlew :app:assembleDebug`（Windows 可用 `.\gradlew.bat :app:assembleDebug`）：构建 Debug APK
- `./gradlew :app:testDebugUnitTest`：运行 `app/src/test` 下的单元测试
- `./gradlew :app:connectedDebugAndroidTest`：在已连接设备/模拟器上运行仪器化测试
- `./gradlew :app:lintDebug`：执行 Android Lint 检查
- `./gradlew clean`：清理构建产物

## 验证职责约定
- 代理在代码修改后默认不执行编译或测试验证（如 `assemble`、`test`、`lint`）。
- 编译与测试验证由用户在本地环境自行执行。

## 代码风格与命名规范
- 遵循 Kotlin 官方风格：4 空格缩进、禁止 Tab、命名语义清晰。
- 类名、Composable 文件名使用 `PascalCase`（如 `LauncherViewModel.kt`、`HomePage.kt`）。
- 函数、属性、变量使用 `camelCase`。
- 包名统一小写，保持在 `cn.whc.launcher` 命名空间下。
- 保持函数职责单一：UI 逻辑放在 `ui/*`，数据逻辑放在 `data/*`。

## 注释与编码约定
- 所有源码文件统一使用 `UTF-8` 编码（建议无 BOM），禁止混用本地编码（如 GBK/ANSI）。
- 修改代码时如需处理注释乱码，优先“修复注释文本”，不要通过删除注释替代修复。
- 新增或修改注释时，保持可读中文表达，并确保保存后不会出现乱码。
- 若终端显示乱码，以文件实际 UTF-8 内容为准；必要时先统一文件编码再改注释。
- 对可访问性文案（如 `contentDescription`）同样按 UTF-8 中文维护，避免出现乱码文本。

## 测试规范
- 单元测试使用 JUnit（`app/src/test`），仪器化测试使用 AndroidX Test（`app/src/androidTest`）。
- 测试文件以 `*Test.kt` 结尾，并尽量与生产代码包结构对应。
- 新增功能时，优先覆盖 Repository 逻辑、工具函数、ViewModel 行为。
- 提交 PR 前至少运行 `:app:testDebugUnitTest`，并补充必要的仪器化测试结果。

## 提交与 Pull Request 规范
- 历史提交以简短中文祈使句为主（例如：`优化加载效率`）。
- 每次提交聚焦单一目的，避免将重构与功能改动混在同一提交中。
- PR 需包含：变更摘要、影响范围、测试证据（命令输出）、UI 变更截图或 GIF。
- 关联对应任务/Issue，并明确标注后续待办项。

## 安全与配置建议
- 禁止提交签名密钥、敏感信息或机器本地专用配置。
- 本地配置放入 `local.properties`，环境相关配置不要直接入库。
- 新增 `receiver` 或 `worker` 时，检查 `AndroidManifest.xml` 的导出与权限配置。
