# 文研 · 南师大文学考研专业课学习App 设计文档

> 文档版本：v2.0
> 创建日期：2026-07-08
> 最后更新：2026-07-08（第二轮深度调研整合）
> 项目代号：wenyan-android
> 开发模式：AI编写全部代码，用户负责测试反馈与内容整理

---

## 一、项目定位

### 1.1 一句话定位

**面向南京师范大学现当代文学考研（050106）的深度专业课学习与背诵工具，以真题为纲、以知识网络为本、以深度背诵为用。**

### 1.2 核心理念

本App不是"把教材搬上手机"，而是把上岸学长学姐验证过的复习方法产品化：

1. **以真题为纲**——南师大真题复现率极高（屈原《离骚》2020/2022重复考，"散文小品成功论"2013/2015/2017三度出现），真题是最高效的复习工具，必须贯穿学习全程
2. **以知识网络为本**——文学考研不是背字典，而是建立知识网络。经验贴强调"横纵向思考""迁移与类比"，知识点不能孤立存在，必须有关联系统
3. **以深度背诵为用**——不追求碎片化刷题，而是构建能沉下心学、能成体系背、能打通关节的学习系统

### 1.3 目标用户

- 用户画像：在职考研生，Windows电脑+Android手机，碎片时间与整段时间并存
- 核心痛点：时间少易中断、内容多易迷失、参考书"重描述轻判断"难以应对论述题
- 使用场景：随身携带背诵、晚上整段学习、周末模考训练

### 1.4 与市面上App的区别

| 维度 | Anki/记乎 | 本App |
|------|-----------|-------|
| 内容组织 | 孤立卡片 | 知识网络图谱 |
| 背诵深度 | 翻卡片看一眼 | 五种模式递进，能完整复述 |
| 真题地位 | 用户自制 | 内置真题工坊，贯穿学习主线 |
| 大题处理 | 不支持 | 多视角对照，训练判断力 |
| 专业适配 | 通用 | 南师大现当代文学专用 |

---

## 二、考试信息背景

### 2.1 考试科目（2026年代码变更后）

| 科目代码 | 科目名称 | 分值 | 题型 |
|---------|---------|------|------|
| 801 | 文学基础 | 150分 | 9道论述/简答题 |
| 610 | 专业写作 | 150分 | 写作题 |
| F008 | 现当代文学专题（复试） | 150分 | 3道论述题 |

### 2.2 801文学基础分值分布

| 科目 | 分值 | 题量 | 说明 |
|------|------|------|------|
| 中国古代文学 | 约50分 | 3题 | 占比最高，2020-2024稳定3题 |
| 文学理论 | 约40分 | 2题 | 童庆炳教材目录即题库 |
| 外国文学 | 约30分 | 2题 | 只考西方文学 |
| 中国现当代文学 | 约30分 | 2题 | 考题灵活但有规律 |
| 合计 | 150分 | 9题 | 全部为论述/简答题 |

### 2.3 官方参考书目（2024-2026）

| 科目 | 教材 |
|------|------|
| 古代文学 | 袁世硕、陈文新《中国古代文学史》（马工程教材） |
| 现当代文学 | 丁帆《中国新文学史》 |
| 外国文学 | 聂珍钊《外国文学史》第2版 |
| 文学理论 | 童庆炳《文学理论教程》第5版 |
| 专业写作（现当代方向） | 无指定参考书 |

---

## 三、功能模块设计

### 3.1 模块总览

App采用底部导航五Tab结构 + 智能悬浮窗：

```
┌──────────────────────────────────────────────┐
│  [当前模块内容区]                  ┌──┐      │
│                                    │AI│ ← 悬浮按钮（可拖动）
│                                    └──┘      │
├──────────────────────────────────────────────┤
│ 知识图谱 │ 深度背诵 │ 真题工坊 │ AI助手 │ 我的 │
└──────────────────────────────────────────────┘
```

专业写作训练作为真题工坊内的子模块，不单独占Tab。

**智能悬浮窗**（核心创新）：
- 在「AI助手」界面设置开关，开启后屏幕边缘出现可拖动的悬浮按钮
- 点击展开为对话框，能识别当前页面内容作为上下文
- 用户无需切换Tab即可随时提问
- 悬浮窗能感知当前所在页面，自动带上上下文：
  - 在知识点详情页 → 自动带上知识点标题和内容
  - 在真题详情页 → 自动带上题目内容
  - 在背诵模式 → 自动带上当前背诵的知识点
  - 在写作练习 → 自动带上用户写的草稿

### 3.2 模块一：知识图谱（KnowledgeGraph）

#### 3.2.1 设计目标

不是知识点列表，而是**关联网络**。帮用户建立知识网络而非堆砌碎片，支持横纵向思考和迁移类比。

#### 3.2.2 内容组织（六级层次）

```
科 → 编 → 章 → 节 → 知识点 → 背诵层
```

**四科分编结构**：

| 科目 | 分编 |
|------|------|
| 古代文学 | 先秦 / 秦汉 / 魏晋南北朝 / 隋唐五代 / 宋 / 元 / 明 / 清 |
| 现当代文学 | 第一个十年(1917-1927) / 第二个十年(1927-1937) / 第三个十年(1937-1949) / 十七年(1949-1966) / 新时期(1978-2000) / 新世纪(2000-) |
| 外国文学 | 古希腊罗马 / 中世纪 / 文艺复兴 / 古典主义 / 启蒙运动 / 浪漫主义 / 现实主义 / 20世纪现代主义 |
| 文学理论 | 本质论 / 创作论 / 作品论 / 接受论 / 发展论 |

#### 3.2.3 知识点数据结构

每个知识点包含三层内容：

```kotlin
data class KnowledgePoint(
    val id: String,                    // 唯一ID
    val subject: Subject,              // 所属科目
    val chapterId: String,             // 所属章节
    val title: String,                 // 标题（如"《离骚》"）
    val summary: String,               // 摘要（一句话）
    
    // 三层内容
    val coreConclusion: String,        // 必背核心（30字内结论句，考场直接用）
    val fullContent: String,           // 展开内容（200-500字完整论述）
    val multiPerspectives: List<Perspective>,  // 多视角补充（不同教材评价对照）
    
    // 关联系统
    val relatedPoints: List<String>,   // 相关知识点ID
    val contrastPoints: List<String>,  // 对比知识点ID（如李白vs杜甫）
    val extensionPoints: List<String>, // 延伸知识点ID
    
    // 真题标记
    val examRecords: List<ExamRecord>, // 曾考记录（年份/题型/角度/分值）
    val examFrequency: Frequency,      // 考频：HIGH / MEDIUM / LOW / NEVER
    
    // 名词解释模板（可选）
    val termTemplate: TermTemplate?,   // 如果是名词解释考点，填模板
    
    // 元数据
    val tags: List<String>,            // 标签（如"浪漫主义""楚辞"）
    val difficulty: Int,               // 难度1-5
    val createdAt: Long,
    val updatedAt: Long
)

data class Perspective(
    val source: String,      // 来源教材/学者（如"洪子诚《中国当代文学史》"）
    val viewpoint: String,   // 观点内容
    val stance: Stance       // 立场：POSITIVE / NEGATIVE / NEUTRAL
)

data class ExamRecord(
    val year: Int,           // 年份
    val examType: ExamType,  // ESSAY / SHORT_ANSWER / TERM_EXPLANATION
    val angle: String,       // 考查角度（如"艺术成就"）
    val score: Int           // 分值
)
```

#### 3.2.4 核心功能

1. **浏览**：按科→编→章→节树形浏览，支持展开折叠
2. **搜索**：全文搜索（标题/摘要/内容/标签），支持高亮匹配
3. **筛选**：按考频（高频优先）、按难度、按标签筛选
4. **详情查看**：三层内容完整呈现，支持折叠/展开各层
5. **关联跳转**：点击相关/对比/延伸知识点，跳转到对应详情
6. **真题标记**：知识点详情页显示"曾考"徽章，点击查看历年考题
7. **多视角对照**：展开"多视角补充"，并排显示不同教材的评价

#### 3.2.5 名词解释模板系统

按类型提供背诵框架（基于经验贴总结）：

| 类型 | 模板字段 |
|------|---------|
| 社团流派 | 时间+地点+主要人物+主要刊物+主张+贡献 |
| 作品 | 作者+年代+体裁+内容梗概+艺术特色+文学史地位 |
| 文学运动 | 时间+背景+主张+代表人物+影响+局限 |
| 批评术语 | 出处+内涵+例证+相关概念辨析 |

#### 3.2.6 多视图切换（6种视图）

文学考研的知识体系可从多个维度观察。知识图谱模块提供**6种视图**，用户可根据学习目的切换：

| 视图 | 适用场景 | 实现方式 | 交互 |
|------|---------|---------|------|
| **树形视图** | 系统浏览知识体系 | 递归ExpandableList | 展开/折叠/点击跳转 |
| **时间轴视图** | 梳理文学史脉络 | 横向时间轴+节点 | 滑动/点击查看详情 |
| **文体视图** | 专题复习（小说/诗歌/散文/戏剧） | 分组列表+筛选 | 筛选/对比 |
| **流派视图** | 流派专题（浪漫主义/现实主义/现代主义等） | 分组列表+筛选 | 筛选/对比 |
| **考频热力图** | 冲刺阶段抓重点 | Calendar热力图样式 | 点击查看高频考点 |
| **作家矩阵** | 纵向对比作家 | 二维矩阵（时期×文体） | 点击单元格查看 |

**考频热力图设计**：

```kotlin
@Composable
fun ExamFrequencyHeatmap(
    points: List<KnowledgePoint>,
    onPointClick: (KnowledgePoint) -> Unit
) {
    // 按章节聚合，颜色深浅表示考频
    val heatmapData = points.groupBy { it.chapterId }
        .mapValues { (_, pts) ->
            pts.sumOf { point ->
                when (point.examFrequency) {
                    Frequency.HIGH -> 3
                    Frequency.MEDIUM -> 2
                    Frequency.LOW -> 1
                    Frequency.NEVER -> 0
                }
            }
        }

    LazyColumn {
        items(heatmapData.entries.toList()) { (chapterId, score) ->
            val color = when {
                score >= 10 -> Color(0xFFB71C1C)  // 深红：超高频
                score >= 6 -> Color(0xFFE53935)   // 红：高频
                score >= 3 -> Color(0xFFFF7043)   // 橙：中频
                score >= 1 -> Color(0xFFFFCC80)   // 浅橙：低频
                else -> Color(0xFFE0E0E0)         // 灰：未考
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color)
                    .clickable { /* 跳转到该章节知识点列表 */ }
                    .padding(12.dp)
            ) {
                Text("章节 $chapterId · 考频 $score")
            }
        }
    }
}
```

**作家矩阵设计**：

```kotlin
@Composable
fun AuthorMatrix(
    authors: List<Author>,
    genres: List<String>,  // 小说/诗歌/散文/戏剧
    onCellClick: (Author, String) -> Unit
) {
    // 二维表格：行=作家（按时期排序），列=文体
    LazyColumn {
        item {
            Row {
                Text("作家", modifier = Modifier.weight(1f))
                genres.forEach { Text(it, modifier = Modifier.weight(1f)) }
            }
        }
        items(authors) { author ->
            Row {
                Text(author.name, modifier = Modifier.weight(1f))
                genres.forEach { genre ->
                    val works = author.works.filter { it.genre == genre }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(if (works.isNotEmpty()) Color(0xFFBBDEFB) else Color.Transparent)
                            .clickable { onCellClick(author, genre) }
                            .padding(8.dp)
                    ) {
                        if (works.isNotEmpty()) {
                            Text("${works.size}部", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
```

#### 3.2.7 作家作品关系网络（Graph视图）

除树形浏览外，提供**关系网络图视图**，可视化展示作家、作品、流派、文学运动之间的关联。这是建立知识网络的核心功能。

**节点类型**：

```kotlin
enum class GraphNodeType {
    AUTHOR,         // 作家（如"苏轼"）
    WORK,           // 作品（如"《念奴娇·赤壁怀古》"）
    SCHOOL,         // 流派/社团（如"婉约派"）
    MOVEMENT,       // 文学运动（如"古文运动"）
    CONCEPT,        // 文学概念（如"以诗为词"）
    KNOWLEDGE_POINT // 知识点（关联到知识图谱）
}

data class GraphNode(
    val id: String,
    val type: GraphNodeType,
    val label: String,             // 显示名称
    val subtitle: String?,         // 副标题（如"1037-1101"）
    val size: Float = 1.0f,        // 节点大小（按重要度/考频）
    val color: Long,               // 节点颜色（按类型）
    val relatedPointId: String?,   // 关联的知识点ID
    val x: Float = 0f,             // 当前位置X（力导向布局用）
    val y: Float = 0f              // 当前位置Y
)
```

**关系类型（8种边）**：

```kotlin
enum class RelationType {
    AUTHORED,           // 作家→作品（创作）
    BELONGS_TO,         // 作品/作家→流派（属于）
    PARTICIPATED_IN,    // 作家→文学运动（参与）
    INFLUENCED_BY,      // 作家→作家（受影响）
    COMPARED_WITH,      // 作家↔作家（对比关系，如李白vs杜甫）
    SAME_PERIOD,        // 同时期（弱关联）
    PRECEDES,           // 文学史先后（如屈原→李白）
    RELATED_CONCEPT     // 关联概念（如"以诗为词"→苏轼）
}

data class GraphEdge(
    val id: String,
    val sourceId: String,
    val targetId: String,
    val type: RelationType,
    val weight: Float = 1.0f,      // 边权重（关系强弱）
    val label: String? = null      // 边标签（如"师承"）
)
```

**关系数据示例**：

```
苏轼(AUTHOR) ──AUTHORED──→ 《念奴娇·赤壁怀古》(WORK)
苏轼(AUTHOR) ──BELONGS_TO──→ 豪放派(SCHOOL)
苏轼(AUTHOR) ──PARTICIPATED_IN──→ 古文运动(MOVEMENT)
苏轼(AUTHOR) ──INFLUENCED_BY──→ 屈原(AUTHOR)
苏轼(AUTHOR) ──COMPARED_WITH──→ 辛弃疾(AUTHOR)
苏轼(AUTHOR) ──RELATED_CONCEPT──→ 以诗为词(CONCEPT)
```

#### 3.2.8 Force-directed力导向布局算法

关系网络图采用**力导向布局**（Force-directed Layout）自动排列节点。该算法模拟物理系统：节点间存在斥力（库仑力），相连节点间存在引力（胡克力），系统达到平衡时节点分布最均匀。

**ForceSimulation实现**：

```kotlin
class ForceSimulation(
    private val nodes: MutableList<GraphNode>,
    private val edges: List<GraphEdge>
) {
    // 物理参数
    private val repulsion = 8000f      // 库仑斥力系数（越大节点越分散）
    private val springLength = 120f    // 弹簧自然长度（相连节点的目标距离）
    private val springStrength = 0.05f // 胡克引力系数
    private val damping = 0.85f        // 阻尼系数（每轮速度衰减）
    private val centerGravity = 0.01f  // 中心引力（防止节点飞太远）
    private val maxVelocity = 30f      // 最大速度（防止爆炸）

    // 节点速度
    private val velocities = mutableMapOf<String, Pair<Float, Float>>()

    /**
     * 执行一轮模拟（在Dispatchers.Default上异步调用）
     * @return 节点是否仍在运动（用于判断是否继续模拟）
     */
    suspend fun step(): Boolean {
        var totalKineticEnergy = 0f

        // 1. 计算库仑斥力（每对节点之间）
        for (i in nodes.indices) {
            for (j in i + 1 until nodes.size) {
                val n1 = nodes[i]
                val n2 = nodes[j]
                val dx = n1.x - n2.x
                val dy = n1.y - n2.y
                val distance = sqrt(dx * dx + dy * dy).coerceAtLeast(1f)
                val force = repulsion / (distance * distance)
                val fx = force * dx / distance
                val fy = force * dy / distance

                updateVelocity(n1.id, fx, fy)
                updateVelocity(n2.id, -fx, -fy)
            }
        }

        // 2. 计算胡克引力（相连节点之间）
        val nodeMap = nodes.associateBy { it.id }
        edges.forEach { edge ->
            val n1 = nodeMap[edge.sourceId] ?: return@forEach
            val n2 = nodeMap[edge.targetId] ?: return@forEach
            val dx = n2.x - n1.x
            val dy = n2.y - n1.y
            val distance = sqrt(dx * dx + dy * dy)
            val displacement = distance - springLength
            val force = springStrength * displacement * edge.weight
            val fx = force * dx / distance.coerceAtLeast(1f)
            val fy = force * dy / distance.coerceAtLeast(1f)

            updateVelocity(n1.id, fx, fy)
            updateVelocity(n2.id, -fx, -fy)
        }

        // 3. 中心引力（防止节点飞太远）
        val centerX = nodes.map { it.x }.average().toFloat()
        val centerY = nodes.map { it.y }.average().toFloat()
        nodes.forEach { node ->
            val dx = centerX - node.x
            val dy = centerY - node.y
            updateVelocity(node.id, dx * centerGravity, dy * centerGravity)
        }

        // 4. 更新位置 + 速度衰减
        nodes.forEach { node ->
            val (vx, vy) = velocities[node.id] ?: (0f to 0f)
            val clampedVx = vx.coerceIn(-maxVelocity, maxVelocity)
            val clampedVy = vy.coerceIn(-maxVelocity, maxVelocity)
            node.x += clampedVx
            node.y += clampedVy
            velocities[node.id] = clampedVx * damping to clampedVy * damping
            totalKineticEnergy += vx * vx + vy * vy
        }

        // 系统动能小于阈值时停止
        return totalKineticEnergy > 1f
    }

    private fun updateVelocity(nodeId: String, fx: Float, fy: Float) {
        val (vx, vy) = velocities[nodeId] ?: (0f to 0f)
        velocities[nodeId] = vx + fx to vy + fy
    }
}
```

**GraphEngine：异步布局调度**：

```kotlin
class GraphEngine(
    private val simulation: ForceSimulation
) {
    private val _graphState = MutableStateFlow<GraphState>(GraphState.Idle)
    val graphState: StateFlow<GraphState> = _graphState

    private var simulationJob: Job? = null

    sealed class GraphState {
        object Idle : GraphState()
        data class Simulating(val nodes: List<GraphNode>) : GraphState()
        data class Stable(val nodes: List<GraphNode>) : GraphState()
    }

    /**
     * 启动力导向布局模拟
     * 在Dispatchers.Default上运行，不阻塞UI
     */
    fun startSimulation(maxSteps: Int = 300) {
        simulationJob?.cancel()
        simulationJob = CoroutineScope(Dispatchers.Default).launch {
            _graphState.value = GraphState.Simulating(simulation.getNodes())

            var step = 0
            var isMoving = true
            while (step < maxSteps && isMoving && isActive) {
                isMoving = simulation.step()
                step++

                // 每5步更新一次UI（避免过度刷新）
                if (step % 5 == 0) {
                    _graphState.value = GraphState.Simulating(simulation.getNodes())
                }
            }

            _graphState.value = GraphState.Stable(simulation.getNodes())
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        _graphState.value = GraphState.Stable(simulation.getNodes())
    }
}
```

#### 3.2.9 KnowledgeGraphCanvas渲染方案

经调研，**Compose原生没有成熟的关系图库**，需基于Canvas自绘。采用视口变换+视口裁剪+分层渲染方案。

**性能基准**（调研验证）：500节点+1000边在Canvas上可达45-60fps。

```kotlin
@Composable
fun KnowledgeGraphCanvas(
    graphState: GraphEngine.GraphState,
    modifier: Modifier = Modifier
) {
    // 视口状态：平移+缩放
    var viewportOffset by remember { mutableStateOf(Offset.Zero) }
    var viewportScale by remember { mutableStateOf(1f) }

    var selectedNode by remember { mutableStateOf<GraphNode?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(graphState) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    viewportOffset += pan
                    viewportScale = (viewportScale * zoom).coerceIn(0.3f, 3f)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            when (graphState) {
                is GraphEngine.GraphState.Simulating -> {
                    drawGraph(graphState.nodes, viewportOffset, viewportScale)
                }
                is GraphEngine.GraphState.Stable -> {
                    drawGraph(graphState.nodes, viewportOffset, viewportScale)
                }
                else -> {}
            }
        }

        // 选中节点的详情卡片（叠加在Canvas上）
        selectedNode?.let { node ->
            NodeDetailCard(
                node = node,
                onDismiss = { selectedNode = null },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

private fun DrawScope.drawGraph(
    nodes: List<GraphNode>,
    offset: Offset,
    scale: Float
) {
    // 1. 视口变换矩阵
    val transform = Matrix().apply {
        translate(offset.x, offset.y)
        scale(scale, scale)
    }

    // 2. 视口裁剪（只绘制可见范围内的节点和边）
    val visibleRect = Rect(
        left = -offset.x / scale,
        top = -offset.y / scale,
        right = (size.width - offset.x) / scale,
        bottom = (size.height - offset.y) / scale
    )

    // 3. 分层渲染：先边后节点
    // 第一层：边
    drawEdges(nodes, edges, visibleRect, transform)

    // 第二层：节点
    drawNodes(nodes, visibleRect, transform)
}

private fun DrawScope.drawNodes(
    nodes: List<GraphNode>,
    visibleRect: Rect,
    transform: Matrix
) {
    nodes.forEach { node ->
        // 视口裁剪：跳过不可见节点
        if (node.x < visibleRect.left - 50 || node.x > visibleRect.right + 50 ||
            node.y < visibleRect.top - 50 || node.y > visibleRect.bottom + 50) {
            return@forEach
        }

        val transformedX = node.x * transform.scaleX + transform.translateX
        val transformedY = node.y * transform.scaleY + transform.translateY
        val radius = node.size * 10f * transform.scaleX

        // 节点圆
        drawCircle(
            color = Color(node.color),
            radius = radius,
            center = Offset(transformedX, transformedY)
        )

        // 节点标签（缩放足够大时才显示）
        if (transform.scaleX > 0.5f) {
            drawIntoCanvas {
                val paint = Paint().apply {
                    textSize = 28f * transform.scaleX
                    color = Color.Black.toArgb()
                    textAlign = Paint.Align.CENTER
                }
                it.nativeCanvas.drawText(
                    node.label,
                    transformedX,
                    transformedY + radius + 20f * transform.scaleX,
                    paint
                )
            }
        }
    }
}
```

**性能优化措施**：

1. **视口裁剪**：只绘制屏幕可见范围内的节点和边
2. **分层渲染**：边在底层，节点在上层，减少重绘
3. **LOD（细节层次）**：缩放小时只画圆点不画标签
4. **异步布局**：力导向模拟在`Dispatchers.Default`运行，不阻塞UI
5. **节流更新**：模拟过程中每5步才更新一次UI

### 3.3 模块二：深度背诵引擎（DeepMemo）

#### 3.3.1 设计目标

不止翻卡片，而是五种模式递进的深度背诵系统，最终目标是能脱稿完整复述。

#### 3.3.2 五种背诵模式

**模式1：通读模式（Read）**
- 完整呈现知识点材料（coreConclusion + fullContent）
- 记录阅读进度（读到哪个知识点、哪个位置）
- 支持续读，下次打开直接跳到上次位置
- 适用场景：第一次学习新内容

**模式2：遮挡背诵（Cloze）**
- 材料分段呈现，每段后跟"背诵要点"
- 点击"开始背诵"，要点部分被遮挡显示为"●●●●"
- 用户在脑中回想/口述
- 点击"显示答案"对照
- 自评："记住了" / "没记住" / "模糊"
- 适用场景：主动回忆训练，效率比阅读高3-5倍

**模式3：提纲背诵（Outline）**
- 只显示关键词提纲（如"《离骚》：屈原、楚辞体、兮字、浪漫主义、香草美人、忠君爱国"）
- 用户自己展开成完整论述
- 可选"显示完整答案"对照
- 适用场景：已背过一遍后的巩固，训练考场组织能力

**模式4：默写自测（Write）**
- 关键名词、作品名、作家字号等填空题
- 系统挖空，用户输入答案
- 自动判分，标记易错字
- 适用场景：防错字（如"荃不察余之中情兮"的"荃"）

**模式5：完整复述（Recall）**
- 不显示任何提示
- 用户口述或文字输入完整复述
- 可选"录音"功能（可选，后期加）
- 对照参考答案自评
- 适用场景：最终考核，确认长期记忆

#### 3.3.3 FSRS复习算法（基于调研升级）

**算法说明**：
采用FSRS（Free Spaced Repetition Scheduler）算法，这是AnkiDroid等现代项目使用的开源间隔重复算法，基于AI优化，比传统艾宾浩斯固定曲线更精准。

**4档复习反馈**（替代原3档）：

```
Again（完全忘记）→ 重置稳定性，1分钟后再来
Hard（困难）     → 稳定性小幅增长，间隔较短
Good（顺利）     → 稳定性正常增长，间隔适中
Easy（轻松）     → 稳定性大幅增长，间隔较长
```

**数据结构**：

```kotlin
data class MemoRecord(
    val pointId: String,
    val state: CardState,          // NEW / LEARNING / REVIEW / RELEARNING
    val stability: Float,          // 记忆稳定性（FSRS核心，单位天）
    val difficulty: Float,         // 难度1-10（FSRS核心，动态调整）
    val lastReviewAt: Long,
    val nextReviewAt: Long,
    val reviewCount: Int,
    val failCount: Int,
    val history: List<ReviewLog>,
    val inPriorityQueue: Boolean
)

data class ReviewLog(
    val reviewedAt: Long,
    val mode: MemoMode,
    val rating: Rating,            // AGAIN / HARD / GOOD / EASY
    val stabilityBefore: Float,
    val stabilityAfter: Float,
    val intervalDays: Int
)
```

**每日复习队列**：
- 启动App自动计算"今日待复习列表"
- 按到期时间排序，过期优先
- 用户完成一个划掉一个
- 评"Again"的知识点自动进入"重点攻坚队列"，当日稍后重复

**续学机制**：
- 记录"上次学到哪个知识点"
- 打开App显示"继续学习：[知识点名]"
- 一键跳转到上次位置

**算法实现细节**：见第5.4节 FSRScheduler

#### 3.3.4 三档复习调度机制（核心创新）

文学考研的背诵内容并非"一刀切"：屈原《离骚》的艺术成就需要精确背诵原文要点，而"京派小说特点"只需框架记忆，鲁迅对当代作家的影响只需理解即可。基于此，引入**三档复习调度机制**，每个知识点根据考试要求被分配到不同记忆档位，FSRS算法使用不同参数集进行调度。

**档位定义**：

| 档位 | 枚举值 | 适用内容 | 目标保留率 | 说明 |
|------|--------|---------|-----------|------|
| 精确记忆档 | `TIER_EXACT` | 名词解释原文、原诗、作家字号、关键术语定义 | 0.95 | 考场需逐字复述，FSRS采用高保留率策略，间隔较短 |
| 框架记忆档 | `TIER_FRAMEWORK` | 论述题答题要点、作品艺术特色分条 | 0.90 | 考场需分条复述要点，关键词不能漏，允许表达自由 |
| 理解记忆档 | `TIER_UNDERSTAND` | 文学史脉络、影响关系、背景知识 | 0.85 | 考场需能用自己的话阐述，重在理解不死记 |

**档位预设参数**：

```kotlin
enum class MemoryTier {
    TIER_EXACT,       // 精确记忆档
    TIER_FRAMEWORK,   // 框架记忆档
    TIER_UNDERSTAND   // 理解记忆档
}

data class TierFSRSConfig(
    val tier: MemoryTier,
    val targetRetention: Float,        // 目标保留率 R_target
    val maxInterval: Int,              // 最大间隔（天）
    val minInterval: Int,              // 最小间隔（天）
    val stabilityGrowthFactor: Float,  // 稳定性增长系数（影响 Good/Easy 的 S'）
    val easyBonus: Float,              // Easy 额外加成
    val againPenalty: Float            // Again 惩罚系数
)

val TIER_CONFIGS = mapOf(
    MemoryTier.TIER_EXACT to TierFSRSConfig(
        tier = MemoryTier.TIER_EXACT,
        targetRetention = 0.95f,
        maxInterval = 180,             // 半年内必复习
        minInterval = 1,
        stabilityGrowthFactor = 0.85f, // 增长慢一些，确保频繁复习
        easyBonus = 1.2f,
        againPenalty = 0.3f            // 遗忘惩罚重
    ),
    MemoryTier.TIER_FRAMEWORK to TierFSRSConfig(
        tier = MemoryTier.TIER_FRAMEWORK,
        targetRetention = 0.90f,
        maxInterval = 365,
        minInterval = 1,
        stabilityGrowthFactor = 1.0f,  // 标准FSRS增长
        easyBonus = 1.3f,
        againPenalty = 0.4f
    ),
    MemoryTier.TIER_UNDERSTAND to TierFSRSConfig(
        tier = MemoryTier.TIER_UNDERSTAND,
        targetRetention = 0.85f,
        maxInterval = 720,             // 两年内复习即可
        minInterval = 1,
        stabilityGrowthFactor = 1.15f, // 增长快，减少复习频次
        easyBonus = 1.5f,
        againPenalty = 0.5f
    )
)
```

**知识点新增字段**：

```kotlin
// KnowledgePoint 新增字段
data class KnowledgePoint(
    // ... 原有字段 ...
    val memoryTier: MemoryTier = MemoryTier.TIER_FRAMEWORK,  // 默认框架档
    val tierReason: String = ""  // 档位判定理由（如"高频名词解释""论述题核心考点"等）
)
```

**FSRScheduler集成档位参数**：

```kotlin
object FSRScheduler {
    /**
     * 根据档位计算间隔
     * I = 9 * S * (1/R_target - 1)
     * R_target 由知识点所属档位决定
     */
    fun nextIntervalWithTier(stability: Float, tier: MemoryTier): Int {
        val config = TIER_CONFIGS[tier]!!
        val interval = (9 * stability * (1f / config.targetRetention - 1f)).toInt()
        return interval.coerceIn(config.minInterval, config.maxInterval)
    }

    /**
     * 根据主动回忆检测的完整度（0-100），自动映射为Rating
     * 用于"完整复述"模式下，AI检测后自动评分
     */
    fun ratingFromCompleteness(completeness: Int, tier: MemoryTier): Rating {
        val threshold = when (tier) {
            MemoryTier.TIER_EXACT -> intArrayOf(95, 80, 60)  // Easy/Good/Hard/Again 阈值
            MemoryTier.TIER_FRAMEWORK -> intArrayOf(85, 70, 50)
            MemoryTier.TIER_UNDERSTAND -> intArrayOf(75, 55, 35)
        }
        return when {
            completeness >= threshold[0] -> Rating.EASY
            completeness >= threshold[1] -> Rating.GOOD
            completeness >= threshold[2] -> Rating.HARD
            else -> Rating.AGAIN
        }
    }
}
```

**档位自动推荐规则**：

App根据知识点的考频和题型，自动推荐档位（用户可手动调整）：

| 条件 | 推荐档位 |
|------|---------|
| 历年名词解释考点 + 高频 | TIER_EXACT |
| 历年论述题核心考点 + 高频 | TIER_FRAMEWORK |
| 历年简答题考点 + 中频 | TIER_FRAMEWORK |
| 文学史背景/影响关系 + 低频 | TIER_UNDERSTAND |
| 原诗/原文引用 | TIER_EXACT |

**数据库迁移**（knowledge_points表新增列）：

```sql
ALTER TABLE knowledge_points ADD COLUMN memory_tier TEXT NOT NULL DEFAULT 'TIER_FRAMEWORK';
ALTER TABLE knowledge_points ADD COLUMN tier_reason TEXT NOT NULL DEFAULT '';
```

#### 3.3.5 主动回忆检测模式（完整复述的智能评判）

模式5"完整复述"原需用户对照参考答案自评，效率低且主观。引入**主动回忆检测引擎**，结合关键词检测、要点覆盖检测和AI完整性检测三层机制，自动评估用户复述质量，并直接对接FSRS生成Rating。

**三层检测机制**：

```
用户输入复述文本
        ↓
┌─────────────────────────────────┐
│ L1: 关键词检测（本地，<10ms）     │  快速过滤：核心术语是否出现
│ - 提取知识点预设的关键词列表       │
│ - 检测关键词覆盖率               │
│ - 标记缺失关键词                 │
└─────────────────────────────────┘
        ↓ L1覆盖率 < 30% → 直接判为 Again
┌─────────────────────────────────┐
│ L2: 要点覆盖检测（本地，<100ms）  │  深度检测：要点是否被覆盖
│ - 每个知识点预设 N 个要点        │
│ - 每个要点有1-3个同义表达模式     │
│ - 正则/模糊匹配检测要点覆盖       │
│ - 计算要点覆盖率                 │
└─────────────────────────────────┘
        ↓ L2覆盖率 < 60% → 判为 Hard
┌─────────────────────────────────┐
│ L3: AI完整性检测（在线，3-5秒）   │  精确评估：语义理解和表达质量
│ - 调用大模型API                 │
│ - 输入：参考答案 + 用户复述       │
│ - 输出：完整度评分（0-100）       │
│ - 输出：缺失要点列表             │
│ - 输出：表达问题列表             │
└─────────────────────────────────┘
        ↓ L3评分 → 映射为 Good/Easy
```

**数据结构**：

```kotlin
data class RecallCheckResult(
    val pointId: String,
    val userText: String,                  // 用户复述文本
    val mode: RecallCheckMode,             // 检测模式：LOCAL_ONLY / LOCAL_AI

    // L1 关键词检测
    val keywordCoverage: Float,            // 关键词覆盖率 0-1
    val missingKeywords: List<String>,     // 缺失关键词
    val extraKeywords: List<String>,       // 多余/错误关键词

    // L2 要点覆盖检测
    val pointCoverage: Float,              // 要点覆盖率 0-1
    val coveredPoints: List<String>,       // 已覆盖要点
    val missingPoints: List<String>,       // 缺失要点

    // L3 AI完整性检测（可选）
    val aiScore: Int?,                     // AI评分 0-100
    val aiFeedback: String?,               // AI反馈
    val aiMissingPoints: List<String>?,    // AI补充的缺失要点
    val aiExpressionIssues: List<String>?, // 表达问题

    // 最终结果
    val finalCompleteness: Int,            // 综合完整度 0-100
    val autoRating: Rating,                // 自动映射的FSRS Rating
    val detectedAt: Long
)

enum class RecallCheckMode {
    LOCAL_ONLY,   // 仅本地检测（无网络/API）
    LOCAL_AI      // 本地 + AI 检测
}
```

**检测引擎实现**：

```kotlin
class RecallCheckEngine(
    private val aiClient: AiStreamClient?  // 可选，无则跳过L3
) {
    fun check(
        point: KnowledgePoint,
        userText: String,
        mode: RecallCheckMode = RecallCheckMode.LOCAL_AI
    ): RecallCheckResult {
        // L1: 关键词检测
        val l1Result = checkKeywords(point, userText)

        // L1快速过滤：覆盖率极低直接判Again
        if (l1Result.coverage < 0.3f) {
            return buildResult(point, userText, l1Result, null, null,
                finalCompleteness = (l1Result.coverage * 60).toInt(),
                autoRating = Rating.AGAIN)
        }

        // L2: 要点覆盖检测
        val l2Result = checkPointCoverage(point, userText)

        // L2过滤：覆盖率偏低判Hard
        if (l2Result.coverage < 0.6f) {
            val completeness = (l1Result.coverage * 30 + l2Result.coverage * 50).toInt()
            return buildResult(point, userText, l1Result, l2Result, null,
                finalCompleteness = completeness,
                autoRating = Rating.HARD)
        }

        // L3: AI完整性检测（可选）
        val l3Result = if (mode == RecallCheckMode.LOCAL_AI && aiClient != null) {
            checkWithAI(point, userText, l2Result)
        } else null

        // 综合评分
        val completeness = if (l3Result != null) {
            (l1Result.coverage * 20 + l2Result.coverage * 30 + l3Result.score * 0.5f).toInt()
        } else {
            (l1Result.coverage * 40 + l2Result.coverage * 60).toInt()
        }

        val rating = FSRScheduler.ratingFromCompleteness(completeness, point.memoryTier)

        return buildResult(point, userText, l1Result, l2Result, l3Result,
            finalCompleteness = completeness, autoRating = rating)
    }
}
```

**与FSRS自动对接**：

检测完成后，自动调用FSRS更新记忆记录：

```kotlin
// 在RecallCheckEngine外部调用
suspend fun applyRecallCheckResult(result: RecallCheckResult) {
    val record = memoRepo.getMemoRecord(result.pointId)
    val updatedCard = FSRS.schedule(
        card = FSRSAdapter.toCard(record),
        rating = result.autoRating,
        now = System.currentTimeMillis()
    )
    val updatedRecord = FSRSAdapter.toMemoRecord(result.pointId, updatedCard, true)
    memoRepo.updateMemoRecord(updatedRecord)

    // 保存检测记录到review_logs
    reviewLogRepo.insert(ReviewLog(
        pointId = result.pointId,
        rating = result.autoRating.ordinal,
        state = updatedCard.state.name,
        stabilityBefore = record.stability,
        stabilityAfter = updatedCard.stability,
        difficultyBefore = record.difficulty,
        elapsedDays = updatedCard.elapsedDays,
        scheduledDays = updatedCard.scheduledDays,
        reviewedAt = result.detectedAt
    ))
}
```

**用户体验设计**：

- 复述完成后，点击"AI检测"，3-5秒内返回结果
- 结果卡片显示：完整度环形进度条 + 缺失要点列表 + AI反馈
- 用户可选择"接受评分"（自动写入FSRS）或"手动调整"（自评Rating）

#### 3.3.6 每日复习量控制机制

在职考研时间有限，不能让复习队列无限膨胀。引入**每日复习量控制机制**，根据用户设定的每日学习时长和考研倒计时，智能控制每日新学和复习的知识点数量。

**配置参数**：

```kotlin
data class DailyQuotaConfig(
    val dailyReviewLimit: Int = 50,        // 每日复习上限
    val dailyNewLimit: Int = 10,           // 每日新学上限
    val dailyTimeLimitMinutes: Int = 90,   // 每日学习时长上限
    val exactTierDailyLimit: Int = 15,     // 精确档每日上限（更耗精力）
    val examCountdownDate: Long,           // 考研日期（用于动态调整）
    val adaptiveAdjustment: Boolean = true // 是否启用倒计时自适应
)

object DailyQuotaDefaults {
    // 根据考研倒计时，动态调整每日新学上限
    fun adaptiveNewLimit(daysToExam: Int, baseLimit: Int): Int {
        return when {
            daysToExam > 180 -> baseLimit           // >6个月：正常节奏
            daysToExam in 90..180 -> (baseLimit * 0.8).toInt()  // 3-6个月：减新学，增复习
            daysToExam in 30..90 -> (baseLimit * 0.5).toInt()   // 1-3个月：新学减半，全力复习
            daysToExam in 7..30 -> (baseLimit * 0.2).toInt()    // 最后1月：几乎不学新内容
            daysToExam < 7 -> 0                                // 最后1周：只复习不学新
            else -> baseLimit
        }
    }
}
```

**队列优先级（9级）**：

为保证关键内容优先复习，定义9级优先级：

| 优先级 | 枚举 | 说明 | 示例 |
|--------|------|------|------|
| P0 | `OVERDUE_EXACT` | 过期的精确档卡片 | 屈原《离骚》昨天到期未复习 |
| P1 | `OVERDUE_FRAMEWORK` | 过期的框架档卡片 | 苏轼词史贡献前天到期 |
| P2 | `TODAY_EXACT` | 今日到期的精确档 | 《红楼梦》叙事艺术今天到期 |
| P3 | `TODAY_FRAMEWORK` | 今日到期的框架档 | 周作人散文特色今天到期 |
| P4 | `OVERDUE_UNDERSTAND` | 过期的理解档 | 文学史背景前天到期 |
| P5 | `TODAY_UNDERSTAND` | 今日到期的理解档 | 影响关系今天到期 |
| P6 | `RELEARNING` | 遗忘后重新学习的卡片 | 刚判Again的卡片 |
| P7 | `NEW_HIGH_FREQ` | 新学：高频考点 | 历年高频但未学过 |
| P8 | `NEW_NORMAL` | 新学：普通考点 | 未学过的中低频考点 |

**调度器实现**：

```kotlin
class ReviewQueueScheduler(
    private val config: DailyQuotaConfig,
    private val memoRepo: MemoRepository,
    private val pointRepo: KnowledgePointRepository
) {
    data class ReviewQueue(
        val items: List<QueueItem>,
        val totalCount: Int,
        val estimatedMinutes: Int,
        val quotaReached: Boolean
    )

    data class QueueItem(
        val pointId: String,
        val pointTitle: String,
        val priority: QueuePriority,
        val estimatedMinutes: Int,
        val isOverdue: Boolean
    )

    suspend fun buildTodayQueue(): ReviewQueue {
        val allRecords = memoRepo.getAllMemoRecords()
        val today = LocalDate.now()

        // 1. 收集到期卡片
        val overdueExact = mutableListOf<QueueItem>()
        val overdueFramework = mutableListOf<QueueItem>()
        val overdueUnderstand = mutableListOf<QueueItem>()
        val todayExact = mutableListOf<QueueItem>()
        val todayFramework = mutableListOf<QueueItem>()
        val todayUnderstand = mutableListOf<QueueItem>()
        val relearning = mutableListOf<QueueItem>()

        allRecords.forEach { record ->
            if (record.state == State.NEW.name) return@forEach
            val point = pointRepo.getPoint(record.pointId) ?: return@forEach
            val dueDate = Instant.ofEpochMilli(record.nextReviewAt)
                .atZone(ZoneId.systemDefault()).toLocalDate()
            val isOverdue = dueDate.isBefore(today)
            val item = QueueItem(
                pointId = record.pointId,
                pointTitle = point.title,
                priority = QueuePriority.P0,  // 占位，后续赋值
                estimatedMinutes = estimateMinutes(point.memoryTier),
                isOverdue = isOverdue
            )
            when {
                isOverdue && point.memoryTier == MemoryTier.TIER_EXACT -> overdueExact.add(item.copy(priority = QueuePriority.P0))
                isOverdue && point.memoryTier == MemoryTier.TIER_FRAMEWORK -> overdueFramework.add(item.copy(priority = QueuePriority.P1))
                isOverdue && point.memoryTier == MemoryTier.TIER_UNDERSTAND -> overdueUnderstand.add(item.copy(priority = QueuePriority.P4))
                !isOverdue && point.memoryTier == MemoryTier.TIER_EXACT -> todayExact.add(item.copy(priority = QueuePriority.P2))
                !isOverdue && point.memoryTier == MemoryTier.TIER_FRAMEWORK -> todayFramework.add(item.copy(priority = QueuePriority.P3))
                !isOverdue && point.memoryTier == MemoryTier.TIER_UNDERSTAND -> todayUnderstand.add(item.copy(priority = QueuePriority.P5))
            }
        }

        // 2. 按优先级合并
        val reviewItems = (overdueExact + overdueFramework + todayExact + todayFramework +
            overdueUnderstand + todayUnderstand + relearning).toMutableList()

        // 3. 应用每日上限
        val exactCount = reviewItems.count { it.priority in listOf(QueuePriority.P0, QueuePriority.P2) }
        val exactToKeep = minOf(exactCount, config.exactTierDailyLimit)
        reviewItems.removeAll { it.priority in listOf(QueuePriority.P0, QueuePriority.P2) && 
            reviewItems.indexOf(it) >= exactToKeep }

        if (reviewItems.size > config.dailyReviewLimit) {
            reviewItems.subList(config.dailyReviewLimit, reviewItems.size).clear()
        }

        // 4. 计算总时长，若超时则截断
        var totalMinutes = reviewItems.sumOf { it.estimatedMinutes }
        while (totalMinutes > config.dailyTimeLimitMinutes && reviewItems.isNotEmpty()) {
            reviewItems.removeAt(reviewItems.lastIndex)
            totalMinutes = reviewItems.sumOf { it.estimatedMinutes }
        }

        // 5. 补充新学卡片
        val daysToExam = ChronoUnit.DAYS.between(today,
            Instant.ofEpochMilli(config.examCountdownDate).atZone(ZoneId.systemDefault()).toLocalDate()).toInt()
        val newLimit = if (config.adaptiveAdjustment) {
            DailyQuotaDefaults.adaptiveNewLimit(daysToExam, config.dailyNewLimit)
        } else config.dailyNewLimit

        val newItems = memoRepo.getTodayNewCards(newLimit).map { record ->
            val point = pointRepo.getPoint(record.pointId)!!
            QueueItem(
                pointId = record.pointId,
                pointTitle = point.title,
                priority = if (point.examFrequency == Frequency.HIGH) QueuePriority.P7 else QueuePriority.P8,
                estimatedMinutes = estimateMinutes(point.memoryTier),
                isOverdue = false
            )
        }

        val allItems = reviewItems + newItems
        return ReviewQueue(
            items = allItems.sortedBy { it.priority.ordinal },
            totalCount = allItems.size,
            estimatedMinutes = allItems.sumOf { it.estimatedMinutes },
            quotaReached = allItems.size >= config.dailyReviewLimit + newLimit
        )
    }

    private fun estimateMinutes(tier: MemoryTier): Int = when (tier) {
        MemoryTier.TIER_EXACT -> 4    // 精确档较慢
        MemoryTier.TIER_FRAMEWORK -> 3
        MemoryTier.TIER_UNDERSTAND -> 2
    }
}

enum class QueuePriority {
    P0_OVERDUE_EXACT, P1_OVERDUE_FRAMEWORK, P2_TODAY_EXACT,
    P3_TODAY_FRAMEWORK, P4_OVERDUE_UNDERSTAND, P5_TODAY_UNDERSTAND,
    P6_RELEARNING, P7_NEW_HIGH_FREQ, P8_NEW_NORMAL
}
```

**用户体验设计**：

- 首页显示"今日复习队列"卡片：`复习 23 / 新学 5 · 预计 45 分钟`
- 队列按优先级排序，过期项标红
- 完成一项划掉一项，进度条实时更新
- 达到每日上限时，提示"今日已达上限，明日再继续新学"
- 设置页可调整每日上限、考研日期等参数

### 3.4 模块三：真题工坊（ExamWorkshop）

#### 3.4.1 设计目标

真题是贯穿学习的主线，不是孤立的练习区。

#### 3.4.2 真题数据结构

```kotlin
data class ExamQuestion(
    val id: String,
    val year: Int,                    // 年份
    val subject: Subject,             // 科目
    val questionType: QuestionType,   // ESSAY / SHORT_ANSWER / TERM_EXPLANATION / WRITING
    val content: String,              // 题目内容
    val score: Int,                   // 分值
    val angle: String,                // 考查角度
    val relatedPointIds: List<String>,// 关联知识点ID
    val answerFramework: String?,     // 参考答案/答题框架
    val sampleEssay: String?,         // 范文（写作题）
    val notes: String?                // 备注
)
```

#### 3.4.3 核心功能

1. **按年份浏览**：2020-2025年真题完整还原（2020-2024已收录，2025待补充）
2. **按科目筛选**：古代/现当代/外国/理论/写作
3. **按考点交叉索引**：搜索"屈原"，显示历年所有相关考题
4. **答题三件套**：
   - 题目+分值+年份
   - 参考答案/答题框架（分条论述①②③）
   - 关联知识点（跳转到知识图谱对应条目）
5. **模考模式**：
   - 选择年份，限时180分钟
   - 自动计时，到时提醒
   - 答题记录保存在本地
   - 支持中断续答
6. **命题规律分析**：
   - 高频考点表（按考频排序）
   - 命题趋势图（按年份分布）
   - 分值分布饼图

#### 3.4.4 专业写作训练（子模块）

专业写作150分，分值最高，需长期训练，作为真题工坊的子模块：

1. **范文库**：历年高分范文、优秀考场作文，按题材分类
2. **素材积累**：
   - 名言警句库（按主题：人性/社会/文化/审美等）
   - 文论引用库（可在作文中引用提升格调）
   - 经典论据库
3. **写作模板**：开头5种+论证结构3种+结尾5种
4. **限时练习**：手机上写提纲/片段，记录灵感
5. **范文精读**：每篇标注亮点、结构拆解、可借鉴句式

#### 3.4.5 答题模板系统（核心创新）

文学考研论述题失分的主因不是"不会"，而是"答不全"和"没条理"。基于经验贴和上岸学长学姐总结，将四种题型的答题套路产品化为**答题模板系统**，帮助用户建立考场答题的肌肉记忆。

**四种题型模板**：

##### ① 名词解释模板（总分总三段论）

```
[总] 一句话定义（10-20字，开门见山）
[分] 展开要素（按类型固定字段）：
     - 社团流派：时间+地点+主要人物+主要刊物+主张+贡献
     - 作品：作者+年代+体裁+内容梗概+艺术特色+文学史地位
     - 文学运动：时间+背景+主张+代表人物+影响+局限
     - 批评术语：出处+内涵+例证+相关概念辨析
[总] 一句话评价/意义（10-15字，收束）
```

**字数标准**：80-150字，绝不超过200字（考场时间宝贵）

##### ② 简答题模板（定义-框架-例证法）

```
[定义] 先用1-2句界定题干核心概念（30字内）
[框架] 分条阐述（①②③），每条一个小标题+展开
       - 一般3-4条，每条30-50字
       - 小标题必须是判断句，不能是描述句
[例证] 每条框架后紧跟1个具体作品/作家例证
       - 例证要"具体"：作品名+人物/情节/诗句
[总结] 1-2句收束，回应题干（20字内）
```

**字数标准**：300-500字，分值15分左右

##### ③ 论述题模板（三步走）

```
[第一步：破题]（50字内）
     - 明确答题角度（如"从艺术成就、思想内容、文学史地位三个维度"）
     - 简述论述思路

[第二步：主体论述]（核心，800-1200字）
     - 分3-4个大层次，每层一个小标题（加粗或①②③）
     - 每个层次内部：论点→论据→分析→小结
     - 论据必须是具体作品/诗句/情节
     - 层次之间要有逻辑递进（时间递进/层面递进/对比递进）

[第三步：总结升华]（100字内）
     - 回应题干，给出整体评价
     - 可适当延伸到文学史意义或当代启示
     - 避免空话套话，要有具体判断
```

**字数标准**：800-1500字，分值20分左右

##### ④ 文学评论写作模板（开头5种+论证3种+结尾5种）

**开头5种**：
1. 破题式：直接点明评论主旨（"《断魂枪》的悲剧意蕴在于..."）
2. 引文式：引用相关文论作为切入点（"宗白华言'艺术的境界...'，老舍《断魂枪》正是..."）
3. 对比式：通过对比引出评论对象（"与同时期鲁迅的国民性批判不同..."）
4. 设问式：提出问题引发思考（"沙子龙的沉默意味着什么？"）
5. 概括式：概括作品核心特征（"《断魂枪》以极简的笔触..."）

**论证结构3种**：
1. 总分总：总论点→分论点（3-4个）→总结
2. 递进式：表层→中层→深层（如：情节分析→人物分析→文化意蕴）
3. 对比式：与1-2部相关作品横向对比

**结尾5种**：
1. 呼应式：呼应开头，形成闭环
2. 升华式：从具体作品升华到文学史/文化意义
3. 反思式：提出值得继续思考的问题
4. 引文式：引用相关文论收束
5. 判断式：给出明确的整体评价

**标题格式**：大标题+副标题
- 大标题：分析角度+理论术语（如"平实语言中的深沉意蕴"）
- 副标题：以题干作品为例（如"——评老舍《断魂枪》"）

**字数标准**：
- 50分：800-1000字
- 100分：1500-1800字
- 150分：2500-3000字

**数据结构**：

```kotlin
enum class QuestionType {
    TERM_EXPLANATION,  // 名词解释
    SHORT_ANSWER,      // 简答题
    ESSAY,             // 论述题
    WRITING            // 文学评论写作
}

data class AnswerTemplate(
    val id: String,
    val questionType: QuestionType,
    val name: String,                       // 模板名称（如"名词解释-社团流派型"）
    val structure: List<TemplateSection>,   // 结构分段
    val applicableTags: List<String>,       // 适用标签（如"社团流派""作品""批评术语"）
    val exampleUsage: String,               // 使用示例
    val scoreRange: IntRange,               // 适用分值范围
    val wordLimit: IntRange                 // 字数范围
)

data class TemplateSection(
    val title: String,                      // 段落标题（如"总：定义"）
    val description: String,                // 段落说明
    val requiredFields: List<TemplateField>, // 必填字段
    val wordLimit: IntRange,                // 本段字数范围
    val isOptional: Boolean = false         // 是否可选
)

data class TemplateField(
    val key: String,                        // 字段键（如"时间""地点""主要人物"）
    val label: String,                      // 显示标签
    val type: FieldType,                    // TEXT / LIST / WORK_REFERENCE / QUOTE
    val placeholder: String,                // 输入提示
    val exampleValue: String,               // 示例值
    val isRequired: Boolean                 // 是否必填
)

enum class FieldType {
    TEXT,               // 普通文本
    LIST,               // 列表（多项）
    WORK_REFERENCE,     // 作品引用（关联知识图谱）
    QUOTE,              // 文论引用
    CRITIC_TERM         // 批评术语
}

// 写作模板（文学评论专用）
data class WritingPattern(
    val id: String,
    val name: String,                       // 如"破题式开头"
    val category: WritingPatternCategory,   // OPENING / ARGUMENT / ENDING
    val description: String,
    val template: String,                   // 模板文本（含占位符）
    val example: String,                    // 使用示例
    val applicableGenres: List<String>      // 适用文体（小说/诗歌/散文/戏剧）
)

enum class WritingPatternCategory {
    OPENING,    // 开头
    ARGUMENT,   // 论证结构
    ENDING      // 结尾
}

// 用户填写的答题
data class TemplateFill(
    val id: String,
    val templateId: String,
    val questionId: String?,                // 关联真题（可为空，用于自由练习）
    val filledSections: Map<String, String>, // 段落标题→用户填写内容
    val filledFields: Map<String, String>,   // 字段键→用户填写值
    val totalText: String,                  // 拼接后的完整答案
    val createdAt: Long,
    val updatedAt: Long
)
```

**预置模板库**：

App预置以下模板（用户可自定义新增）：

| 题型 | 模板数 | 说明 |
|------|--------|------|
| 名词解释 | 4个 | 社团流派型/作品型/文学运动型/批评术语型 |
| 简答题 | 3个 | 定义-框架-例证法/比较分析型/影响分析型 |
| 论述题 | 3个 | 三步走（破题-主体-总结）/对比论述型/影响论述型 |
| 文学评论写作 | 13个 | 5开头+3论证+5结尾，可自由组合 |

**模板引导答题流程**：

1. 用户打开真题，点击"模板答题"
2. 选择题型对应的模板（或系统自动推荐）
3. 按模板段落依次填写：
   - 每段显示字段提示和示例
   - 字数实时统计，超限变红
   - 可随时预览拼接后的完整答案
4. 完成后可"AI批改"（见3.6节）
5. 答题记录保存到TemplateFill，可回看和修改

**与AI批改的协同**：

答题模板不仅引导用户答题，也为AI批改提供结构化输入：

```kotlin
data class AIGradingRequest(
    val questionContent: String,
    val questionType: QuestionType,
    val templateId: String?,                // 使用的模板（可选）
    val userAnswer: String,                 // 用户答案
    val filledFields: Map<String, String>?, // 结构化字段（可选，提升批改精度）
    val referenceAnswer: String?,           // 参考答案（可选）
    val score: Int                          // 题目分值
)
```

当用户使用模板答题时，AI可针对每个模板段落单独评分，给出更精准的改进建议。

### 3.5 模块四：我的（Dashboard）

#### 3.5.1 设计目标

提供方向感与成就感，防止迷失和中断。

#### 3.5.2 核心功能

1. **四科进度地图**：
   - 章节节点+掌握度颜色（灰=未学 / 黄=在学 / 蓝=已背 / 绿=已掌握）
   - 一眼看出哪块薄弱
2. **今日复习队列**：
   - 艾宾浩斯驱动的"今日待复习"列表
   - 完成进度条
3. **学习统计**：
   - 连续打卡日历
   - 累计学时
   - 知识点掌握率（四科分别）
   - 真题练习正确率
4. **里程碑成就**：
   - 第一次完整复述
   - 连续7天打卡
   - 刷完某编
   - 模考完成
5. **数据备份**：
   - 导出JSON备份到手机存储
   - 导入备份恢复
   - 自动定期备份提醒

#### 3.5.3 薄弱诊断系统

基于复习记录和AI批改结果，自动诊断用户的薄弱环节，给出针对性建议。

**诊断维度**：

1. **按科目诊断**：四科（古代/现当代/外国/理论）的掌握率对比，找出最薄弱科目
2. **按章节诊断**：每科内各章节的掌握率，定位薄弱章节
3. **按题型诊断**：论述题/简答/名词解释/写作的得分率，找出弱项题型
4. **按考点诊断**：高频考点的掌握情况，标记"高频但未掌握"的考点
5. **按记忆档诊断**：精确档/框架档/理解档的遗忘率，判断哪类内容易忘

**数据结构**：

```kotlin
data class WeaknessDiagnosis(
    val generatedAt: Long,
    val overallMastery: Float,                    // 总体掌握率 0-1
    val subjectWeakness: List<SubjectWeakness>,   // 按科目诊断
    val chapterWeakness: List<ChapterWeakness>,   // 按章节诊断（取最薄弱5个）
    val questionTypeWeakness: List<QuestionTypeWeakness>,  // 按题型诊断
    val highFreqUnmastered: List<KnowledgePoint>, // 高频但未掌握的知识点
    val tierWeakness: List<TierWeakness>,         // 按记忆档诊断
    val recommendations: List<DailyRecommendation> // 智能建议
)

data class SubjectWeakness(
    val subjectId: String,
    val subjectName: String,
    val masteryRate: Float,           // 掌握率 0-1
    val totalPoints: Int,             // 总知识点数
    val masteredPoints: Int,          // 已掌握数
    val weakPoints: Int,              // 薄弱数
    val overduePoints: Int,           // 过期未复习数
    val trend: Trend                  // 上升/平稳/下降
)

data class ChapterWeakness(
    val chapterId: String,
    val chapterName: String,
    val subjectName: String,
    val masteryRate: Float,
    val weakPointTitles: List<String>,  // 薄弱知识点标题
    val suggestedAction: String         // 建议行动（如"复习第3节"）
)

data class QuestionTypeWeakness(
    val questionType: QuestionType,
    val avgScore: Float,              // 平均得分率
    val practiceCount: Int,           // 练习次数
    val commonIssues: List<String>    // 常见问题（AI批改反馈聚合）
)

data class TierWeakness(
    val tier: MemoryTier,
    val totalCount: Int,
    val overdueCount: Int,            // 过期数
    val avgRetention: Float,          // 平均保留率
    val forgetRate: Float             // 遗忘率（Again占比）
)

enum class Trend { UP, STABLE, DOWN }
```

**诊断逻辑**：

```kotlin
class WeaknessDiagnoser(
    private val memoRepo: MemoRepository,
    private val pointRepo: KnowledgePointRepository,
    private val gradingRepo: AIGradingRepository,
    private val statsRepo: StudyStatsRepository
) {
    suspend fun diagnose(): WeaknessDiagnosis {
        val allPoints = pointRepo.getAllPoints()
        val allRecords = memoRepo.getAllMemoRecords()
        val gradingRecords = gradingRepo.getAllRecords()
        val today = LocalDate.now()

        // 1. 按科目诊断
        val subjectWeakness = allPoints.groupBy { it.subjectId }
            .map { (subjectId, points) ->
                val records = allRecords.filter { r -> points.any { it.id == r.pointId } }
                val mastered = records.count { isMastered(it) }
                val overdue = records.count { isOverdue(it, today) }
                SubjectWeakness(
                    subjectId = subjectId,
                    subjectName = getSubjectName(subjectId),
                    masteryRate = mastered.toFloat() / points.size,
                    totalPoints = points.size,
                    masteredPoints = mastered,
                    weakPoints = points.size - mastered,
                    overduePoints = overdue,
                    trend = calculateTrend(subjectId)
                )
            }.sortedBy { it.masteryRate }  // 最薄弱在前

        // 2. 按章节诊断（取最薄弱5个）
        val chapterWeakness = allPoints.groupBy { it.chapterId }
            .map { (chapterId, points) ->
                // ... 类似逻辑
            }.sortedBy { it.masteryRate }.take(5)

        // 3. 按题型诊断
        val questionTypeWeakness = gradingRecords.groupBy { it.questionType }
            .map { (type, records) ->
                QuestionTypeWeakness(
                    questionType = type,
                    avgScore = records.map { it.scoreRate }.average().toFloat(),
                    practiceCount = records.size,
                    commonIssues = extractCommonIssues(records)
                )
            }

        // 4. 高频但未掌握
        val highFreqUnmastered = allPoints.filter {
            it.examFrequency == Frequency.HIGH && !isMastered(allRecords.find { r -> r.pointId == it.id })
        }

        // 5. 按记忆档诊断
        val tierWeakness = MemoryTier.values().map { tier ->
            val tierRecords = allRecords.filter { r ->
                pointRepo.getPoint(r.pointId)?.memoryTier == tier
            }
            TierWeakness(
                tier = tier,
                totalCount = tierRecords.size,
                overdueCount = tierRecords.count { isOverdue(it, today) },
                avgRetention = tierRecords.map { calculateRetention(it) }.average().toFloat(),
                forgetRate = tierRecords.count { it.failCount > 0 }.toFloat() / tierRecords.size
            )
        }

        // 6. 生成建议
        val recommendations = generateRecommendations(
            subjectWeakness, chapterWeakness, highFreqUnmastered, tierWeakness
        )

        return WeaknessDiagnosis(
            generatedAt = System.currentTimeMillis(),
            overallMastery = allRecords.count { isMastered(it) }.toFloat() / allPoints.size,
            subjectWeakness = subjectWeakness,
            chapterWeakness = chapterWeakness,
            questionTypeWeakness = questionTypeWeakness,
            highFreqUnmastered = highFreqUnmastered,
            tierWeakness = tierWeakness,
            recommendations = recommendations
        )
    }
}
```

#### 3.5.4 每日智能建议

基于薄弱诊断结果，每天生成3-5条具体可执行的学习建议。

**建议类型**：

| 类型 | 示例 |
|------|------|
| 复习提醒 | "你有5个高频考点过期未复习，建议优先处理" |
| 新学建议 | "古代文学·先秦编还有12个未学知识点，建议今天学3个" |
| 弱项强化 | "你的论述题平均得分率62%，建议练习《红楼梦》叙事艺术" |
| 写作练习 | "距考研还有89天，建议本周完成1篇写作练习" |
| 平衡建议 | "你的现当代文学掌握率45%，低于其他科目，建议增加投入" |

**数据结构**：

```kotlin
data class DailyRecommendation(
    val id: String,
    val type: RecommendationType,
    val priority: Int,                // 1-5，1最优先
    val title: String,                // 建议标题
    val description: String,          // 详细说明
    val actionTarget: ActionTarget,   // 点击后的跳转目标
    val estimatedMinutes: Int,        // 预计耗时
    val generatedAt: Long
)

enum class RecommendationType {
    REVIEW_OVERDUE,       // 复习过期
    LEARN_NEW,            // 新学知识
    WEAK_SUBJECT,         // 弱科强化
    WEAK_QUESTION_TYPE,   // 弱题型练习
    WRITING_PRACTICE,     // 写作练习
    BALANCE_ADJUST        // 平衡建议
}

sealed class ActionTarget {
    data class ReviewQueue : ActionTarget()                           // 跳转到复习队列
    data class KnowledgeList(val chapterId: String?) : ActionTarget() // 跳转到知识点列表
    data class ExamQuestion(val questionId: String) : ActionTarget()  // 跳转到真题
    data class WritingPractice : ActionTarget()                       // 跳转到写作练习
    data class SubjectDashboard(val subjectId: String) : ActionTarget() // 跳转到科目仪表盘
}
```

#### 3.5.5 保留率曲线

基于FSRS的可提取性公式 `R = (1 + t/(9*S))^(-1)`，绘制用户的记忆保留率曲线，直观展示遗忘趋势。

**数据结构**：

```kotlin
data class RetentionDataPoint(
    val date: LocalDate,
    val overallRetention: Float,      // 总体保留率
    val exactTierRetention: Float,    // 精确档保留率
    val frameworkTierRetention: Float,// 框架档保留率
    val understandTierRetention: Float// 理解档保留率
)

data class RetentionCurve(
    val points: List<RetentionDataPoint>,  // 最近30天数据
    val currentRetention: Float,           // 当前总体保留率
    val targetRetention: Float,            // 目标保留率（综合三档）
    val trend: Trend,                      // 趋势
    val projectedRetention7Days: Float,    // 7天后预测保留率（不复习的话）
    val projectedRetention30Days: Float    // 30天后预测保留率
)
```

**UI呈现**：

```kotlin
@Composable
fun RetentionCurveChart(
    curve: RetentionCurve,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxWidth().height(120.dp)) {
        val width = size.width
        val height = size.height
        val padding = 16f

        // 绘制坐标轴
        drawLine(
            color = Color.Gray,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding)
        )
        drawLine(
            color = Color.Gray,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding)
        )

        // 绘制目标保留率线（虚线）
        val targetY = height - padding - (curve.targetRetention * (height - 2 * padding))
        // ... 虚线绘制

        // 绘制保留率曲线
        val points = curve.points
        if (points.size > 1) {
            val stepX = (width - 2 * padding) / (points.size - 1)
            val path = Path().apply {
                moveTo(padding, height - padding - (points[0].overallRetention * (height - 2 * padding)))
                for (i in 1 until points.size) {
                    lineTo(
                        padding + i * stepX,
                        height - padding - (points[i].overallRetention * (height - 2 * padding))
                    )
                }
            }
            drawPath(
                path = path,
                color = MaterialTheme.colorScheme.primary,
                style = Stroke(width = 3f)
            )
        }
    }
}
```

#### 3.5.6 考研倒计时

在仪表盘顶部显示考研倒计时，并根据倒计时阶段调整学习建议。

```kotlin
@Composable
fun ExamCountdownCard(
    examDate: LocalDate,
    modifier: Modifier = Modifier
) {
    val today = remember { LocalDate.now() }
    val daysLeft = ChronoUnit.DAYS.between(today, examDate).toInt()

    val stage = when {
        daysLeft > 180 -> CountdownStage.BASE_PHASE      // 基础阶段
        daysLeft in 90..180 -> CountdownStage.STRENGTHEN_PHASE  // 强化阶段
        daysLeft in 30..90 -> CountdownStage.SPRINT_PHASE       // 冲刺阶段
        daysLeft in 7..30 -> CountdownStage.FINAL_SPRINT        // 最后冲刺
        daysLeft in 1..7 -> CountdownStage.FINAL_WEEK           // 最后一周
        else -> CountdownStage.EXAM_DAY
    }

    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (stage) {
                CountdownStage.BASE_PHASE -> MaterialTheme.colorScheme.primaryContainer
                CountdownStage.STRENGTHEN_PHASE -> MaterialTheme.colorScheme.secondaryContainer
                CountdownStage.SPRINT_PHASE -> Color(0xFFFFE0B2)  // 橙色
                CountdownStage.FINAL_SPRINT -> Color(0xFFFFCCBC)  // 深橙
                CountdownStage.FINAL_WEEK -> Color(0xFFEF9A9A)    // 红色
                CountdownStage.EXAM_DAY -> Color(0xFFE53935)      // 深红
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "距考研还有",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$daysLeft",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "天",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stage.displayName,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = stage.suggestion,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

enum class CountdownStage(
    val displayName: String,
    val suggestion: String
) {
    BASE_PHASE("基础阶段", "系统学习知识点，建立知识网络"),
    STRENGTHEN_PHASE("强化阶段", "重点突破高频考点，开始真题练习"),
    SPRINT_PHASE("冲刺阶段", "减少新学，全力复习，模考训练"),
    FINAL_SPRINT("最后冲刺", "只复习不学新，巩固精确档内容"),
    FINAL_WEEK("最后一周", "调整心态，回顾薄弱，保证睡眠"),
    EXAM_DAY("考试日", "加油！")
}
```

### 3.6 模块五：AI助手（AIAssistant）

#### 3.6.1 设计目标

利用大模型API为考研复习提供智能增强，核心解决三个高价值问题：论述题/写作批改、知识点智能问答、真题答题框架生成。

**设计原则**：
- AI是增强，不是必需——App核心功能完全离线可用，AI功能需要联网+API key
- 用户自带API key，费用自理，App不绑定任何特定服务商
- 支持多API切换（DeepSeek/通义千问/智谱/月之暗面/OpenAI兼容接口）

#### 3.6.2 智能悬浮窗（核心交互）

**悬浮窗形态**：
- 默认隐藏，在AI助手界面设置中开启
- 开启后屏幕右边缘出现半圆形悬浮按钮（可拖动到任意边缘位置）
- 点击展开为对话面板（占屏幕60%高度，从底部弹出）
- 再次点击或点击外部区域收起
- 悬浮按钮带未读提示（有上下文推荐问题时显示小红点）

**上下文识别**（关键创新）：
悬浮窗能感知当前所在页面，自动构建上下文prompt：

```kotlin
data class AIContext(
    val screenType: ScreenType,    // 当前页面类型
    val title: String,             // 当前页面标题
    val content: String,           // 当前页面核心内容
    val suggestedQuestions: List<String>  // 推荐问题
)

// 根据页面类型自动生成上下文
when (screenType) {
    ScreenType.KNOWLEDGE_DETAIL -> AIContext(
        title = "知识点：${point.title}",
        content = point.fullContent,
        suggestedQuestions = listOf(
            "详细解释这个知识点",
            "这个知识点怎么背最有效？",
            "这个知识点可能怎么考？"
        )
    )
    ScreenType.EXAM_DETAIL -> AIContext(
        title = "${question.year}年真题（${question.score}分）",
        content = question.content,
        suggestedQuestions = listOf(
            "生成答题框架",
            "这道题考查什么知识点？",
            "给我一个高分答案示例"
        )
    )
    ScreenType.MEMO_MODE -> AIContext(
        title = "正在背诵：${point.title}",
        content = point.coreConclusion,
        suggestedQuestions = listOf(
            "我哪里背错了？",
            "这个知识点的记忆技巧？"
        )
    )
    ScreenType.WRITING_PRACTICE -> AIContext(
        title = "写作练习",
        content = userDraft,
        suggestedQuestions = listOf(
            "批改我的作文",
            "我的文章结构有什么问题？",
            "如何提升文采？"
        )
    )
}
```

#### 3.6.3 三大核心功能

**功能1：论述题/写作智能批改（最大价值）**

针对专业写作（150分）和论述题，提供专业级批改：

- **用户操作**：在真题详情页或写作练习页写完答案后，点击"AI批改"
- **AI任务**：
  1. 分项打分：论点（30分）+ 论据（30分）+ 结构（20分）+ 语言（20分）
  2. 优点指出：哪里写得好
  3. 问题诊断：哪里薄弱
  4. 具体修改建议：逐段修改
  5. 范文对照：AI生成参考范文
- **数据结构**：

```kotlin
data class AIGradingResult(
    val totalScore: Int,            // 总分
    val scores: Map<String, Int>,   // 分项得分
    val strengths: List<String>,    // 优点
    val weaknesses: List<String>,   // 不足
    val suggestions: List<String>,  // 修改建议
    val revisedText: String,        // 修改后的版本
    val referenceEssay: String      // AI参考范文
)
```

**功能2：知识点智能问答**

- **用户操作**：在悬浮窗或AI助手Tab直接提问
- **AI任务**：基于考研语境回答，引导深度理解而非简单背诵
- **上下文增强**：悬浮窗自动带上当前知识点作为上下文，用户可直接问"详细解释""这个怎么背"
- **对话历史**：保存最近30天对话记录，可回看

**功能3：真题答题框架生成**

- **用户操作**：在真题详情页点击"AI生成答题框架"
- **AI任务**：
  1. 分析考查角度和知识点
  2. 生成分条论述框架（①②③④）
  3. 标注每个要点的分值预估
  4. 提示常见答题误区
- **结果可保存**：生成的框架可保存到该真题的"answerFramework"字段

#### 3.6.4 API配置与多服务商支持

**支持的API**（都兼容OpenAI格式）：

| 服务商 | 模型推荐 | 价格 | 特点 |
|--------|---------|------|------|
| DeepSeek | deepseek-chat | 输入¥1/百万token，输出¥2 | 性价比最高，中文优秀 |
| 通义千问 | qwen-max | 输入¥20/百万token | 阿里出品，文学知识丰富 |
| 智谱AI | glm-4 | 输入¥5/百万token | 清华系，学术能力强 |
| 月之暗面 | moonshot-v1-8k | 输入¥12/百万token | 长文本处理强 |
| OpenAI兼容 | gpt-4o等 | - | 国际模型，需代理 |

**API配置数据结构**：

```kotlin
data class ApiConfig(
    val provider: String,          // 服务商标识：deepseek/qwen/zhipu/moonshot/custom
    val displayName: String,       // 显示名称
    val baseUrl: String,           // API地址（如"https://api.deepseek.com/v1"）
    val apiKey: String,            // 加密存储的API key
    val model: String,             // 使用的模型名
    val temperature: Float = 0.7f, // 温度参数
    val maxTokens: Int = 2000,     // 最大token数
    val isEnabled: Boolean         // 是否启用
)
```

**安全措施**：
- API key用Android Keystore加密存储，不明文保存
- 请求通过HTTPS加密传输
- 不记录用户API key到日志
- 支持多套配置切换，用户可保存多个API配置

#### 3.6.5 离线降级策略

- 无网络或未配置API时：悬浮窗隐藏，AI助手Tab显示"请配置API并联网使用"
- 有网络但API调用失败：显示友好错误提示，不影响其他功能
- API余额不足等错误：明确提示用户检查API配置

#### 3.6.6 评分维度体系（防虚高策略）

为保证AI批改的专业性和客观性，建立**分题型评分维度体系**，每个维度有明确的权重和评分标准，防止AI"虚高评分"。

**论述题五维评分（满分20分示例）**：

| 维度 | 权重 | 满分 | 评分标准 |
|------|------|------|---------|
| 论点 | 25% | 5分 | 论点是否明确、有判断力、有深度；是否回应题干 |
| 论据 | 25% | 5分 | 论据是否具体（作品名+人物/情节/诗句）；是否准确无误 |
| 结构 | 20% | 4分 | 是否分条论述（①②③）；层次是否清晰；逻辑是否递进 |
| 语言 | 15% | 3分 | 是否学术规范；是否有文采；是否有病句 |
| 学术性 | 15% | 3分 | 是否引用文论；是否有理论视角；是否有独立见解 |

**文学评论写作六维评分（满分100分示例）**：

| 维度 | 权重 | 满分 | 评分标准 |
|------|------|------|---------|
| 思想深度 | 25% | 25分 | 是否有独立见解；是否触及作品深层意蕴 |
| 理论运用 | 25% | 25分 | 是否恰当运用文学理论；理论与文本是否结合 |
| 文本细读 | 20% | 20分 | 是否有具体的文本分析；引用是否准确 |
| 结构 | 15% | 15分 | 标题是否规范；结构是否完整；层次是否清晰 |
| 语言 | 10% | 10分 | 是否学术规范；是否有文采；用"笔者"而非"我" |
| 规范 | 5% | 5分 | 字数是否达标；格式是否规范；有无错别字 |

**防虚高策略**（4重保障）：

1. **Prompt约束**：在System Prompt中明确要求"严格按评分标准打分，不得虚高，平均分应低于满分的70%"
2. **分数天花板**：单次评分最高88分（满分100），需AI额外说明"为何给高分"
3. **二次校验**：评分>85分时，自动触发二次校验，让AI重新评估
4. **用户反馈**：用户可对评分"偏高/偏低/合理"反馈，用于优化Prompt

**数据结构**：

```kotlin
data class GradingDimension(
    val name: String,           // 维度名称
    val weight: Float,          // 权重 0-1
    val maxScore: Int,          // 满分
    val score: Int,             // 实际得分
    val feedback: String,       // 该维度的反馈
    val evidence: List<String>  // 评分依据（引用用户答案的具体段落）
)

data class AIGradingResult(
    val totalScore: Int,                    // 总分
    val maxScore: Int,                      // 满分
    val dimensions: List<GradingDimension>, // 分维度得分
    val strengths: List<String>,            // 优点
    val weaknesses: List<String>,           // 不足
    val suggestions: List<String>,          // 修改建议（逐段）
    val revisedText: String?,               // 修改后的版本
    val referenceEssay: String?,            // AI参考范文
    val gradingMode: GradingMode,           // 评分模式
    val isHighScoreVerified: Boolean        // 高分二次校验标记
)

enum class GradingMode {
    ESSAY,       // 论述题批改
    WRITING,     // 写作批改
    FRAMEWORK,   // 框架生成
    Q_A          // 知识点问答
}
```

#### 3.6.7 Prompt工程

针对不同任务设计专用Prompt，确保AI输出专业、规范、可控。

**论述题批改 System Prompt**：

```
你是一位资深的中国文学考研阅卷老师，专攻南京师范大学现当代文学专业。
你的任务是批改考生的论述题答案，给出专业、客观、严格的评分。

评分原则：
1. 严格按五维评分标准打分（论点25%/论据25%/结构20%/语言15%/学术性15%）
2. 不得虚高评分，平均分应低于满分的70%
3. 论据必须具体（作品名+人物/情节/诗句），泛泛而谈扣分
4. 必须分条论述（①②③），不分条扣分
5. 引用文论加分，但必须准确

输出格式（严格遵循）：
<score>总分/满分</score>
<dimensions>
  <dimension name="论点" score="X" max="Y">反馈内容</dimension>
  <dimension name="论据" score="X" max="Y">反馈内容</dimension>
  ...
</dimensions>
<strengths>
  <item>优点1</item>
  <item>优点2</item>
</strengths>
<weaknesses>
  <item>不足1</item>
  <item>不足2</item>
</weaknesses>
<suggestions>
  <item>修改建议1</item>
  <item>修改建议2</item>
</suggestions>
<revised>修改后的完整答案（可选）</revised>
```

**论述题批改 User Prompt 模板**：

```
## 题目
{question_content}

## 题目信息
- 题型：{question_type}
- 分值：{score}分
- 考查角度：{angle}

## 参考答案（如有）
{reference_answer}

## 考生答案
{user_answer}

## 使用模板（如有）
{template_name}

请按评分标准批改，输出格式严格遵循System Prompt的要求。
```

**写作批改 System Prompt**：

```
你是一位资深的中国文学评论写作阅卷老师，专攻南京师范大学现当代文学专业。
你的任务是批改考生的文学评论写作，给出专业、客观、严格的评分。

评分原则：
1. 严格按六维评分标准打分（思想深度25%/理论运用25%/文本细读20%/结构15%/语言10%/规范5%）
2. 不得虚高评分，平均分应低于满分的65%（写作要求更高）
3. 必须有具体的文本细读，泛泛而谈扣分
4. 理论运用必须恰当，生搬硬套扣分
5. 标题必须规范（大标题+副标题），不规范扣分
6. 用"笔者"而非"我"，用语不规范扣分

输出格式：同论述题批改格式，但维度改为六维。
```

**Few-shot 示例**（在System Prompt中提供1-2个示例）：

```
## 示例：苏轼词史贡献（14/20分）

<score>14/20</score>
<dimensions>
  <dimension name="论点" score="3" max="5">论点基本明确，但"以诗为词"的判断稍显笼统，未充分展开</dimension>
  <dimension name="论据" score="3" max="5">提到《念奴娇·赤壁怀古》《水调歌头》，但未引用具体诗句分析</dimension>
  <dimension name="结构" score="4" max="4">分条论述，层次清晰</dimension>
  <dimension name="语言" score="2" max="3">基本通顺，但"苏轼的词很好"等表述过于口语化</dimension>
  <dimension name="学术性" score="2" max="3">未引用文论，缺乏理论视角</dimension>
</dimensions>
...
```

**答题框架生成 Prompt**：

```
## 任务
为以下考研真题生成答题框架。

## 题目
{question_content}

## 题目信息
- 题型：{question_type}
- 分值：{score}分
- 考查角度：{angle}

## 要求
1. 按"{template_structure}"结构生成框架
2. 每个要点标注预估分值
3. 每个要点提供1-2个具体论据（作品名+人物/情节/诗句）
4. 提示常见答题误区
5. 总分值必须等于{score}分

## 输出格式
<framework>
  <section title="破题" score="X">
    <content>破题内容</content>
  </section>
  <section title="主体1" score="X">
    <content>要点内容</content>
    <evidence>论据1</evidence>
    <evidence>论据2</evidence>
  </section>
  ...
  <pitfalls>
    <pitfall>常见误区1</pitfall>
    <pitfall>常见误区2</pitfall>
  </pitfalls>
</framework>
```

**知识点问答 Prompt**：

```
## 任务
回答考研专业课相关问题，基于考研语境，引导深度理解而非简单背诵。

## 上下文
当前知识点：{point_title}
知识点内容：{point_full_content}

## 用户问题
{user_question}

## 回答要求
1. 回答需结合考研语境，突出考点
2. 引导深度理解，而非简单复述教材
3. 如适用，提供记忆技巧
4. 如适用，提示可能的考查角度
5. 回答控制在300-500字
```

#### 3.6.8 流式JSON解析

AI批改结果需要结构化存储，但大模型流式输出是逐字返回的，无法直接解析为JSON。采用**标签分隔方案**解决此问题。

**方案设计**：

```
大模型流式输出（逐字） → 标签分隔的半结构化文本 → 解析为结构化数据
```

AI输出使用XML风格标签（如`<score>14</score>`），而非JSON，原因：
1. 标签分隔对流式输出友好，可逐段解析
2. 大模型对XML标签的遵循度高于JSON
3. 即使部分标签缺失，也能提取已返回的部分

**流式解析器实现**：

```kotlin
class StreamingTagParser {
    private val buffer = StringBuilder()
    private val parsedSections = mutableMapOf<String, String>()
    private var currentTag: String? = null
    private var currentContent = StringBuilder()

    /**
     * 增量解析：每收到一段流式文本就调用一次
     */
    fun feed(chunk: String) {
        buffer.append(chunk)
        while (buffer.isNotEmpty()) {
            val text = buffer.toString()
            val openTagMatch = Regex("<(\\w+)>").find(text)
            val closeTagMatch = Regex("</(\\w+)>").find(text)

            when {
                currentTag != null -> {
                    // 正在读取标签内容
                    val closeIndex = text.indexOf("</$currentTag>")
                    if (closeIndex >= 0) {
                        // 标签闭合
                        currentContent.append(text.substring(0, closeIndex))
                        parsedSections[currentTag!!] = currentContent.toString().trim()
                        buffer.delete(0, closeIndex + currentTag!!.length + 3)
                        currentTag = null
                        currentContent = StringBuilder()
                    } else {
                        // 标签未闭合，保留未完成部分
                        val safeLength = text.length - currentTag!!.length - 3
                        if (safeLength > 0) {
                            currentContent.append(text.substring(0, safeLength))
                            buffer.delete(0, safeLength)
                        }
                        break
                    }
                }
                openTagMatch != null -> {
                    // 遇到开标签
                    val tagStart = openTagMatch.range.first
                    if (tagStart > 0) {
                        buffer.delete(0, tagStart)  // 跳过标签前的文本
                    }
                    currentTag = openTagMatch.groupValues[1]
                    buffer.delete(0, openTagMatch.range.last + 1)
                }
                else -> break
            }
        }
    }

    /**
     * 获取已解析的部分（用于实时UI更新）
     */
    fun getParsedSections(): Map<String, String> = parsedSections.toMap()

    /**
     * 解析完成后，构建完整的AIGradingResult
     */
    fun buildResult(maxScore: Int): AIGradingResult {
        val totalScore = parsedSections["score"]?.split("/")?.firstOrNull()?.trim()?.toIntOrNull() ?: 0
        val dimensions = parseDimensions(parsedSections["dimensions"] ?: "")
        val strengths = parseList(parsedSections["strengths"], "item")
        val weaknesses = parseList(parsedSections["weaknesses"], "item")
        val suggestions = parseList(parsedSections["suggestions"], "item")
        val revisedText = parsedSections["revised"]
        val referenceEssay = parsedSections["reference"]

        return AIGradingResult(
            totalScore = totalScore,
            maxScore = maxScore,
            dimensions = dimensions,
            strengths = strengths,
            weaknesses = weaknesses,
            suggestions = suggestions,
            revisedText = revisedText,
            referenceEssay = referenceEssay,
            gradingMode = GradingMode.ESSAY,
            isHighScoreVerified = totalScore < (maxScore * 0.85).toInt()
        )
    }
}
```

**UI实时更新**：

流式解析过程中，UI实时显示已解析的部分：
- 收到`<score>`后，立即显示总分
- 收到`<dimensions>`中的每个`<dimension>`后，立即显示该维度得分
- 收到`<strengths>/<weaknesses>/<suggestions>`后，逐条显示

用户体验：打字机效果，3-5秒内逐步呈现完整批改结果。

#### 3.6.9 悬浮窗技术实现方案（FloatingX）

**技术选型**：FloatingX 1.3.x（App级免权限悬浮窗库，支持Compose内容）

**依赖配置**：

```kotlin
// build.gradle.kts (app模块)
dependencies {
    implementation("com.github.petterpx:FloatingX:1.3.2")
    // 注意：FloatingX从1.3.0开始支持Compose内容
}
```

**悬浮窗管理器**：

```kotlin
@Singleton
class FloatingWindowManager @Inject constructor(
    private val context: Context,
    private val aiClient: AiStreamClient?,
    private val contextProvider: AppContextProvider
) {
    private var floatingView: View? = null
    private var悬浮窗Scope: CoroutineScope? = null

    /**
     * 显示悬浮窗（App级，无需SYSTEM_ALERT_WINDOW权限）
     * 关键：使用 fx.show(activity) 而非 fx.show()
     */
    fun show(activity: Activity) {
        val fxConfig = FloatingConfig.Builder()
            .setShowMode(FloatingConfig.ShowMode.APP_FOREGROUND)  // App前台时显示
            .setEdgeAbsorbed(true)                                 // 贴边吸附
            .setScrollEdge(true)                                   // 边缘滚动
            .setAnimationStyle(FloatingConfig.AnimationStyle.DEFAULT)
            .build()

        FloatingX.with(activity)
            .setConfig(fxConfig)
            .setContent(createComposeContent())
            .show()
    }

    /**
     * Compose内容：悬浮按钮 + 展开面板
     */
    private fun createComposeContent(): View {
        return ComposeView(context).apply {
            setViewCompositionStrategy(
                ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
            )
            setContent {
                MaterialTheme {
                    FloatingWindowContent(
                        contextProvider = contextProvider,
                        aiClient = aiClient,
                        onExpand = { /* 展开面板 */ },
                        onCollapse = { /* 收起面板 */ }
                    )
                }
            }
        }
    }

    fun hide(activity: Activity) {
        FloatingX.hide(activity)
    }

    fun isShowing(activity: Activity): Boolean {
        return FloatingX.isShowing(activity)
    }
}

@Composable
fun FloatingWindowContent(
    contextProvider: AppContextProvider,
    aiClient: AiStreamClient?,
    onExpand: () -> Unit,
    onCollapse: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var aiContext by remember { mutableStateOf<AIContext?>(null) }
    var messages by remember { mutableStateOf(listOf<ChatMessage>()) }
    var isLoading by remember { mutableStateOf(false) }

    // 监听当前页面上下文
    LaunchedEffect(Unit) {
        contextProvider.currentContext.collect { ctx ->
            aiContext = ctx
        }
    }

    if (!isExpanded) {
        // 收起状态：半圆形悬浮按钮
        FloatingActionButton(
            onClick = { isExpanded = true; onExpand() },
            modifier = Modifier.size(48.dp),
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Icon(Icons.Default.AutoAwesome, contentDescription = "AI助手")
        }
    } else {
        // 展开状态：对话面板（占屏幕60%高度）
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(8.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // 标题栏：显示当前上下文
                aiContext?.let { ctx ->
                    Text(
                        text = "上下文：${ctx.title}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                // 对话区域
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    reverseLayout = true
                ) {
                    items(messages.reversed()) { msg ->
                        ChatBubble(msg)
                    }
                }

                // 推荐问题（基于上下文）
                aiContext?.suggestedQuestions?.take(3)?.let { questions ->
                    if (messages.isEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            questions.forEach { q ->
                                AssistChip(
                                    onClick = { sendMessage(q, aiClient, aiContext) { messages = it; isLoading = it2 } },
                                    label = { Text(q, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                // 输入框
                var inputText by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("提问...") },
                        enabled = !isLoading
                    )
                    IconButton(
                        onClick = { sendMessage(inputText, aiClient, aiContext) { messages = it; isLoading = it2 }; inputText = "" }
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "发送")
                    }
                }

                // 收起按钮
                TextButton(onClick = { isExpanded = false; onCollapse() }) {
                    Text("收起")
                }
            }
        }
    }
}
```

#### 3.6.10 上下文识别双层方案

悬浮窗需要感知当前所在页面，自动构建上下文prompt。采用**双层方案**：粗粒度兜底 + 细粒度主动上报。

**第一层：Navigation路由监听（粗粒度兜底）**：

```kotlin
/**
 * 监听Navigation路由变化，粗粒度识别当前页面
 * 优势：无需每个页面单独配置，自动生效
 * 劣势：只能识别页面类型，无法获取具体内容
 */
class NavigationContextWatcher : NavController.OnDestinationChangedListener {
    private val _currentRoute = MutableStateFlow<String?>(null)
    val currentRoute: StateFlow<String?> = _currentRoute

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        _currentRoute.value = destination.route
    }

    /**
     * 根据路由推断ScreenType
     */
    fun inferScreenType(route: String?): ScreenType {
        return when {
            route?.startsWith("knowledge_detail") == true -> ScreenType.KNOWLEDGE_DETAIL
            route?.startsWith("exam_detail") == true -> ScreenType.EXAM_DETAIL
            route?.startsWith("memo_mode") == true -> ScreenType.MEMO_MODE
            route?.startsWith("writing_practice") == true -> ScreenType.WRITING_PRACTICE
            else -> ScreenType.UNKNOWN
        }
    }
}
```

**第二层：CompositionLocal主动上报（细粒度）**：

```kotlin
/**
 * 每个页面主动上报详细上下文
 * 优势：可获取页面具体内容（知识点标题、真题内容等）
 * 劣势：需每个页面单独配置
 */
val LocalPageReporter = compositionLocalOf<PageReporter?> { null }

interface PageReporter {
    fun reportContext(context: AIContext)
}

@Composable
fun KnowledgeDetailScreen(
    pointId: String,
    pointRepo: KnowledgePointRepository,
    pageReporter: PageReporter = LocalPageReporter.current ?: return
) {
    val point by produceState<KnowledgePoint?>(null, pointId) {
        value = pointRepo.getPoint(pointId)
    }

    // 页面加载后，主动上报上下文
    LaunchedEffect(point) {
        point?.let { p ->
            pageReporter.reportContext(
                AIContext(
                    screenType = ScreenType.KNOWLEDGE_DETAIL,
                    title = "知识点：${p.title}",
                    content = p.fullContent,
                    suggestedQuestions = listOf(
                        "详细解释这个知识点",
                        "这个知识点怎么背最有效？",
                        "这个知识点可能怎么考？"
                    )
                )
            )
        }
    }

    // ... 页面内容 ...
}
```

**AppContextProvider：整合双层信息**：

```kotlin
@Singleton
class AppContextProvider @Inject constructor(
    private val navWatcher: NavigationContextWatcher
) {
    private val _currentContext = MutableStateFlow<AIContext?>(null)
    val currentContext: StateFlow<AIContext?> = _currentContext

    init {
        // 监听路由变化，作为兜底
        navWatcher.currentRoute.onEach { route ->
            val screenType = navWatcher.inferScreenType(route)
            if (_currentContext.value?.screenType != screenType) {
                // 路由变化但未收到主动上报，用粗粒度兜底
                _currentContext.value = AIContext(
                    screenType = screenType,
                    title = "",
                    content = "",
                    suggestedQuestions = emptyList()
                )
            }
        }.launchIn(CoroutineScope(Dispatchers.Main))
    }

    /**
     * 接收页面的主动上报（细粒度）
     */
    fun reportContext(context: AIContext) {
        _currentContext.value = context
    }
}
```

**国产ROM兼容**：

悬浮窗在国产ROM上需要特殊处理权限跳转：

```kotlin
object RomPermissionHelper {
    fun jumpToPermissionSettings(activity: Activity) {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intent = when {
            manufacturer.contains("xiaomi") -> {
                // MIUI
                Intent().apply {
                    setClassName("com.miui.securitycenter",
                        "com.miui.permcenter.permissions.PermissionsEditorActivity")
                    putExtra("extra_pkgname", activity.packageName)
                }
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> {
                // EMUI / HarmonyOS
                Intent().apply {
                    setClassName("com.huawei.systemmanager",
                        "com.huawei.permissionmanager.ui.MainActivity")
                }
            }
            manufacturer.contains("oppo") -> {
                // ColorOS
                Intent().apply {
                    setClassName("com.coloros.safecenter",
                        "com.coloros.safecenter.permission.PermissionCenterActivity")
                }
            }
            manufacturer.contains("vivo") -> {
                // OriginOS / FuntouchOS
                Intent().apply {
                    setClassName("com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
                }
            }
            else -> {
                // 原生Android
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
            }
        }
        try {
            activity.startActivity(intent)
        } catch (e: Exception) {
            // 兜底：跳转系统设置
            activity.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
```

> **注意**：FloatingX的App级悬浮窗（`fx.show(activity)`）在大多数国产ROM上**无需**SYSTEM_ALERT_WINDOW权限即可使用，但部分ROM（MIUI旧版）可能仍需手动授权。App在首次显示悬浮窗时检测并引导用户授权。

---

## 四、数据架构

### 4.1 本地数据库（Room）

纯本地存储，完全离线可用。

#### 4.1.1 核心表结构

```sql
-- 科目表
CREATE TABLE subjects (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    short_name TEXT NOT NULL,
    sort_order INTEGER NOT NULL
);

-- 章节表
CREATE TABLE chapters (
    id TEXT PRIMARY KEY,
    subject_id TEXT NOT NULL,
    parent_id TEXT,           -- 支持多级（编→章→节）
    title TEXT NOT NULL,
    sort_order INTEGER NOT NULL,
    FOREIGN KEY (subject_id) REFERENCES subjects(id)
);

-- 知识点表
CREATE TABLE knowledge_points (
    id TEXT PRIMARY KEY,
    chapter_id TEXT NOT NULL,
    title TEXT NOT NULL,
    summary TEXT,
    core_conclusion TEXT NOT NULL,
    full_content TEXT NOT NULL,
    multi_perspectives TEXT,  -- JSON序列化
    related_ids TEXT,         -- JSON数组
    contrast_ids TEXT,        -- JSON数组
    extension_ids TEXT,       -- JSON数组
    exam_records TEXT,        -- JSON数组
    exam_frequency TEXT NOT NULL,  -- HIGH/MEDIUM/LOW/NEVER
    term_template TEXT,       -- JSON
    tags TEXT,                -- JSON数组
    difficulty INTEGER DEFAULT 3,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (chapter_id) REFERENCES chapters(id)
);

-- 真题表
CREATE TABLE exam_questions (
    id TEXT PRIMARY KEY,
    year INTEGER NOT NULL,
    subject_id TEXT NOT NULL,
    question_type TEXT NOT NULL,
    content TEXT NOT NULL,
    score INTEGER NOT NULL,
    angle TEXT,
    related_point_ids TEXT,   -- JSON数组
    answer_framework TEXT,
    sample_essay TEXT,
    notes TEXT,
    created_at INTEGER NOT NULL
);

-- 背诵记录表（FSRS算法）
CREATE TABLE memo_records (
    point_id TEXT PRIMARY KEY,
    state TEXT NOT NULL,           -- NEW/LEARNING/REVIEW/RELEARNING
    stability REAL DEFAULT 0.0,    -- 记忆稳定性（FSRS核心变量）
    difficulty REAL DEFAULT 5.0,   -- 难度1-10（FSRS核心变量）
    last_review_at INTEGER NOT NULL,
    next_review_at INTEGER NOT NULL,
    review_count INTEGER DEFAULT 0,
    fail_count INTEGER DEFAULT 0,
    history TEXT,                  -- JSON数组
    in_priority_queue INTEGER DEFAULT 0,
    FOREIGN KEY (point_id) REFERENCES knowledge_points(id)
);

-- 学习进度表
CREATE TABLE study_progress (
    id TEXT PRIMARY KEY,
    last_point_id TEXT,       -- 上次学到的知识点
    last_visited_at INTEGER,
    total_study_time INTEGER, -- 累计学时（秒）
    streak_days INTEGER,      -- 连续打卡天数
    last_check_in INTEGER     -- 上次打卡时间
);

-- 写作素材表
CREATE TABLE writing_materials (
    id TEXT PRIMARY KEY,
    category TEXT NOT NULL,   -- QUOTE / THEORY / EVIDENCE / TEMPLATE / ESSAY
    sub_category TEXT,        -- 主题分类
    content TEXT NOT NULL,
    source TEXT,
    tags TEXT,
    created_at INTEGER NOT NULL
);

-- AI API配置表
CREATE TABLE api_configs (
    id TEXT PRIMARY KEY,
    provider TEXT NOT NULL,        -- deepseek/qwen/zhipu/moonshot/custom
    display_name TEXT NOT NULL,
    base_url TEXT NOT NULL,
    api_key TEXT NOT NULL,         -- 加密存储
    model TEXT NOT NULL,
    temperature REAL DEFAULT 0.7,
    max_tokens INTEGER DEFAULT 2000,
    is_enabled INTEGER DEFAULT 1,
    is_current INTEGER DEFAULT 0,  -- 当前使用的配置
    created_at INTEGER NOT NULL
);

-- AI对话历史表
CREATE TABLE chat_history (
    id TEXT PRIMARY KEY,
    role TEXT NOT NULL,            -- USER / ASSISTANT / SYSTEM
    content TEXT NOT NULL,
    context_screen TEXT,           -- 对话时的页面类型
    context_title TEXT,            -- 上下文标题
    context_content TEXT,          -- 上下文内容
    api_config_id TEXT,
    tokens_used INTEGER,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (api_config_id) REFERENCES api_configs(id)
);

-- AI批改记录表
CREATE TABLE ai_grading_records (
    id TEXT PRIMARY KEY,
    exam_question_id TEXT,         -- 关联真题（如有）
    user_answer TEXT NOT NULL,     -- 用户提交的答案
    grading_result TEXT NOT NULL,  -- JSON: AIGradingResult
    api_config_id TEXT,
    tokens_used INTEGER,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (exam_question_id) REFERENCES exam_questions(id)
);

-- 答题模板表（预置+自定义）
CREATE TABLE answer_templates (
    id TEXT PRIMARY KEY,
    question_type TEXT NOT NULL,        -- TERM_EXPLANATION / SHORT_ANSWER / ESSAY / WRITING
    name TEXT NOT NULL,                 -- 模板名称
    structure TEXT NOT NULL,            -- JSON: List<TemplateSection>
    applicable_tags TEXT,               -- JSON: List<String>
    example_usage TEXT,
    score_range_min INTEGER,
    score_range_max INTEGER,
    word_limit_min INTEGER,
    word_limit_max INTEGER,
    is_builtin INTEGER NOT NULL DEFAULT 1,  -- 1=预置, 0=用户自定义
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

-- 用户填写的答题记录
CREATE TABLE template_fills (
    id TEXT PRIMARY KEY,
    template_id TEXT NOT NULL,
    exam_question_id TEXT,              -- 关联真题（可为空，自由练习）
    filled_sections TEXT,               -- JSON: Map<String, String>
    filled_fields TEXT,                 -- JSON: Map<String, String>
    total_text TEXT NOT NULL,           -- 拼接后的完整答案
    word_count INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (template_id) REFERENCES answer_templates(id),
    FOREIGN KEY (exam_question_id) REFERENCES exam_questions(id)
);

-- 写作模板（开头/论证/结尾）
CREATE TABLE writing_patterns (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,                 -- 如"破题式开头"
    category TEXT NOT NULL,             -- OPENING / ARGUMENT / ENDING
    description TEXT,
    template TEXT NOT NULL,             -- 模板文本（含占位符）
    example TEXT,
    applicable_genres TEXT,             -- JSON: List<String>（小说/诗歌/散文/戏剧）
    is_builtin INTEGER NOT NULL DEFAULT 1,
    created_at INTEGER NOT NULL
);

-- 关系网络节点表
CREATE TABLE graph_nodes (
    id TEXT PRIMARY KEY,
    type TEXT NOT NULL,                 -- AUTHOR / WORK / SCHOOL / MOVEMENT / CONCEPT / KNOWLEDGE_POINT
    label TEXT NOT NULL,                -- 显示名称
    subtitle TEXT,                      -- 副标题（如"1037-1101"）
    size REAL DEFAULT 1.0,             -- 节点大小
    color INTEGER NOT NULL,            -- 节点颜色
    related_point_id TEXT,             -- 关联知识点ID
    subject_id TEXT,                   -- 所属科目
    metadata TEXT,                     -- JSON: 额外元数据
    FOREIGN KEY (related_point_id) REFERENCES knowledge_points(id),
    FOREIGN KEY (subject_id) REFERENCES subjects(id)
);

-- 关系网络边表
CREATE TABLE graph_edges (
    id TEXT PRIMARY KEY,
    source_id TEXT NOT NULL,
    target_id TEXT NOT NULL,
    type TEXT NOT NULL,                 -- AUTHORED / BELONGS_TO / PARTICIPATED_IN / INFLUENCED_BY / COMPARED_WITH / SAME_PERIOD / PRECEDES / RELATED_CONCEPT
    weight REAL DEFAULT 1.0,
    label TEXT,
    FOREIGN KEY (source_id) REFERENCES graph_nodes(id),
    FOREIGN KEY (target_id) REFERENCES graph_nodes(id)
);

-- AI对话记录表（支持悬浮窗和AI助手Tab）
CREATE TABLE ai_conversations (
    id TEXT PRIMARY KEY,
    role TEXT NOT NULL,                 -- USER / ASSISTANT / SYSTEM
    content TEXT NOT NULL,
    context_screen_type TEXT,           -- 对话时的页面类型
    context_title TEXT,                 -- 对话时的上下文标题
    context_content TEXT,               -- 对话时的上下文内容
    api_config_id TEXT,
    tokens_used INTEGER,
    is_bookmarked INTEGER DEFAULT 0,    -- 是否收藏
    created_at INTEGER NOT NULL,
    FOREIGN KEY (api_config_id) REFERENCES api_configs(id)
);

-- 知识点关键词表（用于主动回忆检测L1）
CREATE TABLE knowledge_keywords (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    point_id TEXT NOT NULL,
    keyword TEXT NOT NULL,              -- 关键词
    is_required INTEGER DEFAULT 1,      -- 是否必须出现
    FOREIGN KEY (point_id) REFERENCES knowledge_points(id)
);

-- 知识点要点表（用于主动回忆检测L2）
CREATE TABLE knowledge_key_points (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    point_id TEXT NOT NULL,
    content TEXT NOT NULL,              -- 要点内容
    synonyms TEXT,                     -- JSON: List<String> 同义表达模式
    sort_order INTEGER DEFAULT 0,
    FOREIGN KEY (point_id) REFERENCES knowledge_points(id)
);

-- 学习统计表（按日聚合）
CREATE TABLE study_stats (
    date TEXT PRIMARY KEY,              -- 日期 YYYY-MM-DD
    review_count INTEGER DEFAULT 0,     -- 当日复习数
    new_count INTEGER DEFAULT 0,        -- 当日新学数
    study_minutes INTEGER DEFAULT 0,    -- 当日学习时长
    exact_tier_count INTEGER DEFAULT 0, -- 精确档复习数
    framework_tier_count INTEGER DEFAULT 0,
    understand_tier_count INTEGER DEFAULT 0,
    ai_grading_count INTEGER DEFAULT 0, -- AI批改次数
    template_fill_count INTEGER DEFAULT 0, -- 模板答题次数
    writing_practice_count INTEGER DEFAULT 0 -- 写作练习次数
);
```

#### 4.1.2 知识点表加列（FSRS-6 + 三档调度）

```sql
-- knowledge_points 表新增列（对应3.3.4节三档调度）
ALTER TABLE knowledge_points ADD COLUMN memory_tier TEXT NOT NULL DEFAULT 'TIER_FRAMEWORK';
ALTER TABLE knowledge_points ADD COLUMN tier_reason TEXT NOT NULL DEFAULT '';

-- memo_records 表新增列（对应5.4.7节FSRS-6 schema调整）
ALTER TABLE memo_records ADD COLUMN elapsed_days INTEGER DEFAULT 0;
ALTER TABLE memo_records ADD COLUMN scheduled_days INTEGER DEFAULT 0;
ALTER TABLE memo_records ADD COLUMN reps INTEGER DEFAULT 0;
```

#### 4.1.3 索引

```sql
-- 原有索引
CREATE INDEX idx_points_chapter ON knowledge_points(chapter_id);
CREATE INDEX idx_points_frequency ON knowledge_points(exam_frequency);
CREATE INDEX idx_points_memory_tier ON knowledge_points(memory_tier);
CREATE INDEX idx_questions_year ON exam_questions(year);
CREATE INDEX idx_questions_subject ON exam_questions(subject_id);
CREATE INDEX idx_memo_next_review ON memo_records(next_review_at);
CREATE INDEX idx_memo_state ON memo_records(state);
CREATE INDEX idx_grading_question ON ai_grading_records(exam_question_id);

-- 新增索引
CREATE INDEX idx_templates_type ON answer_templates(question_type);
CREATE INDEX idx_fills_template ON template_fills(template_id);
CREATE INDEX idx_fills_question ON template_fills(exam_question_id);
CREATE INDEX idx_graph_nodes_type ON graph_nodes(type);
CREATE INDEX idx_graph_nodes_subject ON graph_nodes(subject_id);
CREATE INDEX idx_graph_edges_source ON graph_edges(source_id);
CREATE INDEX idx_graph_edges_target ON graph_edges(target_id);
CREATE INDEX idx_graph_edges_type ON graph_edges(type);
CREATE INDEX idx_ai_conv_created ON ai_conversations(created_at);
CREATE INDEX idx_ai_conv_bookmarked ON ai_conversations(is_bookmarked);
CREATE INDEX idx_keywords_point ON knowledge_keywords(point_id);
CREATE INDEX idx_keypoints_point ON knowledge_key_points(point_id);
CREATE INDEX idx_review_logs_point ON review_logs(point_id);
CREATE INDEX idx_review_logs_reviewed_at ON review_logs(reviewed_at);
```

### 4.2 数据导入导出

- **导出**：全部数据导出为JSON文件，保存到手机Download目录
- **导入**：从JSON文件恢复数据
- **格式**：版本化JSON，支持后续迁移
- **自动备份**：每7天提醒一次手动备份

### 4.3 种子数据

App首次安装时，预置以下种子数据：
1. 四科基本结构（科目+编+章+节，不含知识点内容）
2. 2020-2024年610文学基础真题（已收录，见调研报告）
3. 2022年805外国文学史真题（已收录）
4. 高频考点清单（基于调研报告整理）
5. 复试F008真题2005-2022年精选

---

## 五、技术架构

### 5.1 技术栈

| 层 | 选型 | 版本 | 说明 |
|----|------|------|------|
| 语言 | Kotlin | 2.0+ | 现代Android开发语言 |
| UI框架 | Jetpack Compose | BOM 2024.06+ | 声明式UI，Google主推 |
| UI组件 | Material3 | 最新稳定版 | Google官方设计系统 |
| 导航 | Navigation Compose | 2.7+ | 官方导航方案 |
| 架构 | MVVM | - | ViewModel + StateFlow |
| 依赖注入 | Hilt | 2.51+ | 官方DI方案 |
| 数据库 | Room | 2.6+ | 官方ORM |
| 偏好存储 | DataStore | 1.1+ | 替代SharedPreferences |
| JSON | Kotlinx Serialization | 1.6+ | 官方序列化 |
| 异步 | Coroutines + Flow | 1.8+ | Kotlin标配 |
| 网络请求 | Retrofit + OkHttp | 2.9+/4.12+ | 调用大模型API |
| 加密存储 | AndroidX Security + Keystore | 1.1+ | API key安全存储 |
| 构建 | Gradle Kotlin DSL | 8.5+ | 现代构建配置 |

### 5.2 项目结构

**多模块架构**（参考Now in Android）：

```
wenyan-android/
├── app/                              # 主应用入口
│   ├── src/main/java/com/wenyan/app/
│   │   ├── WenyanApplication.kt       # Application，Hilt入口
│   │   ├── MainActivity.kt            # 单Activity
│   │   └── navigation/
│   │       └── WenyanNavGraph.kt      # 主导航
│   └── build.gradle.kts
├── core/                             # 核心模块（被各feature依赖）
│   ├── common/                       # 通用工具
│   │   └── src/main/java/com/wenyan/core/common/
│   │       ├── NetworkUtil.kt
│   │       └── DateUtils.kt
│   ├── database/                     # 数据库层
│   │   └── src/main/java/com/wenyan/core/database/
│   │       ├── WenyanDatabase.kt
│   │       ├── dao/                   # 所有DAO
│   │       ├── entity/                # 所有实体
│   │       └── converter/
│   ├── data/                         # Repository层
│   │   └── src/main/java/com/wenyan/core/data/
│   │       ├── repository/
│   │       ├── model/                # 领域模型
│   │       └── seed/                 # 种子数据
│   ├── designsystem/                 # 设计系统
│   │   └── src/main/java/com/wenyan/core/designsystem/
│   │       ├── theme/                # Color/Theme/Type
│   │       └── component/            # 通用组件
│   ├── fsrs/                         # FSRS算法模块
│   │   └── src/main/java/com/wenyan/core/fsrs/
│   │       ├── FSRScheduler.kt
│   │       └── Models.kt
│   └── ai/                          # AI核心模块
│       └── src/main/java/com/wenyan/core/ai/
│           ├── api/                  # LLM API客户端
│           ├── prompt/               # 提示词模板
│           ├── grading/              # 批改解析
│           └── crypto/               # API key加密
├── feature/                          # 功能模块（每个独立）
│   ├── knowledge/                    # 知识图谱
│   │   └── src/main/java/com/wenyan/feature/knowledge/
│   │       ├── KnowledgeScreen.kt
│   │       ├── KnowledgeViewModel.kt
│   │       ├── KnowledgeDetailScreen.kt
│   │       └── KnowledgeSearchScreen.kt
│   ├── memo/                         # 深度背诵
│   │   └── src/main/java/com/wenyan/feature/memo/
│   │       ├── MemoScreen.kt
│   │       ├── MemoViewModel.kt
│   │       ├── ReadModeScreen.kt
│   │       ├── ClozeModeScreen.kt
│   │       ├── OutlineModeScreen.kt
│   │       ├── WriteModeScreen.kt
│   │       └── RecallModeScreen.kt
│   ├── exam/                         # 真题工坊
│   │   └── src/main/java/com/wenyan/feature/exam/
│   │       ├── ExamScreen.kt
│   │       ├── ExamViewModel.kt
│   │       ├── ExamDetailScreen.kt
│   │       ├── MockExamScreen.kt
│   │       └── writing/              # 写作训练子模块
│   ├── ai/                           # AI助手UI
│   │   └── src/main/java/com/wenyan/feature/ai/
│   │       ├── AIScreen.kt
│   │       ├── AIViewModel.kt
│   │       └── FloatingWindow.kt     # 悬浮窗组件
│   └── dashboard/                    # 我的
│       └── src/main/java/com/wenyan/feature/dashboard/
│           ├── DashboardScreen.kt
│           ├── ProgressMapScreen.kt
│           └── SettingsScreen.kt
├── gradle/
│   └── libs.versions.toml            # 版本目录
├── build.gradle.kts                  # 根构建文件
├── settings.gradle.kts
└── gradle.properties
```

**多模块设计原则**（参考NIA）：
- `core/*` 模块被多个feature依赖，提供基础设施
- `feature/*` 模块互相独立，可单独编译测试
- `app` 模块负责组装所有feature，配置导航
- 每个模块有独立的 `build.gradle.kts`
- 用 `libs.versions.toml` 统一管理依赖版本

### 5.3 架构分层

```
┌─────────────────────────────────────────┐
│  UI层 (Compose)                          │
│  Screen + ViewModel + StateFlow          │
│  只持有UI状态，不直接访问数据库           │
├─────────────────────────────────────────┤
│  Repository层                            │
│  封装数据访问逻辑，提供单一数据源         │
│  KnowledgeRepo / MemoRepo / ExamRepo     │
├─────────────────────────────────────────┤
│  Data层                                  │
│  Room DAO + Entity + 类型转换器          │
│  纯本地SQLite操作                        │
└─────────────────────────────────────────┘
```

#### 5.3.1 Material3 主题配置（完全跟随系统壁纸取色）

采用 **Material You** 设计哲学：完全跟随系统壁纸取色（Dynamic Color），让 App 融入用户的个性化环境。Android 12+ 自动从壁纸提取种子色生成完整 ColorScheme；Android 12 以下使用 Material3 默认配色兜底。

**依赖配置**（使用2025年最新稳定版）：

```kotlin
// build.gradle.kts (app模块)
dependencies {
    // Material3 1.3.x（2025年稳定版，完整支持Material You）
    implementation("androidx.compose.material3:material3:1.3.2")
    // 如需自定义种子色生成ColorScheme，引入material-color-utilities
    implementation("com.materialkolor:material-kolor:1.4.0")
}
```

**主题定义**：

```kotlin
/**
 * App主题：完全跟随系统壁纸取色
 * - Android 12+：使用系统Dynamic Color，从壁纸提取种子色
 * - Android 12以下：使用Material3默认配色
 * - 用户可在设置中切换"跟随系统/浅色/深色"模式
 */
@Composable
fun WenyanTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // 完全跟随系统，不再允许关闭Dynamic Color
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    // Android 12+ 完全跟随系统壁纸取色
    // 系统会自动从壁纸提取种子色，生成完整的ColorScheme
    // 用户可在系统设置中选"壁纸颜色"或"基本颜色"（Android 13+）
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        // Android 12 以下：Material3 默认配色（不再强行朱砂红）
        darkTheme -> darkColorScheme()
        else -> lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WenyanTypography,
        shapes = WenyanShapes,
        content = content
    )
}
```

**设置项：主题模式切换**（在"我的"页面）

```kotlin
enum class ThemeMode {
    FOLLOW_SYSTEM,  // 跟随系统（默认，壁纸取色）
    LIGHT,          // 强制浅色
    DARK            // 强制深色
}

// 设置页存储用户选择
@Composable
fun ThemeSettingScreen() {
    val themeMode by rememberThemeMode()  // 从DataStore读取

    RadioButtonGroup(
        options = listOf(
            "跟随系统（壁纸取色）" to ThemeMode.FOLLOW_SYSTEM,
            "浅色模式" to ThemeMode.LIGHT,
            "深色模式" to ThemeMode.DARK
        ),
        selected = themeMode,
        onSelect = { saveThemeMode(it) }
    )
}

// 在 MainActivity 应用主题
@Composable
fun AppRoot() {
    val themeMode by rememberThemeMode()
    val darkTheme = when (themeMode) {
        ThemeMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    WenyanTheme(darkTheme = darkTheme) {
        // App内容
    }
}
```

**为什么完全跟随系统而非自定义品牌色**：

1. **Material You 设计哲学**：Google 在 Android 12 引入 Material You，核心理念是"App 应融入用户的个性化环境"，而非强行展示品牌色
2. **用户体验**：用户换了壁纸，App 配色自动跟随，形成统一的视觉体验
3. **现代 Android 应用趋势**：Google 自家应用（Gmail/Maps/Photos）、第三方主流应用（Twitter/Reddit）都已采用 Dynamic Color
4. **减少设计负担**：无需维护明暗两套配色方案，系统自动生成和谐的色彩
5. **Android 13+ 用户可控**：用户可在系统设置中选择"壁纸颜色"或"基本颜色"，App 无需自己实现颜色选择器

**Typography 与 Shapes（仍自定义）**：

虽然颜色完全跟随系统，但排版和形状保持自定义，体现 App 的个性。

```kotlin
// 排版（基于 Material3 Typography，适度定制字号）
private val WenyanTypography = Typography(
    displayLarge = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
    displayMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),  // 正文16sp
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)

// 形状
private val WenyanShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)
```

**降级兼容**（Android 12 以下）：

```kotlin
// Android 12 以下使用Material3默认配色
// lightColorScheme() / darkColorScheme() 已内置合理的色彩
// 无需自定义颜色，保持Material3原生体验
```

**可选扩展：自定义种子色**（未来版本，非必须）

如未来需要支持用户自定义种子色（如选择"朱砂红"作为种子），可用 `material-kolor` 库：

```kotlin
// 未来扩展：用户可选自定义种子色
val customSeedColor = Color(0xFFB71C1C)  // 朱砂红
val customColorScheme = lightColorSchemeFromSeed(customSeedColor)
// 但默认仍跟随系统壁纸
```

#### 5.3.2 Compose 性能优化原则

文学考研App内容密集（知识点列表、关系网络图、复习队列等），必须遵循Compose性能优化原则，确保流畅体验。

**核心优化原则**：

| 原则 | 说明 | 应用场景 |
|------|------|---------|
| **避免不必要的重组** | 使用`remember`/`derivedStateOf`缓存计算结果 | 知识点列表筛选、复习队列排序 |
| **使用`key`标识列表项** | `LazyColumn`的`items`指定`key`，避免整列重组 | 知识点列表、真题列表、复习队列 |
| **延迟读取State** | Lambda传值优于直接传State，推迟到实际需要时读取 | 长列表中每项的状态 |
| **使用`immutable`集合** | `kotlinx.collections.immutable`的`ImmutableList` | 传给子Composable的列表参数 |
| **避免lambda分配** | 用`remember`缓存lambda，或用方法引用 | 高频回调（如点击事件） |
| **`derivedStateOf`节流** | 多个State合并为一个，减少重组次数 | 搜索框+筛选条件联合筛选 |

**代码示例**：

```kotlin
// ❌ 错误：每次重组都创建新的lambda
@Composable
fun BadKnowledgeList(points: List<KnowledgePoint>) {
    LazyColumn {
        items(points) { point ->
            KnowledgeItem(
                point = point,
                onClick = { /* 每次都创建新lambda */ }
            )
        }
    }
}

// ✅ 正确：使用key + remember缓存lambda
@Composable
fun GoodKnowledgeList(points: List<KnowledgePoint>) {
    val onClickMap = remember(points) {
        points.associateWith { point ->
            { /* 点击逻辑 */ }
        }
    }

    LazyColumn {
        items(
            items = points,
            key = { it.id }  // 指定key，避免不必要重组
        ) { point ->
            KnowledgeItem(
                point = point,
                onClick = onClickMap[point] ?: {}
            )
        }
    }
}

// ✅ 正确：derivedStateOf节流
@Composable
fun FilteredKnowledgeList(
    points: List<KnowledgePoint>,
    searchQuery: String,
    selectedSubject: String?,
    frequencyFilter: Frequency?
) {
    val filteredPoints by remember {
        derivedStateOf {
            points.filter { point ->
                (searchQuery.isBlank() || point.title.contains(searchQuery)) &&
                (selectedSubject == null || point.subjectId == selectedSubject) &&
                (frequencyFilter == null || point.examFrequency == frequencyFilter)
            }
        }
    }

    LazyColumn {
        items(filteredPoints, key = { it.id }) { point ->
            KnowledgeItem(point = point)
        }
    }
}
```

**长列表性能基准**：

| 场景 | 数据量 | 目标帧率 | 关键优化 |
|------|--------|---------|---------|
| 知识点列表 | 500+项 | 60fps | `key` + `LazyColumn` |
| 关系网络图 | 500节点+1000边 | 45-60fps | Canvas + 视口裁剪 |
| 复习队列 | 50项 | 60fps | `key` + 滑动删除 |
| 真题列表 | 100+项 | 60fps | `key` + 分页加载 |

### 5.4 关键算法：FSRS-6 间隔重复调度（v2.0 升级版）

> **重要更新（2026-07-08 v2.0）**：基于 FSRS-Kotlin 库（https://github.com/open-spaced-repetition/FSRS-Kotlin）和 fsrs4anki（https://github.com/open-spaced-repetition/fsrs4anki）的深度调研，修复了原 v1.0 简化版的 5 个严重 bug，升级到完整的 FSRS-6 算法实现。

#### 5.4.1 算法选型与升级说明

采用 **FSRS-6（Free Spaced Repetition Scheduler v6）** 算法，这是 AnkiDroid、flashcards-open-source-app 等现代项目采用的开源算法，基于 AI 优化，比传统 SM-2 和固定艾宾浩斯曲线更精准。

**v1.0 简化版的 5 个 bug（已修复）**：

| Bug | 严重度 | 描述 | 修复方案 |
|-----|--------|------|---------|
| 1. 复习队列排除 REVIEW 状态 | 🔴 致命 | `getTodayReviewQueue` 错误过滤 `state==REVIEW`，导致已学卡片永不被复习 | 改为只排除 `state==NEW` |
| 2. Again 稳定性重置过粗暴 | 🔴 严重 | 固定 `S*0.5`，不考虑当前可提取性 R | 用 `nextForgetStability` 公式：`w[11] * D^(-w[12]) * ((S+1)^w[13] - 1) * exp(-w[14]*(1-R))` |
| 3. 难度更新无均值回归 | 🟡 中等 | 长期按 Hard 陷入"难度地狱"，按 Easy 卡在 1 | 增加 `D_next = w[6]*D' + (1-w[6])*w[4]` 均值回归 |
| 4. 间隔计算未用目标保留率 | 🟡 中等 | 直接 `stability.toInt()` 作间隔 | 用 `I = 9*S*(1/R_target - 1)` 公式 |
| 5. 参数只有 17 个（FSRS-4.5） | 🟡 中等 | 缺少 w[17]-w[20] 短期记忆参数 | 升级到 FSRS-6 的 21 个参数 |

#### 5.4.2 FSRS-6 核心数学公式

**1. 可提取性 R（Retrievability）**——基于幂律遗忘曲线：

```
R = (1 + t / (9 * S))^(-1)
```

| 变量 | 含义 |
|------|------|
| t | 距上次复习经过的天数 |
| S | 记忆稳定性（天），约等于 90% 保留率对应的间隔 |
| R | 可提取性 ∈ (0, 1]，当前能回忆起的概率 |

当 `t = S` 时，`R = 0.9`；当 `t = 2S` 时，`R ≈ 0.82`。

**2. 初始稳定性 S0**（新卡首次复习）：

```
S0 = w[rating - 1]   // Again→w[0], Hard→w[1], Good→w[2], Easy→w[3]
```

**3. 初始难度 D0**：

```
D0 = w[4] - (rating - 3) * w[5]
D0 = clamp(D0, 1, 10)
```

**4. 难度更新（含均值回归）**——修复 Bug 3：

```
步骤1：D' = D - w[5] * (rating - 3)
步骤2：D_next = w[6] * D' + (1 - w[6]) * w[4]    ← 均值回归
步骤3：D_next = clamp(D_next, 1, 10)
```

均值回归 `w[6] ≈ 0.86` 表示 86% 保留更新值，14% 回归基准 `w[4]`，防止难度卡在极端值。

**5. 稳定性更新——回忆成功（Hard/Good/Easy）**——修复 Bug 2：

```
S' = S * (1 + exp(w[8]) * (11 - D) * S^(-w[9]) * (exp((1-R) * w[10]) - 1) * factor)
```

| 项 | 含义 |
|----|------|
| `exp(w[8])` | 基础增长率（约 4.4 倍） |
| `(11 - D)` | 难度惩罚：D=1→10倍增长，D=10→1倍 |
| `S^(-w[9])` | 稳定性衰减：S 越大相对增长越小 |
| `(exp((1-R)*w[10]) - 1)` | 遗忘效应：**间隔效应核心**——R 越低（快忘了但想起来了）增长越大 |
| `factor` | Hard=w[15] 惩罚 / Good=1 / Easy=w[16] 奖励 |

**6. 稳定性更新——遗忘（Again）**——修复 Bug 2：

```
S' = w[11] * D^(-w[12]) * ((S + 1)^w[13] - 1) * exp(-w[14] * (1 - R))
S' = max(S', 0.1)   // 防止负数
```

遗忘后的 S' 总是小于原 S，但大于 0（记忆痕迹不会完全消失）。

**7. 间隔计算**——修复 Bug 4：

```
I = 9 * S * (1 / R_target - 1) = 9 * S * (1 - R_target) / R_target
I = clamp(I, 1, maximumInterval)
```

| R_target | I/S 比值 | 适用场景 |
|----------|---------|---------|
| 0.85 | 1.59 | 激进学习，扩大覆盖量 |
| 0.90 | 1.00 | **默认推荐**，间隔=稳定性 |
| 0.95 | 0.47 | 保守，考试冲刺期 |

**8. 模糊因子（Fuzz）**——防止卡片堆积同一天：

```
若 interval >= 2.5：
  fuzz = random(-5%, +5%)   // 间隔越长扰动越大
  I_final = max(1, I + fuzz)
```

#### 5.4.3 FSRS-6 完整实现（替换原 FSRScheduler）

```kotlin
package com.wenyan.core.fsrs

import kotlin.math.*
import kotlin.random.Random

// ===================== 枚举 =====================

enum class Rating(val value: Int) {
    AGAIN(1), HARD(2), GOOD(3), EASY(4);
    companion object { fun fromValue(v: Int) = entries.first { it.value == v } }
}

enum class State { NEW, LEARNING, REVIEW, RELEARNING }

// ===================== 参数（FSRS-6，21个） =====================

/**
 * FSRS-6 参数说明：
 * w[0-3]:   新卡初始稳定性 S0（Again/Hard/Good/Easy）
 * w[4]:     初始难度 D0 基准值
 * w[5]:     难度变化系数
 * w[6]:     均值回归权重（保留更新值的比例）
 * w[7]:     难度振幅（保留参数）
 * w[8-10]:  回忆时稳定性更新参数
 * w[11-14]: 遗忘时稳定性更新参数
 * w[15]:    Hard 惩罚因子（< 1）
 * w[16]:    Easy 奖励因子（> 1）
 * w[17-18]: FSRS-5 短期记忆稳定性
 * w[19-20]: FSRS-6 学习/重学阶段短期参数
 */
data class Parameters(
    val w: FloatArray = DEFAULT_WEIGHTS_FSRS_6,
    val requestRetention: Float = 0.9f,        // 目标保留率
    val maximumInterval: Int = 36500,          // 最大间隔（天）
    val enableFuzz: Boolean = true             // 启用模糊因子
) {
    companion object {
        // FSRS-6 默认参数（21个）
        val DEFAULT_WEIGHTS_FSRS_6 = floatArrayOf(
            0.2172f, 0.3174f, 1.7265f, 5.1816f,  // w[0-3] S0
            4.7284f, 1.0526f, 0.5699f, 0.2197f,  // w[4-7] D
            1.5336f, 0.1752f, 0.9441f, 2.4926f,  // w[8-11] S recall/forget
            0.0606f, 0.4656f, 1.1842f, 0.5316f,  // w[12-15] forget + hard
            0.2316f,                               // w[16] easy bonus
            0.0f, 0.0f,                            // w[17-18] FSRS-5 短期记忆
            0.0f, 0.0f                             // w[19-20] FSRS-6 短期记忆
        )
    }
    override fun equals(other: Any?) = this === other
    override fun hashCode() = w.contentHashCode()
}

// ===================== 核心数据类 =====================

data class Card(
    val due: Long = System.currentTimeMillis(),
    val stability: Float = 0f,
    val difficulty: Float = 0f,
    val elapsedDays: Int = 0,
    val scheduledDays: Int = 0,
    val reps: Int = 0,
    val lapses: Int = 0,             // 遗忘次数（Review→Again）
    val state: State = State.NEW,
    val lastReview: Long? = null
)

data class ReviewLog(
    val rating: Rating,
    val state: State,                // 复习前状态
    val due: Long,
    val stability: Float,
    val difficulty: Float,
    val elapsedDays: Int,
    val lastElapsedDays: Int,
    val scheduledDays: Int,
    val reviewTime: Long
)

data class SchedulingCard(val card: Card, val reviewLog: ReviewLog)

// ===================== FSRS 核心类 =====================

class FSRS(private val params: Parameters) {

    companion object {
        private const val DAY_MS = 86_400_000L
        private const val LEARNING_STEP_AGAIN_MS = 60_000L   // 1 分钟
        private const val LEARNING_STEP_HARD_MS = 300_000L   // 5 分钟
    }

    /** 一次性返回 4 种评分的调度结果（用于 UI 预览） */
    fun repeat(card: Card, now: Long = System.currentTimeMillis()): Map<Rating, SchedulingCard> {
        return Rating.entries.associateWith { schedule(card, it, now) }
    }

    /** 按指定评分调度 */
    fun schedule(card: Card, rating: Rating, now: Long): SchedulingCard {
        val elapsedDays = if (card.lastReview != null) {
            ((now - card.lastReview) / DAY_MS).toInt()
        } else 0

        val s = card.stability
        val d = card.difficulty
        // 新卡不计算 R（stability=0 会除零）
        val r = if (card.lastReview != null && card.stability > 0f) {
            retrievability(elapsedDays, card.stability)
        } else 0f

        val (newS, newD, newState, interval, lapses) = when (card.state) {
            State.NEW -> scheduleNew(card, rating, r)
            State.LEARNING -> scheduleLearning(card, rating, r, elapsedDays)
            State.REVIEW -> scheduleReview(card, rating, r, elapsedDays)
            State.RELEARNING -> scheduleRelearning(card, rating, r, elapsedDays)
        }

        // 应用模糊因子
        val finalInterval = if (params.enableFuzz && interval >= 2.5f) {
            applyFuzz(interval)
        } else maxOf(1f, interval)

        val scheduledDays = finalInterval.toInt().coerceAtLeast(1)
        val due = now + scheduledDays.toLong() * DAY_MS

        val updatedCard = Card(
            due = due, stability = newS,
            difficulty = newD.coerceIn(1f, 10f),
            elapsedDays = elapsedDays, scheduledDays = scheduledDays,
            reps = card.reps + 1, lapses = lapses,
            state = newState, lastReview = now
        )
        val reviewLog = ReviewLog(
            rating = rating, state = card.state, due = card.due,
            stability = s, difficulty = d,
            elapsedDays = elapsedDays, lastElapsedDays = card.scheduledDays,
            scheduledDays = scheduledDays, reviewTime = now
        )
        return SchedulingCard(updatedCard, reviewLog)
    }

    // ---- 状态调度：状态转换规则见 5.4.4 ----
    private fun scheduleNew(c: Card, r: Rating, rR: Float) = when (r) {
        Rating.AGAIN -> T5(initStability(r), initDifficulty(r), State.LEARNING, learningInterval(Rating.AGAIN), c.lapses)
        Rating.HARD  -> T5(initStability(r), initDifficulty(r), State.LEARNING, learningInterval(Rating.HARD), c.lapses)
        Rating.GOOD  -> T5(initStability(r), initDifficulty(r), State.REVIEW, nextInterval(initStability(r)).toFloat(), c.lapses)
        Rating.EASY  -> T5(initStability(r), initDifficulty(r), State.REVIEW, nextInterval(initStability(r)).toFloat(), c.lapses)
    }
    private fun scheduleLearning(c: Card, r: Rating, rR: Float, ed: Int) = when (r) {
        Rating.AGAIN -> T5(nextForgetStability(c.difficulty, c.stability, rR), nextDifficulty(c.difficulty, r), State.RELEARNING, learningInterval(Rating.AGAIN), c.lapses)
        Rating.HARD  -> T5(nextRecallStability(c.difficulty, c.stability, rR, r), nextDifficulty(c.difficulty, r), State.LEARNING, learningInterval(Rating.HARD), c.lapses)
        Rating.GOOD  -> T5(nextRecallStability(c.difficulty, c.stability, rR, r), nextDifficulty(c.difficulty, r), State.REVIEW, nextInterval(nextRecallStability(c.difficulty, c.stability, rR, r)).toFloat(), c.lapses)
        Rating.EASY  -> T5(nextRecallStability(c.difficulty, c.stability, rR, r), nextDifficulty(c.difficulty, r), State.REVIEW, nextInterval(nextRecallStability(c.difficulty, c.stability, rR, r)).toFloat(), c.lapses)
    }
    private fun scheduleReview(c: Card, r: Rating, rR: Float, ed: Int) = when (r) {
        Rating.AGAIN -> T5(nextForgetStability(c.difficulty, c.stability, rR), nextDifficulty(c.difficulty, r), State.RELEARNING, learningInterval(Rating.AGAIN), c.lapses + 1)
        Rating.HARD  -> T5(nextRecallStability(c.difficulty, c.stability, rR, r), nextDifficulty(c.difficulty, r), State.REVIEW, nextInterval(nextRecallStability(c.difficulty, c.stability, rR, r)).toFloat(), c.lapses)
        Rating.GOOD  -> T5(nextRecallStability(c.difficulty, c.stability, rR, r), nextDifficulty(c.difficulty, r), State.REVIEW, nextInterval(nextRecallStability(c.difficulty, c.stability, rR, r)).toFloat(), c.lapses)
        Rating.EASY  -> T5(nextRecallStability(c.difficulty, c.stability, rR, r), nextDifficulty(c.difficulty, r), State.REVIEW, nextInterval(nextRecallStability(c.difficulty, c.stability, rR, r)).toFloat(), c.lapses)
    }
    private fun scheduleRelearning(c: Card, r: Rating, rR: Float, ed: Int) = when (r) {
        Rating.AGAIN -> T5(nextForgetStability(c.difficulty, c.stability, rR), nextDifficulty(c.difficulty, r), State.RELEARNING, learningInterval(Rating.AGAIN), c.lapses)
        Rating.HARD  -> T5(nextForgetStability(c.difficulty, c.stability, rR), nextDifficulty(c.difficulty, r), State.RELEARNING, learningInterval(Rating.HARD), c.lapses)
        Rating.GOOD  -> T5(nextRecallStability(c.difficulty, c.stability, rR, r), nextDifficulty(c.difficulty, r), State.REVIEW, nextInterval(nextRecallStability(c.difficulty, c.stability, rR, r)).toFloat(), c.lapses)
        Rating.EASY  -> T5(nextRecallStability(c.difficulty, c.stability, rR, r), nextDifficulty(c.difficulty, r), State.REVIEW, nextInterval(nextRecallStability(c.difficulty, c.stability, rR, r)).toFloat(), c.lapses)
    }

    // ============ 核心数学公式（对应 5.4.2） ============

    /** R = (1 + t/(9*S))^(-1) */
    fun retrievability(elapsedDays: Int, stability: Float): Float {
        if (stability <= 0f) return 0f
        return (1f + elapsedDays.toFloat() / (9f * stability)).pow(-1f)
    }

    /** S0 = w[rating-1] */
    fun initStability(rating: Rating) = params.w[rating.value - 1]

    /** D0 = w[4] - (rating-3)*w[5] */
    fun initDifficulty(rating: Rating): Float {
        return (params.w[4] - (rating.value - 3) * params.w[5]).coerceIn(1f, 10f)
    }

    /** 难度更新（含均值回归） */
    fun nextDifficulty(d: Float, rating: Rating): Float {
        val dNext = d - params.w[5] * (rating.value - 3)
        val meanReverted = params.w[6] * dNext + (1f - params.w[6]) * params.w[4]
        return meanReverted.coerceIn(1f, 10f)
    }

    /** 稳定性更新——回忆成功 */
    fun nextRecallStability(d: Float, s: Float, r: Float, rating: Rating): Float {
        val hardPenalty = if (rating == Rating.HARD) params.w[15] else 1f
        val easyBonus = if (rating == Rating.EASY) params.w[16] else 1f
        val growth = exp(params.w[8]) * (11f - d) * s.pow(-params.w[9]) *
            (exp((1f - r) * params.w[10]) - 1f) * hardPenalty * easyBonus
        return s * (1f + growth)
    }

    /** 稳定性更新——遗忘 */
    fun nextForgetStability(d: Float, s: Float, r: Float): Float {
        val newS = params.w[11] * d.pow(-params.w[12]) *
            ((s + 1f).pow(params.w[13]) - 1f) * exp(-params.w[14] * (1f - r))
        return maxOf(0.1f, newS)
    }

    /** 间隔计算：I = 9*S*(1/R_target - 1) */
    fun nextInterval(stability: Float): Int {
        if (stability <= 0f) return 1
        val interval = 9f * stability * (1f / params.requestRetention - 1f)
        return minOf(maxOf(interval.toInt(), 1), params.maximumInterval)
    }

    private fun applyFuzz(interval: Float): Float {
        val fuzzRange = when {
            interval < 2.5f -> 0f
            interval < 15f -> 1f
            else -> interval * 0.05f
        }
        val fuzz = Random.nextFloat() * 2f * fuzzRange - fuzzRange
        return maxOf(1f, interval + fuzz)
    }

    private fun learningInterval(rating: Rating): Float = when (rating) {
        Rating.AGAIN -> LEARNING_STEP_AGAIN_MS.toFloat() / DAY_MS
        Rating.HARD -> LEARNING_STEP_HARD_MS.toFloat() / DAY_MS
        else -> 1f
    }

    private data class T5<A,B,C,D,E>(val a:A,val b:B,val c:C,val d:D,val e:E){
        operator fun component1()=a; operator fun component2()=b
        operator fun component3()=c; operator fun component4()=d; operator fun component5()=e
    }
}

// ===================== 适配层（与现有 MemoRecord 互转） =====================

object FSRSAdapter {
    private const val DAY_MS = 86_400_000L

    fun toCard(record: MemoRecord): Card {
        return Card(
            due = record.nextReviewAt, stability = record.stability,
            difficulty = record.difficulty,
            elapsedDays = if (record.lastReviewAt > 0)
                ((System.currentTimeMillis() - record.lastReviewAt) / DAY_MS).toInt() else 0,
            scheduledDays = if (record.lastReviewAt > 0)
                ((record.nextReviewAt - record.lastReviewAt) / DAY_MS).toInt() else 0,
            reps = record.reviewCount, lapses = record.failCount,
            state = State.valueOf(record.state),
            lastReview = if (record.lastReviewAt > 0) record.lastReviewAt else null
        )
    }

    fun toMemoRecord(pointId: String, card: Card, inPriorityQueue: Boolean = false): MemoRecord {
        return MemoRecord(
            pointId = pointId, state = card.state.name,
            stability = card.stability, difficulty = card.difficulty,
            lastReviewAt = card.lastReview ?: System.currentTimeMillis(),
            nextReviewAt = card.due, reviewCount = card.reps,
            failCount = card.lapses, history = emptyList(),
            inPriorityQueue = inPriorityQueue
        )
    }

    /**
     * 今日复习队列（修复 Bug 1）
     * 原错误：filter { it.state != CardState.REVIEW.name } → 排除了所有复习卡！
     * 正确：只排除 NEW（新卡走学习队列），REVIEW/LEARNING/RELEARNING 都应包含
     */
    fun getTodayReviewQueue(allRecords: List<MemoRecord>): List<MemoRecord> {
        val now = System.currentTimeMillis()
        return allRecords
            .filter { it.state != State.NEW.name }    // 排除新卡
            .filter { it.nextReviewAt <= now }          // 已到期
            .sortedBy { it.nextReviewAt }
    }

    /** 今日新卡队列（控制每日新卡数量） */
    fun getTodayNewCards(allRecords: List<MemoRecord>, limit: Int = 10): List<MemoRecord> {
        return allRecords.filter { it.state == State.NEW.name }.take(limit)
    }
}
```

#### 5.4.4 状态转换规则

```
┌──────────────────────────────────────────────────────────────┐
│ 当前状态      │ 评分        │ 新状态         │ 说明         │
├──────────────────────────────────────────────────────────────┤
│ NEW           │ Again/Hard  │ LEARNING       │ 进入学习阶段 │
│ NEW           │ Good/Easy   │ REVIEW         │ 直接毕业     │
│ LEARNING      │ Again       │ RELEARNING     │ 忘记，重学   │
│ LEARNING      │ Hard        │ LEARNING       │ 保持学习     │
│ LEARNING      │ Good/Easy   │ REVIEW         │ 毕业到复习   │
│ REVIEW        │ Again       │ RELEARNING     │ 遗忘，lapses+1 │
│ REVIEW        │ Hard/Good/Easy │ REVIEW      │ 保持复习     │
│ RELEARNING    │ Again/Hard  │ RELEARNING     │ 继续重学     │
│ RELEARNING    │ Good/Easy   │ REVIEW         │ 重新毕业     │
└──────────────────────────────────────────────────────────────┘
```

#### 5.4.5 AnkiDroid 优化经验

| 经验 | 说明 |
|------|------|
| 参数优化数据量 | 需 ≥1000 条 ReviewLog 才能产生有意义的优化参数，否则用默认值更稳 |
| 优化频率 | 每 3-6 个月重新优化一次，不要频繁优化 |
| 目标保留率 | 新用户从 0.90 开始；复习量大降到 0.85；考前冲刺升到 0.95 |
| 模糊因子 | 必须开启，否则同批卡片永远同一天到期 |
| 时区处理 | 用本地时区算"今天"，不要用 UTC（晚上 11 点复习可能算到第二天） |

#### 5.4.6 与原艾宾浩斯设计的对比

| 维度 | 原艾宾浩斯设计 | FSRS-6 算法 |
|------|--------------|------------|
| 复习间隔 | 固定（1/3/7/15/30天） | 动态计算（基于记忆稳定性 S 和目标保留率 R） |
| 用户反馈 | 3档（记住/模糊/忘记） | 4档（Again/Hard/Good/Easy） |
| 个性化 | 无 | 有（21 参数可优化） |
| 遗忘处理 | 固定减半 | 公式计算（考虑 D、S、R 三变量） |
| 难度调整 | 无均值回归 | 有（防止难度卡死在极端值） |
| 间隔效应 | 无 | 有（通过 R 嵌入到稳定性更新） |
| 算法成熟度 | 传统 | AI 优化，现代 SRS 首选 |

#### 5.4.7 数据库 schema 调整

`MemoRecord` 表新增字段（向后兼容）：

```sql
ALTER TABLE memo_records ADD COLUMN elapsed_days INTEGER DEFAULT 0;       -- 距上次复习天数
ALTER TABLE memo_records ADD COLUMN scheduled_days INTEGER DEFAULT 0;     -- 上次调度的间隔
ALTER TABLE memo_records ADD COLUMN reps INTEGER DEFAULT 0;               -- 总复习次数

-- 新增：复习日志表（用于参数优化，必须保存）
CREATE TABLE review_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    point_id TEXT NOT NULL,
    rating INTEGER NOT NULL,           -- 1=Again, 2=Hard, 3=Good, 4=Easy
    state TEXT NOT NULL,               -- 复习前状态
    stability_before REAL NOT NULL,
    stability_after REAL NOT NULL,
    difficulty_before REAL NOT NULL,
    elapsed_days INTEGER NOT NULL,
    scheduled_days INTEGER NOT NULL,
    reviewed_at INTEGER NOT NULL,
    FOREIGN KEY (point_id) REFERENCES knowledge_points(id)
);
CREATE INDEX idx_review_logs_point ON review_logs(point_id);
CREATE INDEX idx_review_logs_time ON review_logs(reviewed_at);
```

---

## 六、UI/UX 设计原则

### 6.1 设计风格

- **基调**：Material3 默认配色 + 适度定制（朱砂红主色调，呼应文学气质）
- **字体**：系统默认中文字体，正文16sp，标题20-24sp，确保可读性
- **留白**：充足留白，避免视觉拥挤
- **对比度**：文字与背景对比度符合WCAG AA标准

### 6.2 交互原则

1. **三层可见**：任何页面最多三层点击就能到达目标内容
2. **续学优先**：打开App首屏显示"继续学习"入口
3. **复习提醒**：今日待复习数量在底部导航徽章显示
4. **手势友好**：支持滑动切换知识点、滑动标记掌握
5. **离线优先**：所有功能完全离线可用

### 6.3 响应式

- 适配竖屏为主（手机阅读）
- 支持横屏（写作练习时）
- 适配不同屏幕尺寸（5.0-7.0英寸）

---

## 六·补、资料整理与内容导入工作流

### 6B.1 问题背景

用户的考研资料来源多样、格式不一，需要一套完整的工作流将其转化为App可用的结构化数据。本章节定义从"原始资料"到"App数据库"的端到端流程。

**资料格式分类**：

| 格式 | 来源示例 | 文本提取难度 | 处理工具 |
|------|---------|-------------|---------|
| PDF（文本型） | 网上下载的电子书、讲义 | ⭐ 简单 | `pdftotext` / `PyPDF2` |
| PDF（扫描型） | 扫描的笔记、教材 | ⭐⭐⭐ 需 OCR | `OCRmyPDF` + `Tesseract` 中文 |
| PDF（加密型） | 付费资料、笃学模考 | 需先解密 | `qpdf` 解密后处理 |
| Word（.doc/.docx） | 学长笔记、自己整理 | ⭐ 简单 | `python-docx` |
| 图片（.jpg/.png） | 拍照、截图、手写笔记 | ⭐⭐⭐ 需 OCR | `Tesseract` / `PaddleOCR` |
| 网页（HTML） | 在线经验贴、文章 | ⭐⭐ 中等 | `BeautifulSoup` / `Defuddle` |
| 视频/音频 | 网课录播 | ⭐⭐⭐⭐ 需 ASR | `Whisper` 转写 |
| PPT（.pptx） | 学长讲义 | ⭐⭐ 中等 | `python-pptx` |
| 手写笔记 | 自己的笔记本 | ⭐⭐⭐⭐⭐ 最难 | `PaddleOCR` 手写体模型 |

### 6B.2 工作流总览（5步）

```
[1.集中] → [2.提取] → [3.分类] → [4.结构化] → [5.导入]
   手动      自动脚本    半自动      AI辅助      双通道
  5分钟     10分钟      5分钟      30分钟      即时
```

### 6B.3 Step 1：集中（手动）

用户将所有资料统一放到一个文件夹：

```
c:\Users\33425\Desktop\语文\wenyan\资料\
├── PDF\
│   ├── 笃学模考分析.pdf（加密）
│   ├── 801模考卷.pdf（加密）
│   ├── 袁行霈中国文学史.pdf
│   └── ...
├── Word\
│   ├── 学长笔记_现当代.docx
│   └── ...
├── 图片\
│   ├── 笔记扫描_001.jpg
│   ├── 笔记扫描_002.jpg
│   └── ...
├── 网页\
│   ├── 经验贴_豆瓣.html
│   └── ...
└── 视频\
    └── 网课片段.mp4
```

### 6B.4 Step 2：提取（桌面端批处理脚本，AI编写）

AI编写 Python 批处理脚本 `extract_content.py`，自动识别格式并提取纯文本。

**脚本功能**：

```python
# 伪代码示意
import os
from pathlib import Path
import subprocess

SOURCE_DIR = r"c:\Users\33425\Desktop\语文\wenyan\资料"
OUTPUT_DIR = r"c:\Users\33425\Desktop\语文\wenyan\提取后"

def extract_all():
    for root, dirs, files in os.walk(SOURCE_DIR):
        for file in files:
            src_path = Path(root) / file
            rel_path = src_path.relative_to(SOURCE_DIR)
            out_path = Path(OUTPUT_DIR) / rel_path.with_suffix('.txt')
            out_path.parent.mkdir(parents=True, exist_ok=True)

            ext = src_path.suffix.lower()
            try:
                if ext == '.pdf':
                    extract_pdf(src_path, out_path)
                elif ext in ('.doc', '.docx'):
                    extract_word(src_path, out_path)
                elif ext in ('.jpg', '.png', '.jpeg', '.bmp', '.webp'):
                    extract_image_ocr(src_path, out_path)
                elif ext in ('.html', '.htm'):
                    extract_html(src_path, out_path)
                elif ext in ('.mp4', '.mp3', '.wav', '.m4a'):
                    extract_media_transcribe(src_path, out_path)
                elif ext in ('.pptx', '.ppt'):
                    extract_ppt(src_path, out_path)
                elif ext == '.txt':
                    shutil.copy(src_path, out_path)
            except Exception as e:
                log_error(src_path, e)

def extract_pdf(src, out):
    """PDF提取：先尝试文本型，失败则OCR"""
    # 1. 检查是否加密
    if is_encrypted(src):
        decrypt_pdf(src, src_with_password)  # 需用户提供密码
        src = src_with_password

    # 2. 尝试直接提取文本
    result = subprocess.run(
        ['pdftotext', '-layout', '-enc', 'UTF-8', str(src), str(out)],
        capture_output=True
    )

    # 3. 若提取结果为空（扫描型PDF），用OCR
    if out.stat().st_size < 100:
        ocr_pdf(src, out)

def ocr_pdf(src, out):
    """扫描型PDF用OCRmyPDF处理"""
    temp_pdf = src.with_suffix('.ocr.pdf')
    subprocess.run([
        'ocrmypdf',
        '-l', 'chi_sim+eng',  # 中文简体+英文
        '--force-ocr',
        str(src), str(temp_pdf)
    ])
    # 再从OCR后的PDF提取文本
    subprocess.run(['pdftotext', '-layout', '-enc', 'UTF-8', str(temp_pdf), str(out)])
    temp_pdf.unlink()

def extract_image_ocr(src, out):
    """图片用Tesseract OCR"""
    subprocess.run([
        'tesseract', str(src), str(out.with_suffix('')),
        '-l', 'chi_sim+eng',
        '--psm', '6'  # 假设为统一文本块
    ])

def extract_media_transcribe(src, out):
    """视频/音频用Whisper转写"""
    subprocess.run([
        'whisper', str(src),
        '--model', 'medium',
        '--language', 'zh',
        '--output_format', 'txt',
        '--output_dir', str(out.parent)
    ])
```

**工具安装清单**（桌面端，AI一次性配置）：

| 工具 | 用途 | 安装命令 |
|------|------|---------|
| Python 3.11+ | 脚本运行环境 | `winget install Python.Python.3.11` |
| poppler | PDF文本提取 | 下载并添加到PATH |
| Tesseract OCR | 图片OCR | `winget install UB-Mannheim.TesseractOCR` |
| OCRmyPDF | 扫描型PDF的OCR | `pip install ocrmypdf` |
| python-docx | Word文档提取 | `pip install python-docx` |
| beautifulsoup4 | 网页提取 | `pip install beautifulsoup4 lxml` |
| python-pptx | PPT提取 | `pip install python-pptx` |
| openai-whisper | 音视频转写 | `pip install openai-whisper` |
| PaddleOCR | 手写体OCR（可选） | `pip install paddlepaddle paddleocr` |

### 6B.5 Step 3：分类（半自动）

提取后的纯文本按科目归类：

```
提取后\
├── 古代文学\
│   ├── 袁行霈中国文学史.txt
│   └── 笔记扫描_古代.txt
├── 现当代文学\
│   ├── 学长笔记_现当代.txt
│   └── 丁帆教材.txt
├── 外国文学\
├── 文学理论\
├── 专业写作\
│   ├── 范文汇编.txt
│   └── 写作技巧.txt
├── 真题汇编\
│   ├── 2020-2024真题.txt
│   └── 笃学模考.txt
└── 未分类\  （AI无法判断的，用户手动归类）
```

**分类方法**：

```python
def classify_text(text):
    """基于关键词的自动分类"""
    keywords = {
        "古代文学": ["屈原", "李白", "杜甫", "苏轼", "红楼梦", "诗经", "楚辞", "古文运动"],
        "现当代文学": ["鲁迅", "周作人", "茅盾", "巴金", "老舍", "京派", "海派", "新文学"],
        "外国文学": ["但丁", "莎士比亚", "歌德", "托尔斯泰", "陀思妥耶夫斯基", "多余人"],
        "文学理论": ["文学活动", "文学创造", "文学接受", "童庆炳", "典型环境", "意境"],
        "专业写作": ["文学评论", "范文", "批评方法", "文本细读"]
    }

    scores = {subject: 0 for subject in keywords}
    for subject, kws in keywords.items():
        for kw in kws:
            scores[subject] += text.count(kw)

    best_match = max(scores, key=scores.get)
    if scores[best_match] == 0:
        return "未分类"
    return best_match
```

### 6B.6 Step 4：结构化（AI辅助，核心环节）

纯文本无法直接用，需要转化为**结构化的知识点/真题/作家作品**。由 AI（DeepSeek API）完成提取，用户审核后入库。

**AI提取流程**：

```
提取后的纯文本
      ↓
  分块（每块2000-3000字，避免超token）
      ↓
  调用DeepSeek API，按结构化Prompt提取
      ↓
  输出JSON数组
      ↓
  合并所有块的结果
      ↓
  去重（基于标题相似度）
      ↓
  生成 seed_data.json
```

**知识点提取 Prompt**：

```
## 任务
从以下考研资料文本中提取知识点，输出JSON数组。

## 输出格式
[
  {
    "id": "kp_auto_001",
    "subject": "古代文学",  // 古代文学/现当代文学/外国文学/文学理论
    "chapter": "先秦",       // 所属编或章
    "title": "《离骚》",
    "summary": "屈原的代表作，楚辞体的巅峰之作",
    "coreConclusion": "《离骚》是中国浪漫主义文学的源头，以香草美人寄托忠君爱国之情",
    "fullContent": "（200-500字完整内容）",
    "tags": ["楚辞", "浪漫主义", "屈原"],
    "examFrequency": "HIGH",  // HIGH/MEDIUM/LOW/NEVER（基于真题对比）
    "memoryTier": "TIER_EXACT",  // TIER_EXACT/TIER_FRAMEWORK/TIER_UNDERSTAND
    "tierReason": "历年高频论述题考点",
    "examRecords": [
      {"year": 2020, "examType": "ESSAY", "angle": "艺术成就", "score": 20},
      {"year": 2022, "examType": "ESSAY", "angle": "对后世影响", "score": 15}
    ]
  }
]

## 提取规则
1. 每个知识点必须是独立的、可背诵的单元
2. coreConclusion 必须是30字内的结论句，考场直接可用
3. fullContent 是200-500字的完整论述，分条组织
4. examFrequency 基于历年真题考查频率判断
5. memoryTier 根据考频和题型推荐（名词解释→EXACT，论述题核心→FRAMEWORK，背景知识→UNDERSTAND）

## 资料文本
{chunk_text}
```

**真题提取 Prompt**：

```
## 任务
从以下资料中提取考研真题，输出JSON数组。

## 输出格式
[
  {
    "id": "q_auto_001",
    "year": 2020,
    "subject": "古代文学",
    "questionType": "ESSAY",  // ESSAY/SHORT_ANSWER/TERM_EXPLANATION/WRITING
    "content": "论述屈原《离骚》的艺术成就和独特风格。",
    "score": 20,
    "angle": "艺术成就",
    "relatedPointIds": ["kp_auto_001"],  // 关联知识点ID
    "answerFramework": "① 浪漫主义手法：香草美人、奇丽想象\n② 楚辞体句式：兮字、对偶\n③ 比兴寄托：忠君爱国\n④ 对后世影响：汉赋、李白",
    "notes": ""
  }
]

## 资料文本
{chunk_text}
```

**作家作品关系提取 Prompt**：

```
## 任务
从以下资料中提取作家、作品、流派及其关系，输出JSON。

## 输出格式
{
  "nodes": [
    {"id": "n_auto_001", "type": "AUTHOR", "label": "苏轼", "subtitle": "1037-1101"},
    {"id": "n_auto_002", "type": "WORK", "label": "《念奴娇·赤壁怀古》"},
    {"id": "n_auto_003", "type": "SCHOOL", "label": "豪放派"}
  ],
  "edges": [
    {"sourceId": "n_auto_001", "targetId": "n_auto_002", "type": "AUTHORED"},
    {"sourceId": "n_auto_001", "targetId": "n_auto_003", "type": "BELONGS_TO"}
  ]
}

## 资料文本
{chunk_text}
```

**桌面端提取脚本** `extract_to_json.py`：

```python
import json
from openai import OpenAI
from pathlib import Path

client = OpenAI(
    api_key="用户的DeepSeek API key",
    base_url="https://api.deepseek.com/v1"
)

def extract_knowledge_points(text_chunks):
    """提取知识点"""
    all_points = []
    for i, chunk in enumerate(text_chunks):
        prompt = KNOWLEDGE_POINT_PROMPT.replace("{chunk_text}", chunk)
        response = client.chat.completions.create(
            model="deepseek-chat",
            messages=[{"role": "user", "content": prompt}],
            temperature=0.3,
            response_format={"type": "json_object"}
        )
        points = json.loads(response.choices[0].message.content)
        all_points.extend(points)

    # 去重（基于标题相似度）
    return deduplicate_by_title(all_points)

def main():
    base_dir = Path(r"c:\Users\33425\Desktop\语文\wenyan\提取后")

    knowledge_points = []
    exam_questions = []
    graph_data = {"nodes": [], "edges": []}

    for txt_file in base_dir.rglob("*.txt"):
        text = txt_file.read_text(encoding="utf-8")
        chunks = split_into_chunks(text, max_chars=3000)

        knowledge_points.extend(extract_knowledge_points(chunks))
        exam_questions.extend(extract_exam_questions(chunks))
        graph = extract_graph(chunks)
        graph_data["nodes"].extend(graph["nodes"])
        graph_data["edges"].extend(graph["edges"])

    # 去重
    knowledge_points = deduplicate_by_title(knowledge_points)
    exam_questions = deduplicate_by_question_content(exam_questions)
    graph_data["nodes"] = deduplicate_nodes(graph_data["nodes"])
    graph_data["edges"] = deduplicate_edges(graph_data["edges"])

    # 输出种子数据
    seed_data = {
        "knowledgePoints": knowledge_points,
        "examQuestions": exam_questions,
        "graphNodes": graph_data["nodes"],
        "graphEdges": graph_data["edges"],
        "metadata": {
            "generatedAt": datetime.now().isoformat(),
            "totalPoints": len(knowledge_points),
            "totalQuestions": len(exam_questions),
            "totalNodes": len(graph_data["nodes"])
        }
    }

    output_path = Path(r"c:\Users\33425\Desktop\语文\wenyan\seed_data.json")
    output_path.write_text(
        json.dumps(seed_data, ensure_ascii=False, indent=2),
        encoding="utf-8"
    )
    print(f"✅ 种子数据已生成: {output_path}")
    print(f"   知识点: {len(knowledge_points)}")
    print(f"   真题: {len(exam_questions)}")
    print(f"   关系节点: {len(graph_data['nodes'])}")
```

### 6B.7 Step 5：导入 App（双通道）

提供两种导入方式，满足首次填充和后续更新需求。

#### 通道一：桌面端预处理 + JSON 种子（首次填充）

将生成的 `seed_data.json` 放到 App 的 `assets/` 目录，App 首次启动时自动导入。

```
wenyan-android/
└── app/
    └── src/
        └── main/
            └── assets/
                └── seed_data.json  ← 桌面端生成的种子数据
```

**App 内种子数据导入器**：

```kotlin
class SeedDataImporter(
    private val context: Context,
    private val knowledgePointDao: KnowledgePointDao,
    private val examQuestionDao: ExamQuestionDao,
    private val graphNodeDao: GraphNodeDao,
    private val graphEdgeDao: GraphEdgeDao
) {
    suspend fun importIfFirstLaunch() {
        // 检查是否首次启动
        if (isFirstLaunch()) {
            val seedJson = context.assets.open("seed_data.json").bufferedReader().use {
                it.readText()
            }
            val seedData = Json.decodeFromString<SeedData>(seedJson)

            // 批量插入（事务）
            withContext(Dispatchers.IO) {
                knowledgePointDao.database.runInTransaction {
                    seedData.knowledgePoints.forEach { knowledgePointDao.insert(it.toEntity()) }
                    seedData.examQuestions.forEach { examQuestionDao.insert(it.toEntity()) }
                    seedData.graphNodes.forEach { graphNodeDao.insert(it.toEntity()) }
                    seedData.graphEdges.forEach { graphEdgeDao.insert(it.toEntity()) }
                }
            }
            markFirstLaunchComplete()
        }
    }

    private fun isFirstLaunch(): Boolean {
        return context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .getBoolean("is_first_launch", true)
    }

    private fun markFirstLaunchComplete() {
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("is_first_launch", false).apply()
    }
}
```

#### 通道二：App 内内容管理界面（后续更新）

在"我的"页面提供"内容管理"入口，支持后续手动导入或编辑。

**界面设计**：

```
我的 → 内容管理
├── 📊 数据统计
│   ├── 知识点：234 个
│   ├── 真题：56 道
│   ├── 作家作品：89 个
│   └── 关系：156 条
├── 📥 导入资料
│   ├── 从文件导入（.json / .md / .txt）
│   ├── 扫码导入（未来扩展）
│   └── 从剪贴板导入
├── 📝 内容编辑
│   ├── 知识点列表（可增删改）
│   ├── 真题列表（可增删改）
│   └── 关系网络（可视化编辑）
├── 📤 导出备份
│   ├── 导出为 JSON
│   ├── 导出为 Markdown
│   └── 分享到其他应用
└── 🗑️ 清空数据（危险操作）
```

**导入解析器**：

```kotlin
class ContentImporter(
    private val knowledgePointDao: KnowledgePointDao,
    private val examQuestionDao: ExamQuestionDao,
    private val graphNodeDao: GraphNodeDao,
    private val graphEdgeDao: GraphEdgeDao
) {
    sealed class ImportResult {
        data class Success(val imported: Int, val skipped: Int) : ImportResult()
        data class PartialSuccess(val imported: Int, val errors: List<String>) : ImportResult()
        data class Failure(val reason: String) : ImportResult()
    }

    suspend fun importFromFile(uri: Uri, context: Context): ImportResult {
        val content = context.contentResolver.openInputStream(uri)?.use {
            it.bufferedReader().readText()
        } ?: return ImportResult.Failure("无法读取文件")

        val extension = uri.lastPathSegment?.substringAfterLast(".")?.lowercase()
        return when (extension) {
            "json" -> importFromJson(content)
            "md", "markdown" -> importFromMarkdown(content)
            "txt" -> importFromText(content)
            else -> ImportResult.Failure("不支持的文件格式: $extension")
        }
    }

    private suspend fun importFromJson(content: String): ImportResult {
        return try {
            val seedData = Json.decodeFromString<SeedData>(content)
            var imported = 0
            var skipped = 0

            withContext(Dispatchers.IO) {
                seedData.knowledgePoints.forEach { point ->
                    if (knowledgePointDao.getById(point.id) == null) {
                        knowledgePointDao.insert(point.toEntity())
                        imported++
                    } else skipped++
                }
                // ... 类似处理 examQuestions, graphNodes, graphEdges
            }

            ImportResult.Success(imported, skipped)
        } catch (e: Exception) {
            ImportResult.Failure("JSON解析失败: ${e.message}")
        }
    }

    private suspend fun importFromMarkdown(content: String): ImportResult {
        // 解析Markdown格式的知识点
        // 格式：
        // # 章节标题
        // ## 知识点标题
        // - 科目：古代文学
        // - 考频：HIGH
        // - 核心结论：...
        // - 完整内容：...
        val points = parseMarkdownToKnowledgePoints(content)
        // ... 插入数据库
        return ImportResult.Success(points.size, 0)
    }

    private suspend fun importFromText(content: String): ImportResult {
        // 纯文本：每行作为一个知识点的fullContent，用户手动编辑其他字段
        // 进入"待审核"列表，用户确认后入库
        return ImportResult.Success(0, 0)
    }
}
```

### 6B.8 整体工作流时间预估

| 步骤 | 执行者 | 耗时 | 备注 |
|------|--------|------|------|
| Step 1 集中 | 用户 | 5分钟 | 把资料放到一个文件夹 |
| Step 2 提取 | AI脚本 | 10-30分钟 | 取决于资料量和OCR需求 |
| Step 3 分类 | AI脚本 | 1分钟 | 关键词自动分类 |
| Step 4 结构化 | AI（DeepSeek） | 30-60分钟 | 调用API提取，取决于资料量 |
| Step 5 导入 | AI脚本+App | 1分钟 | 打包到assets或App内导入 |
| **总计** | - | **约1-2小时** | 一次性工作 |

### 6B.9 加密PDF处理说明

桌面上的21份加密PDF需要特殊处理：

1. **用户提供密码**：用户告知密码后，脚本用 `qpdf --decrypt` 解密
2. **解密后提取**：按普通PDF流程处理
3. **密码安全**：密码不写入脚本，作为命令行参数传入，用后即弃

```python
import subprocess

def decrypt_pdf(src, out, password):
    """解密PDF"""
    subprocess.run([
        'qpdf',
        '--password', password,
        '--decrypt',
        str(src), str(out)
    ], check=True)
```

### 6B.10 质量保证

**AI提取质量保障**：

1. **分块处理**：长文本分块提取，避免超token和上下文丢失
2. **去重机制**：基于标题相似度（Levenshtein距离>0.8视为重复）
3. **人工审核**：App内提供"待审核"列表，用户可批量确认或修改
4. **增量更新**：后续新增资料可单独提取并增量导入，不覆盖已有数据
5. **版本回溯**：每次导入生成备份，可回滚到之前版本

---

## 七、开发计划

### 7.1 分阶段实施

**Phase 1：项目骨架（1-2轮对话）**
- 创建Android项目，配置Gradle依赖
- 搭建Material3主题 + 底部导航
- 建立Room数据库 + 实体类 + DAO
- 实现Hilt依赖注入
- 预置种子数据（科目+章节结构）
- 产出：能跑的空壳App，4个Tab可切换

**Phase 2：知识图谱模块（2-3轮对话）**
- 知识点树形浏览（科→编→章→节→知识点）
- 知识点详情页（三层内容展示）
- 搜索功能
- 关联跳转
- 预置2020-2024真题作为关联数据
- 产出：能浏览知识点、查看真题关联

**Phase 3：深度背诵引擎（2-3轮对话）**
- 通读模式 + 续学机制
- 遮挡背诵
- 提纲背诵
- 默写自测
- 完整复述
- 艾宾浩斯算法 + 每日复习队列
- 产出：核心背诵功能可用

**Phase 4：真题工坊 + 写作训练（2-3轮对话）**
- 真题按年份/科目浏览
- 真题详情 + 答题框架
- 模考模式
- 命题规律分析
- 写作素材库 + 范文库
- 产出：真题与写作功能完整

**Phase 5：AI助手模块（2-3轮对话）**
- Retrofit + OkHttp网络层搭建
- 多API配置管理（DeepSeek/通义/智谱/月之暗面）
- API key加密存储（Android Keystore）
- 智能悬浮窗组件（可拖动、上下文识别）
- AI智能问答（流式输出）
- 论述题/写作智能批改
- 真题答题框架生成
- 对话历史保存
- 离线降级处理
- 产出：AI增强功能可用

**Phase 6：仪表盘 + 打磨（1-2轮对话）**
- 进度地图
- 学习统计
- 里程碑成就
- 数据备份导入导出
- UI细节打磨
- 产出：完整可用App

### 7.2 每轮工作流

```
用户提需求 → AI写代码 → 用户在Android Studio运行 → 
用户在手机测试 → 用户反馈问题 → AI修复 → 循环
```

### 7.3 内容补充计划

软件架构搭好后，内容分批补充：
1. **第一批**：用户购买资料后，AI解析PDF提取内容
2. **第二批**：基于调研报告整理高频考点
3. **第三批**：用户复习过程中持续补充个人笔记

---

## 八、环境配置清单

### 8.1 需要安装的软件

| 软件 | 版本 | 大小 | 安装方式 | 状态 |
|------|------|------|---------|------|
| JDK | OpenJDK 17 | ~200MB | AI命令行安装 | 待装 |
| Android Studio | Hedgehog或更新 | ~3GB | 用户手动下载安装 | 待装 |
| Android SDK | API 34 | ~2GB | Android Studio内下载 | 待装 |
| Git | 最新版 | ~50MB | AI命令行安装 | 待装 |

### 8.2 硬件约束

- 内存：8GB（可用约1.3GB），开发时需关闭其他程序
- 不使用Android模拟器（太吃内存），用真机USB调试
- 需要：一台Android手机（Android 8.0+），打开USB调试模式

### 8.3 环境配置步骤（待用户准备好后执行）

1. AI命令行安装JDK 17 + Git
2. 用户下载安装Android Studio
3. AI远程指导配置Android SDK
4. AI创建项目骨架
5. 用户用Android Studio打开，首次Gradle同步下载依赖
6. 连接手机，点击Run，验证App能在手机上运行

---

## 九、风险与应对

| 风险 | 可能性 | 影响 | 应对措施 |
|------|--------|------|---------|
| Gradle同步失败 | 中 | 阻断开发 | AI远程排查，常见问题有现成方案 |
| 内存不足导致AS卡顿 | 高 | 降低效率 | 开发时关闭浏览器/微信/QQ |
| Compose编译错误 | 低 | 阻断开发 | AI即时修复 |
| 真机USB调试连接问题 | 中 | 无法测试 | AI指导开启开发者模式+USB调试 |
| 内容整理耗时 | 高 | 延迟可用 | 先用种子数据跑通，内容后补 |
| Room数据库迁移 | 低 | 升级数据丢失 | 预留迁移机制，自动备份 |

---

## 十、成功标准

### 10.1 MVP（Phase 1-3完成）

- [ ] App能在Android手机上正常运行
- [ ] 能浏览四科知识点结构
- [ ] 能查看知识点详情（三层内容）
- [ ] 能用三种模式背诵（通读/遮挡/提纲）
- [ ] 每日复习队列能正确计算
- [ ] 续学机制可用

### 10.2 完整版（Phase 1-6完成）

- [ ] 五种背诵模式全部可用
- [ ] 真题工坊完整（浏览+模考+规律分析）
- [ ] 专业写作模块可用
- [ ] AI助手：智能悬浮窗可拖动、上下文识别正确
- [ ] AI助手：论述题/写作批改功能可用
- [ ] AI助手：多API可切换，key安全存储
- [ ] 仪表盘显示进度地图+统计
- [ ] 数据备份导入导出
- [ ] UI打磨完成，交互流畅

---

## 附录

### A. 已有资源

1. 调研报告：`docs/南师大现当代文学考研-专业课深度调研报告.md`（1016行，含2020-2024真题、高频考点、复习方法）
2. 二轮调研：`docs/南师大现当代文学考研-二轮调研报告.md`（1156行，含55+信息源）
3. 加密PDF资料：21份（笃学模考分析+801模考卷+模拟卷），待用户提供密码后解析

### B. 参考经验贴

1. 豆瓣·里奥《从学习方法角度谈谈现当代文学考研经验》——横纵向思考、迁移类比方法
2. 头条《文学史132分+文学理论136分》——按文体脉络记忆法
3. 调研报告内汇总的404分、387分、143分等多篇上岸经验

### D. 技术调研参考项目（2026-07-08调研）

**开源项目（按优先级排序）**：

| 项目 | URL | 价值 |
|------|-----|------|
| Now in Android | https://github.com/android/nowinandroid | Compose+M3+模块化金标准，有Figma设计稿 |
| AnkiDroid | https://github.com/ankidroid/Anki-Android | FSRS算法实现+成熟工程实践 |
| Lexora | https://github.com/mkvSKYi/Lexora | Compose+M3+Hilt+Room最佳实践，热力图 |
| WordMaster | https://github.com/xhmTmax530/word-master-app | 艾宾浩斯/FSRS算法直接参考 |
| QuickMem | https://github.com/pass-with-high-score/quickmem-app | Compose闪卡App完整功能参考 |
| flashcards-open-source-app | https://github.com/kirill-markin/flashcards-open-source-app | AI+FSRS现代产品形态 |
| FloatingX | https://github.com/Petterpx/FloatingX | 悬浮窗现代实现（免权限） |

**市面产品调研**：
- Anki/记乎：有间隔重复，无图谱，无文学内容
- Obsidian/Roam/Logseq：有图谱，无间隔重复，无文学内容
- RemNote：图谱+间隔重复都有，但通用工具，无文学内容（与我们定位最接近）
- 笃学/考研帮：有文学内容，无图谱无算法
- 墨墨背单词：遗忘曲线可视化值得借鉴
- 不背单词：语境化记忆理念值得借鉴
- 西窗烛App：传统文学配色排版标杆

**AI教育应用参考**：
- 笔神作文：LoRA微调+INT8量化做教育专属模型
- 学而思九章：启发式不直答+RAG+用户画像
- okhttp-sse库：Android流式输出首选方案
- WindowManager+View方案：悬浮窗实现，需处理国产ROM权限

**关键结论**：
1. **市场空白**：文学考研+知识图谱+间隔重复三者结合的产品不存在
2. **算法升级**：FSRS优于艾宾浩斯，已被AnkiDroid等现代项目采用
3. **架构参考**：Now in Android多模块架构是Compose项目金标准
4. **AI实现**：okhttp-sse处理流式输出，FloatingX做悬浮窗

### E. 深度调研报告核心结论（2026-07-08 第二轮调研）

基于5个并行深度调研子agent的结果，以下为核心结论与文档整合情况：

#### E.1 FSRS-Kotlin库深度研究

**调研对象**：FSRS-Kotlin（https://github.com/open-spaced-repetition/FSRS-Kotlin）+ fsrs4anki + AnkiDroid集成代码

**核心结论**：
1. FSRS-6有21个参数（w[0]-w[20]），比FSRS-4.5的17个多4个短期记忆参数
2. 可提取性公式：`R = (1 + t/(9*S))^(-1)`，非指数衰减
3. 难度更新含均值回归：`D_next = w[6]*D' + (1-w[6])*w[4]`，防止"难度地狱"
4. 遗忘稳定性更新：`S' = w[11] * D^(-w[12]) * ((S+1)^w[13] - 1) * exp(-w[14]*(1-R))`
5. 间隔计算：`I = 9*S*(1/R_target - 1)`，目标保留率越高间隔越短
6. AnkiDroid需≥1000条ReviewLog才能优化参数，初期用默认参数即可

**文档整合**：已替换5.4节为完整FSRS-6实现（v2.0），修复5个bug

#### E.2 Compose知识图谱Canvas实战调研

**调研对象**：Compose Canvas API + 力导向布局算法 + 关系图库调研

**核心结论**：
1. **无成熟Compose原生关系图库**，必须基于Canvas自绘
2. Force-directed布局算法：库仑斥力 + 胡克引力 + 阻尼 + 中心引力
3. 视口变换+视口裁剪+分层渲染是性能关键
4. 性能基准：500节点+1000边在Canvas上可达45-60fps
5. LOD（细节层次）：缩放小时只画圆点不画标签
6. 异步布局：力导向模拟在`Dispatchers.Default`运行，不阻塞UI
7. 工作量预估：3-5天可完成基础版

**文档整合**：已写入3.2.7-3.2.9节（关系网络+力导向布局+Canvas渲染）

#### E.3 FloatingX悬浮窗与AI流式输出调研

**调研对象**：FloatingX 1.3.x + okhttp-sse + EncryptedSharedPreferences + 国产ROM兼容

**核心结论**：
1. FloatingX 1.3.0+支持Compose内容，使用`fx.show(activity)`实现App级免权限悬浮窗
2. okhttp-sse是OkHttp官方SSE模块，处理大模型流式输出首选
3. EncryptedSharedPreferences + MasterKey + Keystore加密存储API key
4. 密钥丢失兜底：捕获GeneralSecurityException后降级为明文存储+用户提示
5. 国产ROM兼容：MIUI/EMUI/ColorOS/OriginOS需特殊处理权限跳转Intent
6. 上下文识别双层方案：Navigation路由监听（粗粒度兜底）+ CompositionLocal主动上报（细粒度）

**文档整合**：已写入3.6.9-3.6.10节（FloatingX集成+上下文识别双层方案+ROM兼容）

#### E.4 AI批改文学论述题Prompt工程调研

**调研对象**：DeepSeek/通义/智谱API + Prompt工程最佳实践 + 文学评论评分标准

**核心结论**：
1. 论述题五维评分：论点25%/论据25%/结构20%/语言15%/学术性15%
2. 写作六维评分：思想深度25%/理论运用25%/文本细读20%/结构15%/语言10%/规范5%
3. 防虚高4重保障：Prompt约束 + 分数天花板88% + 二次校验 + 用户反馈
4. 流式JSON解析：采用XML标签分隔方案（`<score>...</score>`），而非JSON
5. Few-shot示例：提供1-2个评分示例（如苏轼词史贡献14/20分）
6. 模型选型：DeepSeek默认主力（月成本约¥1.65），通义/智谱备选
7. System Prompt + User Prompt分离，便于维护和切换

**文档整合**：已写入3.6.6-3.6.8节（评分维度+Prompt工程+流式解析）

#### E.5 文学考研背诵方法产品化深度调研

**调研对象**：上岸经验贴 + 答题模板 + 记忆档位 + 复习队列调度

**核心结论**：
1. 答题模板系统：4种题型（名词解释/简答/论述/写作）的完整字段定义
   - 名词解释：总分总三段论，80-150字
   - 简答题：定义-框架-例证法，300-500字
   - 论述题：三步走（破题-主体-总结），800-1500字
   - 写作：5开头+3论证+5结尾，可自由组合
2. 三档复习调度：精确档(0.95)/框架档(0.90)/理解档(0.85)，不同目标保留率
3. 主动回忆检测：L1关键词(本地<10ms) → L2要点覆盖(本地<100ms) → L3 AI完整性(在线3-5秒)
4. 每日量控制：9级优先级队列 + 考研倒计时自适应（最后1周不学新）
5. 多视图知识图谱：6种视图（树形/时间轴/文体/流派/考频热力图/作家矩阵）
6. 作家作品关系网络：6种节点 + 8种边类型
7. 进阶仪表盘：薄弱诊断(5维度) + 每日建议(6类型) + 保留率曲线 + 倒计时(6阶段)

**文档整合**：已写入3.3.4-3.3.6节（三档调度+主动回忆+每日量）、3.4.5节（答题模板）、3.2.6-3.2.9节（多视图+关系网络）、3.5.3-3.5.6节（薄弱诊断+建议+曲线+倒计时）

### C. 命名约定

- 包名：`com.wenyan.app`
- 应用名：文研
- 数据库文件：`wenyan.db`
- 备份目录：`/sdcard/Download/wenyan-backup/`
- 版本号：`MAJOR.MINOR.PATCH`（如1.0.0）

---

> 本设计文档基于70+网络信息源、120+次搜索调研、10轮深度调研（含5个并行子agent）、多篇上岸经验贴综合提炼而成。v2.0版本整合了FSRS-6算法升级、三档复习调度、主动回忆检测、答题模板系统、AI Prompt工程、FloatingX悬浮窗、Force-directed关系网络图、Compose性能优化等核心创新。技术选型遵循"Google官方库优先、AI友好、零实验性依赖"原则，确保开发顺畅。
