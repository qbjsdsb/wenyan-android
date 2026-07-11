package com.wenyan.app.feature.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.data.repository.GraphRepository
import com.wenyan.app.core.database.entity.GraphEdgeEntity
import com.wenyan.app.core.database.entity.GraphNodeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * 知识图谱模块 ViewModel。
 *
 * 管理 UI 状态：图谱节点 + 边。
 *
 * 数据来源：[GraphRepository.getAllNodes] 和 [GraphRepository.getAllEdges]
 * 返回 Room 观察流，数据库变更时自动更新。
 *
 * 可提取性（retrievability）当前设为 0f 占位。逐节点调用
 * [GraphRepository.getRetrievability] 会导致 N+1 查询，
 * 正确方案是在 DAO 层新增 JOIN 查询（graph_nodes JOIN memo_records）
 * 一次性返回带 R 值的节点列表，属于独立优化 Task。
 */
@HiltViewModel
class GraphViewModel @Inject constructor(
    private val graphRepository: GraphRepository,
) : ViewModel() {

    /**
     * 图谱 UI 状态。
     *
     * 合并节点流与边流，数据库变更时自动刷新。
     */
    val uiState: StateFlow<GraphUiState> = combine(
        graphRepository.getAllNodes(),
        graphRepository.getAllEdges(),
    ) { nodes, edges ->
        GraphUiState(
            isLoading = false,
            nodes = nodes.map { it.toUiItem() },
            edges = edges.map { it.toUiItem() },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = GraphUiState(isLoading = true),
    )

    /** 将 [GraphNodeEntity] 映射为 UI 层 [GraphNodeItem] */
    private fun GraphNodeEntity.toUiItem(): GraphNodeItem = GraphNodeItem(
        id = id,
        label = label,
        retrievability = 0f, // TODO: 批量计算R值（需DAO层JOIN查询避免N+1）
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
)

// 图谱节点项（UI 层模型）
data class GraphNodeItem(
    val id: String,
    val label: String,
    val retrievability: Float,
)

// 图谱边项（UI 层模型）
data class GraphEdgeItem(
    val fromId: String,
    val toId: String,
    val relation: String,
)
