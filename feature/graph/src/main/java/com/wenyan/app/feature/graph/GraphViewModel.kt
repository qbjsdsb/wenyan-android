package com.wenyan.app.feature.graph

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.GraphRepository
import com.wenyan.app.core.data.repository.NodeWithRetrievability
import com.wenyan.app.core.database.dao.SubjectDao
import com.wenyan.app.core.database.entity.GraphEdgeEntity
import com.wenyan.app.core.database.entity.SubjectEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 知识图谱模块 ViewModel（v0.8.0 重构：学习导向 + 核心节点策略）。
 *
 * ## v0.8.0 设计理念
 *
 * 基于认知负荷理论与概念图研究（Mayer, Sweller, Novak）：
 * - **默认 12-50 节点**：研究表明首屏 12-20 节点最易理解，最多不超过 50
 * - **核心节点优先**：默认只显示"核心概念 + 高频考点 + 桥接节点"
 * - **三档显示范围**：核心 / 重要 / 全部，渐进式展开
 * - **简化视觉编码**：颜色=掌握度，尺寸=重要性，描边=类型（仅此三层）
 *
 * ## 核心节点判定
 *
 * 满足以下任一条件即为核心节点：
 * 1. `sourceKpIds.size >= 4`：关联 4+ 知识点，核心概念（如鲁迅、诗经）
 * 2. `examFrequency in [HIGH, MEDIUM]`：高频考点
 * 3. `degree >= 3`：有 3+ 条边连接，桥接节点（bridging nodes）
 *
 * 实测：从 2266 → 约 40-60 节点，符合认知负荷理论。
 *
 * ## 筛选流水线
 *
 * 1. 显示范围（CORE / IMPORTANT / ALL）：核心节点策略
 * 2. 类型筛选：selectedTypes 非空时过滤
 * 3. 科目筛选：selectedSubjectId 非空时精确匹配
 * 4. 薄弱筛选：showWeakOnly=true 时只留 R ∈ (0, 0.5)
 * 5. 搜索筛选：searchQuery 非空时按 label 模糊匹配
 * 6. 聚焦模式：focusedNodeId 非空时只显示 N 跳邻居 + 自身
 * 7. 边过滤：只保留两端节点都在筛选结果中的边
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GraphViewModel @Inject constructor(
    private val graphRepository: GraphRepository,
    private val subjectDao: SubjectDao,
) : ViewModel() {

    /** 重试触发器 */
    private val _retryTrigger = MutableStateFlow(0)

    // ── 筛选状态 ──
    /** 选中的节点类型（空 = 全部） */
    private val _selectedTypes = MutableStateFlow<Set<String>>(emptySet())
    val selectedTypes: StateFlow<Set<String>> = _selectedTypes.asStateFlow()

    /** 选中的科目 ID（null = 全部科目） */
    private val _selectedSubjectId = MutableStateFlow<String?>(null)
    val selectedSubjectId: StateFlow<String?> = _selectedSubjectId.asStateFlow()

    /** 是否只显示薄弱节点（R < 0.5 且 R > 0） */
    private val _showWeakOnly = MutableStateFlow(false)
    val showWeakOnly: StateFlow<Boolean> = _showWeakOnly.asStateFlow()

    /** 搜索关键词（空 = 不搜索） */
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    /** 聚焦节点 ID（null = 不聚焦，显示全部） */
    private val _focusedNodeId = MutableStateFlow<String?>(null)
    val focusedNodeId: StateFlow<String?> = _focusedNodeId.asStateFlow()

    /** 聚焦跳数（默认 2 跳邻居） */
    private val _focusHops = MutableStateFlow(2)

    /**
     * 显示范围（v0.8.0 新增：替代 showAllNodes 二元开关）。
     *
     * - CORE（默认）：核心节点（sourceKpIds>=4 或 高频考点 或 degree>=3），约 40-60 节点
     * - IMPORTANT：重要节点（sourceKpIds>=2 或 有边连接），约 200-400 节点
     * - ALL：全部节点（含边缘实体），约 2000+ 节点
     */
    private val _displayScope = MutableStateFlow(DisplayScope.CORE)
    val displayScope: StateFlow<DisplayScope> = _displayScope.asStateFlow()

    /**
     * 布局模式（v0.8.0 新增：三模式可切换）。
     *
     * - TIMELINE（默认）：文学史时间轴泳道布局，建立文学史脉络
     * - NEIGHBORHOOD：邻域力导向布局，深挖某节点关系（Obsidian Local Graph 范式）
     * - RADIAL：径向科目概览，鸟瞰科目全局
     */
    private val _layoutMode = MutableStateFlow(LayoutMode.TIMELINE)
    val layoutMode: StateFlow<LayoutMode> = _layoutMode.asStateFlow()

    /** UI 状态 */
    private val _uiState = MutableStateFlow<GraphUiState>(GraphUiState(isLoading = true))
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    /**
     * 知识点标题映射（v0.8.1 新增：供 NodeDetailSheet 显示标题而非 UUID）。
     *
     * key = 知识点 ID（如 "kp_00613"），value = 知识点标题（如"《呐喊》与新小说的奠基"）。
     * 当 [uiState] 的节点列表变化时，收集所有 sourceKpIds + relatedPointId，
     * 批量查询标题并更新此 StateFlow。
     */
    private val _knowledgePointTitles = MutableStateFlow<Map<String, String>>(emptyMap())
    val knowledgePointTitles: StateFlow<Map<String, String>> = _knowledgePointTitles.asStateFlow()

    /** 科目列表（供筛选栏显示） */
    val subjects: StateFlow<List<SubjectEntity>> = subjectDao.observeAll()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 筛选状态聚合（解决 combine 最多 5 参数限制）。
     *
     * 将 6 个筛选 StateFlow 合并为单一 FilterState Flow，
     * 再与 nodes/edges/layoutMode（3 个 Flow）合并，共 4 个 Flow 进入 combine。
     */
    private data class FilterState(
        val types: Set<String>,
        val subjectId: String?,
        val weakOnly: Boolean,
        val query: String,
        val focusedId: String?,
        val scope: DisplayScope,
    )

    /** 聚合筛选状态 Flow（任意筛选变化即重发） */
    private val filterState: Flow<FilterState> = combine(
        _selectedTypes,
        _selectedSubjectId,
        _showWeakOnly,
        _searchQuery,
        _focusedNodeId,
    ) { types, subjectId, weakOnly, query, focusedId ->
        FilterState(types, subjectId, weakOnly, query, focusedId, _displayScope.value)
    }.combine(_displayScope) { fs, scope -> fs.copy(scope = scope) }

    init {
        viewModelScope.launch {
            _retryTrigger
                .flatMapLatest {
                    combine(
                        graphRepository.getNodesWithRetrievability(),
                        graphRepository.getAllEdges(),
                        filterState,
                        _layoutMode,
                    ) { nodesWithR, edges, fs, layoutMode ->
                        buildUiState(
                            nodesWithR = nodesWithR,
                            edges = edges,
                            selectedTypes = fs.types,
                            selectedSubjectId = fs.subjectId,
                            showWeakOnly = fs.weakOnly,
                            searchQuery = fs.query,
                            focusedNodeId = fs.focusedId,
                            displayScope = fs.scope,
                            layoutMode = layoutMode,
                        )
                    }
                }
                .catch { e ->
                    emit(GraphUiState(error = e.message ?: "加载失败"))
                }
                .collect { _uiState.value = it }
        }

        // v0.8.1 新增：当 UI 节点列表变化时，批量加载关联知识点标题。
        // 替代 NodeDetailSheet 直接显示 UUID 的糟糕体验。
        viewModelScope.launch {
            _uiState
                .map { state ->
                    buildSet {
                        state.nodes.forEach { node ->
                            node.relatedPointId?.let { add(it) }
                            node.sourceKpIds.forEach { add(it) }
                        }
                    }.toList()
                }
                .distinctUntilChanged()
                .collect { ids ->
                    if (ids.isEmpty()) {
                        _knowledgePointTitles.value = emptyMap()
                        return@collect
                    }
                    // 增量合并：已缓存的不再重复查询
                    val cached = _knowledgePointTitles.value
                    val missing = ids.filter { it !in cached }
                    if (missing.isEmpty()) return@collect
                    // v0.8.2 修复：分批查询，避免 SQLite IN 子句 999 参数限制
                    val newTitles = missing.chunked(900).flatMap { batch ->
                        graphRepository.getKnowledgePointTitles(batch).toList()
                    }.toMap()
                    _knowledgePointTitles.value = cached + newTitles
                }
        }
    }

    /**
     * 构建筛选后的 UI 状态（v0.8.0 重构）。
     *
     * 筛选流水线：
     * 1. 基础转换：NodeWithRetrievability → GraphNodeItem
     * 2. 显示范围筛选（CORE / IMPORTANT / ALL）
     * 3. 类型筛选
     * 4. 科目筛选
     * 5. 薄弱筛选
     * 6. 搜索筛选
     * 7. 聚焦模式（NEIGHBORHOOD 模式强制需要聚焦节点）
     * 8. 边过滤
     * 9. 节点统计
     */
    private fun buildUiState(
        nodesWithR: List<NodeWithRetrievability>,
        edges: List<GraphEdgeEntity>,
        selectedTypes: Set<String>,
        selectedSubjectId: String?,
        showWeakOnly: Boolean,
        searchQuery: String,
        focusedNodeId: String?,
        displayScope: DisplayScope,
        layoutMode: LayoutMode,
    ): GraphUiState {
        // 1. 基础转换
        val allNodes = nodesWithR.map { it.toUiItem() }

        // 2. 显示范围筛选（v0.8.0 核心：控制节点数量在认知负荷范围内）
        val nodeDegree = mutableMapOf<String, Int>()
        for (edge in edges) {
            nodeDegree[edge.sourceId] = (nodeDegree[edge.sourceId] ?: 0) + 1
            nodeDegree[edge.targetId] = (nodeDegree[edge.targetId] ?: 0) + 1
        }

        val filteredNodes = when (displayScope) {
            DisplayScope.CORE -> {
                // 核心节点：sourceKpIds>=4 或 高频考点 或 degree>=3
                allNodes.filter { node ->
                    node.sourceKpIds.size >= 4 ||
                        node.examFrequency == "HIGH" ||
                        node.examFrequency == "MEDIUM" ||
                        (nodeDegree[node.id] ?: 0) >= 3
                }
            }
            DisplayScope.IMPORTANT -> {
                // 重要节点：sourceKpIds>=2 或 有边连接
                val connectedIds = mutableSetOf<String>()
                for (edge in edges) {
                    connectedIds.add(edge.sourceId)
                    connectedIds.add(edge.targetId)
                }
                allNodes.filter { node ->
                    node.sourceKpIds.size >= 2 || node.id in connectedIds
                }
            }
            DisplayScope.ALL -> allNodes
        }

        // 3. 类型筛选
        var resultNodes = if (selectedTypes.isNotEmpty()) {
            filteredNodes.filter { it.type in selectedTypes }
        } else filteredNodes

        // 4. 科目筛选
        if (selectedSubjectId != null) {
            resultNodes = resultNodes.filter { it.subjectId == selectedSubjectId }
        }

        // 5. 薄弱筛选
        if (showWeakOnly) {
            resultNodes = resultNodes.filter {
                it.retrievability > 0f && it.retrievability < 0.5f
            }
        }

        // 6. 搜索筛选
        if (searchQuery.isNotBlank()) {
            val queryLower = searchQuery.lowercase().trim()
            resultNodes = resultNodes.filter {
                it.label.lowercase().contains(queryLower) ||
                    (it.subtitle?.lowercase()?.contains(queryLower) == true)
            }
        }

        // 7. 聚焦模式
        // NEIGHBORHOOD 模式必须有聚焦节点，否则自动选度数最大的节点
        val effectiveFocusedId = when {
            focusedNodeId != null -> focusedNodeId
            layoutMode == LayoutMode.NEIGHBORHOOD && resultNodes.isNotEmpty() -> {
                resultNodes.maxByOrNull { nodeDegree[it.id] ?: 0 }?.id
            }
            else -> null
        }
        if (effectiveFocusedId != null && layoutMode == LayoutMode.NEIGHBORHOOD) {
            val focusHops = _focusHops.value
            val neighbors = findNeighbors(effectiveFocusedId, edges, focusHops)
            resultNodes = resultNodes.filter { it.id in neighbors }
        } else if (focusedNodeId != null) {
            // 其他模式下，用户主动聚焦时也过滤到邻居
            val focusHops = _focusHops.value
            val neighbors = findNeighbors(focusedNodeId, edges, focusHops)
            resultNodes = resultNodes.filter { it.id in neighbors }
        }

        // 8. 边过滤
        val nodeIdSet = resultNodes.map { it.id }.toSet()
        val filteredEdges = edges
            .map { it.toUiItem() }
            .filter { it.fromId in nodeIdSet && it.toId in nodeIdSet }

        // 9. 统计
        val weakCount = resultNodes.count { it.retrievability > 0f && it.retrievability < 0.5f }
        val masteredCount = resultNodes.count { it.retrievability >= 0.8f }
        val unlearnedCount = resultNodes.count { it.retrievability == 0f }

        // 10. 搜索高亮集合
        val highlightedIds = if (searchQuery.isNotBlank()) {
            resultNodes.map { it.id }.toSet()
        } else emptySet()

        return GraphUiState(
            isLoading = false,
            nodes = resultNodes,
            edges = filteredEdges,
            totalCount = nodesWithR.size,
            highlightedNodeIds = highlightedIds,
            focusedNodeId = effectiveFocusedId ?: focusedNodeId,
            weakCount = weakCount,
            masteredCount = masteredCount,
            unlearnedCount = unlearnedCount,
            layoutMode = layoutMode,
        )
    }

    /**
     * 查找节点的 N 跳邻居（含自身）。
     *
     * BFS 遍历，无向图（边双向）。
     */
    private fun findNeighbors(
        startNodeId: String,
        edges: List<GraphEdgeEntity>,
        hops: Int,
    ): Set<String> {
        if (hops <= 0) return setOf(startNodeId)

        val adjacency = mutableMapOf<String, MutableList<String>>()
        for (edge in edges) {
            adjacency.getOrPut(edge.sourceId) { mutableListOf() }.add(edge.targetId)
            adjacency.getOrPut(edge.targetId) { mutableListOf() }.add(edge.sourceId)
        }

        val visited = mutableSetOf(startNodeId)
        val queue = ArrayDeque<Pair<String, Int>>()
        queue.add(startNodeId to 0)

        while (queue.isNotEmpty()) {
            val (current, depth) = queue.removeFirst()
            if (depth >= hops) continue
            val neighbors = adjacency[current] ?: emptyList()
            for (neighbor in neighbors) {
                if (neighbor !in visited) {
                    visited.add(neighbor)
                    queue.add(neighbor to depth + 1)
                }
            }
        }
        return visited
    }

    /** 重试加载 */
    fun retry() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        _retryTrigger.value++
    }

    // ── 筛选操作 ──

    /** 切换节点类型筛选 */
    fun toggleTypeFilter(type: String) {
        _selectedTypes.value = _selectedTypes.value.toMutableSet().apply {
            if (contains(type)) remove(type) else add(type)
        }
    }

    /** 清空类型筛选 */
    fun clearTypeFilter() {
        _selectedTypes.value = emptySet()
    }

    /** 设置科目筛选（null = 全部） */
    fun setSubjectFilter(subjectId: String?) {
        _selectedSubjectId.value = subjectId
    }

    /** 切换薄弱节点筛选 */
    fun toggleWeakOnly() {
        _showWeakOnly.value = !_showWeakOnly.value
    }

    /** 设置搜索关键词 */
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    /** 设置聚焦节点 */
    fun setFocusedNode(nodeId: String?) {
        _focusedNodeId.value = nodeId
    }

    /** 设置聚焦跳数 */
    fun setFocusHops(hops: Int) {
        _focusHops.value = hops.coerceIn(1, 3)
    }

    /** 设置显示范围（v0.8.0 新增） */
    fun setDisplayScope(scope: DisplayScope) {
        _displayScope.value = scope
    }

    /**
     * 设置布局模式（v0.8.0 新增）。
     *
     * 切换到 TIMELINE/RADIAL 时清除聚焦（这两模式为全局视图）。
     * 切换到 NEIGHBORHOOD 时保留聚焦，若未聚焦则由 buildUiState 自动选度数最大节点。
     */
    fun setLayoutMode(mode: LayoutMode) {
        if (mode != LayoutMode.NEIGHBORHOOD) {
            _focusedNodeId.value = null
        }
        _layoutMode.value = mode
    }

    /** 清空所有筛选 */
    fun clearAllFilters() {
        _selectedTypes.value = emptySet()
        _selectedSubjectId.value = null
        _showWeakOnly.value = false
        _searchQuery.value = ""
        _focusedNodeId.value = null
    }

    /** 将 [NodeWithRetrievability] 映射为 UI 层 [GraphNodeItem] */
    private fun NodeWithRetrievability.toUiItem(): GraphNodeItem {
        val sourceKpIds = node.metadata?.get("sourceKpIds")
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList()

        val examFrequency = node.metadata?.get("examFrequency") ?: "NEVER"

        return GraphNodeItem(
            id = node.id,
            label = node.label,
            retrievability = retrievability,
            relatedPointId = node.relatedPointId,
            color = node.color,
            type = node.type,
            subtitle = node.subtitle,
            metadata = node.metadata,
            subjectId = node.subjectId,
            examFrequency = examFrequency,
            sourceKpIds = sourceKpIds,
        )
    }

    /** 将 [GraphEdgeEntity] 映射为 UI 层 [GraphEdgeItem] */
    private fun GraphEdgeEntity.toUiItem(): GraphEdgeItem = GraphEdgeItem(
        fromId = sourceId,
        toId = targetId,
        relation = type,
    )
}

/**
 * 显示范围（v0.8.0 新增）。
 *
 * 控制 GraphCanvas 默认显示的节点数量，避免认知超载：
 * - CORE：核心节点（40-60），适合首次浏览与全局理解
 * - IMPORTANT：重要节点（200-400），适合科目内深入
 * - ALL：全部节点（2000+），适合搜索特定实体
 */
enum class DisplayScope(val displayName: String, val description: String) {
    CORE("核心", "核心概念 + 高频考点 + 桥接节点（40-60）"),
    IMPORTANT("重要", "关联多个知识点或有连接的节点（200-400）"),
    ALL("全部", "全部节点含边缘实体（2000+）"),
}

/**
 * 布局模式（v0.8.0 新增：三模式可切换）。
 *
 * 每种模式对应不同的学习任务（详见调研报告 §4）：
 * - TIMELINE：文学史时间轴泳道布局（默认），建立文学史脉络
 *   横轴=时间（1915-2030），纵轴=5 泳道（流派/小说/诗歌/散文/戏剧）
 * - NEIGHBORHOOD：邻域力导向布局，深挖某节点关系（Obsidian Local Graph 范式）
 *   以聚焦节点为中心，1-3 跳邻居用力导向算法布局
 * - RADIAL：径向科目概览，鸟瞰科目全局
 *   按 subjectId 分扇区，扇区内按 type 分子扇区
 */
enum class LayoutMode(val displayName: String, val description: String) {
    TIMELINE("时间轴", "文学史时间轴 · 横轴年份 · 纵轴体裁泳道"),
    NEIGHBORHOOD("邻域", "聚焦节点 · 1-3 跳邻居力导向布局"),
    RADIAL("概览", "径向科目扇区 · 鸟瞰全局分布"),
}

// 图谱 UI 状态（v0.8.0 扩展：掌握度统计 + 布局模式）
data class GraphUiState(
    val isLoading: Boolean = false,
    val nodes: List<GraphNodeItem> = emptyList(),
    val edges: List<GraphEdgeItem> = emptyList(),
    val error: String? = null,
    val totalCount: Int = 0,
    val highlightedNodeIds: Set<String> = emptySet(),
    val focusedNodeId: String? = null,
    /** 薄弱节点数（R ∈ (0, 0.5)） */
    val weakCount: Int = 0,
    /** 已掌握节点数（R >= 0.8） */
    val masteredCount: Int = 0,
    /** 未学习节点数（R = 0） */
    val unlearnedCount: Int = 0,
    /** 当前布局模式（v0.8.0 新增） */
    val layoutMode: LayoutMode = LayoutMode.TIMELINE,
)

// 图谱节点项（UI 层模型）
@Immutable
data class GraphNodeItem(
    val id: String,
    val label: String,
    val retrievability: Float,
    val relatedPointId: String? = null,
    val color: Int = 0,
    val type: String = "",
    val subtitle: String? = null,
    val metadata: Map<String, String>? = null,
    val subjectId: String? = null,
    val examFrequency: String = "NEVER",
    val sourceKpIds: List<String> = emptyList(),
)

// 图谱边项（UI 层模型）
@Immutable
data class GraphEdgeItem(
    val fromId: String,
    val toId: String,
    val relation: String,
)
