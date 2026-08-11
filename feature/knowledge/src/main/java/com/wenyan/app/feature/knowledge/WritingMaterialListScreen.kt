package com.wenyan.app.feature.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.database.dao.WritingMaterialDao
import com.wenyan.app.core.database.entity.WritingMaterialWithSources
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.ProvenanceBadge
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.TonalCardLow
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar
import com.wenyan.app.core.designsystem.component.sourceEvidenceLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class WritingMaterialListViewModel @Inject constructor(
    writingMaterialDao: WritingMaterialDao,
) : ViewModel() {
    val materials: StateFlow<List<WritingMaterialWithSources>> = writingMaterialDao.observeAllWithSources()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

/** Read-only provenance entry; editing is deliberately deferred to the writing workbench. */
@Composable
fun WritingMaterialListScreen(
    onBack: () -> Unit,
    onStartWriting: () -> Unit = {},
    viewModel: WritingMaterialListViewModel = hiltViewModel(),
) {
    val materials by viewModel.materials.collectAsStateWithLifecycle()
    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "写作素材",
                subtitle = "${materials.size} 条 · 只读来源核对",
                onBack = onBack,
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item { androidx.compose.material3.Button(onClick = onStartWriting, modifier = Modifier.fillMaxWidth()) { androidx.compose.material3.Text("开始离线写作") } }
            items(materials, key = { it.material.id }) { item ->
                val material = item.material
                TonalCardLow {
                    Column(
                        modifier = Modifier.padding(Spacing.lg),
                        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                    ) {
                        Text(
                            material.title?.takeIf(String::isNotBlank) ?: material.subCategory ?: material.category,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        ProvenanceBadge(material.contentStatus)
                        Text(material.content, style = MaterialTheme.typography.bodyMedium)
                        val sources = item.sources.map { source ->
                            com.wenyan.app.core.designsystem.component.ProvenanceSourceUiModel(
                                title = source.sourceTitle?.takeIf(String::isNotBlank) ?: source.sourceFile,
                                evidenceStatus = source.sourceStatus,
                                edition = source.sourceEdition,
                                pageStart = source.sourcePageStart ?: source.sourcePage,
                                pageEnd = source.sourcePageEnd ?: source.sourcePage,
                                reviewNote = source.reviewNote,
                            )
                        }
                        if (sources.isNotEmpty()) {
                            com.wenyan.app.core.designsystem.component.SourceSection(sources, title = "来源")
                        } else {
                            visibleWritingMaterialSource(material.source)?.let { source ->
                                Text(
                                    "来源：$source · ${sourceEvidenceLabel("UNKNOWN")}",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun visibleWritingMaterialSource(source: String?): String? = source
    ?.trim()
    ?.takeIf { it.isNotEmpty() && it !in setOf("其他", "未知", "待补", "无", "N/A") }
