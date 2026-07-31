package com.wenyan.app.feature.knowledge

import com.wenyan.app.core.data.repository.KnowledgeRepository
import com.wenyan.app.core.data.repository.WrongAnswerRepository
import com.wenyan.app.core.database.dao.DataSourceDao
import com.wenyan.app.core.database.dao.ExamQuestionDao
import com.wenyan.app.core.database.dao.KnowledgePointDao
import com.wenyan.app.core.database.entity.DataSourceEntity
import com.wenyan.app.core.database.entity.ExamQuestionEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.KnowledgePointWithSubject
import com.wenyan.app.core.database.entity.WrongAnswerEntity
import com.wenyan.app.core.database.entity.WrongAnswerWithDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * [KnowledgePointDao] 的 Fake 实现,供 knowledge 模块测试使用(v0.8.19 P1-REL-2 新增)。
 *
 * 仅 stub [KnowledgeRepository] 实际调用的 4 个方法:
 * - [observeById] / [getByIds] / [observeVerifiedWithSubject] / [observeSearchWithSubject]
 *
 * 其他方法抛 [UnsupportedOperationException],避免静默返回错误默认值。
 *
 * 通过 [pointsById] 可控注入知识点数据,支持详情页 + 列表 + 搜索场景测试。
 */
class FakeKnowledgePointDao(
    initialPoints: Map<String, KnowledgePointEntity> = emptyMap(),
) : KnowledgePointDao {

    private val _pointsById = MutableStateFlow(initialPoints)

    /** 当前知识点表快照(测试可读写) */
    val pointsById: Map<String, KnowledgePointEntity> get() = _pointsById.value

    /** 设置知识点表内容(覆盖) */
    fun setPoints(points: Map<String, KnowledgePointEntity>) {
        _pointsById.value = points
    }

    /** 设置知识点列表(以 id 为 key) */
    fun setPointsList(points: List<KnowledgePointEntity>) {
        _pointsById.value = points.associateBy { it.id }
    }

    override suspend fun insert(entity: KnowledgePointEntity) {
        _pointsById.value = _pointsById.value + (entity.id to entity)
    }

    override suspend fun insertAll(entities: List<KnowledgePointEntity>) {
        _pointsById.value = _pointsById.value + entities.associateBy { it.id }
    }

    override suspend fun update(entity: KnowledgePointEntity) {
        _pointsById.value = _pointsById.value + (entity.id to entity)
    }

    override suspend fun deleteById(id: String) {
        _pointsById.value = _pointsById.value - id
    }

    override suspend fun getById(id: String): KnowledgePointEntity? = _pointsById.value[id]

    override suspend fun getByIds(ids: List<String>): List<KnowledgePointEntity> =
        ids.mapNotNull { _pointsById.value[it] }

    override fun observeById(id: String): Flow<KnowledgePointEntity?> =
        _pointsById.mapStateFlow { it[id] }

    override fun observeByChapter(chapterId: String): Flow<List<KnowledgePointEntity>> =
        _pointsById.mapStateFlow { values -> values.values.filter { it.chapterId == chapterId } }

    override fun observeByExamFrequency(frequency: String): Flow<List<KnowledgePointEntity>> =
        _pointsById.mapStateFlow { values -> values.values.filter { it.examFrequency == frequency } }

    override fun observeByOcrStatus(status: String): Flow<List<KnowledgePointEntity>> =
        _pointsById.mapStateFlow { values -> values.values.filter { it.ocrStatus == status } }

    override fun observeByContentSource(source: String): Flow<List<KnowledgePointEntity>> =
        _pointsById.mapStateFlow { values -> values.values.filter { it.contentSource == source } }

    override suspend fun countByChapter(chapterId: String): Int =
        _pointsById.value.values.count { it.chapterId == chapterId }

    override fun observeAll(): Flow<List<KnowledgePointEntity>> =
        _pointsById.mapStateFlow { it.values.toList() }

    override fun observeVerifiedForReview(): Flow<List<KnowledgePointEntity>> =
        _pointsById.mapStateFlow { values ->
            values.values.filter { it.ocrStatus == "VERIFIED" }.sortedByDescending { it.updatedAt }
        }

    override suspend fun updateOcrStatus(id: String, status: String) {
        val current = _pointsById.value[id] ?: return
        _pointsById.value = _pointsById.value + (id to current.copy(ocrStatus = status))
    }

    override suspend fun searchByKeyword(keyword: String, limit: Int): List<KnowledgePointEntity> =
        throw UnsupportedOperationException("searchByKeyword not used in knowledge feature tests")

    /**
     * 简化搜索实现:对 keyword 做 contains 匹配(v0.8.19 P1-REL-2)。
     *
     * 生产是 SQL LIKE,测试用 contains 近似(中文搜索场景无 % _ 通配符,
     * LIKE 等价于 contains)。仅搜索 VERIFIED 知识点。
     */
    override fun observeSearchWithSubject(keyword: String): Flow<List<KnowledgePointWithSubject>> =
        _pointsById.mapStateFlow { values ->
            values.values
                .filter { it.ocrStatus == "VERIFIED" }
                .filter { point ->
                    point.title.contains(keyword) ||
                        point.coreConclusion.contains(keyword) ||
                        point.fullContent.contains(keyword) ||
                        (point.studyText?.contains(keyword) == true)
                }
                .sortedByDescending { it.updatedAt }
                .map { KnowledgePointWithSubject(point = it, subjectName = "中国古代文学") }
        }

    override fun observeVerifiedWithSubject(): Flow<List<KnowledgePointWithSubject>> =
        _pointsById.mapStateFlow { values ->
            values.values
                .filter { it.ocrStatus == "VERIFIED" }
                .sortedByDescending { it.updatedAt }
                .map { KnowledgePointWithSubject(point = it, subjectName = "中国古代文学") }
        }
}

/**
 * [DataSourceDao] 的 Fake 实现,供 knowledge 模块测试使用(v0.8.19 P1-REL-2 新增)。
 *
 * 仅 stub [observeByKnowledgePoint](详情页来源溯源用),其他方法抛异常。
 */
class FakeDataSourceDao(
    initialSourcesByPoint: Map<String, List<DataSourceEntity>> = emptyMap(),
) : DataSourceDao {

    private val _sourcesByPoint = MutableStateFlow(initialSourcesByPoint)

    /** 设置某知识点的来源列表 */
    fun setSourcesForPoint(pointId: String, sources: List<DataSourceEntity>) {
        _sourcesByPoint.value = _sourcesByPoint.value + (pointId to sources)
    }

    override suspend fun insert(entity: DataSourceEntity) {
        throw UnsupportedOperationException("insert not used in knowledge feature tests")
    }

    override suspend fun insertAll(entities: List<DataSourceEntity>) {
        throw UnsupportedOperationException("insertAll not used in knowledge feature tests")
    }

    override suspend fun update(entity: DataSourceEntity) {
        throw UnsupportedOperationException("update not used in knowledge feature tests")
    }

    override suspend fun deleteById(id: String) {
        throw UnsupportedOperationException("deleteById not used in knowledge feature tests")
    }

    override suspend fun getById(id: String): DataSourceEntity? {
        throw UnsupportedOperationException("getById not used in knowledge feature tests")
    }

    override fun observeByKnowledgePoint(pointId: String): Flow<List<DataSourceEntity>> =
        _sourcesByPoint.mapStateFlow { it[pointId] ?: emptyList() }

    override fun observeByExamQuestion(questionId: String): Flow<List<DataSourceEntity>> =
        throw UnsupportedOperationException("observeByExamQuestion not used in knowledge tests")

    override fun observeByContentSource(source: String): Flow<List<DataSourceEntity>> =
        throw UnsupportedOperationException("observeByContentSource not used in knowledge tests")

    override fun observeByOcrStatus(status: String): Flow<List<DataSourceEntity>> =
        throw UnsupportedOperationException("observeByOcrStatus not used in knowledge tests")

    override fun observeAll(): Flow<List<DataSourceEntity>> =
        throw UnsupportedOperationException("observeAll not used in knowledge feature tests")
}

/**
 * [WrongAnswerRepository] 的 Fake 实现,供 knowledge 模块测试使用(v0.8.19 P1-REL-2 新增)。
 *
 * 与 cards 模块的 FakeWrongAnswerRepository 类似,但增加 [observeByPoint] 的真实实现
 * (返回注入的错题列表),供详情页错题关联测试用。
 *
 * - [initialByPoint]:observeByPoint 初始数据
 * - [resolvedIds]:记录所有 markResolved 调用的 id(供断言)
 * - [markResolvedThrowable]:非 null 时 markResolved 抛异常(测异常分支)
 */
class FakeKnowledgeWrongAnswerRepository(
    initialByPoint: Map<String, List<WrongAnswerEntity>> = emptyMap(),
    var markResolvedThrowable: Throwable? = null,
) : WrongAnswerRepository {

    private val _byPoint = MutableStateFlow(initialByPoint)

    val resolvedIds: MutableList<String> = mutableListOf()

    /** 设置某知识点的错题列表 */
    fun setWrongAnswersForPoint(pointId: String, wrongAnswers: List<WrongAnswerEntity>) {
        _byPoint.value = _byPoint.value + (pointId to wrongAnswers)
    }

    override fun observeAll(): Flow<List<WrongAnswerWithDetails>> =
        throw UnsupportedOperationException("observeAll not used in knowledge detail tests")

    override fun observeUnresolved(): Flow<List<WrongAnswerWithDetails>> =
        throw UnsupportedOperationException("observeUnresolved not used in knowledge detail tests")

    /**
     * v0.9.4 新增:观察待复习错题(FSRS 调度)。
     *
     * knowledge 详情页测试不依赖此方法(由 WrongAnswerViewModel 使用),
     * 但接口扩展后必须实现,抛 UnsupportedOperationException 保持与现有方法一致语义。
     */
    override fun observeDueWrongAnswers(now: Long): Flow<List<WrongAnswerWithDetails>> =
        throw UnsupportedOperationException("observeDueWrongAnswers not used in knowledge detail tests")

    override fun observeByPoint(pointId: String): Flow<List<WrongAnswerEntity>> =
        _byPoint.mapStateFlow { it[pointId] ?: emptyList() }

    override fun observeByExamQuestion(examQuestionId: String): Flow<List<WrongAnswerEntity>> =
        throw UnsupportedOperationException("observeByExamQuestion not used in knowledge tests")

    override suspend fun recordWrongAnswer(
        pointId: String?,
        examQuestionId: String?,
        userAnswer: String,
        correctAnswer: String?,
        source: String,
    ): String = throw UnsupportedOperationException("recordWrongAnswer not used in knowledge tests")

    override suspend fun markResolved(id: String) {
        markResolvedThrowable?.let { throw it }
        resolvedIds.add(id)
    }

    override suspend fun deleteById(id: String) {
        throw UnsupportedOperationException("deleteById not used in knowledge detail tests")
    }

    override suspend fun countUnresolved(): Int =
        throw UnsupportedOperationException("countUnresolved not used in knowledge detail tests")
}

/**
 * 构造测试用 [KnowledgeRepository](真实实例 + Fake DAOs)。
 *
 * v0.8.19 P1-REL-2:用真实 Repository + Fake DAOs,
 * 顺带覆盖 Repository 的 observeKnowledgePointDetail 合并逻辑(relatedPoints 分组等),
 * 避免为测试把 KnowledgeRepository 改为 interface(较大重构)。
 *
 * v0.9.8:新增 [examQuestionDao] 参数,支持论述题板块(observeRelatedEssays /
 * observeEssayById / getKnowledgePointsByIds)测试。
 */
fun buildKnowledgeRepository(
    knowledgePointDao: FakeKnowledgePointDao,
    dataSourceDao: FakeDataSourceDao,
    examQuestionDao: FakeExamQuestionDao = FakeExamQuestionDao(),
): KnowledgeRepository = KnowledgeRepository(
    knowledgePointDao = knowledgePointDao,
    dataSourceDao = dataSourceDao,
    examQuestionDao = examQuestionDao,
)

/**
 * MutableStateFlow 映射辅助(避免每个方法重复 .map { })。
 *
 * 返回新 Flow,每次 StateFlow 值变化时用 [transform] 计算新值。
 */
private fun <T, R> MutableStateFlow<T>.mapStateFlow(transform: (T) -> R): Flow<R> =
    map { transform(it) }

/**
 * [ExamQuestionDao] 的 Fake 实现(v0.9.8 论述题板块新增)。
 *
 * 仅 stub [KnowledgeRepository] 论述题板块用到的方法:
 * - [observeAllEssays]:返回注入的论述题列表(供 observeRelatedEssays 内存过滤)
 * - [observeById]:按 ID 查找(供 observeEssayById)
 *
 * 其他方法抛 [UnsupportedOperationException],避免静默返回错误默认值。
 *
 * 通过 [setEssays] 可控注入论述题数据,支持:
 * - 知识点详情页"相关论述题"区块(observeRelatedEssays)
 * - 论述题详情页主体数据(observeEssayById)
 */
class FakeExamQuestionDao(
    initialEssays: List<ExamQuestionEntity> = emptyList(),
) : ExamQuestionDao {

    private val _essays = MutableStateFlow(initialEssays)

    /** 当前论述题列表快照(测试可读写) */
    val essays: List<ExamQuestionEntity> get() = _essays.value

    /** 设置论述题列表(覆盖) */
    fun setEssays(essays: List<ExamQuestionEntity>) {
        _essays.value = essays
    }

    override suspend fun insert(entity: ExamQuestionEntity) {
        _essays.value = _essays.value + entity
    }

    override suspend fun insertAll(entities: List<ExamQuestionEntity>) {
        _essays.value = _essays.value + entities
    }

    override suspend fun update(entity: ExamQuestionEntity) {
        _essays.value = _essays.value.map { if (it.id == entity.id) entity else it }
    }

    override suspend fun deleteById(id: String) {
        _essays.value = _essays.value.filter { it.id != id }
    }

    override suspend fun getById(id: String): ExamQuestionEntity? =
        _essays.value.firstOrNull { it.id == id }

    override fun observeById(id: String): Flow<ExamQuestionEntity?> =
        _essays.mapStateFlow { essays -> essays.firstOrNull { it.id == id } }

    override fun observeBySubject(subjectId: String): Flow<List<ExamQuestionEntity>> =
        _essays.mapStateFlow { it.filter { e -> e.subjectId == subjectId } }

    override fun observeByYear(year: Int): Flow<List<ExamQuestionEntity>> =
        _essays.mapStateFlow { it.filter { e -> e.year == year } }

    override fun observeByQuestionType(type: String): Flow<List<ExamQuestionEntity>> =
        _essays.mapStateFlow { it.filter { e -> e.questionType == type } }

    /**
     * 返回全部 ESSAY 题,按年份倒序(与生产 SQL ORDER BY year DESC 一致)。
     *
     * 调用方(KnowledgeRepository.observeRelatedEssays)在内存中按
     * relatedPointIds.contains(pointId) 过滤。
     */
    override fun observeAllEssays(): Flow<List<ExamQuestionEntity>> =
        _essays.mapStateFlow { it.filter { e -> e.questionType == "ESSAY" }.sortedByDescending { e -> e.year } }

    override fun observeByExamPaperCode(code: String): Flow<List<ExamQuestionEntity>> =
        _essays.mapStateFlow { it.filter { e -> e.examPaperCode == code } }

    override fun observeByAnswerStatus(status: String): Flow<List<ExamQuestionEntity>> =
        _essays.mapStateFlow { it.filter { e -> e.answerStatus == status } }

    override fun observeYears(): Flow<List<Int>> =
        _essays.mapStateFlow { it.map { e -> e.year }.distinct().sortedDescending() }

    override suspend fun countBySubject(subjectId: String): Int =
        _essays.value.count { it.subjectId == subjectId }
}
