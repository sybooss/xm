# 聊天图片 C2PA 内容凭证检测计划

## 1. 背景

当前聊天图片已经支持上传，并在客服工作台中展示“图片真实性预审”。系统已经接入视觉模型，可以识别图片中可见的 AI 平台水印、明显合成感、裁剪拼接等风险。

本轮不再实现 SynthID。原因是 SynthID 属于 Google 的隐藏水印体系，普通后端无法可靠自行解码；如果没有官方检测接口，系统不应伪装成已经能识别 Gemini 隐藏水印。为了保证答辩可信，本轮只做可解释、可落地的 C2PA 内容凭证检测。

## 2. 功能目标

1. 用户在聊天中发送图片后，后端在原有视觉模型预审之前或之后读取图片文件。
2. 如果本机配置了 `c2patool`，系统调用它检测图片是否包含 C2PA 内容凭证。
3. 如果 C2PA 内容凭证中出现 OpenAI、ChatGPT、GPT Image、DALL-E 等来源信号，图片 AI 生成风险提高，并在客服面板中显示“C2PA 内容凭证已命中”。
4. 如果未安装或未配置工具，系统返回“未配置”，但不影响聊天和图片风险预审主流程。
5. 所有结果写入 `process_trace.detail_json.imageRisk`，方便答辩展示系统处理链路。

## 3. 非目标

- 不实现 SynthID 检测。
- 不承诺 100% 判断图片是否 AI 生成。
- 不把 C2PA 命中结果作为自动退款、自动驳回的唯一依据。
- 不要求用户必须安装 Docker 或额外服务。
- 不在源码中写入任何第三方密钥。

## 4. 后端设计

### 4.1 配置项

在 `application.yml` 中新增：

```yaml
app:
  provenance:
    c2pa:
      enabled: ${C2PA_ENABLED:true}
      tool-path: ${C2PATOOL_PATH:c2patool}
      timeout-seconds: ${C2PA_TIMEOUT_SECONDS:8}
```

本机没有安装 `c2patool` 时，检测状态返回 `NOT_CONFIGURED` 或 `FAILED`，但主业务继续执行。

### 4.2 结果对象

新增 `C2paDetectionResult`，核心字段：

| 字段 | 说明 |
| --- | --- |
| `status` | `DETECTED` / `NOT_FOUND` / `NOT_CONFIGURED` / `FAILED` / `SKIPPED` |
| `provider` | 检测到的来源，如 `OpenAI`、`Google`、`Unknown` |
| `generator` | 生成工具或模型，如 `gpt-image`、`DALL-E` |
| `signal` | 给客服看的简短解释 |
| `rawSummary` | 截断后的原始输出摘要，写入处理轨迹 |

### 4.3 检测服务

新增 `C2paDetectionService` 与实现类 `C2paDetectionServiceImpl`：

1. 校验文件路径必须位于上传目录下，避免路径穿越。
2. 使用 `ProcessBuilder` 调用 `c2patool <imagePath> --json`。
3. 对 stdout 做 JSON/文本两级解析：
   - 能解析 JSON 时，递归搜索 provenance、assertions、claim、generator 等字段。
   - 不能解析 JSON 时，退化为关键词扫描。
4. 关键词命中：
   - OpenAI：`openai`、`chatgpt`、`gpt-image`、`dall-e`、`dalle`
   - Google：`google`、`gemini`、`imagen`
5. 命中时返回 `DETECTED`，未命中但工具正常返回时返回 `NOT_FOUND`。

### 4.4 接入聊天图片预审

`ChatImageRiskService.scan(...)` 保留现有规则和视觉模型逻辑，新增 C2PA 信号合并：

- `DETECTED + OpenAI/gpt-image`：`aiRiskScore += 4`，`authenticityScore += 1`
- `DETECTED + 其他 AI 来源`：`aiRiskScore += 3`
- `NOT_FOUND`：只展示“未发现 C2PA 内容凭证”
- `NOT_CONFIGURED` / `FAILED`：展示检测不可用，不加风险分

`ChatImageRisk` 新增：

- `c2paStatus`
- `c2paProvider`
- `c2paGenerator`
- `c2paSignal`

## 5. 前端设计

`ChatImageRiskPanel.vue` 在信号区增加一行：

```text
C2PA 内容凭证：DETECTED · OpenAI · 内容凭证显示图片来源可能为 GPT Image
```

如果未安装工具，显示：

```text
C2PA 内容凭证：NOT_CONFIGURED · 未配置 c2patool，已使用视觉模型和本地规则兜底
```

这样老师能直接看到系统不是只“猜图片”，而是把内容凭证、视觉模型、本地规则分层展示。

## 6. 验证计划

1. `mvn -q -DskipTests package`
2. `npm run build`
3. `tools/full-smoke-test.ps1`
   - 断言聊天图片返回 `imageRisk`
   - 断言 `imageRisk.c2paStatus` 不为空
   - 断言原有 `RISKY/HIGH` 视觉风险仍然有效
4. `npm run test:browser`
   - 聊天页面发送图片后能看到“图片真实性预审”
   - 能看到“C2PA 内容凭证”
   - 工单原始会话仍能展示聊天图片和风险摘要

## 7. 自审结论

这个方案比“做 SynthID”更适合当前课程项目：它真实、可解释、可演示，也不会因为缺少 Google 官方隐藏水印接口而露馅。它的边界也必须讲清楚：C2PA 依赖图片保留内容凭证，截图、压缩、平台二次转码可能导致凭证丢失；因此 C2PA 只能作为证据链的一层，最终仍要结合视觉模型、订单上下文和人工复核。

本轮计划完整，可以进入实现。
