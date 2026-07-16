# consumer-rules.pro：feature:knowledge 模块消费者 ProGuard 规则（P1-PG Wave 4 补齐）
#
# feature:knowledge 模块包含：KnowledgeViewModel / KnowledgePointDetailViewModel。
# 两个 ViewModel 均 @HiltViewModel 注解。

# ============ Hilt ViewModel ============
-keep @dagger.hilt.android.lifecycle.HiltViewModel class com.wenyan.app.feature.knowledge.** { *; }
