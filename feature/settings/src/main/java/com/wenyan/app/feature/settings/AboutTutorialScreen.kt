package com.wenyan.app.feature.settings

import androidx.compose.ui.res.stringResource
import com.wenyan.app.feature.settings.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wenyan.app.core.designsystem.component.ExpressiveScaffold
import com.wenyan.app.core.designsystem.component.GroupedCard
import com.wenyan.app.core.designsystem.component.GroupedCardDivider
import com.wenyan.app.core.designsystem.component.GroupedCardItem
import com.wenyan.app.core.designsystem.component.MaxContentWidth
import com.wenyan.app.core.designsystem.component.Spacing
import com.wenyan.app.core.designsystem.component.TonalCard
import com.wenyan.app.core.designsystem.component.WenyanLargeTopAppBar

/**
 * 关于与教程页面（v0.9.6 精简重构）。
 *
 * 设计目标：简洁、层次清晰、竖屏友好。默认视图只展示核心信息，
 * 深度原理用可折叠组件包裹，用户按需展开。
 *
 * 信息架构：
 * 1. 欢迎卡片 — 定位 + 三大理念
 * 2. 快速上手 — 3 步入门
 * 3. 功能模块 — 5 个 Tab 简介
 * 4. 学习原理 — FSRS + 三档记忆（可折叠）
 * 5. 关于 — 技术栈 + 致谢
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutTutorialScreen(
    onBack: () -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(
        state = rememberTopAppBarState(),
    )

    ExpressiveScaffold(
        topBar = {
            WenyanLargeTopAppBar(
                title = "关于与教程",
                subtitle = "理解原理 · 高效备考",
                onBack = onBack,
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(padding),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier.widthIn(max = MaxContentWidth.compact),
                verticalArrangement = Arrangement.spacedBy(Spacing.xl),
                contentPadding = PaddingValues(
                    start = Spacing.lg,
                    end = Spacing.lg,
                    top = Spacing.lg,
                    bottom = Spacing.xxl,
                ),
            ) {
                item { HeroCard() }
                item { SectionQuickStart() }
                item { SectionModules() }
                item { SectionPrinciples() }
                item { SectionAbout() }
            }
        }
    }
}

// ============================================================
// 欢迎卡片
// ============================================================

@Composable
private fun HeroCard() {
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(
                text = "文研",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "南师大文学院现当代文学考研（050106）备考工具",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(
                modifier = Modifier.padding(top = Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                PrincipleRow("以真题为纲", "真题复现率高，贯穿学习全程")
                PrincipleRow("以知识网络为本", "建立关联，培养迁移能力")
                PrincipleRow("以深度背诵为用", "FSRS 调度，成体系记忆")
            }
        }
    }
}

@Composable
private fun PrincipleRow(title: String, desc: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "·",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = Spacing.sm),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(end = Spacing.sm),
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ============================================================
// 快速上手
// ============================================================

@Composable
private fun SectionQuickStart() {
    GroupedCard(title = "快速上手") {
        GroupedCardItem(
            title = "1. 配置 AI 服务",
            subtitle = "设置 → AI 服务",
            leadingIcon = Icons.Filled.Psychology,
            description = "推荐 DeepSeek。填入 API key 即可，不配置也能正常学习。",
        )
        GroupedCardDivider()
        GroupedCardItem(
            title = "2. 浏览知识点",
            subtitle = "知识点 Tab",
            leadingIcon = Icons.Filled.AutoStories,
            description = "按科目 → 时段章节浏览，先读核心结论建立框架。",
        )
        GroupedCardDivider()
        GroupedCardItem(
            title = "3. 每天复习卡片",
            subtitle = "卡片 Tab · 15-30 分钟",
            leadingIcon = Icons.Filled.Style,
            description = "完成当天到期卡片，诚实评分，算法自动安排下次复习。",
        )
    }
}

// ============================================================
// 功能模块
// ============================================================

@Composable
private fun SectionModules() {
    GroupedCard(title = "功能模块") {
        GroupedCardItem(
            title = "知识点",
            subtitle = "四科文学史",
            leadingIcon = Icons.Filled.AutoStories,
            description = "古代 / 现当代 / 外国 / 理论，章节树 + 三层内容。",
        )
        GroupedCardDivider()
        GroupedCardItem(
            title = "论述题",
            subtitle = "真题论述题",
            leadingIcon = Icons.AutoMirrored.Filled.MenuBook,
            description = "历年真题论述题 · 审题思路 + 依据 + 知识点串联",
        )
        GroupedCardDivider()
        GroupedCardItem(
            title = "卡片",
            subtitle = "FSRS 调度",
            leadingIcon = Icons.Filled.Style,
            description = "6 种模板，间隔重复复习，Leech 警告防死记硬背。",
        )
        GroupedCardDivider()
        GroupedCardItem(
            title = "错题本",
            subtitle = "间隔重复",
            leadingIcon = Icons.Filled.ErrorOutline,
            description = "卡片与真题错题统一复习，四档评分调度下次时间。",
        )
        GroupedCardDivider()
        GroupedCardItem(
            title = "AI 助手",
            subtitle = "苏格拉底式引导",
            leadingIcon = Icons.Filled.Psychology,
            description = "不直接给答案，引导你找到答案。基于资料库检索，不编造。",
        )
    }
}

// ============================================================
// 学习原理（可折叠）
// ============================================================

@Composable
private fun SectionPrinciples() {
    GroupedCard(title = "学习原理") {
        ExpandableInfoItem(
            title = "FSRS-6 间隔重复",
            summary = "基于遗忘曲线的智能复习调度",
            icon = Icons.Filled.Biotech,
            detail = "FSRS（Free Spaced Repetition Scheduler）在即将遗忘的临界点安排复习，" +
                "用最少次数维持长期记忆，比 Anki 的 SM-2 更精确，已被 Anki 官方推荐。" +
                "\n\n四档评分：不会（重学）/ 困难（放缓）/ 良好（标准）/ 简单（加速）。" +
                "诚实评分是算法准确预测的前提。" +
                "\n\nClockGuard 防护：修改系统时间时算法仍按真实时间调度。",
        )
        GroupedCardDivider()
        ExpandableInfoItem(
            title = "三档记忆机制",
            summary = "按内容类型差异化复习强度",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            detail = "不同内容对记忆精度要求不同，三档机制精准投放复习资源：" +
                "\n\n· 精确档（R=0.95）：原诗默写、名句、术语定义，逐字精确" +
                "\n· 框架档（R=0.90）：名词解释、流派对照，分条复述要点（最常用）" +
                "\n· 理解档（R=0.85）：论述题、文学史脉络，用自己的话阐述" +
                "\n\n档位越高复习越频繁，避免理解档浪费时间和精确档考场忘字。",
        )
    }
}

/**
 * 可折叠信息项。默认只显示标题 + 摘要，点击展开详情。
 * 用于学习原理等深度内容，让默认视图保持简洁。
 */
@Composable
private fun ExpandableInfoItem(
    title: String,
    summary: String,
    icon: ImageVector,
    detail: String,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {}
            .clickable(role = Role.Button) { expanded = !expanded }
            .padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.md,
                bottom = Spacing.md,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = Spacing.md),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
            contentDescription = if (expanded) "收起" else "展开",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    AnimatedVisibility(
        visible = expanded,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
    ) {
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = Spacing.lg,
                end = Spacing.lg,
                bottom = Spacing.md,
            ),
        )
    }
}

// ============================================================
// 关于
// ============================================================

@Composable
private fun SectionAbout() {
    GroupedCard(title = "关于") {
        GroupedCardItem(
            title = "技术栈",
            leadingIcon = Icons.Filled.Code,
            description = "Kotlin / Jetpack Compose / Material 3 Expressive / Room / FSRS-6 自实现。",
        )
        GroupedCardDivider()
        GroupedCardItem(
            title = "FSRS 算法致谢",
            leadingIcon = Icons.Filled.School,
            description = "FSRS 由 Jarrett Ye 开源，已被 Anki 官方推荐为默认调度器。",
        )
        GroupedCardDivider()
        GroupedCardItem(
            title = "免责声明",
            leadingIcon = Icons.Filled.Verified,
            description = "题目来自公开真题，版权归原命题方。AI 生成内容仅供参考，以官方参考书为准。",
        )
    }
}
