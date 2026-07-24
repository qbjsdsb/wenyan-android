package com.wenyan.app.feature.graph

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.GraphRepository
import com.wenyan.app.core.data.repository.NodeWithRetrievability
import com.wenyan.app.core.database.entity.GraphEdgeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 知识图谱模块 ViewModel。
 *
 * 管理 UI 状态：图谱节点（含 R 值）+ 边。
 *
 * 数据来源：
 * - [GraphRepository.getNodesWithRetrievability]：批量返回节点 + 可提取性 R（阶段3接通）
 * - [GraphRepository.getAllEdges]：图谱边
 *
 * R 值通过 combine(observeAll nodes, observeAll memos) 批量计算，
 * 评分后 memo_records 变更时 R 值自动刷新。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class GraphViewModel @Inject constructor(
    private val graphRepository: GraphRepository,
) : ViewModel() {

    /**
     * 重试触发器（P0-6 新增）。点击重试时自增，[flatMapLatest] 会重新订阅数据流。
     */
    private val _retryTrigger = MutableStateFlow(0)

    /**
     * 图谱 UI 状态（P1-4 改造为 MutableStateFlow 包装）。
     *
     * 合并节点流（含 R 值）与边流，数据库变更时自动刷新。
     *
     * P0-6 修复：加 [catch] 捕获数据流异常，避免异常冒泡导致 app 崩溃。
     *
     * P1-4 修复：原 [stateIn] 模式 retry() 后 UI 仍显示旧 error 状态无 loading 反馈，
     * 现改为 MutableStateFlow + [collect]，retry() 可立即设置 isLoading=true。
     */
    private val _uiState = MutableStateFlow<GraphUiState>(GraphUiState(isLoading = true))
    val uiState: StateFlow<GraphUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _retryTrigger
                .flatMapLatest {
                    combine(
                        graphRepository.getNodesWithRetrievability(),
                        graphRepository.getAllEdges(),
                    ) { nodesWithR, edges ->
                        GraphUiState(
                            isLoading = false,
                            nodes = nodesWithR.map { it.toUiItem() },
                            edges = edges.map { it.toUiItem() },
                        )
                    }
                }
                .catch { e ->
                    emit(GraphUiState(error = e.message ?: "加载失败"))
                }
                .collect { _uiState.value = it }
        }
    }

    /**
     * 重试加载（P0-6 新增，P1-4 增强）。
     *
     * P1-4 修复：先立即设置 isLoading=true 并清空 error，再触发数据流重新订阅。
     */
    fun retry() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        _retryTrigger.value++
    }

    /** 将 [NodeWithRetrievability] 映射为 UI 层 [GraphNodeItem] */
    private fun NodeWithRetrievability.toUiItem(): GraphNodeItem = GraphNodeItem(
        id = node.id,
        label = node.label,
        retrievability = retrievability,
        relatedPointId = node.relatedPointId,
        // v0.7.4 修复：传递实体颜色 + 类型，让 Canvas 启用分类色（作家粉/流派紫/体裁蓝/时段绿）
        // 并按类型分组布局。原实现丢弃这两字段，Canvas 退化为单圆周布局 + 仅按 R 值着色，
        // 33 节点全部挤在一个圆上，边交叉严重、视觉杂乱（用户反馈"看不清"）。
        color = node.color,
        type = node.type,
        subtitle = node.subtitle,
        // v0.7.6 新增：传递 metadata，让 Canvas 时间轴布局读取 birthYear/deathYear/year/
        // startYear/endYear 等时间字段，按文学史时间轴定位节点横轴。
        metadata = node.metadata,
    )

    /** 将 [GraphEdgeEntity] 映射为 UI 层 [GraphEdgeItem] */
    private fun GraphEdgeEntity.toUiItem(): GraphEdgeItem = GraphEdgeItem(
        fromId = sourceId,
        toId = targetId,
        relation = type,
    )
}

// 图谱 UI 状态
data class GraphUiState(
    val isLoading: Boolean = false,
    val nodes: List<GraphNodeItem> = emptyList(),
    val edges: List<GraphEdgeItem> = emptyList(),
    /** 加载失败时的错误信息（P0-6 新增） */
    val error: String? = null,
)

// 图谱节点项（UI 层模型）
@Immutable
data class GraphNodeItem(
    val id: String,
    val label: String,
    val retrievability: Float,
    /** 关联知识点 ID,非空时点击节点可跳转知识点详情 */
    val relatedPointId: String? = null,
    /**
     * 实体颜色（ARGB Int）。v0.7.4 新增：用于按分类着色（作家粉/流派紫/体裁蓝/时段绿/作品橙）。
     * 0 表示未设置，Canvas 退化为按 R 值着色。
     */
    val color: Int = 0,
    /**
     * 节点类型（GraphNodeType.name）：AUTHOR / WORK / SCHOOL / MOVEMENT / CONCEPT / KNOWLEDGE_POINT。
     * v0.7.4 新增：用于分组布局（同类型节点聚集在同一扇区）。
     */
    val type: String = "",
    /** 副标题（如生卒年/流派年代），v0.7.4 新增：节点选中时可展示更多上下文 */
    val subtitle: String? = null,
    /**
     * 节点元数据（v0.7.6 新增）。
     *
     * 用于时间轴布局：Canvas 从此字段读取 [GraphSkeleton.META_KEY_BIRTH_YEAR] /
     * [GraphSkeleton.META_KEY_DEATH_YEAR]（作家生卒年）/ [GraphSkeleton.META_KEY_YEAR]
     * （流派年代）/ [GraphSkeleton.META_KEY_START_YEAR] / [GraphSkeleton.META_KEY_END_YEAR]
     * （时段起止年）等时间字段，计算节点在横轴（时间轴）上的位置。
     *
     * 也用于布局分组：metadata["dimension"]="genre" 表示体裁节点、"dimension"="period" 表示时段节点，
     * Canvas 据此分配泳道。
     */
    val metadata: Map<String, String>? = null,
)

// 图谱边项（UI 层模型）
@Immutable
data class GraphEdgeItem(
    val fromId: String,
    val toId: String,
    val relation: String,
)
