package com.wenyan.app.core.common.model

/**
 * 内容来源标注。
 *
 * - [TEXTBOOK_NATIVE]：原生电子文本 → UI 绿色"资料"标签
 * - [TEXTBOOK_OCR]：扫描 OCR 文本 → UI 绿色"资料"标签（带 OCR 角标）
 * - [TEXTBOOK_CONFLICT]：多本教材的表述存在实质差异 → UI 警示标签
 * - [AI_GENERATED]：AI 生成内容 → UI 蓝色"AI"标签
 * - [HYBRID]：混合（资料 + AI）→ UI 黄色"资料+AI"标签
 * - [USER_CREATED]：用户创建 → UI 灰色"我的"标签
 * - [MISSING]：OCR 失败 / 资料缺失 → UI 红色"缺失"标签（特殊状态，提示用户手动处理）
 *
 * P1-7 修复：原 designsystem 模块定义了 `object ContentSource`（字符串常量），
 * core/database 模块定义了 `enum class ContentSource`（零 import 的死代码），
 * 导致设计系统模块反向依赖 database 模块（仅为此常量）。
 * 现统一迁移到 core/common 作为权威字符串常量定义，供 designsystem 与 feature 模块共享。
 *
 * 保持 String 常量（而非 enum）的原因：
 * 实体字段（KnowledgePointEntity.contentSource / DataSourceEntity.contentSource /
 * ChatMessage.contentSource）在数据库 schema 中是 String 列，
 * 直接传 String 避免在每个调用点显式做 enum.valueOf 转换，降低使用成本。
 */
object ContentSource {
    const val TEXTBOOK_NATIVE = "TEXTBOOK_NATIVE"
    const val TEXTBOOK_OCR = "TEXTBOOK_OCR"
    const val TEXTBOOK_CONFLICT = "TEXTBOOK_CONFLICT"
    const val AI_GENERATED = "AI_GENERATED"
    const val HYBRID = "HYBRID"
    const val USER_CREATED = "USER_CREATED"
    const val MISSING = "MISSING"
}
