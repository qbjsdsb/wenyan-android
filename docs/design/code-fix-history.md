# 已完成代码修复设计文档

> 日期：2026-07-11
> 状态：已批准（方案A：渐进式修复）
> 范围：d:\wenyan\tools 全部Python脚本 + output数据 + manifest

---

## 一、背景

对文研App项目已完成部分进行全面代码审查，发现7个P0严重bug、15个P1设计缺陷、以及多处数据质量问题。这些问题如不修复，将导致：
- RapidOCR全量执行崩溃即丢失全部进度（manifest非原子写入）
- 6个文件1111页全是水印无实际内容（需重新OCR）
- OCR校对闭环完全失效（post_correct不支持RapidOCR格式）
- 多教材交叉校验失效（标题精确匹配）
- LLM调用脆弱（无重试机制）

用户决定：**全部修完再继续RapidOCR全量执行**。

---

## 二、修复原则

1. **不破坏已完成的103个文件**：修复脚本必须幂等，可安全重复运行
2. **先修基础设施再修逻辑**：manifest写入、数据修复优先于代码逻辑修复
3. **每层修复后有验证**：用Python脚本验证修复效果
4. **不引入新抽象**：在现有文件内修复，不创建common.py等新模块
5. **分级升级ocr_status**：RapidOCR高置信度（≥0.95）直接VERIFIED，低置信度保持PENDING

---

## 三、第1层 基础设施与数据修复（6项）

### 修复1.1：rapidocr_pipeline.py manifest原子写入

- **文件**：`d:\wenyan\tools\rapidocr_pipeline.py`
- **行号**：312-321（update_manifest_status函数）
- **问题**：直接`open(MANIFEST_PATH, "w")`写入，崩溃会导致manifest损坏
- **修复**：改用"先写.tmp再os.replace"模式
```python
def update_manifest_status(manifest, file_id, status, extra=None):
    for f in manifest["files"]:
        if f["id"] == file_id:
            f["status"] = status
            if extra:
                f.update(extra)
            break
    tmp_path = MANIFEST_PATH + ".tmp"
    with open(tmp_path, "w", encoding="utf-8") as fp:
        json.dump(manifest, fp, ensure_ascii=False, indent=2)
    os.replace(tmp_path, MANIFEST_PATH)
```

### 修复1.2：6个水印文件重分类

- **文件**：`d:\wenyan\tools\manifest.json`中file_203-208
- **问题**：1111页全是"扫描全能王 创建"水印，ocr_layer只提取了水印
- **修复**：`pdf_type`从`ocr_layer`改为`scan_only`，`status`改为`pending`，删除output中的旧JSON
- **验证**：修复后确认这6个文件在pending列表中，会被RapidOCR处理

### 修复1.3：file_077状态修复

- **文件**：`d:\wenyan\tools\manifest.json`中file_077
- **问题**：JSON已生成（6554段落）但manifest标failed
- **修复**：`status`改为`completed`，清除error/failed_at，补充result_summary
```json
{
  "status": "completed",
  "result_summary": {
    "output_file": "file_077.json",
    "content_source": "TEXTBOOK_NATIVE",
    "ocr_status": "VERIFIED"
  }
}
```

### 修复1.4：file_085状态清理

- **文件**：`d:\wenyan\tools\manifest.json`中file_085
- **问题**：同时含completed + error + failed_at，状态矛盾
- **修复**：清除error和failed_at字段，补充result_summary

### 修复1.5：file_149 NULL字节清理

- **文件**：`d:\wenyan\tools\output\file_149.json`
- **问题**：文本含381个`\u0000`（NULL字节）
- **修复**：替换所有`\u0000`为空格，重新写入文件
- **验证**：grep搜索`\u0000`返回0个结果

### 修复1.6：file_086首页乱码标记

- **文件**：`d:\wenyan\tools\output\file_086.json`
- **问题**：page 1严重乱码但标记为VERIFIED
- **修复**：page 1添加`"quality_flag": "garbled_cover"`标记
- **注意**：不删除乱码文本，保留供后续参考

---

## 四、第2层 管线脚本修复（9项）

### 修复2.1：rapidocr_pipeline.py增加重试机制

- **文件**：`d:\wenyan\tools\rapidocr_pipeline.py`
- **行号**：428-483（文件处理循环）
- **问题**：单次失败就标记failed
- **修复**：引入process_with_retry包装器，3次指数退避重试（1s/2s/4s）
```python
import time

def process_with_retry(func, max_retries=3):
    for attempt in range(max_retries):
        try:
            return func(), None
        except Exception as e:
            if attempt < max_retries - 1:
                wait = 2 ** attempt
                print(f"  重试 {attempt+1}/{max_retries}（等待{wait}秒）: {e}")
                time.sleep(wait)
            else:
                return None, str(e)
```

### 修复2.2：rapidocr_pipeline.py --dpi参数修复

- **文件**：`d:\wenyan\tools\rapidocr_pipeline.py`
- **行号**：411
- **问题**：硬编码DPI=200，--dpi参数失效
- **修复**：
```python
# 在文件处理循环中
if args.dpi is not None:
    DPI = args.dpi
else:
    DPI = 200
```

### 修复2.3：rapidocr_pipeline.py增加zip文件处理

- **文件**：`d:\wenyan\tools\rapidocr_pipeline.py`
- **行号**：354（file_type过滤逻辑）
- **问题**：zip文件被跳过，file_084无法处理
- **修复**：file_type=="zip"时解压，对内部PDF调用ocr_pdf_with_rapidocr
```python
def process_zip_with_rapidocr(engine, zip_path, file_name):
    import zipfile, tempfile, shutil
    temp_dir = tempfile.mkdtemp(prefix="wenyan_zip_")
    try:
        with zipfile.ZipFile(zip_path, "r") as zf:
            zf.extractall(temp_dir)
        # 找到PDF
        for root, dirs, filenames in os.walk(temp_dir):
            for fn in filenames:
                if fn.lower().endswith(".pdf"):
                    return ocr_pdf_with_rapidocr(engine, os.path.join(root, fn), file_name)
        raise RuntimeError("ZIP中未找到PDF")
    finally:
        shutil.rmtree(temp_dir, ignore_errors=True)
```

### 修复2.4：rapidocr_pipeline.py mixed PDF保留置信度

- **文件**：`d:\wenyan\tools\rapidocr_pipeline.py`
- **行号**：270-280（process_mixed_pdf扫描页处理）
- **问题**：扫描页只存储文本，丢弃置信度信息
- **修复**：扫描页调用ocr_image_with_rapidocr获取完整lines/scores

### 修复2.5：post_correct.py增加RapidOCR支持

- **文件**：`d:\wenyan\tools\post_correct.py`
- **行号**：588-606（process_file函数）
- **问题**：无mineru_output_dir时直接skip，RapidOCR文件无法校对
- **修复**：新增parse_rapidocr_pages函数，从data.pages[].lines[]提取行级置信度
```python
def parse_rapidocr_pages(pages):
    """将RapidOCR的pages/lines结构转换为post_correct内部blocks格式。"""
    blocks = []
    for page in pages:
        page_num = page.get("page_num", 0)
        for line in page.get("lines", []):
            blocks.append({
                "text": line.get("text", ""),
                "score": float(line.get("score", 0.0)),
                "page_idx": page_num - 1,
                "block_type": "text",
            })
    return blocks
```

### 修复2.6：统一置信度阈值

- **文件**：`d:\wenyan\tools\post_correct.py`
- **行号**：39-40
- **修复**：`CONFIDENCE_HIGH=0.95`，`CONFIDENCE_MEDIUM=0.85`

### 修复2.7：rapidocr_pipeline.py ocr_status分级升级

- **文件**：`d:\wenyan\tools\rapidocr_pipeline.py`
- **行号**：175-213
- **修复**：高置信度（overall_avg_score≥0.95）→VERIFIED，低置信度→PENDING
```python
ocr_status = "VERIFIED" if overall_avg_score >= 0.95 else "PENDING"
```

### 修复2.8：rapidocr_pipeline.py补充result_summary

- **文件**：`d:\wenyan\tools\rapidocr_pipeline.py`
- **行号**：312-321
- **修复**：update_manifest_status补充result_summary字段
```python
extra["result_summary"] = {
    "output_file": f"{file_id}.json",
    "content_source": data["content_source"],
    "ocr_status": data["ocr_status"],
}
```

### 修复2.9：rapidocr_pipeline.py统一manifest output_file格式

- **文件**：`d:\wenyan\tools\manifest.json`
- **修复**：26个绝对路径output_file改为相对文件名

---

## 五、第3层 下游脚本修复（7项）

### 修复3.1：post_correct.py LLM重试机制

- **文件**：`d:\wenyan\tools\post_correct.py`
- **行号**：309-355
- **修复**：3次指数退避重试（1s/2s/4s），429状态码等待Retry-After

### 修复3.2：extract_knowledge.py LLM重试机制

- **文件**：`d:\wenyan\tools\extract_knowledge.py`
- **行号**：439-480
- **修复**：同上

### 修复3.3：extract_knowledge.py RapidOCR切块优化

- **文件**：`d:\wenyan\tools\extract_knowledge.py`
- **行号**：812-826
- **修复**：无content_list时，利用data.pages[].lines[].text重建段落结构

### 修复3.4：cross_validate.py标题相似度匹配

- **文件**：`d:\wenyan\tools\cross_validate.py`
- **行号**：189-207
- **修复**：标题归一化+difflib.SequenceMatcher相似度>0.8视为同一知识点

### 修复3.5：cross_validate.py移除sys.exit(1)

- **文件**：`d:\wenyan\tools\cross_validate.py`
- **行号**：414-416
- **修复**：改为写空结果文件+警告

### 修复3.6：generate_seed.py真题拆分正则修复

- **文件**：`d:\wenyan\tools\generate_seed.py`
- **行号**：263-266
- **修复**：增加上下文约束，题号行前应为空行/文档开头，不以19/20开头

### 修复3.7：extract_knowledge.py random种子固定

- **文件**：`d:\wenyan\tools\extract_knowledge.py`
- **行号**：703-706
- **修复**：`random.seed(42)`

---

## 六、验证计划

### 第1层验证
- 检查manifest状态分布（completed/pending/skipped/failed数量变化）
- 检查file_077/file_085状态是否正确
- 检查file_203-208是否在pending列表
- 检查file_149是否还有NULL字节

### 第2层验证
- 用file_075测试rapidocr_pipeline的重试机制
- 用file_085测试post_correct是否能解析RapidOCR格式
- 检查manifest写入是否使用原子替换

### 第3层验证
- 模拟LLM API失败测试重试机制
- 用"建安风骨"vs"建安文学"测试cross_validate标题匹配
- 用"1988年..."测试generate_seed真题拆分

---

## 七、不在本次修复范围的问题

以下P1/P2问题记录但不在本次修复范围，后续遇到时再处理：
- P1-1: pipeline_runner.py硬编码用户名和绝对路径（不影响功能）
- P1-3: work_dir不清理（可手动清理）
- P1-4: 代码重复（不引入common.py）
- P2-1至P2-15: 各种代码质量改进建议

---

## 八、修复后重启计划

1. 停止当前RapidOCR进程（PID 19296）
2. 执行第1层修复（基础设施与数据）
3. 执行第2层修复（管线脚本）
4. 执行第3层修复（下游脚本）
5. 验证所有修复
6. 重新启动RapidOCR全量执行（`python rapidocr_pipeline.py --resume`）
