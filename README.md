# 冰箱菜谱（FridgeRecipeManager）

Android 课程设计主 App：菜谱 / 冰箱管理（B6，B 档）。
必做：食材库存管理、过期提醒；自选：按食材智能推荐菜谱（本地算法）。

包名 `com.liang.fridgerecipe`，应用名「冰箱菜谱」，minSdk 24 / targetSdk 37。

## 一、功能清单

| 模块 | 已实现内容 |
| --- | --- |
| 食材库存管理 | 新增 / 编辑 / 删除食材；分类（9 类，带 emoji 与参考保质期）、数量 + 单位、购买日期、到期日期、备注；名称搜索、按「全部 / 临期 / 已过期 / 新鲜」筛选 |
| 过期提醒 | WorkManager 每天定时检查，WorkManager 周期任务 `expiry_daily_check`；提醒时间可在设置页调整；通知权限申请 + 拒绝后在列表页显示降级提示条 |
| 智能推荐菜谱 | 本地规则打分（覆盖率 50 + 临期消耗 20 + 缺料惩罚 15 + 快手加分 10），菜谱页按匹配度排序，可切换「只看能做的」；食材详情页给出「这个食材可以做的菜」 |
| 菜谱详情 | 逐条用料比对库存，标「有 / 临期 / 已过期 / 缺」，做法步骤、小贴士、匹配理由与得分 |
| 数据看板 | 库存总数、临期/过期数量、预计浪费率；自定义 View（Canvas）绘制品类分布柱状图；「最该先吃的」前 5 条 |
| 设置 | 提醒开关、提醒时间（MaterialTimePicker）、通知权限状态与跳转、导入示例食材、恢复内置菜谱、清空全部数据、版本信息 |
| 联网能力 | ① 在线同步：菜谱页点「☁ 同步云端」，用 OkHttp 拉取远程 `recipes.json` 并覆盖本地菜谱库；② AI 生成：点「✨ AI 生成一道菜」，把冰箱现有食材作为提示词调用 DeepSeek，返回结构化菜谱并预览、一键保存。断网 / 无 Key 时均降级为本地内置菜谱并给出文案提示 |
| 伴生模块 P2（数据看板） | 自定义 View 用 Canvas 手绘品类柱状图；点「⬇ 导出看板 CSV」可导出统计数据（UTF-8 BOM，Excel 直接打开）或品类柱状图 PNG（View.draw 抓 Bitmap），经 FileProvider 走系统分享面板发出，文件落在 App 专属目录、无需存储权限 |

内置 12 道家常菜（assets/recipes.json），首次启动自动写入 Room。

## 二、界面（7 个，含列表 → 详情）

1. 食材列表（首页，底部导航第 1 项）
2. 食材详情（列表点击进入，可编辑 / 删除，并展示相关菜谱）
3. 添加 / 编辑食材
4. 菜谱列表
5. 菜谱详情
6. 数据看板
7. 设置

## 三、技术点

- **持久化**：Room 2.8.5（KSP），库 `fridge_recipe.db`，3 张表 `ingredients` / `recipes` / `recipe_ingredients`（外键 CASCADE + 索引，`RecipeWithIngredients` 用 `@Relation` 做一对多）。数据唯一入口是 `FridgeRepository`。
- **后台任务（硬能力）**：`ExpiryReminderWorker`（CoroutineWorker）+ `ReminderScheduler`（周期 1 天，按用户设定的时分计算首次延迟）+ 通知渠道 `channel_expiry`。
- **网络（硬能力）**：OkHttp 4.12 + Gson。
  - `data/remote/RecipeSyncService`：GET 远程菜谱 JSON，校验状态码、空响应、格式错误三层异常，成功返回 `List<Recipe>`，失败抛给 ViewModel 转成「离线模式」。
  - `data/remote/AiRecipeService`：POST `https://api.deepseek.com/chat/completions`，system 提示固定「只输出 JSON」，user 提示携带冰箱食材清单；返回内容做 ```json 包裹清洗 + 必填字段兜底。
  - `ui/recipe/CloudRecipeViewModel`：`SyncState`（Idle/Loading/Success/Error）与 `AiState`（Idle/Loading/Result/Error）两个状态机，请求全部在 `viewModelScope` 内，页面销毁自动取消。
  - Key 不硬编码：`local.properties` 的 `DEEPSEEK_API_KEY` 通过 Gradle `buildConfigField` 注入，源码与 Git 历史里都没有明文。
- **协程 + ViewModel**：所有页面 `AndroidViewModel` + `StateFlow`，`combine` 聚合「数据库流 + 用户输入」，`repeatOnLifecycle(STARTED)` 收集，退到后台自动停止。
- **自定义业务逻辑（非纯 CRUD）**：
  - `domain/ExpiryAnalyzer`：按「归零到当天 0 点」算剩余天数、四档状态（过期 / 今明到期 / 快到期 / 新鲜）、0–100 风险分、优先级排序、看板聚合统计。
  - `domain/RecipeRecommender`：名称归一化 + 双向包含做模糊匹配，输出覆盖率、缺料、过期料、得分、推荐理由。
- **权限与降级**：Android 13+ `POST_NOTIFICATIONS`，首次进入主动申请一次；拒绝后不发通知，列表页显示「点击开启」提示条，设置页显示权限状态并可跳系统设置。
- **三态 UI**：列表页 `Loading / Content / Error` + 空态，错误可重试；其它页面空数据有空态文案。
- **自定义 View**：`ui/common/CategoryBarChartView`，Canvas 画横向柱状图，坐标/比例/文字全部自算（不依赖第三方图表库）。
- **文件导出（P2 伴生模块）**：`ui/dashboard/DashboardExporter`，CSV（带 BOM）+ PNG（`View.draw` 抓 Bitmap），`FileProvider`（`${applicationId}.fileprovider`，`res/xml/file_paths.xml`）授权分享，全程不申请存储权限。

## 四、源码结构

```
app/src/main/java/com/liang/fridgerecipe/
├── FridgeApp.kt                 # 通知渠道 + 提醒排期 + 首次导入菜谱
├── data/
│   ├── FridgeRepository.kt      # 唯一数据入口
│   └── local/                   # Entities / Daos / AppDatabase
├── domain/
│   ├── ExpiryAnalyzer.kt        # 自定义逻辑①：临期分析
│   └── RecipeRecommender.kt     # 自定义逻辑②：菜谱推荐
├── model/                       # FoodCategory / Ingredient / Recipe
├── ui/
│   ├── MainActivity.kt          # 底部导航 + 通知权限
│   ├── common/                  # StatusColors、CategoryBarChartView
│   ├── ingredient/              # 列表 / 详情 / 编辑 + 三个 ViewModel
│   ├── recipe/                  # 列表 / 详情 + 两个 ViewModel
│   ├── dashboard/               # 数据看板
│   └── settings/                # 设置
├── util/                        # PreferenceStore、DateUtils
└── work/                        # Worker / 通知 / 排期
```

## 五、构建

```powershell
cd D:\recyclerviewtest
.\gradlew.bat :app:assembleDebug --console=plain
```

产物：`app\build\outputs\apk\debug\app-debug.apk`。

联网功能需要在 `local.properties` 里补两项（该文件不进 Git）：

```properties
# DeepSeek 的 API Key，用于「AI 生成一道菜」
DEEPSEEK_API_KEY=sk-xxxxxxxxxxxxxxxx
# 可选：覆盖在线菜谱地址，默认走 ghproxy 代理（本机直连 raw.githubusercontent 不通）
RECIPES_REMOTE_URL=https://ghproxy.net/https://raw.githubusercontent.com/liang2005-lzx/FridgeRecipeManager/master/server/recipes.json
```

在线菜谱的源文件在仓库根目录 `server/recipes.json`，推送到 GitHub 对应分支后，
App 里点「☁ 同步云端」即可拉到；不推送也不影响使用（会提示「离线模式」并继续用内置菜谱）。

> 本机 `gradlew` 使用 JDK 23 的 Gradle daemon；AGP 9 内置 Kotlin 支持，KSP 需要
> `gradle.properties` 里的 `android.disallowKotlinSourceSets=false`，否则配置阶段报错。

单元测试（两个算法模块）：`app/src/test/java/com/liang/fridgerecipe/AlgorithmTest.kt`，6 个用例。
本机 `:app:testDebugUnitTest` 会因 Gradle 测试进程环境问题（`GradleWorkerMain` 找不到）失败，
可直接用 JUnitCore 跑测试类验证。

## 六、已知限制

- 未做真机 / 模拟器运行验证（本机无 AVD、adb 无设备），当前仅验证到「构建成功 + 安装包元数据正确 + 算法单测通过」。
- 提醒为「每天固定时间检查一次」的周期任务，系统省电策略下可能有几分钟级延迟。
- 菜谱除了内置 12 道，也可在线同步 / AI 生成，但界面暂不支持手动新建菜谱。
- AI 生成依赖 DeepSeek 的可用 Key 与网络；Key 失效时只提示错误，不影响本地功能。
