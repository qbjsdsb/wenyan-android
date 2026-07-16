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
)

// 图谱边项（UI 层模型）
@Immutable
data class GraphEdgeItem(
    val fromId: String,
    val toId: String,
    val relation: String,
)
