# tools.zip 历史元数据归档

此目录是 `tools.zip` 的**脱敏、元数据-only**归档，不是 App 运行时输入，也不是正式教材、题库或答案来源。

## 内容

- `curate_tools_zip.py`：用 Python 标准库生成本目录的确定性筛选结果。
- `source-manifest.json`：172 条已处理资料的来源索引；已移除绝对路径，跳过资料只保留汇总数量。
- `textbook-structure-summary.json`：15 个教材处理对象的页数、字符数、OCR 评分和章节切分统计；不含目录正文。
- `exam-question-audit.json`：481 条历史真题候选的年份、学科、题型和质量债务统计；不含题干、解析或答案。
- `knowledge-candidate-audit.json`：909 条历史候选知识点的学科、来源覆盖、冲突 ID 和关系数量统计；不含知识正文。

## 重新生成

```bash
python tools/legacy/curate_tools_zip.py /path/to/tools.zip tools/legacy
```

输出 JSON 使用稳定排序和固定换行。每个文件都记录输入压缩包 SHA-256，便于核对是否来自同一份历史快照。

## 明确排除

不得将原压缩包、`output/file_*.json`、完整 `exam_questions.json`、完整 `cross_validated_knowledge.json`、教材/PDF、完整 OCR、下载包、SDK/JDK/Gradle、缓存、日志、`__pycache__` 或 Windows 私有路径复制回仓库。

历史 OCR 的 `VERIFIED` 只是旧流程状态，不等于人工事实核验；真题和候选知识点必须经过可靠原始材料或用户确认后，才能进入正式内容审校流程。
