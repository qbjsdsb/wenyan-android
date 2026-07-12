# 术语表

> 项目中使用的术语和缩写解释。

## 考试相关

| 术语 | 全称 | 含义 |
|------|------|------|
| 南师大 | 南京师范大学 | 目标院校 |
| 050106 | — | 现当代文学专业代码 |
| 现当代文学 | 中国现当代文学 | 考研方向 |
| 专业课 | — | 文学评论 + 现代文学史 + 当代文学史 |
| 真题 | — | 历年考研试题 |
| 名著 | — | 32 个预定义文学名著（OCR 跳过） |

## 技术相关

| 术语 | 全称 | 含义 |
|------|------|------|
| FSRS-6 | Free Spaced Repetition Scheduler v6 | 记忆调度算法，自实现 |
| M3 | Material 3 | Material Design 3 |
| M3 Expressive | Material 3 Expressive | 2025 版 M3，含动态色彩、形状变体、动效方案 |
| materialkolor | — | 动态色彩生成库，KSU 同款 |
| KSU | KernelSU | 参考 UI 风格的 Android 项目 |
| Compose | Jetpack Compose | Android 声明式 UI 框架 |
| BOM | Bill of Materials | Compose 版本统一管理 |
| AGP | Android Gradle Plugin | Android 构建插件 |
| KSP | Kotlin Symbol Processing | Kotlin 注解处理 |
| Hilt | — | Android 依赖注入框架 |
| Room | — | Android SQLite ORM |
| AMOLED | — | 纯黑模式，OLED 省电 |
| PKCS12 | — | keystore 格式（Java 17+ 默认） |
| DPI | Dots Per Inch | OCR 渲染精度 |

## OCR 相关

| 术语 | 含义 |
|------|------|
| OCR | 光学字符识别 |
| MinerU | OCR 工具，3.x 版本 |
| RapidOCR | 轻量 OCR 引擎（小模型） |
| manifest.json | OCR 文件清单，208 文件状态索引 |
| content_source | 内容来源标注 |
| ocr_status | OCR 置信度状态 |
| seed_data.json | App 种子数据，含知识点/真题/卡片 |
| P1-P5 | OCR 优先级分级 |
| post_correct | OCR 校对步骤 |
| extract_knowledge | 知识点提取步骤 |
| cross_validate | 交叉验证步骤 |

## 内容来源标注（ContentSource）

| 值 | 含义 | 颜色角色 |
|----|------|----------|
| TEXTBOOK_NATIVE | 原生教材 | secondaryContainer |
| TEXTBOOK_OCR | OCR 教材 | secondaryContainer |
| AI_GENERATED | AI 生成 | tertiaryContainer |
| HYBRID | 资料+AI | surfaceContainerHighest |
| USER_CREATED | 用户创建 | surfaceContainerHigh |
| MISSING | 缺失 | errorContainer |

## 文件命名

| 文件 | 说明 |
|------|------|
| `file_XXX.json` | OCR 输出文件，XXX 为文件 ID（001-208） |
| `seed_data.json` | App 种子数据 |
| `manifest.json` | OCR 文件清单 |
| `libs.versions.toml` | Gradle 版本目录 |
| `AGENTS.md` | AI 协作入口文件 |
| `SESSION_LOG.md` | 会话日志 |
| `00-STATUS.md` | 当前状态快照 |
| `02-VERSION-MATRIX.md` | 版本兼容性矩阵 |
| `03-FAILED-ATTEMPTS.md` | 失败方案档案 |

## 项目阶段

| 阶段 | 含义 |
|------|------|
| Phase 1 | 资料数字化（OCR） |
| Phase 2 | Android 骨架（架构+数据库） |
| Phase 3 | FSRS 调度（记忆算法） |
| Phase 4 | AI 服务（OpenAI 兼容） |
| Phase 5 | UI 增强（9 个 Screen） |
| M3 改造 | M3 Expressive 风格改造 |

## 三层记忆调度

| 层级 | 含义 | FSRS 配置 |
|------|------|-----------|
| L1 瞬时记忆 | 短期复习 | 短间隔 |
| L2 工作记忆 | 中期巩固 | 中间隔 |
| L3 长期记忆 | 长期保持 | 长间隔 |

## 图谱相关

| 术语 | 含义 |
|------|------|
| R-value | 记忆保留率（FSRS-6 幂律公式计算） |
| pointId | 知识点 ID（卡片关联） |
| 知识图谱 | 知识点关系网络 |
| 苏格拉底引导 | AI 引导式提问教学 |
