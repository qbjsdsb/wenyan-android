package com.wenyan.app.feature.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * 真题练习界面骨架。
 *
 * 布局参考 Web 原型（年份选择 + 题目列表），
 * 采用 LazyRow 渲染可选年份、LazyColumn 渲染题目卡片。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: QuizViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("真题练习") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // 年份选择行
            YearSelector(
                years = uiState.availableYears,
                selectedYear = uiState.selectedYear,
                onYearSelected = viewModel::selectYear,
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.questions.isEmpty()) {
                EmptyState(
                    text = if (uiState.selectedYear == null) {
                        "请选择年份查看真题"
                    } else {
                        "该年份暂无真题数据"
                    },
                )
            } else {
                QuestionList(
                    questions = uiState.questions,
                    contentPadding = PaddingValues(16.dp),
                )
            }
        }
    }
}

// 年份选择行
@Composable
private fun YearSelector(
    years: List<Int>,
    selectedYear: Int?,
    onYearSelected: (Int) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(years) { year ->
            AssistChip(
                onClick = { onYearSelected(year) },
                label = { Text("${year}年") },
                leadingIcon = if (selectedYear == year) {
                    { Text("✓") }
                } else {
                    null
                },
            )
        }
    }
}

// 题目列表
@Composable
private fun QuestionList(
    questions: List<QuizQuestionItem>,
    contentPadding: PaddingValues,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(questions) { question ->
            QuestionCard(question)
        }
    }
}

// 单个题目卡片
@Composable
private fun QuestionCard(question: QuizQuestionItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "${question.year}年 · ${question.subject}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = question.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "题型：${question.questionType}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// 空状态占位
@Composable
private fun EmptyState(text: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
