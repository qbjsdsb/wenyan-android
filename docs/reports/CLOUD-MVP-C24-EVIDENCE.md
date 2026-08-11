# Cloud MVP C24 最终证据（2026-08-11）

## 闭环映射

1. 冷启动 Today：四段导航测试锁定 Today start destination。
2. 到期 LearningUnit：unit queue/rate/undo 与 FSRS 测试覆盖。
3. 真题提纲、4. 主动揭示/错因：PracticeAttempt 状态机与详情恢复测试覆盖。
5. 610 微写作、6. 本地量规：WritingSession 离线编辑、七维自评与编码恢复测试覆盖。
7. 次日修复：Room 事务测试覆盖 later-date、幂等和回滚。
8. 同日重启不重排：DailyPlan repository 测试覆盖。
9. 杀进程恢复草稿：SavedState session ID + Room reload + assessment round-trip 覆盖。
10. 离线运行：写作路径无网络/API 依赖。
11. UNKNOWN 降级：provenance 与 reviewed-only evidence tests 覆盖。
12. v10→v15：JVM SQLite 生产 migration 链校验最终 schema 与 fixture 保留。
13. seed/ID：只读双 audit/cmp；seed SHA-256 `d6385911bf31fbec8af168d5e882ec0bfc32be32c333fe14a28fc19db2726446`，seed/baseline diff 为空。

## 十分钟设备验收清单

1. 1 分钟：冷启动确认进入“今日”，四个顶层 Tab 可切换且返回栈正确。
2. 1 分钟：完成一个到期卡片，撤销一次，确认同一 unit 状态恢复。
3. 2 分钟：进入真题，先填提纲再揭示；选择错因并完成，确认次日修复提示。
4. 3 分钟：进入写作素材→开始离线写作；切换 10/30/完整模式，暂停/恢复，填写长正文并观察自动保存。
5. 1 分钟：杀进程重开相同 session，确认草稿、自评和计时累计恢复。
6. 1 分钟：断网重试上述写作和自评；确认无 API 错误。
7. 1 分钟：检查 UNKNOWN/LEGACY 来源仅显示待核，不进入可引用证据。

Cloud runner 无 `/dev/kvm`，故设备清单交由 KVM runner/人工执行；instrumentation APK 已成功构建，未冒充设备执行。

## 完整 diff 自审

- 新 migration 均显式、增量；无升级 destructive fallback。现有 downgrade-only fallback 未扩大。
- 新用户草稿外键均 `SET NULL`；新增 ownership CASCADE 仅限新派生 unit/plan 子记录，未对旧用户主体表增加级联删除入口。
- 无新增 `REPLACE`；DailyPlan/Practice/Writing 插入使用 ABORT/事务。
- 续审发现旧 `WritingMaterialDao.REPLACE` 与新来源 CASCADE 组合会在 seed 更新时先删父行并误删 provenance；已改为 Room `@Upsert`，Robolectric 测试验证更新素材后来源行仍存在。
- seed、baseline、versionName/versionCode、签名和 release workflow 未修改；仅 app 增加 `feature:today` 模块依赖。
- 大文件主要是 Room 导出 schema 与 verifier；新业务文件保持职责单一。
- 未新增教材事实、题干、答案、来源、既有 ID 或 API 调用。
- C00–C23 的 PASS 均有实际 JVM/构建证据；Android instrumentation 始终诚实标注 PENDING_KVM。

## 续审强化证据

- 写作活动计时使用 `SystemClock.elapsedRealtime()` 单调锚点，系统 wall clock 回拨不改变已累计用时；跨进程时才由持久 wall start 重建基线。
- 返回导航先 flush 最新草稿；debounce、失败重试、flush、长正文与 JSON evidence ID 均有 JVM 测试。
- 写作证据不再允许自由文本伪造 ID：repository 提供真实素材，只有 REVIEWED 可选择，legacy/unknown 显示为禁用待核线索。
- 七维自评接入每维备注、首次/多次历史趋势和弱项离线后续任务；自评仍先于任何未来 AI，未新增 API。
