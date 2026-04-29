# LangChain4j 真实模型接入说明

## 1. 接入目标

当前后端已经实现本地规则兜底版 `AiService`，可以保证课堂演示时即使没有模型密钥也能正常回复。下一步接入 LangChain4j 和真实大模型时，目标不是把业务逻辑交给 AI，而是在现有业务链路后增加“自然语言组织与语义增强”能力。

接入后主链路保持不变：

```text
用户消息 -> 意图识别 -> 订单上下文 -> 知识库检索 -> 业务规则判断 -> LangChain4j 生成回复 -> 失败时本地兜底
```

必须保留的原则：

- Spring Boot 仍然负责业务控制、规则判断、订单状态和异常兜底。
- LangChain4j 只作为 AI 增强层，负责提示词装配和真实模型调用。
- AI 调用失败不能导致聊天接口失败，必须回退到本地规则回复。
- API Key 不写入代码、不写入 Git，只通过环境变量或本地配置传入。

## 2. 依赖方案

本项目当前实现采用 LangChain4j 的 OpenAI 模块直接构造 `OpenAiChatModel`，这样可以继续保留现有 Spring Boot `3.3.13`，并且在没有配置 API Key 时仍能正常启动本地兜底模式。

当前 `server/pom.xml` 使用：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

版本选择说明：

- 当前后端 `server/pom.xml` 使用的是 Spring Boot `3.3.13`。
- 直接使用 `langchain4j-open-ai` 不要求升级 Spring Boot parent，适合当前课程工程。
- 官方 Spring Boot starter 方案仍可作为后续升级方向，但 Spring Boot starter 对 Spring Boot 版本有额外要求；如果后续改用 starter，再统一升级 Spring Boot 并回归测试。
- 当前工程已设置 `langchain4j.version=1.13.1`。

本项目优先采用直接注入 `ChatModel` 的方式，不先使用声明式 `@AiService`。原因是当前项目已经有自己的 `AiService`、日志表和兜底逻辑，直接注入 `ChatModel` 更容易和现有 `ai_call_log`、`process_trace` 对接。

`server/pom.xml` 中的关键配置：

```xml
<properties>
    <java.version>17</java.version>
    <langchain4j.version>1.13.1</langchain4j.version>
</properties>

<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

说明：

- 当前项目已经是 Java 17，满足 LangChain4j Spring Boot 集成的 Java 要求。
- 真实接入阶段优先采用 `langchain4j-open-ai` 直接调用方式，降低对 Spring Boot 版本的影响。
- 如果 Maven 编译出现依赖冲突，先检查 LangChain4j 版本，再检查 MyBatis、PageHelper 等 starter 的兼容性。

## 3. 配置方案

在 `server/src/main/resources/application.yml` 中增加真实模型配置。推荐用环境变量，不直接写密钥：

```yaml
app:
  ai:
    enabled: ${AI_ENABLED:false}
    provider: openai-compatible
    model-name: ${OPENAI_MODEL:gpt-4o-mini}
    fallback-enabled: true

langchain4j:
  open-ai:
    chat-model:
      api-key: ${OPENAI_API_KEY:}
      base-url: ${OPENAI_BASE_URL:https://api.openai.com/v1}
      model-name: ${OPENAI_MODEL:gpt-4o-mini}
      temperature: ${OPENAI_TEMPERATURE:0.2}
      max-retries: ${OPENAI_MAX_RETRIES:2}
      timeout-seconds: ${OPENAI_TIMEOUT_SECONDS:30}
      log-requests: false
      log-responses: false
```

OpenAI 官方接口示例：

```powershell
$env:OPENAI_API_KEY="你的 OpenAI Key"
$env:OPENAI_BASE_URL="https://api.openai.com/v1"
$env:OPENAI_MODEL="gpt-4o-mini"
$env:AI_ENABLED="true"
```

OpenAI 兼容接口示例：

```powershell
$env:OPENAI_API_KEY="你的兼容接口 Key"
$env:OPENAI_BASE_URL="https://你的兼容接口域名/v1"
$env:OPENAI_MODEL="服务商提供的模型名"
$env:AI_ENABLED="true"
```

本地或代理模型也可以走 OpenAI-compatible 方式，只要服务端提供 `/v1/chat/completions` 兼容接口即可。

本机 `D:\sub2api` 已验证可用，作为 OpenAI-compatible 服务接入时使用：

```powershell
$env:OPENAI_API_KEY="本地 sub2api Key"
$env:OPENAI_BASE_URL="http://127.0.0.1:8080/v1"
$env:OPENAI_MODEL="gpt-4o-mini"
$env:AI_ENABLED="true"
```

也可以直接运行：

```powershell
powershell -ExecutionPolicy Bypass -File D:\复制软件系统\tools\start-server-with-sub2api.ps1
```

## 4. 后端代码改造点

### 4.1 `AiServiceImpl`

当前 `AiServiceImpl` 是本地兜底实现。接入真实模型后改造为：

```java
@Service
public class AiServiceImpl implements AiService {
    private final ChatModel chatModel;

    public AiServiceImpl(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public AiResult generate(String prompt) {
        long start = System.currentTimeMillis();
        try {
            String reply = chatModel.chat(prompt);
            return new AiResult(
                    true,
                    "SUCCESS",
                    false,
                    provider,
                    modelName,
                    reply,
                    (int) (System.currentTimeMillis() - start),
                    null
            );
        } catch (Exception e) {
            return new AiResult(
                    false,
                    "FAILED",
                    true,
                    provider,
                    modelName,
                    "",
                    (int) (System.currentTimeMillis() - start),
                    e.getMessage()
            );
        }
    }
}
```

注意：

- 异常只写入 `ai_call_log.error_message`，聊天接口仍然返回本地规则回复。
- `request_summary` 只保存提示词摘要，不保存 API Key。
- `response_summary` 保存模型回复摘要，便于日志页面展示。

### 4.2 提示词装配

聊天主链路中传给 `AiService.generate()` 的内容不应只是本地回复，而应包含结构化上下文：

```text
你是电商退换货智能客服系统的回复生成模块。
请基于业务判断、订单上下文和知识库依据生成简洁、可靠、可执行的客服回复。

要求：
1. 不要编造平台规则。
2. 如果订单信息不足，要引导用户补充订单号或必要信息。
3. 如果知识依据为空，要说明只能给出通用建议。
4. 保持语气礼貌、清楚，适合客服工作台展示。

用户问题：
{userMessage}

识别意图：
{intentCode} / {intentName}

订单上下文：
{orderContext}

知识库命中：
{knowledgeHits}

业务规则初步判断：
{localRuleReply}
```

### 4.3 日志写入

真实模型接入后，`ai_call_log` 字段约定如下：

| 字段 | 成功时 | 失败时 | 未启用时 |
| --- | --- | --- | --- |
| provider | `openai-compatible` | `openai-compatible` | `local-fallback` |
| model_name | 实际模型名 | 实际模型名 | `local-rule-template` |
| status | `SUCCESS` | `FAILED` | `SKIPPED` |
| response_summary | 模型回复摘要 | 空 | 空 |
| error_message | 空 | 异常摘要 | `AI 未启用` |

## 5. 接口变化

### 5.1 `GET /system/status`

接入真实模型后返回：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "appName": "returns-assistant",
    "database": {
      "status": "UP",
      "schema": "test3"
    },
    "ai": {
      "status": "UP",
      "provider": "openai-compatible",
      "modelName": "gpt-4o-mini",
      "baseUrlConfigured": true,
      "fallbackEnabled": true
    }
  }
}
```

如果未配置密钥：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "ai": {
      "status": "SKIPPED",
      "provider": "local-fallback",
      "modelName": "local-rule-template",
      "fallbackEnabled": true
    }
  }
}
```

### 5.2 `POST /ai-tests`

请求：

```json
{
  "prompt": "请用一句话说明退货申请流程。"
}
```

真实模型成功时：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "used": true,
    "status": "SUCCESS",
    "fallbackUsed": false,
    "provider": "openai-compatible",
    "modelName": "gpt-4o-mini",
    "reply": "用户可在订单详情页提交退货申请，等待商家审核后按提示寄回商品。",
    "latencyMs": 950,
    "errorMessage": null
  }
}
```

模型失败但兜底可用时：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "used": false,
    "status": "FAILED",
    "fallbackUsed": true,
    "provider": "openai-compatible",
    "modelName": "gpt-4o-mini",
    "reply": "",
    "latencyMs": 3000,
    "errorMessage": "模型调用失败摘要"
  }
}
```

### 5.3 `POST /chat-sessions/{id}/messages`

接口路径和请求体不变：

```json
{
  "content": "这个订单能不能退货？",
  "orderNo": "DD202604290001",
  "useAi": true
}
```

成功使用真实模型时，核心差异是：

```json
{
  "assistantMessage": {
    "sourceType": "AI_ENHANCED"
  },
  "ai": {
    "used": true,
    "status": "SUCCESS",
    "provider": "openai-compatible",
    "modelName": "gpt-4o-mini",
    "fallbackUsed": false
  }
}
```

模型失败时，接口仍返回 `code=1`，但：

```json
{
  "assistantMessage": {
    "sourceType": "FALLBACK"
  },
  "ai": {
    "used": false,
    "status": "FAILED",
    "fallbackUsed": true
  }
}
```

## 6. 验收步骤

1. 设置环境变量：

```powershell
$env:OPENAI_API_KEY="你的真实 Key"
$env:OPENAI_BASE_URL="https://api.openai.com/v1"
$env:OPENAI_MODEL="gpt-4o-mini"
$env:AI_ENABLED="true"
```

2. 启动后端：

```powershell
cd D:\复制软件系统\server
mvn spring-boot:run
```

3. 检查系统状态：

```http
GET http://localhost:8081/system/status
```

验收点：

- `ai.status` 为 `UP`
- `ai.provider` 为 `openai-compatible`
- `ai.modelName` 为实际模型名

4. 测试模型：

```http
POST http://localhost:8081/ai-tests
```

验收点：

- `data.used=true`
- `data.status=SUCCESS`
- `data.reply` 有真实模型回复

5. 测试聊天主链路：

```http
POST http://localhost:8081/chat-sessions/{id}/messages
```

验收点：

- `intent.intentCode` 正常识别
- `knowledgeHits` 有命中依据
- `assistantMessage.sourceType=AI_ENHANCED`
- `ai.status=SUCCESS`
- `ai_call_log` 有成功记录

6. 断开或填错 API Key 再测一次。

验收点：

- 聊天接口仍返回 `code=1`
- `assistantMessage.sourceType=FALLBACK`
- `ai.status=FAILED`
- `ai_call_log.error_message` 记录异常摘要

## 7. 安全注意事项

- 不要把真实 API Key 写入 `application.yml` 并提交。
- 不要在 `ai_call_log.request_summary` 中保存密钥、Authorization Header 或完整敏感请求。
- `log-requests` 和 `log-responses` 开发期可短暂打开，演示和提交前建议关闭。
- 如果使用第三方 OpenAI-compatible 服务，必须确认模型名和 base URL 由服务商提供。
