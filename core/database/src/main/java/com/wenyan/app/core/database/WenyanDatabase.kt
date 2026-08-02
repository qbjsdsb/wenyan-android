package com.wenyan.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wenyan.app.core.database.converter.WenyanTypeConverters
import com.wenyan.app.core.database.dao.AiGradingRecordDao
import com.wenyan.app.core.database.dao.AnswerTemplateDao
import com.wenyan.app.core.database.dao.ApiConfigDao
import com.wenyan.app.core.database.dao.AppMetaDao
import com.wenyan.app.core.database.dao.ChapterDao
import com.wenyan.app.core.database.dao.ChatConversationDao
import com.wenyan.app.core.database.dao.ChatMessageDao
import com.wenyan.app.core.database.dao.DataSourceDao
import com.wenyan.app.core.database.dao.ExamCodeHistoryDao
import com.wenyan.app.core.database.dao.ExamQuestionDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.dao.MemoRecordDao
import com.wenyan.app.core.database.dao.ReviewLogDao
import com.wenyan.app.core.database.dao.StudyProgressDao
import com.wenyan.app.core.database.dao.SubjectDao
import com.wenyan.app.core.database.dao.TemplateFillDao
import com.wenyan.app.core.database.dao.WritingMaterialDao
import com.wenyan.app.core.database.dao.WritingPatternDao
import com.wenyan.app.core.database.dao.WrongAnswerDao
import com.wenyan.app.core.database.entity.AiGradingRecordEntity
import com.wenyan.app.core.database.entity.AnswerTemplateEntity
import com.wenyan.app.core.database.entity.ApiConfigEntity
import com.wenyan.app.core.database.entity.AppMetaEntity
import com.wenyan.app.core.database.entity.ChapterEntity
import com.wenyan.app.core.database.entity.ChatConversationEntity
import com.wenyan.app.core.database.entity.ChatMessageEntity
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.ExamCodeHistoryEntity
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.MemoRecordEntity
import com.wenyan.app.core.database.entity.ReviewLogEntity
import com.wenyan.app.core.database.entity.StudyProgressEntity
import com.wenyan.app.core.database.entity.SubjectEntity
import com.wenyan.app.core.database.entity.TemplateFillEntity
import com.wenyan.app.core.database.entity.WritingMaterialEntity
import com.wenyan.app.core.database.entity.WritingPatternEntity
import com.wenyan.app.core.database.entity.WrongAnswerEntity

/**
 * 文研App Room 数据库。
 *
 * - 数据库名：wenyan.db
 * - 版本：10（v0.9.24：为 exam_questions/knowledge_points 补 3 个筛选索引）
 *   - v1→v2：memo_records 补 elapsed_days/scheduled_days/reps 字段
 *   - v2→v3：回填 reps = review_count（修复 v1→v2 未回填导致老卡片被误判为新卡）
 *   - v3→v4：新增 app_meta 表（通用 key-value，存储 last_known_timestamp_ms 等应用级元数据，
 *     供 ClockGuard 检测系统时钟回拨，避免 FSRS 调度异常）
 *   - v4→v5：合并 schema 变更（NF-PP4 删 memo_records.history / NF-PP6 合并 chat_history +
 *     ai_conversations → chat_conversations + chat_messages / NF-PP5 新增 wrong_answers）
 *   - v5→v6：删除 exam_questions.sample_essay 列（seed v2.9.0 同步删除范文冗余字段）
 *   - v6→v7：DROP graph_nodes + graph_edges（v0.9.0 删 feature:graph UI 后 core 层图谱设施无消费者）
 *   - v7→v8：wrong_answers 添加 10 个 sched_* FSRS 调度字段 + sched_next_review_at 索引
 *   - v8→v9：补建 wrong_answers 两个复合索引（[point_id, source] / [exam_question_id, source]，
 *     修复 MIGRATION_7_8 遗漏导致存量 v8 用户缺索引的性能问题）
 *   - v9→v10：为 exam_questions 补 question_type/answer_status 索引、knowledge_points
 *     补 content_source 索引（筛选查询数据量增长后避免全表扫描）
 *
 * 共 19 张表（v7 移除 graph_nodes + graph_edges；无 mentors 表，导师信息改为外链官网）：
 * 1. subjects                科目
 * 2. chapters                章节
 * 3. knowledge_points        知识点（含 Spec 新增字段）
 * 4. exam_questions          真题（含 Spec 新增字段）
 * 5. memo_records            记忆记录（FSRS，v5 移除 history 列）
 * 6. study_progress          学习进度
 * 7. writing_materials       写作素材
 * 8. api_configs             API 配置
 * 9. ai_grading_records      AI 批改记录
 * 10. answer_templates       答题模板
 * 11. template_fills         模板填写记录
 * 12. writing_patterns       写作句式
 * 13. review_logs            复习日志
 * 14. exam_code_history      科目代码变动历史（Spec 新增表）
 * 15. data_sources           资料来源溯源（Spec 新增表）
 * 16. app_meta               应用元数据（NF-B 新增，key-value 存储 last_known_timestamp_ms 等）
 * 17. chat_conversations     AI 对话元数据（NF-PP6 新增，替代 chat_history + ai_conversations）
 * 18. chat_messages          AI 对话消息内容（NF-PP6 新增，FK→chat_conversations CASCADE）
 * 19. wrong_answers          错题本（NF-PP5 新增，Cards AGAIN + Quiz 答错双来源）
 *
 * v5 移除的表：chat_history、ai_conversations（死代码表，0 Repository 引用，合并为 chat_conversations + chat_messages）
 * v7 移除的表：graph_nodes、graph_edges（v0.9.0 删 feature:graph UI 后 core 层图谱设施无消费者）
 *
 * 通过 Hilt 模块（DatabaseModule）提供单例实例与各 DAO。
 */
@Database(
    entities = [
        SubjectEntity::class,
        ChapterEntity::class,
        KnowledgePointEntity::class,
        ExamQuestionEntity::class,
        MemoRecordEntity::class,
        StudyProgressEntity::class,
        WritingMaterialEntity::class,
        ApiConfigEntity::class,
        AiGradingRecordEntity::class,
        AnswerTemplateEntity::class,
        TemplateFillEntity::class,
        WritingPatternEntity::class,
        ReviewLogEntity::class,
        ExamCodeHistoryEntity::class,
        DataSourceEntity::class,
        AppMetaEntity::class,
        ChatConversationEntity::class,
        ChatMessageEntity::class,
        WrongAnswerEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
@TypeConverters(WenyanTypeConverters::class)
abstract class WenyanDatabase : RoomDatabase() {

    abstract fun subjectDao(): SubjectDao
    abstract fun chapterDao(): ChapterDao
    abstract fun knowledgePointDao(): KnowledgePointDao
    abstract fun examQuestionDao(): ExamQuestionDao
    abstract fun memoRecordDao(): MemoRecordDao
    abstract fun studyProgressDao(): StudyProgressDao
    abstract fun writingMaterialDao(): WritingMaterialDao
    abstract fun apiConfigDao(): ApiConfigDao
    abstract fun aiGradingRecordDao(): AiGradingRecordDao
    abstract fun answerTemplateDao(): AnswerTemplateDao
    abstract fun templateFillDao(): TemplateFillDao
    abstract fun writingPatternDao(): WritingPatternDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun examCodeHistoryDao(): ExamCodeHistoryDao
    abstract fun dataSourceDao(): DataSourceDao
    abstract fun appMetaDao(): AppMetaDao
    abstract fun chatConversationDao(): ChatConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun wrongAnswerDao(): WrongAnswerDao

    companion object {
        // 数据库文件名，与 Spec 要求一致
        const val DATABASE_NAME = "wenyan.db"
    }
}
