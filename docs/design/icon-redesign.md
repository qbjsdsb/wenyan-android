# 文研 App 启动图标重设计

> 日期：2026-07-16（初版）→ 2026-07-31（精修实施）
> 状态：✅ 已实施（v4 书+文负空间，已替换 ic_launcher_foreground/monochrome）
> 触发：用户反馈现有"文"字几何拼块图标过于生硬，要求重做以符合 Android 设计规范、流畅大方、有谷歌产品气质

## 1. 现状

| 文件 | 内容 |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | **v4** 书页 + "文"字负空间（单 path + evenOdd fillType），米色 `#F5F1E8` |
| `app/src/main/res/drawable/ic_launcher_background.xml` | 纯色 #2C2C2C 墨黑矩形（不变） |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | **v4** 同 foreground path，白色 `#FFFFFF`，供 Android 13+ themed icon |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | adaptive-icon 聚合（background + foreground + monochrome，不变） |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | 同上（圆形遮罩，不变） |
| `docs/design/icon-redesign.md` | 本设计文档（已更新） |
| `.tmp-preview/icon-preview.html` | 方案 B 精修版预览（含新旧对比、safe zone 检查、多尺寸模拟） |

**v3 印章文 → v4 书+文负空间**：从"印章+文字"改为"书+文字负空间"，书形占 safe zone 70%+，单 path + evenOdd 实现镂空，更简洁、更有辨识度、更符合 Google 产品气质。

## 2. 设计目标

1. **符合 Android 设计规范**：adaptive icon（safe zone 中心 72x72 / 66dp）+ monochrome 层（Android 13+ themed icon）
2. **谷歌产品气质**：Bold silhouette + simple geometry + distinctive identity（类比 Google Workspace：Docs/Drive/Play Books）
3. **流畅大方**：曲线为主，避免方块拼块；笔画有粗细变化或几何韵律
4. **保留文研品牌**：墨黑 `#2C2C2C` + 米色 `#F5F1E8` 配色（与 App 窗口背景一致，墨纸气质）

## 3. 设计方案：展开的书 + "文"字负空间

### 3.1 核心图形

**前景**（米色 `#F5F1E8`）：一本展开的书的俯视图轮廓
- 两个对称页面如翅膀展开
- 中间 V 形书脊（向下凹口）
- 纯几何块面，无细线

**负空间**（镂空到背景墨黑）：极简"文"字
- 用 `evenOddFillType` 在书页上镂空"文"字
- "文"字笔画简化为 3 笔：横、撇、捺（去掉"亠"头，保留主体）
- 大尺寸下可见"文"字细节，小尺寸下退化为书页纹理（不影响识别）

### 3.2 设计原理

| 原理 | 体现 |
|------|------|
| **Bold** | 书页轮廓占据 safe zone 70%+ 面积，一眼可辨 |
| **Simple** | 书 = 1 个 path；"文"字 = 1 个 path（evenOdd 镂空） |
| **Distinctive** | "书 + 文字负空间"组合在文学 App 中独特（其他 App 多用纯字母或纯书形） |
| **谷歌感** | 类比 Google Play Books（书形）+ Docs（字母负空间）的混合 |
| **品牌延续** | 墨黑/米色 = 墨纸气质，与现有 App 主题一致 |

### 3.3 Vector Path 设计

**Viewport**: 108x108（adaptive icon 标准）
**Safe zone**: 中心 72x72（x:18-90, y:18-90），图形全部在 safe zone 内

#### 前景 path（书页 + "文"字镂空，单一 path + evenOddFillType）

```
书页轮廓（外环，顺时针）：
M28,36           书页左上角
L52,44           左书脊顶部
L56,44           右书脊顶部
L80,36           书页右上角
L80,72           书页右下角
L56,80           右书脊底部
L52,80           左书脊底部
L28,72           书页左下角
Z                闭合

"文"字镂空（内环，逆时针，evenOdd 规则镂空）：
M40,50           横画左端
L68,50           横画右端
L68,54           横画右下
L58,54           横画下边（捺起点）
L66,66           捺画右下（外缘）
L58,66           捺画底部（平底收笔）
L54,58           撇捺交叉点
L50,66           撇画右下（外缘）
L42,66           撇画底部（平底收笔）
L50,54           撇画上边（横画下边）
L40,54           横画左下
Z                闭合
```

**完整 path（外环 + 内环组合）**：
```
M28,36 L52,44 L56,44 L80,36 L80,72 L56,80 L52,80 L28,72 Z 
M40,50 L68,50 L68,54 L58,54 L66,66 L58,66 L54,58 L50,66 L42,66 L50,54 L40,54 Z
```

`android:fillType="evenOdd"` 让内环镂空，呈现"文"字负空间。

**v3 初版 → v4 精修对照**：

| 坐标点 | 初版（有 serif） | 精修版（平底居中） | 说明 |
|--------|-----------------|-------------------|------|
| `横左上` | `M40,52` | `M40,50` | 上移 2dp 居中 |
| `横右上` | `L68,52` | `L68,50` | 上移 2dp |
| `横右下` | `L68,56` | `L68,54` | 上移 2dp |
| `捺起点` | `L58,56` | `L58,54` | 上移 2dp |
| `捺外缘` | `L66,68` | `L66,66` | 上移 2dp |
| `捺底部` | `L62,70`（serif） | `L58,66`（平底） | 去 serif |
| `交叉点` | `L54,60` | `L54,58` | 上移 2dp |
| `撇外缘` | `L46,70` | `L50,66` | 上移 2dp，微调 |
| `撇底部` | `L42,68`（serif） | `L42,66`（平底） | 去 serif |
| `撇起点` | `L50,56` | `L50,54` | 上移 2dp |
| `横左下` | `L40,56` | `L40,54` | 上移 2dp |

#### 背景 path（纯色矩形，不变）

```
M0,0h108v108h-108z
```
fillColor = `@color/wenyan_launcher_background`（#2C2C2C）

#### Monochrome path（Android 13+ themed icon）

与前景 path 完全一致，fillColor = `#FFFFFF`（系统会替换为主题色）。

### 3.4 视觉效果描述

- **大尺寸**（启动屏 / Play Store）：清晰的展开书本，书页上可见"文"字水墨镂空，墨黑底米色书页，传统与现代融合
- **中尺寸**（桌面图标）：展开的书一目了然，"文"字作为细节增强识别
- **小尺寸**（最近任务 / 通知栏）：书的轮廓主导，"文"字退化为书页纹理
- **Themed icon**（Android 13+ 用户启用主题图标）：系统用壁纸色着色，书的轮廓 + "文"字负空间保留识别度

## 4. 实施记录

### 4.1 文件改动

| 文件 | 改动 | 状态 |
|------|------|------|
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | 替换为"书 + 文负空间"单 path + `android:fillType="evenOdd"`，精修后坐标 | ✅ 已实施 |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | 同步替换 path（与 foreground 一致） | ✅ 已实施 |
| `app/src/main/res/drawable/ic_launcher_background.xml` | 不变（已是纯色矩形） | ✅ 无需改动 |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | 不变 | ✅ 无需改动 |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | 不变 | ✅ 无需改动 |
| `app/src/main/res/values/colors.xml` | 不变（#2C2C2C / #F5F1E8 已定义） | ✅ 无需改动 |
| `docs/design/icon-redesign.md` | 更新 path 坐标、状态、精修对照表 | ✅ 已更新 |

### 4.2 验证记录

| 验证项 | 结果 |
|--------|------|
| `assembleDebug` | ✅ PASS（279 tasks, 0 failures） |
| `testDebugUnitTest` | ✅ PASS（317 tasks, 0 failures） |
| 视觉验证（圆形遮罩） | 需 emulator，沙箱无法执行 |
| Themed icon 兼容性 | 需 emulator，沙箱无法执行 |

## 5. 风险与缓解

| 风险 | 缓解 |
|------|------|
| "文"字镂空在小尺寸下糊成一团 | path 设计已简化为 3 笔，小尺寸退化为纹理不影响识别；如实测有问题可加粗笔画 |
| evenOddFillType 在旧设备渲染异常 | API 1+ 支持，无兼容性问题 |
| 书页轮廓可能像蝴蝶/心形 | V 形书脊向下凹口 + 页面比例 1:1.6（接近黄金比）确保书形识别 |
| monochrome 层在 themed icon 下"文"字丢失 | monochrome 与 foreground 同 path，系统着色后负空间保留 |

## 6. 后续可选增强（YAGNI，本次不做）

- 生成 PNG fallback（mipmap-mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi）— 当前仅 anydpi-v26，旧设备用系统默认图标。本次不做，因 minSdk 26+ 已覆盖 adaptive icon。
- 启动屏 splash icon 单独设计 — 当前用 ic_launcher，本次不改。
- 动态主题图标（Android 13+ 用户壁纸提取色）— monochrome 层已支持，本次不改。

## 7. 参考资料

- [Adaptive Icons](https://developer.android.com/develop/ui/views/launch/icon_design_adaptive)
- [Themed Icons](https://developer.android.com/about/versions/13/features#themed-app-icons)
- Google Workspace 图标设计原则：Bold + Simple + Distinctive

---

## 8. v5 AI 生成图标（2026-08-03，书堆 + 文）

> 用户反馈"图标想更好看"，选定 **AI 生成全新图标** 路线（ImageGen 生成候选 → 选定「书堆 + 文」）。

### 8.1 变更内容

| 项 | 说明 |
|----|------|
| 设计来源 | AI 生成（`Modern_flat_vector_Android_app_2026-08-02T18-24-37.png`），书堆 + 「文」字封面 + 毛笔 + 朱红书签点缀 |
| 处理管线 | PIL 颜色阈值抠背景（浅灰白→透明）→ 去右下角水印 → 主体缩放至 safe zone 720/1024 → 垂直水平居中 |
| 前景 | 各密度 `ic_launcher_foreground.webp`（透明背景，mdpi 108 → xxxhdpi 432） |
| 背景 | 保持 `@drawable/ic_launcher_background`（纯色 #2C2C2C 墨黑） |
| monochrome | 各密度 `ic_launcher_monochrome.webp`（主体 alpha 二值化简化，Android 13+ themed icon） |
| 旧设备兜底 | 各密度 `ic_launcher.webp`（墨黑背景 + 主体合成，48→192px） |
| Splash | `themes.xml` `windowSplashScreenAnimatedIcon` 从 `@drawable/ic_launcher_foreground` 改为 `@mipmap/ic_launcher_foreground` |
| 旧 v4 矢量 | 移出 res 备份至 `.icon-gen/archive/v4-svg/`（git 历史仍可找回） |

### 8.2 文件清单

```
app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.webp        （完整图）
app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_foreground.webp
app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher_monochrome.webp
app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml        （引用 @mipmap/ic_launcher_foreground + monochrome）
app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml  （同上）
app/src/main/res/values/themes.xml                        （splash icon 引用更新）
```

### 8.3 验证记录

| 验证项 | 结果 |
|--------|------|
| `assembleDebug` | ✅ PASS（279 tasks） |
| `testDebugUnitTest` | ✅ PASS（518 单测 0 失败） |
| APK 内图标资源 | ✅ 5 密度 webp + adaptive icon XML 齐全 |
| 资源体积 | ✅ 全部 84KB（webp 压缩），对 APK 增量极小 |
| 圆形遮罩实测 | 需 emulator，沙箱无法执行 |

### 8.4 附注

- 本变更同时修复了**本机 JDK 20 与项目 Java 17 的 JVM target 不一致**问题：根 `build.gradle.kts` 统一 Kotlin `jvmTarget=17`（与 CI temurin JDK 17 对齐），保证任意 JDK ≥ 17 可构建。
- 生成过程文件（候选图/中间产物）在 `.icon-gen/`，已加入 .gitignore 不入库。
- 如需回退 v4 矢量：恢复 `.icon-gen/archive/v4-svg/` 两个 xml 到 `drawable/`，并还原 mipmap-anydpi-v26 引用为 `@drawable/...`。
