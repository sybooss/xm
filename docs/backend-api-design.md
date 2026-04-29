# 后端 RESTful 接口设计说明书

## 1. 设计依据

本文档用于指导新版电商退换货智能客服系统的 Spring Boot 后端开发。接口风格参考 `D:\DEVELOP\web-ai\webproject2\tilians` 项目，采用资源导向的 RESTful 写法，并尽量保持该项目已有的 Controller、返回值和分页习惯。

参考项目中的主要约定：

- Controller 使用 `@RestController`，资源类接口使用 `@RequestMapping("/资源名复数")`。
- 查询使用 `GET`，新增使用 `POST`，修改使用 `PUT`，删除使用 `DELETE`。
- 成功返回 `Result.success()` 或 `Result.success(data)`。
- 失败返回 `Result.error(msg)`。
- 统一响应字段为 `code`、`msg`、`data`，其中成功 `code=1`，失败 `code=0`。
- 分页结果使用 `PageResult<T>`，字段为 `total` 和 `rows`。
- 新增、修改、删除类接口后续可加 `@OperatorAnno` 记录操作日志。

本系统后端连接 `test3` 数据库，主要支撑 Vue 3 前端页面、MySQL 数据表和 LangChain4j AI 增强层。

## 2. 通用约定

### 2.1 基础地址

参考现有 `tilians` 项目的写法，第一阶段不强制加 `/api` 前缀：

```text
http://localhost:8080
```

如果后续前端统一代理到 `/api`，可以在 Vite 代理层处理，不要求 Controller 路径全部带 `/api`。

### 2.2 统一返回结构

成功无数据：

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```

成功有数据：

```json
{
  "code": 1,
  "msg": "success",
  "data": {}
}
```

失败：

```json
{
  "code": 0,
  "msg": "订单不存在",
  "data": null
}
```

建议沿用参考项目中的 `Result` 类：

```java
public class Result {
    private Integer code;
    private String msg;
    private Object data;

    public static Result success() { ... }
    public static Result success(Object data) { ... }
    public static Result error(String msg) { ... }
}
```

### 2.3 分页返回结构

分页接口统一返回 `PageResult<T>`：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 35,
    "rows": []
  }
}
```

对应 Java 结构：

```java
public class PageResult<T> {
    private Long total;
    private List<T> rows;
}
```

分页参数统一放在 query string 中：

```text
page=1&pageSize=10
```

### 2.4 命名规范

路径命名：

- 使用资源名复数，例如 `/chat-sessions`、`/knowledge-docs`、`/orders`。
- 子资源使用层级路径，例如 `/chat-sessions/{id}/messages`。
- 查询列表用 `GET /resources`。
- 查询详情用 `GET /resources/{id}`。
- 新增用 `POST /resources`。
- 修改用 `PUT /resources/{id}`。
- 删除用 `DELETE /resources/{id}`。

Controller 命名建议：

| Controller | 路径 |
| --- | --- |
| `ChatSessionController` | `/chat-sessions` |
| `KnowledgeCategoryController` | `/knowledge-categories` |
| `KnowledgeDocController` | `/knowledge-docs` |
| `OrderController` | `/orders` |
| `AfterSaleRecordController` | `/after-sale-records` |
| `ProcessTraceController` | `/process-traces` |
| `SystemController` | `/system` |

Service、Mapper、Pojo 命名沿用参考项目风格：

```text
pojo/
  Result.java
  PageResult.java
  ChatSession.java
  ChatMessage.java
  KnowledgeDoc.java
  KnowledgeDocSearch.java
service/
mapper/
controller/
```

### 2.5 主要枚举

```text
intentCode:
PRE_SALE, RETURN_APPLY, EXCHANGE_APPLY, REFUND_PROGRESS,
LOGISTICS_QUERY, RULE_EXPLAIN, COMPLAINT_TRANSFER

messageRole:
USER, ASSISTANT, SYSTEM

replySource:
RULE_TEMPLATE, AI_ENHANCED, FALLBACK

docStatus:
ENABLED, DISABLED

sessionStatus:
ACTIVE, CLOSED
```

### 2.6 异常处理

建议沿用参考项目的 `@RestControllerAdvice` 全局异常处理方式：

- 参数错误：返回 `Result.error("请求参数错误")`。
- 数据不存在：返回 `Result.error("数据不存在")`。
- 重复数据：返回 `Result.error("xxx已经存在")`。
- AI 调用失败：聊天接口不直接失败，而是记录日志并返回兜底回复。

## 3. 系统接口

### 3.1 查询系统状态

```http
GET /system/status
```

用途：检查后端、数据库和 AI 增强层状态。

返回示例：

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
      "fallbackEnabled": true
    }
  }
}
```

### 3.2 查询枚举字典

```http
GET /system/enums
```

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "intentCodes": [
      { "code": "RETURN_APPLY", "name": "退货申请" },
      { "code": "EXCHANGE_APPLY", "name": "换货申请" },
      { "code": "REFUND_PROGRESS", "name": "退款进度" }
    ],
    "docTypes": [
      { "code": "FAQ", "name": "常见问题" },
      { "code": "POLICY", "name": "平台规则" }
    ]
  }
}
```

## 4. 会话资源 `/chat-sessions`

### 4.1 分页查询会话

```http
GET /chat-sessions?page=1&pageSize=10&status=ACTIVE&keyword=退货
```

参数说明：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| page | 否 | 当前页 |
| pageSize | 否 | 每页条数 |
| status | 否 | `ACTIVE`、`CLOSED` |
| keyword | 否 | 按标题、订单号、最近意图搜索 |

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 1,
    "rows": [
      {
        "id": 12,
        "sessionNo": "S202604290001",
        "title": "退货咨询",
        "status": "ACTIVE",
        "currentIntent": "RETURN_APPLY",
        "orderNo": "DD202604290001",
        "updatedAt": "2026-04-29 08:45:00"
      }
    ]
  }
}
```

### 4.2 新增会话

```http
POST /chat-sessions
```

请求体：

```json
{
  "userId": 1,
  "orderNo": "DD202604290001",
  "title": "退货咨询"
}
```

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 12,
    "sessionNo": "S202604290001",
    "title": "退货咨询",
    "status": "ACTIVE",
    "orderNo": "DD202604290001"
  }
}
```

实现建议：

```java
@PostMapping
@OperatorAnno
public Result save(@RequestBody ChatSession chatSession) {
    chatSessionService.save(chatSession);
    return Result.success(chatSession);
}
```

### 4.3 查询会话详情

```http
GET /chat-sessions/{id}
```

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 12,
    "sessionNo": "S202604290001",
    "title": "退货咨询",
    "status": "ACTIVE",
    "currentIntent": "RETURN_APPLY",
    "summary": "用户咨询已签收订单是否可以退货。",
    "order": {
      "orderNo": "DD202604290001",
      "productName": "无线蓝牙耳机",
      "orderStatus": "SIGNED",
      "afterSaleStatus": "NONE"
    },
    "messages": [
      {
        "id": 101,
        "role": "USER",
        "content": "这个订单能不能退货？",
        "seqNo": 1,
        "createdAt": "2026-04-29 08:41:00"
      }
    ]
  }
}
```

### 4.4 修改会话

```http
PUT /chat-sessions/{id}
```

用途：修改会话标题、状态、绑定订单等信息。

请求体：

```json
{
  "title": "退货和退款咨询",
  "orderNo": "DD202604290001",
  "status": "ACTIVE"
}
```

返回：

```json
{
  "code": 1,
  "msg": "success",
  "data": null
}
```

### 4.5 删除会话

```http
DELETE /chat-sessions/{id}
```

说明：课程项目中建议删除会话时同步删除该会话下消息、意图记录、检索日志、AI 调用日志和处理轨迹。也可以扩展为软删除。

### 4.6 查询会话消息

```http
GET /chat-sessions/{id}/messages
```

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {
      "id": 101,
      "role": "USER",
      "content": "这个订单能不能退货？",
      "seqNo": 1,
      "createdAt": "2026-04-29 08:41:00"
    },
    {
      "id": 102,
      "role": "ASSISTANT",
      "content": "该订单已签收 3 天，通常仍在七天无理由退货范围内。",
      "seqNo": 2,
      "intentCode": "RETURN_APPLY",
      "sourceType": "AI_ENHANCED",
      "createdAt": "2026-04-29 08:41:03"
    }
  ]
}
```

### 4.7 新增会话消息

```http
POST /chat-sessions/{id}/messages
```

用途：聊天主接口。该接口虽然是新增消息，但后端内部会完成意图识别、订单上下文读取、知识检索、AI 增强回复和处理轨迹记录。

请求体：

```json
{
  "content": "这个订单能不能退货？",
  "orderNo": "DD202604290001",
  "useAi": true
}
```

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "userMessage": {
      "id": 101,
      "role": "USER",
      "content": "这个订单能不能退货？",
      "seqNo": 1
    },
    "assistantMessage": {
      "id": 102,
      "role": "ASSISTANT",
      "content": "该订单已签收 3 天，通常仍在七天无理由退货范围内。建议保持商品完好，并在订单详情页提交退货申请。",
      "seqNo": 2,
      "intentCode": "RETURN_APPLY",
      "sourceType": "AI_ENHANCED"
    },
    "intent": {
      "intentCode": "RETURN_APPLY",
      "intentName": "退货申请",
      "confidence": 0.9200,
      "method": "HYBRID"
    },
    "orderContext": {
      "orderNo": "DD202604290001",
      "productName": "无线蓝牙耳机",
      "orderStatus": "SIGNED",
      "signedDays": 3
    },
    "knowledgeHits": [
      {
        "docId": 6,
        "title": "七天无理由退货规则",
        "rankNo": 1,
        "score": 0.8700
      }
    ],
    "ai": {
      "used": true,
      "status": "SUCCESS",
      "fallbackUsed": false
    },
    "trace": [
      { "stepName": "INTENT_RECOGNIZE", "stepStatus": "SUCCESS" },
      { "stepName": "ORDER_CONTEXT", "stepStatus": "SUCCESS" },
      { "stepName": "KNOWLEDGE_RETRIEVAL", "stepStatus": "SUCCESS" },
      { "stepName": "AI_GENERATION", "stepStatus": "SUCCESS" },
      { "stepName": "FINAL_REPLY", "stepStatus": "SUCCESS" }
    ]
  }
}
```

兜底规则：

- AI 调用失败时，接口仍返回 `code=1`。
- `assistantMessage.sourceType` 返回 `FALLBACK`。
- `ai.status` 返回 `FAILED`，`fallbackUsed=true`。
- 后端写入 `ai_call_log` 和 `process_trace`。

## 5. 知识分类资源 `/knowledge-categories`

### 5.1 查询分类列表

```http
GET /knowledge-categories?enabled=1
```

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {
      "id": 1,
      "categoryCode": "RETURN_RULE",
      "categoryName": "退货规则",
      "sortOrder": 10,
      "enabled": 1
    }
  ]
}
```

### 5.2 新增分类

```http
POST /knowledge-categories
```

请求体：

```json
{
  "categoryCode": "RETURN_RULE",
  "categoryName": "退货规则",
  "sortOrder": 10,
  "enabled": 1
}
```

### 5.3 查询分类详情

```http
GET /knowledge-categories/{id}
```

### 5.4 修改分类

```http
PUT /knowledge-categories/{id}
```

### 5.5 删除分类

```http
DELETE /knowledge-categories/{id}
```

说明：如果该分类下存在知识文档，后端应返回 `Result.error("分类下存在知识文档，不能删除")`。

## 6. 知识文档资源 `/knowledge-docs`

### 6.1 分页查询知识文档

```http
GET /knowledge-docs?page=1&pageSize=10&categoryId=1&status=ENABLED&keyword=退货
```

参数说明：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| page | 否 | 当前页 |
| pageSize | 否 | 每页条数 |
| categoryId | 否 | 分类 ID |
| docType | 否 | `FAQ`、`POLICY`、`SCRIPT`、`NOTICE` |
| intentCode | 否 | 适用意图 |
| status | 否 | `ENABLED`、`DISABLED` |
| keyword | 否 | 标题、问题、正文关键词 |

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 1,
    "rows": [
      {
        "id": 6,
        "categoryId": 1,
        "categoryName": "退货规则",
        "title": "七天无理由退货规则",
        "docType": "POLICY",
        "intentCode": "RETURN_APPLY",
        "scenario": "已签收订单退货",
        "status": "ENABLED",
        "priority": 10,
        "updatedAt": "2026-04-29 08:40:00"
      }
    ]
  }
}
```

### 6.2 新增知识文档

```http
POST /knowledge-docs
```

请求体：

```json
{
  "categoryId": 1,
  "title": "七天无理由退货规则",
  "docType": "POLICY",
  "intentCode": "RETURN_APPLY",
  "scenario": "已签收订单退货",
  "question": "已签收订单还能退货吗？",
  "answer": "签收 7 天内且商品不影响二次销售，一般可以申请退货。",
  "content": "用户签收商品后 7 天内，如商品完好且不影响二次销售，可申请七天无理由退货。",
  "keywords": "退货,七天无理由,签收",
  "priority": 10,
  "status": "ENABLED"
}
```

### 6.3 查询知识文档详情

```http
GET /knowledge-docs/{id}
```

### 6.4 修改知识文档

```http
PUT /knowledge-docs/{id}
```

请求体字段同新增接口。后端更新成功后建议 `versionNo + 1`。

### 6.5 删除知识文档

```http
DELETE /knowledge-docs/{id}
```

说明：使用软删除，将 `deleted=1`，避免影响历史检索日志中的依据快照。

### 6.6 检索知识文档

```http
GET /knowledge-docs/search?query=退货多久到账&intentCode=RETURN_APPLY&limit=5
```

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {
      "id": 8,
      "title": "退款到账时间说明",
      "categoryName": "退款规则",
      "score": 0.8200,
      "hitReason": "命中关键词：退款、到账",
      "contentPreview": "退款审核通过后，通常按原支付渠道退回..."
    }
  ]
}
```

## 7. 订单资源 `/orders`

### 7.1 分页查询订单

```http
GET /orders?page=1&pageSize=10&keyword=耳机&orderStatus=SIGNED
```

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 1,
    "rows": [
      {
        "id": 1,
        "orderNo": "DD202604290001",
        "productName": "无线蓝牙耳机",
        "orderAmount": 199.00,
        "orderStatus": "SIGNED",
        "logisticsStatus": "DELIVERED",
        "afterSaleStatus": "NONE"
      }
    ]
  }
}
```

### 7.2 新增演示订单

```http
POST /orders
```

请求体：

```json
{
  "orderNo": "DD202604290001",
  "userId": 1,
  "productName": "无线蓝牙耳机",
  "skuName": "白色 标准版",
  "orderAmount": 199.00,
  "payStatus": "PAID",
  "orderStatus": "SIGNED",
  "logisticsStatus": "DELIVERED",
  "afterSaleStatus": "NONE",
  "paidAt": "2026-04-25 10:00:00",
  "shippedAt": "2026-04-25 18:00:00",
  "signedAt": "2026-04-26 14:30:00"
}
```

### 7.3 查询订单详情

```http
GET /orders/{id}
```

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "id": 1,
    "orderNo": "DD202604290001",
    "productName": "无线蓝牙耳机",
    "skuName": "白色 标准版",
    "orderAmount": 199.00,
    "payStatus": "PAID",
    "orderStatus": "SIGNED",
    "logisticsStatus": "DELIVERED",
    "afterSaleStatus": "NONE",
    "signedAt": "2026-04-26 14:30:00",
    "afterSales": []
  }
}
```

### 7.4 按订单号查询订单

```http
GET /orders/no/{orderNo}
```

说明：RESTful 标准详情接口使用 ID，但聊天场景经常只知道订单号，所以保留该查询接口。

### 7.5 修改订单

```http
PUT /orders/{id}
```

### 7.6 删除订单

```http
DELETE /orders/{id}
```

说明：仅用于演示数据维护。真实业务中订单通常不物理删除。

## 8. 售后记录资源 `/after-sale-records`

### 8.1 分页查询售后记录

```http
GET /after-sale-records?page=1&pageSize=10&orderId=1&status=APPLIED
```

### 8.2 新增售后记录

```http
POST /after-sale-records
```

请求体：

```json
{
  "orderId": 1,
  "serviceType": "RETURN",
  "reason": "不喜欢，想退货",
  "refundAmount": 199.00,
  "remark": "演示数据"
}
```

处理规则：

- 后端生成 `afterSaleNo`。
- 初始 `status=APPLIED`。
- 同步更新订单 `afterSaleStatus`。

### 8.3 查询售后记录详情

```http
GET /after-sale-records/{id}
```

### 8.4 修改售后记录

```http
PUT /after-sale-records/{id}
```

### 8.5 删除售后记录

```http
DELETE /after-sale-records/{id}
```

### 8.6 查询某订单的售后记录

```http
GET /orders/{id}/after-sale-records
```

说明：这是订单资源下的子资源接口，前端订单详情页可以直接调用。

## 9. 处理轨迹与日志资源

### 9.1 查询会话处理轨迹

```http
GET /chat-sessions/{id}/process-traces
```

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": [
    {
      "messageId": 101,
      "stepName": "INTENT_RECOGNIZE",
      "stepStatus": "SUCCESS",
      "detail": {
        "intentCode": "RETURN_APPLY",
        "confidence": 0.92
      },
      "createdAt": "2026-04-29 08:41:00"
    }
  ]
}
```

### 9.2 查询 AI 调用日志

```http
GET /ai-call-logs?page=1&pageSize=10&status=FAILED
```

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 1,
    "rows": [
      {
        "id": 1,
        "sessionId": 12,
        "messageId": 101,
        "provider": "openai-compatible",
        "modelName": "gpt-4.1-mini",
        "status": "FAILED",
        "latencyMs": 3000,
        "errorMessage": "AI service timeout",
        "createdAt": "2026-04-29 08:41:03"
      }
    ]
  }
}
```

### 9.3 查询知识检索日志

```http
GET /retrieval-logs?page=1&pageSize=10&keyword=退货
```

### 9.4 查询操作日志

```http
GET /operate-logs?page=1&pageSize=10
```

说明：如果沿用 `tilians` 项目中的 `@OperatorAnno` 和 AOP 操作日志，可以把新增、修改、删除接口的操作记录到操作日志表中。

## 10. AI 测试资源

### 10.1 测试 AI 可用性

```http
POST /ai-tests
```

请求体：

```json
{
  "prompt": "请用一句话说明退货申请流程。"
}
```

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "status": "SUCCESS",
    "provider": "openai-compatible",
    "modelName": "gpt-4.1-mini",
    "reply": "用户可在订单详情页提交退货申请，等待商家审核后按提示寄回商品。",
    "latencyMs": 950
  }
}
```

说明：该接口只用于配置测试。正式聊天流程统一通过 `POST /chat-sessions/{id}/messages` 触发 AI 增强。

### 10.2 查询和切换当前模型

查询可选模型：

```http
GET /system/ai-models
```

返回示例：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "status": "UP",
    "provider": "openai-compatible",
    "modelName": "gpt-4o-mini",
    "selectedModelName": "gpt-4o-mini",
    "modelOptions": ["gpt-4o-mini", "gpt-4.1-mini", "gpt-4.1", "o4-mini"],
    "enabled": true,
    "apiKeyConfigured": true,
    "baseUrlConfigured": true,
    "fallbackEnabled": true
  }
}
```

切换当前模型：

```http
PUT /system/ai-models/current
```

请求体：

```json
{
  "modelName": "gpt-4.1-mini"
}
```

说明：模型切换在后端运行时生效，`AiService` 会清理已缓存的 LangChain4j 客户端，下一次 AI 测试或聊天回复会使用新的模型名。前端下拉框允许输入兼容网关支持的自定义模型名。

## 11. 推荐开发顺序

第一批先实现能跑通主流程的接口：

1. `GET /system/status`
2. `POST /chat-sessions`
3. `GET /chat-sessions/{id}`
4. `POST /chat-sessions/{id}/messages`
5. `GET /knowledge-docs`
6. `GET /orders/no/{orderNo}`

第二批实现后台维护：

1. `GET /knowledge-categories`
2. `POST /knowledge-categories`
3. `POST /knowledge-docs`
4. `PUT /knowledge-docs/{id}`
5. `DELETE /knowledge-docs/{id}`
6. `GET /knowledge-docs/search`

第三批实现日志和演示增强：

1. `GET /chat-sessions/{id}/process-traces`
2. `GET /ai-call-logs`
3. `GET /retrieval-logs`
4. `POST /ai-tests`
5. `GET /orders`
6. `POST /after-sale-records`

## 12. Controller 写法示例

### 12.1 知识文档 Controller 示例

```java
@RestController
@RequestMapping("/knowledge-docs")
public class KnowledgeDocController {

    @Autowired
    private KnowledgeDocService knowledgeDocService;

    @GetMapping
    public Result page(KnowledgeDocSearch search) {
        PageResult<KnowledgeDoc> pageResult = knowledgeDocService.page(search);
        return Result.success(pageResult);
    }

    @PostMapping
    @OperatorAnno
    public Result save(@RequestBody KnowledgeDoc knowledgeDoc) {
        knowledgeDocService.save(knowledgeDoc);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result getById(@PathVariable Long id) {
        KnowledgeDoc knowledgeDoc = knowledgeDocService.getById(id);
        return Result.success(knowledgeDoc);
    }

    @PutMapping("/{id}")
    @OperatorAnno
    public Result update(@PathVariable Long id, @RequestBody KnowledgeDoc knowledgeDoc) {
        knowledgeDoc.setId(id);
        knowledgeDocService.update(knowledgeDoc);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @OperatorAnno
    public Result delete(@PathVariable Long id) {
        knowledgeDocService.delete(id);
        return Result.success();
    }
}
```

### 12.2 聊天消息 Controller 示例

```java
@RestController
@RequestMapping("/chat-sessions")
public class ChatSessionController {

    @Autowired
    private ChatService chatService;

    @PostMapping("/{id}/messages")
    public Result sendMessage(@PathVariable Long id, @RequestBody ChatMessageRequest request) {
        ChatReplyVO replyVO = chatService.sendMessage(id, request);
        return Result.success(replyVO);
    }
}
```

## 13. 和数据库表的对应关系

| 接口资源 | 主要数据表 |
| --- | --- |
| `/chat-sessions` | `chat_session`、`chat_message` |
| `/knowledge-categories` | `knowledge_category` |
| `/knowledge-docs` | `knowledge_doc`、`retrieval_log` |
| `/orders` | `demo_order` |
| `/after-sale-records` | `after_sale_record` |
| `/ai-call-logs` | `ai_call_log` |
| `/retrieval-logs` | `retrieval_log` |
| `/process-traces` | `process_trace` |

## 14. LangChain4j 真实模型接口约定

真实模型接入方案见 `docs/langchain4j-real-model-integration.md`。接口层保持 RESTful 路径不变，主要调整 AI 状态字段和日志语义。

### 14.1 `GET /system/status`

接入真实模型后，`data.ai` 应返回：

```json
{
  "status": "UP",
  "provider": "openai-compatible",
  "modelName": "gpt-4o-mini",
  "baseUrlConfigured": true,
  "fallbackEnabled": true
}
```

未配置或停用 AI 时，`status` 返回 `SKIPPED`，但 `fallbackEnabled` 仍为 `true`。

### 14.2 `POST /ai-tests`

该接口用于验证 LangChain4j 和真实模型配置是否可用。真实模型成功时：

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
    "reply": "模型返回内容",
    "latencyMs": 950,
    "errorMessage": null
  }
}
```

模型失败时该接口仍返回 `code=1`，但 `status=FAILED`，并在 `errorMessage` 中返回异常摘要。

### 14.3 `POST /chat-sessions/{id}/messages`

聊天主接口请求体不变。接入真实模型后：

- 模型成功：`assistantMessage.sourceType=AI_ENHANCED`，`ai.status=SUCCESS`。
- 模型失败：`assistantMessage.sourceType=FALLBACK`，`ai.status=FAILED`，接口仍返回 `code=1`。
- 每次调用都需要写入 `ai_call_log`，用于前端日志页和答辩展示。

### 14.4 `GET /ai-call-logs`

真实模型接入后，该接口用于展示模型调用审计信息，前端日志页至少展示：

| 字段 | 说明 |
| --- | --- |
| `provider` | 模型服务提供方，统一使用 `openai-compatible` 或 `local-fallback` |
| `modelName` | 实际模型名，例如 `gpt-4o-mini` 或第三方兼容模型名 |
| `status` | `SUCCESS`、`FAILED`、`SKIPPED` |
| `latencyMs` | 模型调用耗时，失败时也要记录 |
| `requestSummary` | 提示词摘要，不能包含 API Key、Authorization Header 等敏感信息 |
| `responseSummary` | 模型回复摘要，便于答辩展示 |
| `errorMessage` | 失败摘要，避免直接暴露完整密钥、完整请求头或过长堆栈 |

### 14.5 前端对接口径

前端不直接关心 LangChain4j 的 Java 类名，只根据接口字段判断状态：

- `ai.status=UP`：真实模型可用。
- `ai.status=SKIPPED`：未启用真实模型，但本地兜底可用。
- `ai.status=FAILED`：本次模型调用失败，聊天主链路应展示兜底回复。
- `assistantMessage.sourceType=AI_ENHANCED`：回复来自真实模型增强。
- `assistantMessage.sourceType=FALLBACK`：回复来自本地规则兜底。

因此真实模型接入不会改变已有 RESTful 路径，只扩展 AI 状态字段和日志内容。
