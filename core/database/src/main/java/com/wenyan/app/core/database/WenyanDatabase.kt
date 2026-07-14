package com.wenyan.app.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.wenyan.app.core.database.converter.WenyanTypeConverters
import com.wenyan.app.core.database.dao.AiConversationDao
import com.wenyan.app.core.database.dao.AiGradingRecordDao
import com.wenyan.app.core.database.dao.AnswerTemplateDao
import com.wenyan.app.core.database.dao.ApiConfigDao
import com.wenyan.app.core.database.dao.ChapterDao
import com.wenyan.app.core.database.dao.ChatHistoryDao
import com.wenyan.app.core.database.dao.DataSourceDao
import com.wenyan.app.core.database.dao.ExamCodeHistoryDao
import com.wenyan.app.core.database.dao.ExamQuestionDao
import com.wenyan.app.core.database.dao.GraphEdgeDao
import com.wenyan.app.core.database.dao.GraphNodeDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.dao.MemoRecordDao
import com.wenyan.app.core.database.dao.ReviewLogDao
import com.wenyan.app.core.database.dao.StudyProgressDao
import com.wenyan.app.core.database.dao.SubjectDao
import com.wenyan.app.core.database.dao.TemplateFillDao
import com.wenyan.app.core.database.dao.WritingMaterialDao
import com.wenyan.app.core.database.dao.WritingPatternDao
import com.wenyan.app.core.database.entity.AiConversationEntity
import com.wenyan.app.core.database.entity.AiGradingRecordEntity
import com.wenyan.app.core.database.entity.AnswerTemplateEntity
import com.wenyan.app.core.database.entity.ApiConfigEntity
import com.wenyan.app.core.database.entity.ChapterEntity
import com.wenyan.app.core.database.entity.ChatHistoryEntity
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.ExamCodeHistoryEntity
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.GraphEdgeEntity
import com.wenyan.app.core.database.entity.GraphNodeEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.MemoRecordEntity
import com.wenyan.app.core.database.entity.ReviewLogEntity
import com.wenyan.app.core.database.entity.StudyProgressEntity
import com.wenyan.app.core.database.entity.SubjectEntity
import com.wenyan.app.core.database.entity.TemplateFillEntity
import com.wenyan.app.core.database.entity.WritingMaterialEntity
import com.wenyan.app.core.database.entity.WritingPatternEntity

/**
 * 文研App Room 数据库。
 *
 * - 数据库名：wenyan.db
 * - 版本：3（NF-D1 修复：Migration_2_3 回填 reps 字段）
 *   - v1→v2：memo_records 补 elapsed_days/scheduled_days/reps 字段
 *   - v2→v3：回填 reps = review_count（修复 v1→v2 未回填导致老卡片被误判为新卡）
 *
 * 共 19 张表（无 mentors 表，导师信息改为外链官网）：
 * 1. subjects                科目
 * 2. chapters                章节
 * 3. knowledge_points        知识点（含 Spec 新增字段）
 * 4. exam_questions          真题（含 Spec 新增字段）
 * 5. memo_records            记忆记录（FSRS）
 * 6. study_progress          学习进度
 * 7. writing_materials       写作素材
 * 8. api_configs             API 配置
 * 9. chat_history            聊天历史
 * 10. ai_grading_records     AI 批改记录
 * 11. answer_templates       答题模板
 * 12. template_fills         模板填写记录
 * 13. writing_patterns       写作句式
 * 14. graph_nodes            图谱节点（含 Spec 新增 prerequisites）
 * 15. graph_edges            图谱边（含 Spec 新增 PREREQUISITE 关系）
 * 16. ai_conversations       AI 对话记录
 * 17. review_logs            复习日志
 * 18. exam_code_history      科目代码变动历史（Spec 新增表）
 * 19. data_sources           资料来源溯源（Spec 新增表）
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
        ChatHistoryEntity::class,
        AiGradingRecordEntity::class,
        AnswerTemplateEntity::class,
        TemplateFillEntity::class,
        WritingPatternEntity::class,
        GraphNodeEntity::class,
        GraphEdgeEntity::class,
        AiConversationEntity::class,
        ReviewLogEntity::class,
        ExamCodeHistoryEntity::class,
        DataSourceEntity::class,
    ],
    version = 3,
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
    abstract fun chatHistoryDao(): ChatHistoryDao
    abstract fun aiGradingRecordDao(): AiGradingRecordDao
    abstract fun answerTemplateDao(): AnswerTemplateDao
    abstract fun templateFillDao(): TemplateFillDao
    abstract fun writingPatternDao(): WritingPatternDao
    abstract fun graphNodeDao(): GraphNodeDao
    abstract fun graphEdgeDao(): GraphEdgeDao
    abstract fun aiConversationDao(): AiConversationDao
    abstract fun reviewLogDao(): ReviewLogDao
    abstract fun examCodeHistoryDao(): ExamCodeHistoryDao
    abstract fun dataSourceDao(): DataSourceDao

    companion object {
        // 数据库文件名，与 Spec 要求一致
        const val DATABASE_NAME = "wenyan.db"
    }
}
