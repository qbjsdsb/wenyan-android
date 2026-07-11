package com.wenyan.app.core.database.entity

/**
 * 内容来源五级标注 + 1 特殊状态（Spec 第 43-47 行、第 201-205 行）。
 *
 * - [TEXTBOOK_NATIVE]：原生电子文本 → UI 绿色"资料"标签
 * - [TEXTBOOK_OCR]：扫描 OCR 文本 → UI 绿色"资料"标签（带 OCR 角标）
 * - [AI_GENERATED]：AI 生成内容 → UI 蓝色"AI"标签
 * - [HYBRID]：混合（资料 + AI）→ UI 黄色"资料+AI"标签
 * - [USER_CREATED]：用户创建 → UI 灰色"我的"标签
 * - [MISSING]：OCR 失败 / 资料缺失 → UI 红色"缺失"标签（特殊状态，提示用户手动处理）
 */
enum class ContentSource {
    TEXTBOOK_NATIVE,
    TEXTBOOK_OCR,
    AI_GENERATED,
    HYBRID,
    USER_CREATED,
    MISSING,
}
