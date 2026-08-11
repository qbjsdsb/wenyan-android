package com.wenyan.app.feature.knowledge

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.wenyan.app.core.database.dao.WritingMaterialDao
import com.wenyan.app.core.database.entity.WritingMaterialWithSources
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.MaxContentWidth
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingMaterialListScreen(
    onBack: () -> Unit,
    onStartWriting: (String?) -> Unit = { _ -> },
    viewModel: WritingMaterialListViewModel = hiltViewModel(),
) {
    val materials by viewModel.materials.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )
    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "写作素材",
                subtitle = "${materials.size} 条 · 只读来源核对",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = MaxContentWidth.comfortable),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
            item {
                Button(
                    onClick = { onStartWriting(null) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("开始离线写作") }
            }
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
                        OutlinedButton(
                            onClick = { onStartWriting(material.id) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("用此素材开始写作")
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
