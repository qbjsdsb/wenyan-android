package com.wenyan.app.core.fsrs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Task 30 - ContentTierMapper 单元测试。
 *
 * 验证 checklist C3.1c-C3.1d 项：4 种内容类型映射到三档机制（含目标保持率）、
 * mapContentTypeToTier 返回 MemoryTier 枚举（复用设计文档 memoryTier 字段）、
 * USER_CUSTOM 默认映射 TIER_FRAMEWORK、shouldEnableFuzz（作品背诵为 false，其余 true）、
 * getConfig 与 TIER_CONFIGS 一致。
 *
 * 运行环境：纯 JVM 单元测试（core:fsrs/src/test）。
 */
class ContentTierMapperTest {

    // ===================== C3.1c: 4 种内容类型映射到三档机制 =====================

    /** C3.1c: 4 种内容类型映射到三档机制（含目标保持率） */
    @Test
    fun mapContentTypeToTier_mapsFourTypesToThreeTiers() {
        // 作品背诵 → TIER_EXACT / 0.95（考场需逐字复述）
        assertEquals(MemoryTier.TIER_EXACT, ContentTierMapper.mapContentTypeToTier(ContentType.WORK_RECITATION))
        assertEquals(0.95f, TIER_CONFIGS[MemoryTier.TIER_EXACT]!!.targetRetention, 0.0001f)
        // 名词解释 → TIER_FRAMEWORK / 0.90（考场需分条复述要点）
        assertEquals(MemoryTier.TIER_FRAMEWORK, ContentTierMapper.mapContentTypeToTier(ContentType.TERM_EXPLANATION))
        assertEquals(0.90f, TIER_CONFIGS[MemoryTier.TIER_FRAMEWORK]!!.targetRetention, 0.0001f)
        // 流派特征 → TIER_FRAMEWORK / 0.90
        assertEquals(MemoryTier.TIER_FRAMEWORK, ContentTierMapper.mapContentTypeToTier(ContentType.SCHOOL_FEATURE))
        assertEquals(0.90f, TIER_CONFIGS[MemoryTier.TIER_FRAMEWORK]!!.targetRetention, 0.0001f)
        // 论述题 → TIER_UNDERSTAND / 0.85（考场需能用自己的话阐述）
        assertEquals(MemoryTier.TIER_UNDERSTAND, ContentTierMapper.mapContentTypeToTier(ContentType.ESSAY))
        assertEquals(0.85f, TIER_CONFIGS[MemoryTier.TIER_UNDERSTAND]!!.targetRetention, 0.0001f)
    }

    // ===================== C3.1d: 复用设计文档 memoryTier 字段 =====================

    /** C3.1d: mapContentTypeToTier 返回 MemoryTier 枚举（复用设计文档 memoryTier 字段，非另立体系） */
    @Test
    fun mapContentTypeToTier_returnsMemoryTierEnum() {
        val tier = ContentTierMapper.mapContentTypeToTier(ContentType.TERM_EXPLANATION)
        assertTrue("返回值应为 MemoryTier 枚举", tier is MemoryTier)
        assertTrue(
            "返回值应属于三档枚举之一",
            tier in listOf(MemoryTier.TIER_EXACT, MemoryTier.TIER_FRAMEWORK, MemoryTier.TIER_UNDERSTAND)
        )
    }

    // ===================== USER_CUSTOM 默认映射 =====================

    /** USER_CUSTOM 默认映射到 TIER_FRAMEWORK（名词解释预设 0.90，用户可手动调整） */
    @Test
    fun mapContentTypeToTier_userCustom_defaultsToTierFramework() {
        assertEquals(MemoryTier.TIER_FRAMEWORK, ContentTierMapper.mapContentTypeToTier(ContentType.USER_CUSTOM))
    }

    // ===================== shouldEnableFuzz =====================

    /** shouldEnableFuzz：WORK_RECITATION→false（精确到天），其他→true */
    @Test
    fun shouldEnableFuzz_workRecitationFalse_othersTrue() {
        assertFalse(ContentTierMapper.shouldEnableFuzz(ContentType.WORK_RECITATION))
        assertTrue(ContentTierMapper.shouldEnableFuzz(ContentType.TERM_EXPLANATION))
        assertTrue(ContentTierMapper.shouldEnableFuzz(ContentType.SCHOOL_FEATURE))
        assertTrue(ContentTierMapper.shouldEnableFuzz(ContentType.ESSAY))
        assertTrue(ContentTierMapper.shouldEnableFuzz(ContentType.USER_CUSTOM))
    }

    // ===================== getConfig 与 TIER_CONFIGS 一致 =====================

    /** getConfig 返回的 TierFsrsConfig 与 TIER_CONFIGS 一致 */
    @Test
    fun getConfig_matchesTierConfigs() {
        for (ct in ContentType.entries) {
            val tier = ContentTierMapper.mapContentTypeToTier(ct)
            assertEquals(TIER_CONFIGS[tier], ContentTierMapper.getConfig(ct))
        }
    }
}
