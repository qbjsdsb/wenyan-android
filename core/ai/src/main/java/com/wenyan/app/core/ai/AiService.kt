package com.wenyan.app.core.ai

import kotlinx.coroutines.flow.Flow

/**
 * AI 服务接口。
 *
 * ── 与设计文档的关系（增强而非替代）──────────────────────────────────
 * 设计文档 3.6 节（第 1826-1990 行）已定义完整的 AI 助手功能体系：
 * - 智能悬浮窗（3.6.2）
 * - 论述题批改（3.6.3 功能1）
 * - 知识点问答（3.6.3 功能2）
 * - 答题框架生成（3.6.3 功能3）
 * - API 多服务商配置（3.6.4）
 * - 离线降级（3.6.5）
 * - 评分维度体系（3.6.6）
 *
 * 上述功能由设计文档定义，本接口聚焦 Spec 第 344-388 行新增的增强能力：
 * - 苏格拉底式引导（不直接给答案）
 * - "解释我的答案"机制（答错后分析错误思路）
 * - RAG 架构（用户资料库 + 权威教材库检索，引用可溯源）
 * - 内容来源五级标注 + MISSING 特殊状态
 * ──────────────────────────────────────────────────────────────
 */
interface AiService {

    /**
     * 发送对话消息，返回流式响应。
     *
     * 设计文档 3.6.3 功能2"知识点问答"的增强版本：
     * - 增强点：回答前先通过 [RagEngine] 检索用户资料库 + 权威教材库
     * - 增强点：回答中标注引用来源（如"据袁行霈《中国文学史》第二卷 P156"）
     * - 增强点：区分资料原文与 AI 生成内容（内容来源五级标注）
     * - 增强点：RAG 无结果时不编造答案，明确告知用户
     *
     * 注意：本方法把所有异常吞为 emit(errorString)，调用方无法区分"AI 真实回复"
     * vs"错误提示字符串"。需要区分的场景请用 [chatResult]。
     *
     * @param query 用户提问
     * @return 流式 AI 回复片段（失败时 emit 错误提示字符串）
     */
    fun chat(query: String): Flow<String>

    /**
     * 发送对话消息，返回 Result 包装的流式响应（P1-6 修复）。
     *
     * 与 [chat] 的区别：
     * - 成功：emit `Result.success(content)`
     * - 失败：emit `Result.failure(exception)`，异常 message 含差异化提示
     *
     * 用途：调用方需要区分成功/失败以做短路（如 [SocraticTutor.guideEssayAnswer]
     * 三阶段：阶段1失败时不执行阶段2/3，避免错误字符串层层传播）。
     *
     * @param query 用户提问
     * @return 流式 Result，success 为 AI 回复内容，failure 为异常（含差异化错误信息）
     */
    fun chatResult(query: String): Flow<Result<String>>

    /**
     * 判断当前是否在线可用（设计文档 3.6.5 离线降级支持）。
     *
     * @return true 表示 AI 服务可用，false 时触发离线降级
     */
    fun isAvailable(): Flow<Boolean>
}
