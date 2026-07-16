# consumer-rules.pro：feature:aiassistant 模块消费者 ProGuard 规则（P1-PG Wave 4 补齐）
#
# feature:aiassistant 模块包含：AiAssistantViewModel / ApiConfigViewModel。
# 两个 ViewModel 均 @HiltViewModel 注解，由 Hilt 代码生成器创建实例。

# ============ Hilt ViewModel ============
# 虽然 app/proguard-rules.pro 已有通用 @HiltViewModel 规则，
# 此处显式声明，使模块自包含（被其他 app 复用时也能保护）。
-keep @dagger.hilt.android.lifecycle.HiltViewModel class com.wenyan.app.feature.aiassistant.** { *; }
