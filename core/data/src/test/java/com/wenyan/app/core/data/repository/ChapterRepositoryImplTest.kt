package com.wenyan.app.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.wenyan.app.core.database.WenyanDatabase
import com.wenyan.app.core.database.entity.ChapterEntity
import com.wenyan.app.core.database.entity.KnowledgePointEntity
import com.wenyan.app.core.database.entity.SubjectEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * [ChapterRepositoryImpl] 单元测试（ADR-001 B1.2）。
 *
 * 用 Robolectric + in-memory Room 做真实持久化测试，验证：
 * - observeSubjects 返回所有科目
 * - observeRootChapters 返回指定科目的根章节（parentId IS NULL）
 * - observeChildren 返回直接子章节
 * - observeTree 递归 CTE 返回整棵子树（关键：验证 WITH RECURSIVE 在 Room + SQLite 正确工作）
 * - observeKnowledgePointsByChapter 返回章节下知识点
 * - countNonRootChapters 返回非根章节数
 *
 * 用 in-memory Room 而非 Fake DAO 的理由：
 * - observeTree 用 WITH RECURSIVE CTE，Fake DAO 无法验证 SQL 正确性
 * - 真实 DB 能验证 Room 编译的 SQL 在运行时行为正确
 *
 * 测试树结构：
 * ```
 * subj_1
 *   └── ch_root_1 (parentId=null)
 *        ├── ch_child_1a (parentId=ch_root_1)
 *        │     └── ch_grand_1a1 (parentId=ch_child_1a)
 *        └── ch_child_1b (parentId=ch_root_1)
 * subj_2
 *   └── ch_root_2 (parentId=null)
 * ```
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ChapterRepositoryImplTest {

    private lateinit var db: WenyanDatabase
    private lateinit var repository: ChapterRepositoryImpl

    @Before
    fun setup() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, WenyanDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = ChapterRepositoryImpl(
            subjectDao = db.subjectDao(),
            chapterDao = db.chapterDao(),
            knowledgePointDao = db.knowledgePointDao(),
        )

        // 插入两科
        db.subjectDao().insertAll(
            listOf(
                SubjectEntity(id = "subj_1", name = "中国古代文学", shortName = "古文", sortOrder = 1),
                SubjectEntity(id = "subj_2", name = "中国现当代文学", shortName = "现当代", sortOrder = 2),
            ),
        )

        // 插入三级章节树
        db.chapterDao().insertAll(
            listOf(
                // subj_1 树
                ChapterEntity(id = "ch_root_1", subjectId = "subj_1", parentId = null, title = "第一编 先秦文学", sortOrder = 1),
                ChapterEntity(id = "ch_child_1a", subjectId = "subj_1", parentId = "ch_root_1", title = "第一章 诗经", sortOrder = 1),
                ChapterEntity(id = "ch_grand_1a1", subjectId = "subj_1", parentId = "ch_child_1a", title = "第一节 国风", sortOrder = 1),
                ChapterEntity(id = "ch_child_1b", subjectId = "subj_1", parentId = "ch_root_1", title = "第二章 楚辞", sortOrder = 2),
                // subj_2 树（仅根）
                ChapterEntity(id = "ch_root_2", subjectId = "subj_2", parentId = null, title = "第一编 五四文学", sortOrder = 1),
            ),
        )

        // 插入知识点（挂到叶子章节）
        db.knowledgePointDao().insertAll(
            listOf(
                knowledgePoint("point_1", "ch_grand_1a1", "关雎"),
                knowledgePoint("point_2", "ch_child_1b", "离骚"),
                knowledgePoint("point_3", "ch_root_2", "狂人日记"),
            ),
        )
    }

    @After
    fun teardown() {
        db.close()
    }

    /**
     * 场景 1：observeSubjects 返回所有科目，按 sortOrder ASC。
     */
    @Test
    fun `observeSubjects 返回所有科目按 sortOrder`() = runTest {
        val subjects = repository.observeSubjects().first()
        assertEquals("应有 2 个科目", 2, subjects.size)
        assertEquals("sortOrder=1 在前", "subj_1", subjects[0].id)
        assertEquals("sortOrder=2 在后", "subj_2", subjects[1].id)
    }

    /**
     * 场景 2：observeRootChapters 返回指定科目的根章节（parentId IS NULL）。
     */
    @Test
    fun `observeRootChapters 返回指定科目的根章节`() = runTest {
        val roots1 = repository.observeRootChapters("subj_1").first()
        assertEquals("subj_1 应有 1 个根章节", 1, roots1.size)
        assertEquals("ch_root_1", roots1[0].id)
        assertTrue("根章节 parentId 应为 null", roots1[0].parentId == null)

        val roots2 = repository.observeRootChapters("subj_2").first()
        assertEquals("subj_2 应有 1 个根章节", 1, roots2.size)
        assertEquals("ch_root_2", roots2[0].id)
    }

    /**
     * 场景 3：observeChildren 返回直接子章节（不含孙章节）。
     */
    @Test
    fun `observeChildren 返回直接子章节`() = runTest {
        val children = repository.observeChildren("ch_root_1").first()
        assertEquals("ch_root_1 应有 2 个直接子章节", 2, children.size)
        assertEquals("sortOrder=1 在前", "ch_child_1a", children[0].id)
        assertEquals("sortOrder=2 在后", "ch_child_1b", children[1].id)
    }

    /**
     * 场景 4（关键）：observeTree 递归 CTE 返回整棵子树（含根 + 所有后代）。
     *
     * 这是 ADR-001 B1.1 的核心验证点：WITH RECURSIVE 在 Room + SQLite 运行时正确工作。
     */
    @Test
    fun `observeTree 递归返回整棵子树`() = runTest {
        val tree = repository.observeTree("ch_root_1").first()
        // ch_root_1 + ch_child_1a + ch_grand_1a1 + ch_child_1b = 4 节点
        assertEquals("ch_root_1 子树应有 4 节点（含根）", 4, tree.size)
        val ids = tree.map { it.id }.toSet()
        assertTrue("含根 ch_root_1", ids.contains("ch_root_1"))
        assertTrue("含子 ch_child_1a", ids.contains("ch_child_1a"))
        assertTrue("含孙 ch_grand_1a1", ids.contains("ch_grand_1a1"))
        assertTrue("含子 ch_child_1b", ids.contains("ch_child_1b"))
    }

    /**
     * 场景 5：observeTree 对叶子节点返回单元素列表。
     */
    @Test
    fun `observeTree 叶子节点返回单元素`() = runTest {
        val tree = repository.observeTree("ch_grand_1a1").first()
        assertEquals("叶子节点子树应只有自身", 1, tree.size)
        assertEquals("ch_grand_1a1", tree[0].id)
    }

    /**
     * 场景 6：observeKnowledgePointsByChapter 返回章节下知识点。
     */
    @Test
    fun `observeKnowledgePointsByChapter 返回章节下知识点`() = runTest {
        val points = repository.observeKnowledgePointsByChapter("ch_grand_1a1").first()
        assertEquals("ch_grand_1a1 应有 1 个知识点", 1, points.size)
        assertEquals("point_1", points[0].id)
        assertEquals("关雎", points[0].title)
    }

    /**
     * 场景 7：countNonRootChapters 返回非根章节数（FF4 健身函数）。
     *
     * 当前 fixture：ch_child_1a + ch_grand_1a1 + ch_child_1b = 3 个非根章节。
     */
    @Test
    fun `countNonRootChapters 返回非根章节数`() = runTest {
        val count = repository.countNonRootChapters()
        assertEquals("应有 3 个非根章节", 3, count)
    }

    private fun knowledgePoint(id: String, chapterId: String, title: String) = KnowledgePointEntity(
        id = id,
        chapterId = chapterId,
        title = title,
        summary = null,
        coreConclusion = "",
        fullContent = "",
        multiPerspectives = null,
        relatedIds = null,
        contrastIds = null,
        extensionIds = null,
        examRecords = null,
        examFrequency = "MEDIUM",
        termTemplate = null,
        tags = null,
        difficulty = 3,
        createdAt = 1_000_000L,
        updatedAt = 1_000_000L,
        contentSource = null,
        ocrStatus = "VERIFIED",
        sourceFile = null,
        sourcePage = null,
        studyText = null,
    )
}
