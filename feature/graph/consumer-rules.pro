# consumer-rules.pro：feature:graph 模块消费者 ProGuard 规则（P1-PG Wave 4 补齐）
#
# feature:graph 模块包含：GraphViewModel（@HiltViewModel）。
# ViewModel 由 Hilt 代码生成器创建实例。

# ============ Hilt ViewModel ============
-keep @dagger.hilt.android.lifecycle.HiltViewModel class com.wenyan.app.feature.graph.** { *; }
