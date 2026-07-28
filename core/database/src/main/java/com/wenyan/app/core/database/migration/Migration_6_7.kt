package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移 6 → 7（v0.9.3 优化 4：移除知识图谱 UI 死代码）。
 *
 * **背景**：v0.9.0 已删除 feature:graph UI 模块（B4），但 core 层图谱基础设施
 * （GraphNodeEntity/GraphEdgeEntity + GraphRepository + 3 个算法服务）仍保留，
 * 每次 App 启动 SeedDataLoader.importGraphSkeleton 会生成 2123 节点 968 边，
 * 无任何 UI 消费者，纯死代码。
 *
 * 验证（2026-07-28）：全局搜索 GraphRepository/InterferenceWarner/
 * WeakSubgraphDetector/PrerequisiteChecker，确认无 FSRS、ViewModel、UI 层调用，
 * 仅自引用 + 测试。详见 v0.9.3 优化 4 commit。
 *
 * **变更**：DROP graph_nodes + graph_edges 两张表。
 *
 * **安全性**：
 * - 两张表数据均由 SeedDataLoader 自动生成（非用户创建），DROP 无数据损失
 * - 无其他表 FK 引用 graph_nodes/graph_edges（FK 方向相反：graph_edges FK→graph_nodes）
 * - 已有用户的复习记录（memo_records）、错题（wrong_answers）不受影响
 *
 * **幂等性**：不幂等（DROP TABLE IF EXISTS 在表不存在时不报错，但数据已丢）。
 * Room 仅在 6→7 时调用一次。
 *
 * @see com.wenyan.app.core.database.WenyanDatabase
 */
val MIGRATION_6_7: Migration = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // 先 DROP graph_edges（FK 引用 graph_nodes，需先删子表）
        database.execSQL("DROP TABLE IF EXISTS `graph_edges`")
        // 再 DROP graph_nodes
        database.execSQL("DROP TABLE IF EXISTS `graph_nodes`")
    }
}
