# 文研 App 启动图标设计

> 版本：v8（2026-08-06）  
> 状态：✅ 已实施，等待 PR #8 的完整 CI 验证  
> 目标：克制、精致、清晰，具备 Google 产品常见的简洁几何感，同时保留文研的“阅读与文学”语义。

## 1. 为什么重做

主分支此前的 v7.5 是“黑底 + 两个矩形书页 + 八条文字线”。它在大图上能够表达书本，但在桌面小尺寸、圆形蒙版和通知栏里容易退化为普通文档图标；单色主题图标也保留了过多细线，主题着色后不够稳定。

这次不再继续叠加装饰，而是收紧视觉语言：

- 一个清楚的展开书轮廓；
- 两页仅用轻微的暖色差区分；
- 两组内页曲线暗示阅读，不模拟真实排版；
- 一枚很小的朱砂色书签，提供品牌记忆点；
- 不使用渐变、投影、描边堆叠或复杂文字负空间。

设计参考的是 Google/Material 图标的原则：bold、simple、distinctive；不是复制任何现有产品图标。

## 2. v8 方案

### 2.1 彩色 adaptive foreground

ic_launcher_foreground.xml 使用 108×108 的 Android adaptive-icon 视口：

- 左页：#FFFDF6 暖白；
- 右页：#F0E5D4 暖米；
- 书签：#C76652 克制的朱砂色；
- 内页曲线：#82796E，仅两组、圆头；
- 书页主体大约位于 x=21..87, y=28..84，关键轮廓留在中心安全区域；
- 书脊留出清晰的中间间隙，圆形、方圆形和水滴形蒙版下都不会误读。

图形的重心略低于视口中心，模拟展开书自然的下坠感；书页底部向中央收拢，避免看起来像两个并排的文件卡片。

### 2.2 Monochrome themed icon

ic_launcher_monochrome.xml 只保留同一书形的两块白色轮廓，不带彩色书签和内页曲线。系统可以把非透明像素统一染成用户主题色，而中央间隙仍然保留，因此：

- 不依赖彩色对比；
- 不依赖细线抗锯齿；
- 在 Android 13+ themed icon 下仍然容易辨认；
- 与彩色图标保持同一品牌语义，而不是另一个图案。

### 2.3 背景色

ic_launcher_background.xml 改为引用 @color/wenyan_launcher_background，颜色统一定义为 #202124。这是偏中性的深墨色，比纯黑更柔和，也便于暖纸色书页和朱砂色书签形成层次。

## 3. 自适应图标资源关系

| 资源 | 用途 | v8 处理 |
|------|------|---------|
| drawable/ic_launcher_background.xml | adaptive 背景层 | 深墨色，改为引用颜色资源 |
| drawable/ic_launcher_foreground.xml | adaptive 前景层、Splash 图标 | 全新双页书形 |
| drawable/ic_launcher_monochrome.xml | Android 13+ themed icon | 简化为双页轮廓 |
| mipmap-anydpi-v26/ic_launcher.xml | 普通 adaptive icon | 保持聚合方式不变 |
| mipmap-anydpi-v26/ic_launcher_round.xml | 圆形 adaptive icon | 保持聚合方式不变 |
| values/themes.xml | 冷启动图标 | 继续引用新的 vector foreground |

项目 minSdk = 26，因此运行时优先使用 anydpi-v26 的 adaptive vector 资源；现有各密度 WebP 作为历史兼容资源保留，避免无必要的二进制 churn。若未来降低 minSdk，应先重新生成同一 v8 设计的 fallback 位图，不能让旧图标重新成为用户可见资源。

## 4. 验证清单

### 已完成的静态和视觉检查

- 108×108 viewport 下检查了 safe-zone 边界；
- 432px 大尺寸预览检查轮廓、页色和书签比例；
- 48px 小尺寸预览确认第一识别仍是“书”，而不是“文档”或“两个方块”；
- 主题图标预览确认去掉颜色和细线后仍能辨认；
- 检查 foreground、monochrome、background 的资源引用关系没有改变 adaptive-icon 结构；
- 检查 Splash 仍使用同一份前景 vector，避免冷启动和桌面图标风格分裂。

### 待 CI 完成的工程检查

- testDebugUnitTest
- assembleDebug
- APK 资源打包成功且没有 vector XML 解析错误

### 真机验收重点

发布前在至少一种圆形蒙版和一种方圆形蒙版下检查：

1. 桌面常规尺寸；
2. 最近任务和设置页小尺寸；
3. Android 13+ themed icon；
4. 浅色/深色壁纸；
5. 冷启动 Splash 与桌面图标是否视觉连续。

## 5. 维护规则

- 图标只保留一个主轮廓，新增细节必须先通过 48px 预览；
- 彩色 foreground 和 monochrome 必须共享同一书形几何；
- 重要形状不得越过 adaptive safe zone；
- 修改颜色时同步检查 App 内暖纸色主题，不引入新的无来源品牌色；
- 每次图标改动都必须跑完整 Debug 构建，不能只看 XML 能否打开；
- 设计文档只维护当前生效版本，历史方案交给 Git 历史追溯，避免再次出现 v4/v5/v7 混写。
