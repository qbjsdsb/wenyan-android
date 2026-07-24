package com.wenyan.app.core.data.cards

/**
 * 卡片拆分器（Task 18.1-18.2）。
 *
 * 严格遵循 Wozniak 最小信息原则（20条规则核心）：
 * - 一个名词解释拆成5-6张卡片（[splitTermExplanation]）
 * - 避免集合题：集合名词转分组枚举卡（[splitCollection]）
 * - 易混淆内容自动生成区分卡（[generateDistinctionCards]）
 *
 * 设计文档3.3.2节5种背诵模式与本拆分器生成的卡片模板正交：
 * 拆分后的卡片可适配任意背诵模式复习。
 */
object CardSplitter {

    /** 名词解释目标拆分张数（Wozniak最小信息原则：5-6张） */
    private const val TARGET_SPLIT_MIN = 5
    private const val TARGET_SPLIT_MAX = 6

    /** 集合枚举卡每组最大成员数（避免单卡信息过载） */
    private const val COLLECTION_GROUP_SIZE = 3

    /**
     * 将一个名词解释拆成5-6张卡片（Task 18.1，最小信息原则）。
     *
     * 实现策略：
     * 1. 优先识别 [definition] 中的结构化标签（如"时代：""代表作家：""风格：""意义："
     *    "区别：""影响："等），按标签拆出各维度，每个维度一张卡。
     * 2. 若无结构化标签，按句末标点（。；；\n）切分为分句，每句一张卡。
     * 3. 控制5-6张：不足5张时不再强行拆分（保持信息完整）；超过6张时合并尾段。
     *
     * 示例："建安风骨"拆成6张：时代/代表作家/风格特征/文学史意义/与正始区别/对后世影响。
     *
     * @param term 名词（如"建安风骨"）
     * @param definition 名词解释全文
     * @param pointId 关联知识点 ID（阶段3新增，用于 FSRS 调度回写）
     * @return 拆分后的卡片列表（5-6张，遵循最小信息原则）
     */
    fun splitTermExplanation(
        term: String,
        definition: String,
        pointId: String = "",
        fullExplanation: String? = null,
        studyText: String? = null,
    ): List<CardTemplate> {
        val dimensions = parseStructuredDimensions(term, definition)

        // 解析到结构化维度时，推断类别并构建结构化字段
        val category = if (dimensions.isNotEmpty()) determineCategory(dimensions) else TermCategory.SOCIETY
        val societyFields = if (dimensions.isNotEmpty() && category == TermCategory.SOCIETY) {
            buildSocietyFields(dimensions)
        } else {
            null
        }
        val workFields = if (dimensions.isNotEmpty() && category == TermCategory.WORK) {
            buildWorkFields(dimensions)
        } else {
            null
        }

        val cards = if (dimensions.isNotEmpty()) {
            // 结构化标签命中：每个维度一张卡
            // v0.8.9 P2-8 修复:sibling 卡冗余展示完整字段
            // 原实现每张 sibling 卡都附带完整 society/work/fullExplanation/studyText,
            // 导致 5 张卡背面都显示相同的 6 个结构化字段 + 完整解释 + 教材原文,
            // 信息高度冗余(如"时代"卡背面既显示"汉末建安年间",又显示"时间：汉末建安年间")
            //
            // 修复策略:
            // - 首张 sibling 卡作为"概览卡",附带完整 society/work 结构化字段
            //   (供用户翻第一张时建立整体认知)
            // - 后续 sibling 卡仅附带 fullExplanation/studyText(提供上下文)
            //   不再附带 society/work(避免与 back 内容重复)
            // - 所有 sibling 卡仍共享同一 pointId(FSRS 调度 sibling 去重)
            dimensions.mapIndexed { index, (question, answer) ->
                val isFirstSibling = index == 0
                buildTermDimensionCard(
                    term = term,
                    dimension = question,
                    answer = answer,
                    pointId = pointId,
                    category = category,
                    society = if (isFirstSibling) societyFields else null,
                    work = if (isFirstSibling) workFields else null,
                    fullExplanation = fullExplanation,
                    studyText = studyText,
                )
            }
        } else {
            // 无标签：按分句拆分（无结构化字段）
            val sentences = splitSentences(definition)
            sentences.mapIndexed { index, sentence ->
                buildTermDimensionCard(term, "第${indexToChinese(index + 1)}点", sentence, pointId, fullExplanation = fullExplanation, studyText = studyText)
            }
        }

        // 控制5-6张：超过6张时合并尾段（保留结构化字段）
        val trimmed = if (cards.size > TARGET_SPLIT_MAX) {
            val head = cards.take(TARGET_SPLIT_MAX - 1)
            val tail = cards.drop(TARGET_SPLIT_MAX - 1)
            val mergedBack = tail.joinToString(separator = "\n") { it.back }
            // 合并卡不附带 society/work(避免与非首张 sibling 卡行为不一致)
            head + buildTermDimensionCard(
                term = term,
                dimension = "其他要点",
                answer = mergedBack,
                pointId = pointId,
                fullExplanation = fullExplanation,
                studyText = studyText,
            )
        } else {
            cards
        }

        // 不足5张时不强行拆分（保持信息完整，避免碎片化）
        return trimmed.ifEmpty { listOf(buildTermDimensionCard(term, "解释", definition, pointId, fullExplanation = fullExplanation, studyText = studyText)) }
    }

    /**
     * 集合题拆成分组枚举卡（Task 18.1，避免集合题）。
     *
     * Wozniak规则：避免集合题（一次回忆过多条目易遗忘）。
     * 将集合成员按 [COLLECTION_GROUP_SIZE] 分组，每组一张枚举卡，
     * 正面问"X包含哪些"，背面列出本组成员。
     *
     * 示例："唐宋八大家"转为分组枚举：
     * - 第1组：韩愈、柳宗元
     * - 第2组：欧阳修、苏洵、苏轼
     * - 第3组：苏辙、王安石、曾巩
     *
     * @param collectionName 集合名（如"唐宋八大家"）
     * @param members 集合成员列表
     * @param pointId 关联知识点 ID（阶段3新增，用于 FSRS 调度回写）
     * @return 分组枚举卡列表
     */
    fun splitCollection(
        collectionName: String,
        members: List<String>,
        pointId: String = "",
    ): List<CardTemplate> {
        if (members.isEmpty()) return emptyList()

        return members.chunked(COLLECTION_GROUP_SIZE).mapIndexed { groupIndex, groupMembers ->
            EssayPointsCard(
                front = "「$collectionName」第${indexToChinese(groupIndex + 1)}组包含哪些？",
                back = groupMembers.joinToString(separator = "、"),
                pointId = pointId,
                question = "$collectionName 第${indexToChinese(groupIndex + 1)}组",
                keyPoints = groupMembers,
            )
        }
    }

    /**
     * 检测易混淆内容并自动生成区分卡（Task 18.2）。
     *
     * 检测策略：按姓氏（首字）聚类，同姓的作家/同名前缀的作品视为易混淆项，
     * 两两生成 [DistinctionCard]（正反面都出）。
     *
     * 示例：
     * - 苏轼/苏辙/苏洵 → 两两对比区分卡
     * - 李白/李贺/李商隐 → 两两对比区分卡
     *
     * @param items 待检测的作家/作品名列表
     * @param pointId 关联知识点 ID（阶段3新增，用于 FSRS 调度回写）
     * @return 自动生成的区分卡列表
     */
    fun generateDistinctionCards(
        items: List<String>,
        pointId: String = "",
    ): List<DistinctionCard> {
        if (items.size < 2) return emptyList()

        // 按首字（姓氏/前缀）聚类
        val grouped = items.groupBy { it.firstOrNull()?.toString() ?: "" }
            .filter { it.key.isNotEmpty() && it.value.size >= 2 }

        return grouped.values.flatMap { confusedItems ->
            // 两两组合生成区分卡
            confusedItems.flatMapIndexed { i, item1 ->
                confusedItems.drop(i + 1).map { item2 ->
                    DistinctionCard(
                        front = "区分：$item1 与 $item2",
                        back = "$item1 与 $item2 的区别见要点",
                        pointId = pointId,
                        item1 = item1,
                        item2 = item2,
                        differences = buildDefaultDifferences(item1, item2),
                    )
                }
            }
        }
    }

    // ---------- 内部工具方法 ----------

    /** 文学名词常用结构化标签（顺序即拆卡顺序） */
    private val TERM_LABELS: List<Pair<String, String>> = listOf(
        "时代" to "时代",
        "时间" to "时代",
        "年代" to "年代",
        "时期" to "时期",
        "地点" to "地点",
        "代表作家" to "代表作家",
        "人物" to "人物",
        "作家" to "代表作家",
        "刊物" to "刊物",
        "主张" to "主张",
        "风格" to "风格特征",
        "特色" to "特色",
        "特征" to "特征",
        "内容" to "内容",
        "意义" to "文学史意义",
        "贡献" to "贡献",
        "影响" to "对后世影响",
        "区别" to "区别",
        "不同" to "区别",
    )

    /**
     * 解析 [definition] 中的结构化标签，提取"维度-内容"对。
     * 标签格式："标签：内容"或"标签：内容。"，支持中英文冒号。
     */
    private fun parseStructuredDimensions(
        term: String,
        definition: String,
    ): List<Pair<String, String>> {
        val result = mutableListOf<Pair<String, String>>()
        val seenLabels = mutableSetOf<String>()

        for ((keyword, dimension) in TERM_LABELS) {
            if (seenLabels.contains(dimension)) continue
            val content = extractLabeledContent(definition, keyword) ?: continue
            if (content.isBlank()) continue
            result.add(dimension to content)
            seenLabels.add(dimension)
            // 命中6个维度即满足最大张数
            if (result.size >= TARGET_SPLIT_MAX) break
        }
        return result
    }

    /** 提取 [keyword] 标签后的内容（到下一个标签或句末） */
    private fun extractLabeledContent(definition: String, keyword: String): String? {
        val patterns = listOf("$keyword：", "$keyword:", "「$keyword」：")
        for (pattern in patterns) {
            val start = definition.indexOf(pattern)
            if (start < 0) continue
            val contentStart = start + pattern.length
            // 内容延伸到下一个句末标点
            val end = definition.indexOfAny(charArrayOf('。', '；', ';', '\n'), contentStart)
            val content = if (end < 0) {
                definition.substring(contentStart)
            } else {
                definition.substring(contentStart, end)
            }
            return content.trim().ifBlank { null }
        }
        return null
    }

    /** 按句末标点切分句子 */
    private fun splitSentences(definition: String): List<String> =
        definition.split('。', '；', ';', '\n')
            .map { it.trim() }
            .filter { it.isNotBlank() }

    /** 构建名词解释单维度卡片（最小信息原则：一张卡一个维度） */
    private fun buildTermDimensionCard(
        term: String,
        dimension: String,
        answer: String,
        pointId: String = "",
        category: TermCategory = TermCategory.SOCIETY,
        society: SocietyTermFields? = null,
        work: WorkTermFields? = null,
        fullExplanation: String? = null,
        studyText: String? = null,
    ): CardTemplate = TermExplanationCard(
        front = "$term — $dimension",
        back = answer,
        pointId = pointId,
        category = category,
        society = society,
        work = work,
        fullExplanation = fullExplanation,
        studyText = studyText,
    )

    /**
     * 根据解析到的维度推断名词类别（社团类/作品类）。
     *
     * - "刊物"或"主张"出现 → 社团类（SOCIETY 独有维度）
     * - "内容"出现 → 作品类（WORK 独有维度）
     * - 其他 → 默认社团类
     */
    private fun determineCategory(dimensions: List<Pair<String, String>>): TermCategory {
        val names = dimensions.map { it.first }.toSet()
        return when {
            "刊物" in names || "主张" in names -> TermCategory.SOCIETY
            "内容" in names -> TermCategory.WORK
            else -> TermCategory.SOCIETY
        }
    }

    /** 从解析维度构建社团类结构化字段（缺失维度用空字符串） */
    private fun buildSocietyFields(dimensions: List<Pair<String, String>>): SocietyTermFields {
        val map = dimensions.toMap()
        return SocietyTermFields(
            time = map["时代"] ?: map["年代"] ?: map["时期"] ?: "",
            place = map["地点"] ?: "",
            members = map["代表作家"] ?: map["人物"] ?: "",
            publication = map["刊物"] ?: "",
            proposition = map["主张"] ?: "",
            contribution = map["贡献"] ?: map["文学史意义"] ?: map["对后世影响"] ?: "",
        )
    }

    /** 从解析维度构建作品类结构化字段（缺失维度用空字符串） */
    private fun buildWorkFields(dimensions: List<Pair<String, String>>): WorkTermFields {
        val map = dimensions.toMap()
        return WorkTermFields(
            author = map["代表作家"] ?: map["人物"] ?: "",
            era = map["时代"] ?: map["年代"] ?: map["时期"] ?: "",
            content = map["内容"] ?: "",
            feature = map["特色"] ?: map["特征"] ?: map["风格特征"] ?: "",
            influence = map["对后世影响"] ?: map["文学史意义"] ?: map["贡献"] ?: "",
        )
    }

    /**
     * 数字转中文（1-99），用于"第N组""第N点"展示。
     *
     * NF-BB4 修复：原实现仅支持 1-10，index > 10 时返回阿拉伯数字字符串，
     * 导致"第十一组""第十二点"等场景中文数字与阿拉伯数字混排，风格不统一。
     * 现扩展到 1-99，覆盖实际业务场景（单卡片拆分点数极少超过 20）。
     *
     * 规则：
     * - 1-10：一、二、...、十
     * - 11-19：十一、十二、...、十九
     * - 20/30/.../90：二十、三十、...、九十
     * - 21-99（非整十）：二十一、二十二、...、九十九
     * - ≤0 或 >99：回退阿拉伯数字（防御性，业务不应触达）
     */
    private fun indexToChinese(index: Int): String {
        if (index <= 0 || index > 99) return index.toString()
        val digits = arrayOf("零", "一", "二", "三", "四", "五", "六", "七", "八", "九")
        val tens = index / 10
        val ones = index % 10
        return when {
            tens == 0 -> digits[ones]
            tens == 1 -> if (ones == 0) "十" else "十${digits[ones]}"
            ones == 0 -> "${digits[tens]}十"
            else -> "${digits[tens]}十${digits[ones]}"
        }
    }

    /** 为两个易混淆项生成默认区别要点（占位提示，后续可由AI补全） */
    private fun buildDefaultDifferences(item1: String, item2: String): List<String> =
        listOf(
            "$item1 与 $item2 字号/别称不同",
            "$item1 与 $item2 生平年代对比",
            "$item1 与 $item2 代表作对比",
            "$item1 与 $item2 文学风格/主张对比",
        )
}
