package com.wenyan.app.core.database.migration

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Adds provenance semantics without upgrading any historical content to reviewed. */
internal val MIGRATION_10_11_STATEMENTS = listOf(
    "ALTER TABLE knowledge_points ADD COLUMN content_status TEXT NOT NULL DEFAULT 'LEGACY_UNVERIFIED'",
    "ALTER TABLE exam_questions ADD COLUMN content_status TEXT NOT NULL DEFAULT 'LEGACY_UNVERIFIED'",
    "ALTER TABLE writing_materials ADD COLUMN title TEXT",
    "ALTER TABLE writing_materials ADD COLUMN related_point_ids TEXT",
    "ALTER TABLE writing_materials ADD COLUMN content_status TEXT NOT NULL DEFAULT 'LEGACY_UNVERIFIED'",
    "ALTER TABLE data_sources ADD COLUMN writing_material_id TEXT REFERENCES writing_materials(id) ON UPDATE NO ACTION ON DELETE CASCADE",
    "ALTER TABLE data_sources ADD COLUMN source_status TEXT NOT NULL DEFAULT 'UNKNOWN'",
    "ALTER TABLE data_sources ADD COLUMN source_title TEXT",
    "ALTER TABLE data_sources ADD COLUMN source_edition TEXT",
    "ALTER TABLE data_sources ADD COLUMN source_page_start INTEGER",
    "ALTER TABLE data_sources ADD COLUMN source_page_end INTEGER",
    "ALTER TABLE data_sources ADD COLUMN source_checksum TEXT",
    "ALTER TABLE data_sources ADD COLUMN review_note TEXT",
    "CREATE INDEX IF NOT EXISTS index_knowledge_points_content_status ON knowledge_points(content_status)",
    "CREATE INDEX IF NOT EXISTS index_exam_questions_content_status ON exam_questions(content_status)",
    "CREATE INDEX IF NOT EXISTS index_writing_materials_content_status ON writing_materials(content_status)",
    "CREATE INDEX IF NOT EXISTS index_data_sources_writing_material_id ON data_sources(writing_material_id)",
    "CREATE INDEX IF NOT EXISTS index_data_sources_source_status ON data_sources(source_status)",
)

val MIGRATION_10_11: Migration = object : Migration(10, 11) {
    override fun migrate(database: SupportSQLiteDatabase) {
        MIGRATION_10_11_STATEMENTS.forEach(database::execSQL)
    }
}
