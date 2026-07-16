# 文研 App 启动图标重设计

> 日期：2026-07-16
> 状态：设计已确认，待实施
> 触发：用户反馈现有"文"字几何拼块图标过于生硬，要求重做以符合 Android 设计规范、流畅大方、有谷歌产品气质

## 1. 现状

| 文件 | 内容 |
|------|------|
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | 米色"文"字（5 个矩形拼块 path），过于方块化 |
| `app/src/main/res/drawable/ic_launcher_background.xml` | 纯色 #2C2C2C 墨黑矩形 |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | 白色"文"字（与 foreground 同 path），供 Android 13+ themed icon |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | adaptive-icon 聚合（background + foreground + monochrome） |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | 同上（圆形遮罩） |

**结构完整**（adaptive + monochrome 三层），**问题在前景图形**：现有"文"字由 5 个独立矩形拼接，笔画转折生硬、字形失衡，小尺寸下识别度低。

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
M28,36           起点：左上角
L52,44           左页面右上（书脊左侧顶点）
L56,44           书脊右侧顶点
L80,36           右页面右上角
L80,72           右页面右下角
L56,80           书脊右侧底点
L52,80           书脊左侧底点
L28,72           左页面左下角
Z                闭合

"文"字镂空（内环，逆时针，evenOdd 规则镂空）：
M40,52           横画左端
L68,52           横画右端
L68,56           横画右下
L58,56           横画中部下（撇捺交叉点上方）
L66,68           捺画右下
L62,70           捺画收笔
L54,60           撇捺交叉点
L46,70           撺画收笔
L42,68           撺画右下
L50,56           横画中部下（撇捺交叉点上方）
L40,56           横画左下
Z                闭合
```

**完整 path（外环 + 内环组合）**：
```
M28,36 L52,44 L56,44 L80,36 L80,72 L56,80 L52,80 L28,72 Z 
M40,52 L68,52 L68,56 L58,56 L66,68 L62,70 L54,60 L46,70 L42,68 L50,56 L40,56 Z
```

`android:fillType="evenOdd"` 让内环镂空，呈现"文"字负空间。

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

## 4. 实施步骤

### 4.1 文件改动

| 文件 | 改动 |
|------|------|
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | 替换 path 为"书 + 文字镂空"，加 `android:fillType="evenOdd"` |
| `app/src/main/res/drawable/ic_launcher_monochrome.xml` | 同步替换 path（与 foreground 一致） |
| `app/src/main/res/drawable/ic_launcher_background.xml` | 不变（已是纯色矩形） |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | 不变 |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml` | 不变 |
| `app/src/main/res/values/colors.xml` | 不变（#2C2C2C / #F5F1E8 已定义） |

### 4.2 验证

1. **编译验证**：`CI=false gradle assembleDebug` BUILD SUCCESSFUL
2. **测试回归**：`CI=false gradle testDebugUnitTest` 220 tests 0 failures（图标改动不影响测试）
3. **视觉验证**（需 emulator，沙箱无法执行）：
   - 启动屏图标显示正确
   - 桌面图标显示正确（方形 + 圆形遮罩）
   - 最近任务栏小尺寸图标清晰
   - Android 13+ themed icon 模式下"文"字负空间保留
   - 深色模式下图标不变（adaptive icon 不跟随系统主题，只有 themed icon 模式才变色）

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
