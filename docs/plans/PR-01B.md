# PR-01B：把 seed 审计与未漂移检查接入普通 Android CI

> 状态：计划已补齐，尚未实施
> 前置工单：PR-01A 必须先合并到最新 `main`
> 本文件用途：作为 Luna 执行 PR-01B 的唯一范围合同、验收清单和交接说明
> 重要说明：本文件本身不授权实施。开始 PR-01B 时仍须先做只读核对，等待用户明确发送“允许实施”。

## 1. 工单唯一目标

PR-01B 只解决一个问题：把已经在 PR-01A 中用本地测试证明稳定的 seed 质量门禁接入普通 Android CI，使每个 PR 都能在 Android 单测和 APK 构建前发现内容结构、来源、ID、引用、baseline ratchet 或 seed 漂移问题。

本工单的完成状态必须同时具备：

1. 普通 CI 会读取正式 seed、版本化 schema 和机器可读 baseline；
2. CI 会执行确定性的审计和等价的 seed 未漂移检查；
3. 审计失败会阻止后续 Android 测试/构建，并留下不含教材正文的精简报告；
4. CI 只能检查，不能自动重写 seed、baseline 或提交任何文件；
5. 现有 Android 测试先于 debug APK 构建的安全顺序保持不变；
6. release workflow、签名、tag 触发、Secrets 和产品行为完全不变。

PR-01B 不重新设计审计规则，不处理当前 seed 债务，不补教材来源，不修改任何正式内容。发现 PR-01A 的审计合同或实现不足时，先停在证据报告，不通过放宽 CI 规则来掩盖问题。

## 2. 前置条件与开工门槛

### 2.1 必须满足的前置条件

PR-01B 只能从“PR-01A 已合并后的最新 `main`”开始，不能从仍含 PR-01A 未提交改动的工作树直接实施。开工前必须确认：

- PR-01A 的 Draft PR 已经过 diff 审阅、定向测试和 GitHub Actions 验证；
- PR-01A 已合并，且 `origin/main` 已包含 `tools/content_pipeline/`、`content/schema/`、`content/baselines/` 和 `tools/tests/`；
- 当前新分支从最新 `origin/main` 创建，不能直接在 `main` 上改；
- 当前工作树干净，没有把用户已有改动、临时报告或附件带入 PR-01B；
- `.github/workflows/release.yml`、`app/src/main/assets/seed_data.json` 和 Android 源码仍与基线一致。

建议的只读开工命令：

```bash
git fetch origin
git status --short --branch
git rev-parse HEAD
git rev-parse origin/main
git merge-base --is-ancestor origin/main HEAD
git diff --name-status origin/main...HEAD
git ls-files --others --exclude-standard
```

若 PR-01A 尚未合并、`origin/main` 缺少其审计文件、工作树不干净或发现 seed/业务代码已有未解释变化，应停止并报告，不自行拼接分支或覆盖改动。

### 2.2 开工时要重新读取的文件

Luna 必须在只读核对阶段读取：

- `AGENTS.md`；
- `docs/00-STATUS.md`；
- `docs/plans/WENYAN-MASTER-PLAN.md`；
- `docs/plans/PR-01A.md`；
- 本文件；
- `tools/content_pipeline/audit_seed.py`；
- `content/schema/seed.schema.json`；
- `content/baselines/seed-baseline.json`；
- `tools/tests/test_audit_seed.py` 及其 fixture；
- `.github/workflows/android.yml`；
- `.github/workflows/release.yml`；
- Gradle wrapper/版本约束和现有 CI 失败记录。

## 3. 当前架构事实与实现选择

### 3.1 现有普通 CI 的安全顺序

当前普通 Android workflow 已固定使用 JDK 17、Gradle 8.14.4，并采用“`testDebugUnitTest` 成功后才 `assembleDebug`”的顺序。PR-01B 只能在 Android 单测之前插入内容门禁，不能把构建提前，也不能改成先打 APK 后测试。

目标顺序如下：

| 顺序 | CI 步骤 | 失败时的行为 |
| ---: | --- | --- |
| 1 | checkout | job 失败，后续不运行 |
| 2 | 设置 Python 运行时 | job 失败，后续不运行 |
| 3 | Python 审计测试与正式 seed 审计 | 阻止 Android 测试和构建 |
| 4 | 正式 seed 审计报告上传 | `always()` 上传已生成的精简报告 |
| 5 | 设置 JDK 17、Gradle 8.14.4 | 保持现有配置 |
| 6 | `gradle testDebugUnitTest --no-daemon --stacktrace` | 测试失败时不构建 APK |
| 7 | `gradle assembleDebug --no-daemon --stacktrace` | 构建失败，不上传伪成功 APK |
| 8 | 上传 debug APK | 仅在构建成功时上传 |

实现时可以调整步骤的具体排列，但必须保留两个语义：内容门禁在 Android 测试/构建前，Android 单测在 debug 构建前。

### 3.2 “可重复生成”采用等价的未漂移检查

当前仓库没有可以在干净 CI runner 上安全、无网络、无版权材料依赖地重建正式 seed 的输入快照。旧的 `tools/generate_seed.py` 会写回目标 seed，并使用动态生成时间；它不能直接作为 CI 命令。`tools.zip` 也是历史快照，不是 CI 数据源。

因此本工单固定采用当前架构下的等价方案，而不虚构“CI 已重新生成教材 seed”：

1. `audit_seed.py` 计算正式 seed 的 SHA-256，并与 baseline 的 `seed_sha256` 比较；任何字节变化都必须产生 `BASELINE_SEED_SHA256_MISMATCH` 并失败；
2. 同一 baseline 同时保护 seed version、各集合 ID manifest 和已记录指标的 ratchet；
3. CI 不传 `--write-baseline`，不执行任何 baseline 写入路径；
4. CI 运行同一审计两次，并用 `cmp` 比较报告字节，证明 runner 上的输出仍然确定；
5. 若实现核对发现现有 CLI 的 `--check` 只被解析但未明确表达只读检查语义，可以在当前允许工具目录内做最小修复并增加回归测试；不得改变默认 fail-closed 行为，也不得引入第二套互相漂移的 hash 规则。

这是一项“仓库正式 seed 未漂移检查”，不是对缺失的 OCR/LLM 原始资料进行再生成。未来若要实现真正的可重复生成，必须另立工单补齐版本化、可公开放入仓库的输入快照，不能在 PR-01B 偷渡。

## 4. 允许修改范围

PR-01B 实施时仅允许修改：

- `.github/workflows/android.yml`：增加 Python 审计步骤、报告处理和必要的 CI 环境配置；
- `tools/content_pipeline/`：仅在现有 CLI 无法清晰表达未漂移检查或 CI 入口时增加最小标准库实现；优先复用 `audit_seed.py`，不重复实现审计规则；
- `tools/tests/`：补充 CI 入口所需的回归测试，例如 baseline seed SHA 改变失败、`--check` 不写 seed/baseline、报告连续运行字节一致；
- `tools/README.md`：说明普通 CI 命令、报告位置、禁止 `--write-baseline` 和等价未漂移检查的边界。

本文件和 PR-01A 的正式文档属于计划资料，不是 PR-01B 的业务实现范围。除非只为修正可核验的工单交叉引用，不要顺手重写其他文档。

## 5. 明确不做与保护性不变量

PR-01B 明确不得修改：

- `app/src/main/assets/seed_data.json` 的内容、格式、顺序、版本或任何既有 ID；
- `content/schema/seed.schema.json` 或 `content/baselines/seed-baseline.json` 来让 CI 变绿；
- Room、Kotlin、Compose、导航、ViewModel、业务逻辑、用户数据或学习记录；
- `gradle/libs.versions.toml`、Gradle wrapper、App versionCode/versionName；
- `.github/workflows/release.yml`、签名配置、tag 触发条件、Secrets 或发布流程；
- `tools.zip` 的内容，及其中的教材、扫描件、SDK、JDK、Gradle、缓存、日志、绝对路径和 `__pycache__`；
- OCR/LLM/网络依赖、云服务或需要 API key 的步骤。

必须继续保护：

- 既有 `knowledge point`、`exam question`、`chapter` ID 不删除、不重排、不复用；
- memo、FSRS、review log、错题、练习记录、写作草稿、设置和 AI 配置不被清理；
- baseline 只记录已人工承认的存量债务，新债务、删除旧 ID、悬空引用和未批准跨科关系仍失败；
- CI 日志和 artifact 只出现规则、摘要、集合、字段、ID 和短模式名，不输出教材正文、完整题干或原始 OCR 文本。

## 6. 预期 CI 实现合同

### 6.1 Python 环境

- 使用 GitHub Actions 官方 `actions/setup-python` 固定 Python 3.12 或仓库已验证的同一小版本；
- 不安装第三方包，不读取用户目录，不访问网络资料；
- 从仓库根目录执行 `python -m ...`，保证模块导入路径与本地命令一致；
- 不把 `tools.zip`、原始 OCR 目录或任何教材资料复制到 runner artifact。

### 6.2 正式审计命令

CI 应使用以下语义等价的命令；若路径变量写法不同，最终报告必须仍使用稳定逻辑路径：

```bash
python -m tools.content_pipeline.audit_seed --seed app/src/main/assets/seed_data.json --schema content/schema/seed.schema.json --baseline content/baselines/seed-baseline.json --report "$RUNNER_TEMP/wenyan-seed-audit-1.json" --as-of-year 2026 --check

python -m tools.content_pipeline.audit_seed --seed app/src/main/assets/seed_data.json --schema content/schema/seed.schema.json --baseline content/baselines/seed-baseline.json --report "$RUNNER_TEMP/wenyan-seed-audit-2.json" --as-of-year 2026 --check

cmp "$RUNNER_TEMP/wenyan-seed-audit-1.json" "$RUNNER_TEMP/wenyan-seed-audit-2.json"
```

这里不能出现 `--write-baseline`。如果审计失败，必须保留非零退出码；不能使用 `|| true`、删除违规、降低断言、替换 baseline 或继续执行构建来吞掉失败。

### 6.3 报告与版权边界

CI 至少应上传两个报告中的一个或合并后的精简报告，且上传步骤使用 `if: always()`，这样 schema/审计失败时仍能下载证据。报告应：

- 使用固定 JSON 排序、UTF-8 和末尾换行；
- 只包含 counts、metrics、violation code、collection、ID、field、短 reason/pattern 和 result；
- 不包含正式 seed 的完整文本、题干、答案、教材引文或扫描图；
- artifact 使用短保留期，不改变现有 APK artifact 的名称和保留策略；
- 报告上传失败不能把已经失败的审计伪装成成功。

若 GitHub Step Summary 需要增加提示，只输出类似“`CROSS_SUBJECT_RELATION: exam_questions/eq_0038 -> kp_...`”的摘要，不复制报告正文。

### 6.4 Android 步骤保护

除插入内容门禁和必要的 Python setup 外，保留现有 JDK、Gradle、缓存、超时、单测命令、debug 构建命令和 APK artifact 语义。不得为了缩短 CI 而跳过 `testDebugUnitTest`、改跑 release、降低 JVM 内存、删除 `--stacktrace` 或改变发布 workflow。

## 7. 测试与证据矩阵

### 7.1 必须补齐或重新确认的定向测试

至少覆盖：

| 场景 | 预期证据 |
| --- | --- |
| 正式/最小合法 seed 审计两次 | 两份报告字节完全一致，退出码为 0 |
| baseline 的 `seed_sha256` 与当前 seed 不同 | 非零退出，出现 `BASELINE_SEED_SHA256_MISMATCH` |
| 传入 `--check` | 只检查，seed 和 baseline 字节均不变化 |
| 不传 `--write-baseline` | baseline 不被自动生成或覆盖 |
| 新增 debt、删除旧 ID、悬空引用 | 非零退出，原规则仍生效 |
| CI 失败报告 | 不含教材正文或完整题干 |
| 旧 Android workflow 顺序 | Python 门禁在 Android 单测/构建之前，单测仍在 assembleDebug 之前 |

### 7.2 本地验证命令

在 PR-01B 分支完成实现后，至少运行并真实记录：

```bash
python -m unittest discover -s tools/tests -p 'test*.py'
```

```bash
python -m tools.content_pipeline.audit_seed --seed app/src/main/assets/seed_data.json --schema content/schema/seed.schema.json --baseline content/baselines/seed-baseline.json --report /tmp/wenyan-seed-audit-1.json --as-of-year 2026 --check
python -m tools.content_pipeline.audit_seed --seed app/src/main/assets/seed_data.json --schema content/schema/seed.schema.json --baseline content/baselines/seed-baseline.json --report /tmp/wenyan-seed-audit-2.json --as-of-year 2026 --check
cmp /tmp/wenyan-seed-audit-1.json /tmp/wenyan-seed-audit-2.json
git diff --exit-code -- app/src/main/assets/seed_data.json
git diff --check
```

若环境允许且不引入无关改动，再运行：

```bash
gradle testDebugUnitTest --no-daemon --stacktrace
gradle assembleDebug --no-daemon --stacktrace
```

若本地 Gradle 因网络、JDK、SDK 或 wrapper 锁问题无法运行，必须报告“未运行及具体原因”，不能把 GitHub Actions 尚未运行写成通过。

### 7.3 GitHub Actions 验收

因为本工单修改 workflow，本地命令通过不等于验收完成。必须：

1. 只提交 PR-01B 范围内改动并推送独立分支；
2. 创建 Draft PR，目标为 `main`；
3. 等待真实 GitHub Actions 完整运行；
4. 确认内容门禁、Python 测试、`testDebugUnitTest`、`assembleDebug` 和 artifact 步骤均符合预期；
5. CI 失败时先读取 job/step/log 诊断，不通过跳过审计或修改 baseline 修复；
6. 在人工检查 diff、报告和保护性不变量后，才允许将 Draft 标记为 Ready 或合并。

## 8. 验收标准

PR-01B 只有在以下条件全部满足时才算完成：

- [ ] 从 PR-01A 合并后的最新 `main` 新建独立分支；
- [ ] `.github/workflows/android.yml` 在 Android 单测和构建前执行内容审计；
- [ ] 正式 seed、schema、baseline 的路径和参数明确且无机器绝对路径；
- [ ] `--write-baseline` 未出现在 CI，CI 不会修改或提交 baseline；
- [ ] seed 任意字节变化、版本变化、旧 ID 删除、悬空引用、新 debt 和非法 schema/JSON 均能阻止 job 通过；
- [ ] 同一审计连续运行两次的报告字节完全一致；
- [ ] CI 失败时可下载精简报告，报告没有教材正文或完整题干；
- [ ] Python 工具测试全绿；
- [ ] `testDebugUnitTest` 仍先于 `assembleDebug`，两者均真实通过；
- [ ] release workflow、签名、tag、Secrets、版本号、seed、Room、Kotlin、Compose 和用户数据没有变化；
- [ ] `git diff --check` 通过，修改文件未越出允许范围；
- [ ] GitHub Actions 全绿，Draft PR 经人工审阅后才进入 Ready/合并流程。

## 9. 风险、停工和回滚

### 9.1 必须停工的情况

- PR-01A 没有合并或 `origin/main` 不是实际基线；
- 审计报告依赖当前时间、runner 绝对路径或环境随机顺序；
- 旧 seed 生成脚本需要教材、OCR 输出、LLM、Secrets 或写入仓库；
- 报告包含完整教材正文、题干、答案或扫描图；
- 为了让 CI 通过需要改 seed、删规则、扩大 baseline、放宽 schema 或跳过 Android 测试；
- 需要修改 release workflow、签名、版本号、Room 或业务代码；
- GitHub Actions 失败原因不清楚，或怀疑是已有主分支/网络/runner 问题。

### 9.2 回滚方式

PR-01B 应尽量形成一个可独立回滚的 workflow/工具提交。若 CI 接入造成问题，回滚该 PR 的提交即可恢复原有 Android 测试和构建流程；不得删除 seed、baseline、历史 ID、tag 或重写 `main` 历史。任何 baseline 更新都必须另立人工审阅提交，不能作为 CI 故障回滚手段。

## 10. 给 Luna 的执行话术

### 10.1 开工核对话术

```text
从 PR-01A 已合并后的最新 main 开始。本次只做 PR-01B 的只读核对，暂时不得修改文件。

请先完整读取：AGENTS.md、docs/00-STATUS.md、docs/plans/WENYAN-MASTER-PLAN.md、docs/plans/PR-01A.md、docs/plans/PR-01B.md、tools/content_pipeline/audit_seed.py、content/schema/seed.schema.json、content/baselines/seed-baseline.json、tools/tests/test_audit_seed.py、.github/workflows/android.yml、.github/workflows/release.yml，以及 Gradle 版本约束和现有 CI 失败记录。

本工单唯一目标：把 PR-01A 已证明稳定的 seed schema/audit/baseline ratchet 接入普通 Android CI，并用现有 audit_seed 对 baseline.seed_sha256、seed version、ID manifest 和 ratchet 指标执行等价的 seed 未漂移检查。连续两次审计报告必须字节一致。不要调用旧 tools/generate_seed.py，不要把 tools.zip 当 CI 数据源。

请只读报告：当前分支/HEAD/main 基线，PR-01A 是否已合并，现有 workflow 步骤与顺序，审计命令和失败退出码，报告是否会泄露教材正文，Python 环境来源，拟修改文件，预计运行时间，风险与回滚方式。

允许修改仅限：.github/workflows/android.yml、必要的 tools/content_pipeline/ 轻量入口、tools/tests/、tools/README.md。不得改 seed、schema、baseline、Room、Kotlin、Compose、Gradle 版本、版本号、用户数据、release workflow、签名 Secrets 或 tools.zip。

报告后停止，等待我明确发送“允许实施”。
```

### 10.2 允许实施话术

只有只读报告与本合同一致后发送：

```text
我已核对你的 PR-01B 开工报告，当前基线、前置条件和改动范围符合合同。现在允许你只实施 PR-01B。

请从你刚刚核对的最新 main 创建独立分支。先补能证明 seed SHA 漂移失败、--check 只读、报告确定性的测试，再做最小 CI 接入。优先复用 audit_seed.py，不调用旧 generate_seed.py，不自动写 baseline。

CI 必须在 testDebugUnitTest 和 assembleDebug 之前运行；保留 JDK 17、Gradle 8.14.4、现有测试命令和“测试通过后再构建”的顺序。失败时上传不含教材正文的精简报告。不要修改正式 seed、schema、baseline、Room、Kotlin、Compose、Gradle、版本号、用户数据或 release workflow。

完成后不要提交、推送或创建 Draft PR。运行定向 Python 测试、正式 seed 两次审计并 cmp、git diff --check；如果环境允许再运行 testDebugUnitTest 和 assembleDebug。报告实际修改文件、真实测试结果、未运行项目及原因、CI 风险和回滚点，然后等待自审。
```

## 11. 提交、Draft PR 与合并边界

PR-01B 只能在本地自审通过后提交；提交信息必须说明“为什么把内容审计接入 CI”，不能只写 `update workflow`。Draft PR 正文必须包含：

- 目标与明确不做；
- workflow、工具、测试和文档修改分类；
- seed、ID、用户数据和 release 流程保护说明；
- Python 审计、两次报告 `cmp`、Android 单测和 debug 构建的真实结果；
- 未运行命令及原因；
- CI 失败报告和版权边界；
- 真机/人工验收清单、风险和回滚方法。

不要在 PR-01B 中合并、打 tag、发布 APK 或开始 PR-02A。只有 CI 全绿且人工验收完成后，才使用项目固定的“允许合并”话术。
