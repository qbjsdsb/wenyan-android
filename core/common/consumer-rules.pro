# consumer-rules.pro：core:common 模块消费者 ProGuard 规则（P1-PG Wave 4）
#
# core:common 模块仅包含通用工具类与常量，无反射调用 / 序列化 / Room / Retrofit 依赖。
# Hilt 注入的工具类（如有）由 app/proguard-rules.pro 的 Hilt 通用规则覆盖。
# 保持占位，无需补充规则。
