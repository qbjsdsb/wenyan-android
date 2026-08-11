package com.wenyan.app.feature.knowledge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import androidx.hilt.navigation.compose.hiltViewModel
import com.wenyan.app.core.data.repository.WritingEvidenceItem
import com.wenyan.app.core.data.writing.*
import com.wenyan.app.core.database.entity.ContentReviewStatus
import com.wenyan.app.core.database.entity.WritingSessionEntity

@Composable
fun WritingEditorRoute(onBack: () -> Unit, viewModel: WritingEditorViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(state.session?.state) {
        while (state.session?.state == "RUNNING") { delay(1_000); viewModel.refreshElapsed() }
    }
    WritingEditorScreen(
        state = state,
        onBack = { viewModel.flushAndThen(onBack) },
        onEdit = { field, value ->
            viewModel.edit {
                when (field) {
                    "analysis" -> it.copy(promptAnalysis = value)
                    "thesis" -> it.copy(thesis = value)
                    "outline" -> it.copy(outlineJson = value)
                    else -> it.copy(body = value)
                }
            }
        },
        onRetry = viewModel::retrySave,
        onRate = viewModel::rate,
        onNote = viewModel::note,
        onMode = viewModel::selectMode,
        onPause = viewModel::pause,
        onResume = viewModel::resume,
        onDiscard = viewModel::discard,
        onEvidence = viewModel::toggleEvidence,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritingEditorScreen(
    state: WritingEditorState,
    onBack: () -> Unit,
    onEdit: (String, String) -> Unit,
    onRetry: () -> Unit,
    onRate: (RubricDimension, RubricLevel) -> Unit,
    onNote: (RubricDimension, String) -> Unit,
    onMode: (WritingMode) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    onEvidence: (WritingEvidenceItem) -> Unit,
) {
    var confirmDiscard by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("离线写作") },
                navigationIcon = { TextButton(onClick = onBack) { Text("返回") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val session = state.session
            if (session == null) {
                if (state.saveError == null) CircularProgressIndicator() else ErrorAndRetry(state.saveError, onRetry)
            } else {
                Text(session.promptSnapshot, style = MaterialTheme.typography.titleMedium)
                Text("已用时 ${formatElapsed(state.elapsedMs)}", style = MaterialTheme.typography.labelLarge)
                ModeSelector(session.mode, onMode)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = if (session.state == "PAUSED") onResume else onPause) {
                        Text(if (session.state == "PAUSED") "恢复" else "暂停")
                    }
                    OutlinedButton(onClick = { confirmDiscard = true }) { Text("放弃") }
                }
                EditorField("审题", session.promptAnalysis) { onEdit("analysis", it) }
                EditorField("中心论点", session.thesis) { onEdit("thesis", it) }
                EditorField("分论点 / 提纲", session.outlineJson) { onEdit("outline", it) }
                EvidenceSelector(state.evidence, decodeEvidenceRefs(session.evidenceRefsJson), onEvidence)
                EditorField("正文", session.body, 6) { onEdit("body", it) }
                Text(if (state.saving) "正在自动保存…" else "已离线保存")
                state.saveError?.let { ErrorAndRetry(it, onRetry) }
                WritingRubricSection(decodeAssessment(session.selfAssessmentJson), state.trends, onRate, onNote)
            }
        }
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("放弃草稿？") },
            text = { Text("草稿会保留为已放弃记录，不会删除。") },
            confirmButton = { TextButton(onClick = { confirmDiscard = false; onDiscard() }) { Text("确认放弃") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("继续写作") } },
        )
    }
}

@Composable
private fun ModeSelector(selected: String, onMode: (WritingMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        WritingMode.entries.forEach { mode ->
            FilterChip(
                selected = selected == mode.name,
                onClick = { onMode(mode) },
                label = { Text(when (mode) {
                    WritingMode.OUTLINE_10_MIN -> "10分提纲"
                    WritingMode.MICRO_30_MIN -> "30分微写作"
                    WritingMode.FULL_TIMED -> "完整限时"
                }) },
            )
        }
    }
}

@Composable
private fun EvidenceSelector(items: List<WritingEvidenceItem>, selected: List<String>, onEvidence: (WritingEvidenceItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("证据卡", style = MaterialTheme.typography.titleSmall)
        if (items.isEmpty()) Text("暂无可核对素材")
        items.forEach { item ->
            FilterChip(
                selected = item.id in selected,
                enabled = item.isCitable,
                onClick = { onEvidence(item) },
                label = { Text("${item.title ?: item.preview.take(24)} · ${if (item.isCitable) "已审校可引用" else "待核线索"}") },
            )
        }
    }
}

@Composable private fun ErrorAndRetry(message: String, onRetry: () -> Unit) {
    Text("保存失败：$message", color = MaterialTheme.colorScheme.error)
    Button(onClick = onRetry) { Text("重试") }
}
@Composable private fun EditorField(label: String, value: String, lines: Int = 3, onChange: (String) -> Unit) =
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), minLines = lines)
internal fun formatElapsed(value: Long): String = "%02d:%02d".format(value / 60_000, value / 1_000 % 60)

@Preview(showBackground = true, widthDp = 900, heightDp = 500, fontScale = 2f)
@Composable private fun PreviewWritingEditor() {
    MaterialTheme {
        WritingEditorScreen(
            WritingEditorState(session = previewWritingSession(), evidence = listOf(WritingEvidenceItem("legacy", null, "待核素材", ContentReviewStatus.LEGACY_UNVERIFIED, emptyList()))),
            {}, { _, _ -> }, {}, { _, _ -> }, { _, _ -> }, {}, {}, {}, {}, {},
        )
    }
}

private fun previewWritingSession() = WritingSessionEntity(
    "preview", null, null, null, WritingMode.MICRO_30_MIN.name, "610 写作练习",
    "先明确题目限定", "中心论点", "[\"分论点一\",\"分论点二\"]", "[]", "正文草稿",
    "PAUSED", WritingMode.MICRO_30_MIN.durationMs, null, 120_000, 0, 1, null, "", 1, 1,
)
