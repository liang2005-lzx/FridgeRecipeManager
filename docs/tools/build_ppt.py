"""生成《冰箱菜谱》答辩 PPT（12 页，16:9）。依赖 python-pptx。"""
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.text import MSO_AUTO_SIZE

# ---------- 配色 ----------
DARK = RGBColor(0x1F, 0x3A, 0x2E)
GREEN = RGBColor(0x2E, 0x6B, 0x4F)
LIGHT = RGBColor(0xF7, 0xF9, 0xF7)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
GRAY = RGBColor(0x5B, 0x66, 0x60)
AMBER = RGBColor(0xC9, 0x8A, 0x3D)
RED = RGBColor(0xB5, 0x4A, 0x3C)
CARD = RGBColor(0xEC, 0xF2, 0xEC)
LINE = RGBColor(0xD5, 0xDE, 0xD6)

FONT = "微软雅黑"
FONT_B = "微软雅黑"

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
BLANK = prs.slide_layouts[6]


def slide():
    s = prs.slides.add_slide(BLANK)
    bg = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, prs.slide_width, prs.slide_height)
    bg.fill.solid()
    bg.fill.fore_color.rgb = LIGHT
    bg.line.fill.background()
    bg.shadow.inherit = False
    return s


def text(s, x, y, w, h, content, size=16, color=GRAY, bold=False, align=PP_ALIGN.LEFT,
         anchor=MSO_ANCHOR.TOP, spacing=1.0, font=FONT):
    tb = s.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.auto_size = MSO_AUTO_SIZE.NONE
    tf.vertical_anchor = anchor
    lines = content.split("\n")
    for i, line in enumerate(lines):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        p.line_spacing = spacing
        r = p.add_run()
        r.text = line
        r.font.size = Pt(size)
        r.font.color.rgb = color
        r.font.bold = bold
        r.font.name = font
    return tb


def rect(s, x, y, w, h, fill=CARD, line_color=LINE, radius=True, line_w=1.0):
    shape = s.shapes.add_shape(
        MSO_SHAPE.ROUNDED_RECTANGLE if radius else MSO_SHAPE.RECTANGLE,
        Inches(x), Inches(y), Inches(w), Inches(h)
    )
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill
    shape.line.color.rgb = line_color
    shape.line.width = Pt(line_w)
    shape.shadow.inherit = False
    if radius:
        shape.adjustments[0] = 0.06
    return shape


def band(s, x, y, w, h, label, sub, fill=WHITE, label_color=DARK):
    rect(s, x, y, w, h, fill=fill)
    text(s, x + 0.25, y + 0.12, w - 0.5, 0.35, label, 15, label_color, True)
    if sub:
        text(s, x + 0.25, y + 0.45, w - 0.5, h - 0.55, sub, 11, GRAY)


def header(s, title, kicker=None, page=None):
    bar = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.6), Inches(0.55), Inches(0.09), Inches(0.62))
    bar.fill.solid(); bar.fill.fore_color.rgb = GREEN; bar.line.fill.background(); bar.shadow.inherit = False
    text(s, 0.85, 0.45, 10.6, 0.6, title, 28, DARK, True)
    if kicker:
        text(s, 0.87, 1.02, 11.4, 0.35, kicker, 12, GRAY)
    ln = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(0.6), Inches(1.42), Inches(12.1), Inches(0.015))
    ln.fill.solid(); ln.fill.fore_color.rgb = LINE; ln.line.fill.background(); ln.shadow.inherit = False
    if page is not None:
        text(s, 12.1, 7.0, 0.7, 0.3, f"{page:02d}", 11, GRAY, False, PP_ALIGN.RIGHT)


# ========== 1 封面 ==========
s = prs.slides.add_slide(BLANK)
bg = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, 0, prs.slide_width, prs.slide_height)
bg.fill.solid(); bg.fill.fore_color.rgb = DARK; bg.line.fill.background(); bg.shadow.inherit = False
deco = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, 0, Inches(4.9), prs.slide_width, Inches(0.06))
deco.fill.solid(); deco.fill.fore_color.rgb = AMBER; deco.line.fill.background(); deco.shadow.inherit = False
text(s, 1.0, 1.55, 9.5, 0.5, "ANDROID 课程设计 · 主 App 答辩", 14, RGBColor(0xB9, 0xC9, 0xBE), True)
text(s, 1.0, 2.05, 11.0, 1.0, "冰箱菜谱", 54, WHITE, True)
text(s, 1.0, 3.05, 11.0, 0.6, "把冰箱库存、临期提醒和菜谱推荐放在一起", 20, RGBColor(0xD6, 0xE2, 0xD8))
text(s, 1.0, 3.8, 11.4, 0.6, "选题 B6（菜谱 / 冰箱管理）· 档位 B · 伴生模块 P2（数据看板）", 15, RGBColor(0xB9, 0xC9, 0xBE))
text(s, 1.0, 5.15, 6.0, 0.9, "梁朝霞　选题登记序号 63\n浙江农林大学 · 物联网专业", 15, WHITE, False, spacing=1.3)
text(s, 9.4, 5.15, 3.4, 0.9, "2026 年 11 月\nKotlin · Room · WorkManager · OkHttp", 12, RGBColor(0xB9, 0xC9, 0xBE), False, PP_ALIGN.RIGHT, spacing=1.3)

# ========== 2 选题背景 ==========
s = slide()
header(s, "为什么做这个 App", "从“买了忘记吃”到“今天做什么”，一个 App 解决两头的问题", 2)
cards = [
    ("痛点①　买了忘记吃", "食材没有集中台账，冷藏室深处的东西\n往往到发霉才被发现。"),
    ("痛点②　过期的全扔掉", "缺少临期提醒，一周里最该先吃的东西\n反而最后才被想起。"),
    ("痛点③　菜谱和库存脱节", "菜谱 App 只教“做什么菜”，不告诉你\n现有食材能做哪道。"),
]
for i, (t, d) in enumerate(cards):
    x = 0.6 + i * 4.2
    rect(s, x, 1.75, 3.9, 2.0)
    text(s, x + 0.3, 1.9, 3.3, 0.4, t, 15, DARK, True)
    text(s, x + 0.3, 2.4, 3.3, 1.2, d, 11.5, GRAY, spacing=1.25)
rect(s, 0.6, 4.15, 12.1, 2.35, fill=WHITE)
text(s, 0.95, 4.35, 11.6, 0.4, "产品定位：一个以“库存”为核心的家常菜助手", 16, GREEN, True)
for i, line in enumerate([
    "库存为真源：所有菜谱推荐、提醒、统计都从同一份食材数据算出来，不做第二套账。",
    "算法不靠云：临期判断与匹配打分都是本地纯函数，断网照样能用、答辩可现场改参数。",
    "联网做增强：在线拉取菜谱库、AI 按现有食材生成菜谱，失败只降级不阻塞。",
]):
    text(s, 1.0, 4.85 + i * 0.48, 11.3, 0.4, "· " + line, 12.5, GRAY)

# ========== 3 功能地图 ==========
s = slide()
header(s, "功能地图：必做 + 自选 + 伴生 + 加分", "B6 的四块要求全部落地，共 11 个功能点、7 个界面", 3)
quads = [
    ("必做　库存与提醒", GREEN, ["食材 CRUD：分类 / 数量单位 / 购买与到期日 / 备注", "列表搜索 + 四档筛选 + 三态与空态", "WorkManager 每日检查，2 天内到期汇总通知", "提醒时间可调、通知权限可降级"]),
    ("自选　智能推荐", DARK, ["覆盖率 + 临期 + 缺料 + 快手 四因子打分", "名称归一化 + 双向包含模糊匹配", "逐条用料比对：有 / 临期 / 已过期 / 缺", "“只看能做的”一键过滤"]),
    ("伴生 P2　数据看板", AMBER, ["总数 / 临期 / 浪费率三张统计卡", "Canvas 手绘品类柱状图（零第三方库）", "最该先吃的 Top 5", "导出 CSV 与 PNG，走系统分享"]),
    ("加分　联网", RED, ["云端菜谱同步：OkHttp 拉 JSON 覆盖本地库", "AI 生成菜谱：DeepSeek 按库存出结构化菜谱", "Key 走 local.properties 注入，不硬编码", "断网 / 无 Key 全部降级为本地数据"]),
]
for i, (t, color, items) in enumerate(quads):
    x = 0.6 + (i % 2) * 6.15
    y = 1.75 + (i // 2) * 2.65
    rect(s, x, y, 5.95, 2.45, fill=WHITE)
    tag = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(x + 0.25), Inches(y + 0.22), Inches(2.55), Inches(0.36))
    tag.fill.solid(); tag.fill.fore_color.rgb = color; tag.line.fill.background(); tag.shadow.inherit = False
    tf = tag.text_frame; tf.word_wrap = False
    p = tf.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
    r = p.add_run(); r.text = t; r.font.size = Pt(12); r.font.bold = True; r.font.color.rgb = WHITE; r.font.name = FONT
    body = "\n".join("· " + it for it in items)
    text(s, x + 0.3, y + 0.75, 5.5, 1.6, body, 11, GRAY, spacing=1.35)

# ========== 4 架构 ==========
s = slide()
header(s, "四层架构：UI 只订阅状态，数据只有一个入口", "单 Activity + Navigation + MVVM + Repository，算法与框架解耦", 4)
layers = [
    ("UI 层", "7 个 Fragment + MainActivity：ViewBinding 绑定，只做渲染与事件转发", RGBColor(0xDD, 0xEA, 0xDF)),
    ("ViewModel 层", "9 个 AndroidViewModel：StateFlow 持 UiState，combine 聚合数据流", RGBColor(0xCF, 0xE0, 0xD4)),
    ("领域层", "ExpiryAnalyzer / RecipeRecommender：纯函数业务规则，可在 JVM 单测", RGBColor(0xBF, 0xD4, 0xC6)),
    ("数据层", "FridgeRepository（唯一入口）+ Room 三张表 + OkHttp 远程菜谱与 AI", RGBColor(0xAE, 0xC8, 0xB7)),
]
for i, (t, d, c) in enumerate(layers):
    y = 1.75 + i * 1.2
    rect(s, 0.6, y, 8.4, 1.05, fill=c, line_color=RGBColor(0x9F, 0xBA, 0xA8))
    text(s, 0.85, y + 0.12, 2.2, 0.35, t, 14, DARK, True)
    text(s, 0.85, y + 0.5, 7.9, 0.45, d, 10.5, GRAY)

notes = [
    ("不碰 DAO", "Fragment 只订阅 StateFlow，换数据源不动界面。"),
    ("生命周期安全", "协程跑在 viewModelScope，收集用 repeatOnLifecycle(STARTED)。"),
    ("算法可测", "两个领域类不依赖 Android，6 个单测直接跑。"),
    ("可替换数据源", "联网与 AI 都复用 Repository，失败不影响本地。"),
]
rect(s, 9.25, 1.75, 3.45, 4.75, fill=WHITE)
text(s, 9.5, 1.9, 3.0, 0.35, "设计取舍", 14, GREEN, True)
for i, (t, d) in enumerate(notes):
    text(s, 9.5, 2.4 + i * 1.1, 3.0, 0.3, "· " + t, 11.5, DARK, True)
    text(s, 9.55, 2.7 + i * 1.1, 2.95, 0.7, d, 10, GRAY, spacing=1.15)

# ========== 5 数据库 ==========
s = slide()
header(s, "数据设计：三张表，一条关系链", "Room 2.8.5 + KSP，外键级联 + 索引，查询全部返回 Flow", 5)
tables = [
    ("ingredients 食材", 0.6, ["id  PK 自增", "name / category", "quantity / unit", "addedAt / expiresAt", "note"]),
    ("recipes 菜谱", 4.7, ["id  PK 自增", "name / emoji / category", "minutes / difficulty", "stepsJson（Gson 序列化）", "tips"]),
    ("recipe_ingredients 用料", 8.8, ["id  PK 自增", "recipeId  FK → recipes.id", "name / amount", "optional（可选料）", "ON DELETE CASCADE"]),
]
for t, x, rows in tables:
    rect(s, x, 1.75, 3.75, 2.85, fill=WHITE)
    text(s, x + 0.25, 1.9, 3.3, 0.35, t, 13.5, DARK, True)
    text(s, x + 0.25, 2.35, 3.3, 2.1, "\n".join("· " + r for r in rows), 10.5, GRAY, spacing=1.35)
text(s, 1.9, 4.75, 2.6, 0.3, "1 条菜谱 → N 条用料", 11, GREEN, True, PP_ALIGN.CENTER)
text(s, 6.05, 4.75, 2.4, 0.3, "外键级联删除", 11, GREEN, True, PP_ALIGN.CENTER)
rect(s, 0.6, 5.2, 12.1, 1.5, fill=WHITE)
text(s, 0.9, 5.35, 11.6, 1.2,
     "· 一对多用 @Relation 一次取出「菜谱 + 全部用料」，避免 N+1 查询\n"
     "· 所有 List 查询返回 Flow，数据一变 UI 自动刷新，不需要手动通知\n"
     "· 种子数据 12 道菜来自 assets/recipes.json，首次启动写入；云端与 AI 结果复用同一套写入逻辑",
     11.5, GRAY, spacing=1.35)

# ========== 6 算法一 ==========
s = slide()
header(s, "算法①：临期分析（自定义业务逻辑）", "不是简单比大小：时间归零、四档状态、风险分、统计聚合", 6)
rect(s, 0.6, 1.7, 6.0, 2.2, fill=WHITE)
text(s, 0.9, 1.85, 5.4, 0.35, "剩余天数：先归零到当天 0 点", 14, GREEN, True)
text(s, 0.9, 2.35, 5.4, 0.8, "daysLeft =\n⌊(startOfDay(到期) − startOfDay(现在)) / 86400000⌋", 13, DARK, True, spacing=1.3)
text(s, 0.9, 3.25, 5.4, 0.55, "目的：保证“今天到期”永远是 0 天，不会因录入时刻不同变成 0.9 天。", 10.5, GRAY)
rect(s, 0.6, 4.05, 6.0, 1.55, fill=WHITE)
text(s, 0.9, 4.2, 5.4, 0.3, "风险分（0–100）", 13, GREEN, True)
text(s, 0.9, 4.6, 5.4, 0.7, "risk = clamp(100 − daysLeft × 12, 0, 100)\n过期项直接记 100", 12.5, DARK, True, spacing=1.3)
text(s, 0.9, 5.25, 5.4, 0.3, "系数 12 ≈ 一周衰减完，正好覆盖关注窗口", 10, GRAY)
states = [
    ("已过期", "daysLeft < 0", RED),
    ("今明到期", "0 ≤ daysLeft ≤ 2", AMBER),
    ("快到期", "3 ≤ daysLeft ≤ 5", RGBColor(0xB8, 0x8A, 0x2E)),
    ("新鲜", "daysLeft > 5", GREEN),
]
text(s, 7.0, 1.75, 5.6, 0.35, "四档状态与排序", 14, GREEN, True)
for i, (name, cond, color) in enumerate(states):
    y = 2.2 + i * 0.95
    rect(s, 7.0, y, 5.65, 0.8, fill=WHITE)
    dot = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(7.2), Inches(y + 0.2), Inches(0.12), Inches(0.4))
    dot.fill.solid(); dot.fill.fore_color.rgb = color; dot.line.fill.background(); dot.shadow.inherit = False
    text(s, 7.45, y + 0.1, 1.9, 0.3, name, 12.5, DARK, True)
    text(s, 7.45, y + 0.42, 4.8, 0.3, cond, 10, GRAY)
text(s, 7.0, 6.05, 5.65, 0.6, "排序：状态优先级 → 剩余天数升序；看板再用同一份数据算浪费率与前 5 条。", 10, GRAY)

# ========== 7 算法二 ==========
s = slide()
header(s, "算法②：菜谱匹配打分（为什么这道菜排第一）", "覆盖率 50 + 临期 20 + 缺料 15 + 快手 10，四因子可解释", 7)
weights = [
    ("覆盖率", 50, "现有食材能覆盖的必需用料越多越高"),
    ("临期消耗", 20, "每命中一样临期食材 +20，优先吃掉快坏的"),
    ("缺料惩罚", 15, "缺 1 样扣 8，缺 2 样归零，避免推荐要买菜的"),
    ("快手加分", 10, "≤15 分钟 +10，≤30 分钟 +6，其它 +3"),
]
for i, (name, val, desc) in enumerate(weights):
    y = 1.7 + i * 1.08
    text(s, 0.65, y + 0.05, 1.5, 0.3, name, 12.5, DARK, True)
    bar_w = 0.35 + val / 50 * 4.2
    rect(s, 2.25, y + 0.08, bar_w, 0.42, fill=RGBColor(0xCF, 0xDF, 0xD2), line_color=RGBColor(0xBF, 0xD1, 0xC4))
    text(s, 2.4, y + 0.12, 0.9, 0.3, f"{val} 分", 10.5, DARK, True)
    text(s, 7.0, y + 0.05, 5.6, 0.7, desc, 10, GRAY)
rect(s, 0.6, 5.85, 12.1, 1.0, fill=WHITE)
text(s, 0.85, 5.95, 11.7, 0.85,
     "示例：番茄（临期 1 天）+ 鸡蛋 →「番茄炒蛋」覆盖率 100%（50）+ 临期 1 样（20）+ 无缺料（15）+ 10 分钟（10）= 95 分，可直接做\n"
     "名称归一化（去空格括号）+ 双向 contains：番茄能匹配“小番茄”，鸡蛋能匹配“土鸡蛋”，规则可现场演示。",
     10, GREEN, False, spacing=1.2)

# ========== 8 界面 ==========
s = slide()
header(s, "7 个界面：列表 → 详情 → 编辑闭环", "底部 4 个 Tab，单 Activity 单栈 Navigation，参数用 bundle 传 id", 8)
screens = [
    ("食材列表", "搜索 · 四档筛选 · FAB\n权限降级条 · 三态重试", 0),
    ("食材详情", "风险分进度条 · 备注\n相关菜谱 3 条 · 删除确认", 1),
    ("添加 / 编辑", "分类与单位下拉\n日期选择 · 3/7/30/90 天", 2),
    ("菜谱列表", "匹配度排序 · 只看能做的\n云同步 / AI 入口", 3),
    ("菜谱详情", "逐条用料徽标\n步骤 · 贴士 · 得分理由", 4),
    ("数据看板", "3 张统计卡 · Canvas 图表\nTop5 · 导出按钮", 5),
    ("设置", "提醒开关与时间\n权限状态 · 数据操作", 6),
    ("共用规范", "状态色 / 空态文案统一\n返回键与参数校验", 7),
]
for i, (t, d, idx) in enumerate(screens):
    col = idx % 4
    row = idx // 4
    x = 0.6 + col * 3.12
    y = 1.7 + row * 1.85
    rect(s, x, y, 2.9, 1.62, fill=WHITE)
    text(s, x + 0.2, y + 0.13, 2.5, 0.3, t, 12, DARK, True)
    text(s, x + 0.2, y + 0.62, 2.5, 0.9, d, 9.5, GRAY, spacing=1.3)
rect(s, 0.6, 5.6, 12.1, 0.95, fill=WHITE)
text(s, 0.85, 5.7, 11.7, 0.8,
     "三态齐全：Loading / Content / Error 均有独立视图，错误带重试；空态区分“还没数据”和“筛选后为空”。权限被拒只在列表页加提示条，不挡功能。",
     10, GRAY, spacing=1.2)

# ========== 9 联网 ==========
s = slide()
header(s, "联网：在线菜谱同步 + AI 生成（两条都做）", "硬能力加分项，失败一律降级为本地 12 道内置菜谱", 9)
blocks = [
    ("① 云端菜谱同步", GREEN, ["点「☁ 同步云端」→ OkHttp GET 远程 recipes.json", "校验三层：HTTP 状态码 / 响应非空 / JSON 可解析", "replaceAllRecipes() 只换菜谱表，食材库存不动", "成功后 Flow 推送，列表自动刷新"]),
    ("② AI 生成菜谱", RED, ["取库存前 12 条拼提示词，要求只输出 JSON", "POST api.deepseek.com/chat/completions", "清洗 Markdown 代码块包裹 + 校验菜名与步骤", "弹窗预览 →「保存到我的菜谱」写入 Room"]),
]
for i, (t, color, items) in enumerate(blocks):
    x = 0.6 + i * 6.2
    rect(s, x, 1.7, 5.95, 3.4, fill=WHITE)
    tag = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(x + 0.25), Inches(1.88), Inches(2.9), Inches(0.38))
    tag.fill.solid(); tag.fill.fore_color.rgb = color; tag.line.fill.background(); tag.shadow.inherit = False
    tf = tag.text_frame; p = tf.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
    r = p.add_run(); r.text = t; r.font.size = Pt(12.5); r.font.bold = True
    r.font.color.rgb = WHITE; r.font.name = FONT
    for j, it in enumerate(items):
        text(s, x + 0.3, 2.4 + j * 0.62, 5.45, 0.55, f"{j + 1}. {it}", 10, GRAY, spacing=1.2)
rect(s, 0.6, 5.25, 12.1, 1.6, fill=WHITE)
text(s, 0.9, 5.38, 11.6, 1.35,
     "降级设计：网络异常 / 4xx / 数据为空 → 状态行显示「离线模式：原因」，按钮变「重试」，本地菜谱照常可用；\n"
     "AI 未配 Key 或 401 → 只弹提示，不影响其它页面。Key 存在 local.properties，经 buildConfigField 注入，Git 历史无明文。",
     10.5, GRAY, spacing=1.3)

# ========== 10 后台 + 导出 ==========
s = slide()
header(s, "两块硬骨头：后台提醒 与 文件导出", "WorkManager 周期任务 + FileProvider 分享，均不依赖额外权限", 10)
rect(s, 0.6, 1.7, 5.95, 3.3, fill=WHITE)
text(s, 0.9, 1.85, 5.4, 0.35, "过期提醒（后台硬能力）", 13.5, GREEN, True)
for j, it in enumerate([
    "PeriodicWorkRequest 1 天一次，unique name expiry_daily_check",
    "initialDelay 按用户设定时分算到下一个整点",
    "CoroutineWorker 筛 daysLeft ≤ 2，汇总成一条通知",
    "channel_expiry + BigTextStyle + 点击进 App",
    "13+ 未授权只记日志，不抛异常",
]):
    text(s, 0.9, 2.3 + j * 0.5, 5.4, 0.45, "· " + it, 10, GRAY, spacing=1.15)
rect(s, 6.75, 1.7, 5.95, 3.3, fill=WHITE)
text(s, 7.0, 1.85, 5.4, 0.35, "看板导出（P2 文件导出）", 13.5, AMBER, True)
for j, it in enumerate([
    "CSV：统计 / 分类 / 优先清单三段，UTF-8 BOM 防乱码",
    "PNG：View.draw 抓 Bitmap，不需要截图权限",
    "FileProvider + ACTION_SEND 走系统分享面板",
    "落在 getExternalFilesDir，无需存储权限",
]):
    text(s, 7.0, 2.3 + j * 0.55, 5.4, 0.5, "· " + it, 10, GRAY, spacing=1.15)
rect(s, 0.6, 5.25, 12.1, 1.35, fill=WHITE)
text(s, 0.9, 5.4, 11.6, 1.1,
     "统计口径统一：看板与提醒都调用 ExpiryAnalyzer，避免“通知说 2 天、看板显示 3 天”的口径不一致。\n"
     "导出与图表共用 CategoryBarChartView，导出的图就是用户看到的图。", 10.5, GRAY, spacing=1.25)

# ========== 11 测试 ==========
s = slide()
header(s, "验证：能跑、能算、有据可查", "算法单测 + 构建校验 + 安装包元数据，未验证项如实列出", 11)
cols = [
    ("算法单测", GREEN, "JUnitCore 直跑\n\nOK (6 tests)\n\n覆盖：天数归零、四档状态、风险分单调、推荐打分、过期料处理", "6/6 通过"),
    ("构建与包校验", DARK, "assembleDebug 成功\n\naapt2 dump badging\n\n包名 com.liang.fridgerecipe，min 24 / target 37，含 INTERNET 与通知权限", "BUILD SUCCESSFUL"),
    ("尚未验证（如实）", RED, "本机无 AVD、adb 无设备\n\n界面点击、通知弹出、分享面板均未实际运行\n\n结论：仅验证到编译 + 元数据 + 算法", "待真机"),
]
for i, (t, color, body, badge) in enumerate(cols):
    x = 0.6 + i * 4.15
    rect(s, x, 1.7, 3.95, 3.7, fill=WHITE)
    text(s, x + 0.25, 1.85, 3.4, 0.35, t, 13.5, color, True)
    text(s, x + 0.25, 2.35, 3.45, 2.2, body, 10, GRAY, spacing=1.25)
    tag = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(x + 0.25), Inches(4.55), Inches(1.85), Inches(0.38))
    tag.fill.solid(); tag.fill.fore_color.rgb = color; tag.line.fill.background(); tag.shadow.inherit = False
    p = tag.text_frame.paragraphs[0]; p.alignment = PP_ALIGN.CENTER
    r = p.add_run(); r.text = badge; r.font.size = Pt(10.5); r.font.bold = True
    r.font.color.rgb = WHITE; r.font.name = FONT
rect(s, 0.6, 5.6, 12.1, 1.0, fill=WHITE)
text(s, 0.9, 5.7, 11.6, 0.8,
     "为什么直跑单测：本机 :app:testDebugUnitTest 因 Gradle 测试进程环境问题报 GradleWorkerMain 缺失，改用 JUnitCore + classpath 执行，用例与断言完全相同。",
     9.5, GRAY, spacing=1.2)

# ========== 12 收尾 ==========
s = slide()
header(s, "完成度与下一步", "17 次提交已推送到 GitHub，交付物还差 PPT 与录屏", 12)
rect(s, 0.6, 1.7, 5.4, 3.4, fill=WHITE)
text(s, 0.85, 1.85, 4.9, 0.35, "Git 提交（17 次）", 13.5, GREEN, True)
for j, it in enumerate([
    "1–4　构建配置 / 骨架 / 数据层 / 算法①",
    "5–8　推荐算法 + 单测 / 食材页 / 菜谱页（含联网）",
    "9–11　后台提醒 / 看板设置 / 文档",
    "12–16　补文档 / P2 导出 / 联网数据源",
]):
    text(s, 0.85, 2.35 + j * 0.55, 4.9, 0.5, "· " + it, 10, GRAY, spacing=1.15)
text(s, 0.85, 4.5, 4.9, 0.5, "40 个 Kotlin 文件 · 3073 行 · 14 个布局", 10.5, DARK, True)
rect(s, 6.2, 1.7, 6.5, 1.95, fill=WHITE)
text(s, 6.45, 1.83, 6.0, 0.35, "已交付", 13.5, GREEN, True)
text(s, 6.45, 2.3, 6.0, 1.3,
     "✅ 源码 + 17 次提交 + GitHub 推送\n✅ 开发文档（Word / Markdown）\n✅ 可安装 APK（含联网与导出）",
     10.5, GRAY, spacing=1.3)
rect(s, 6.2, 3.8, 6.5, 1.65, fill=WHITE)
text(s, 6.45, 3.93, 6.0, 0.35, "还差", 13.5, RED, True)
text(s, 6.45, 4.38, 6.0, 1.05,
     "· 真机自测（通知 / 分享 / 权限弹窗）\n· 3 分钟带学号姓名水印录屏\n· 答辩代码问答准备",
     10.5, GRAY, spacing=1.3)
text(s, 0.6, 5.65, 12.1, 0.8,
     "如果现场只能演示一分钟：添加两样食材 → 看板看临期 → 菜谱页「只看能做的」→ AI 生成一道菜 → 导出 CSV。",
     12, GREEN, True, PP_ALIGN.CENTER, spacing=1.3)

prs.save(r"D:\recyclerviewtest\docs\冰箱菜谱-答辩PPT.pptx")
print("saved: docs/冰箱菜谱-答辩PPT.pptx，共", len(prs.slides.__iter__.__self__._sldIdLst), "页")
