# consumer-rules.pro：feature:settings 模块消费者 ProGuard 规则（P1-PG Wave 4）
#
# feature:settings 模块仅包含 SettingsScreen，无自己的 ViewModel。
# SettingsScreen 使用的 ThemeViewModel 来自 core:designsystem 模块，
# 其 @HiltViewModel 规则由 app/proguard-rules.pro 通用规则覆盖。
# 保持占位，无需补充规则。
