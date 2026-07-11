package com.wenyan.app.core.fsrs

/**
 * 三档复习调度机制 —— 档位枚举
 *
 * 对应设计文档第722-793行（3.3.4节 三档复习调度机制）。
 * 文学考研的背诵内容并非"一刀切"，不同知识点根据考试要求分配到不同记忆档位，
 * FSRS算法使用不同参数集进行调度。
 *
 * @property TIER_EXACT      精确记忆档：名词解释原文、原诗、作家字号、关键术语定义（目标保留率0.95）
 * @property TIER_FRAMEWORK  框架记忆档：论述题答题要点、作品艺术特色分条（目标保留率0.90）
 * @property TIER_UNDERSTAND 理解记忆档：文学史脉络、影响关系、背景知识（目标保留率0.85）
 */
enum class MemoryTier {
    TIER_EXACT,
    TIER_FRAMEWORK,
    TIER_UNDERSTAND
}
