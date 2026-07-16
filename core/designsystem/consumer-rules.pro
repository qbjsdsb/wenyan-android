# consumer-rules.pro：core:designsystem 模块消费者 ProGuard 规则（P1-PG Wave 4）
#
# core:designsystem 模块包含：M3 组件 / 主题 / ThemeViewModel。
#
# ThemeViewModel 通过 @HiltViewModel 注入，由 app/proguard-rules.pro 的
# `@HiltViewModel` 通用规则覆盖，无需单独声明。
#
# Compose 组件由 Compose Compiler 自动处理 keep 规则，无需手动添加。
# 保持占位，无需补充规则。
