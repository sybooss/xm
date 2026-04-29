# 后端项目说明书

## 1. 项目概述

本后端项目是“电商退换货智能客服系统”的 Spring Boot 服务端，工程目录为：

```text
D:\复制软件系统\server
```

系统面向课程实践和答辩演示，核心目标是把退货申请、换货申请、退款进度、物流异常、规则咨询、投诉与人工转接等售后场景整理成可运行、可维护、可展示的业务系统。后端负责 RESTful 接口、数据库访问、会话管理、订单上下文、知识库检索、处理轨迹、AI 调用日志和 LangChain4j 真实模型接入。

当前代码已经实现本地规则兜底版 AI 流程。真实模型接入阶段在此基础上增加 LangChain4j，不改变已有业务接口路径。

## 2. 技术栈

| 层次 | 技术 | 说明 |
| --- | --- | --- |
| Web 框架 | Spring Boot | 当前工程为 `3.3.13` |
| 运行环境 | Java 17 | 满足 Spring Boot 3 和 LangChain4j 当前集成要求 |
| 数据访问 | MyBatis | Mapper 注解方式，符合参考项目 `tilians` 的简洁写法 |
| 数据库 | MySQL 8 | 数据库名 `test3` |
| 分页 | PageHelper | 统一返回 `PageResult<T>` |
| AI 增强 | LangChain4j OpenAI 模块 | 作为 AI 调用和提示词编排层，不替代业务规则 |
| 返回结构 | Result | `code=1` 成功，`code=0` 失败 |

## 3. 工程结构

```text
server/
  pom.xml
  src/main/resources/application.yml
  src/main/java/com/user/returnsassistant/
    ReturnsAssistantApplication.java
    anno/
    controller/
    exception/
    mapper/
    pojo/
    service/
      impl/
    utils/
```

主要包职责如下：

| 包名 | 职责 |
| --- | --- |
| `controller` | 暴露 RESTful 接口，负责参数接收和统一返回 |
| `service` | 定义业务接口 |
| `service.impl` | 处理业务流程、知识检索、会话编排、AI 兜底 |
| `mapper` | MyBatis 数据访问层 |
| `pojo` | 实体、请求对象、分页对象、返回对象 |
| `exception` | 全局异常和业务异常处理 |
| `anno` | 操作日志注解 |
| `utils` | 编号生成等工具 |

## 4. 数据库配置

当前连接本机 MySQL：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/test3?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: 1234
```

建表和初始化脚本：

```text
D:\复制软件系统\sql\schema.sql
D:\复制软件系统\sql\seed.sql
```

主要数据表：

| 表名 | 用途 |
| --- | --- |
| `user_account` | 演示用户 |
| `chat_session` | 客服会话 |
| `chat_message` | 用户和助手消息 |
| `knowledge_category` | 知识分类 |
| `knowledge_doc` | 规则、FAQ、政策文档 |
| `demo_order` | 演示订单 |
| `after_sale_record` | 退换货、退款等售后记录 |
| `intent_record` | 意图识别结果 |
| `retrieval_log` | 知识库检索日志 |
| `ai_call_log` | AI 调用日志 |
| `process_trace` | 聊天处理轨迹 |

## 5. 后端模块

### 5.1 系统模块

提供系统状态和枚举字典：

```http
GET /system/status
GET /system/enums
```

状态接口用于展示后端、数据库和 AI 层是否可用。接入真实模型后，`data.ai` 会显示模型提供方、模型名、base URL 是否配置和兜底开关。

### 5.2 会话模块

提供会话创建、分页查询、详情查询、消息发送和处理轨迹查询：

```http
GET /chat-sessions
POST /chat-sessions
GET /chat-sessions/{id}
PUT /chat-sessions/{id}
DELETE /chat-sessions/{id}
GET /chat-sessions/{id}/messages
POST /chat-sessions/{id}/messages
GET /chat-sessions/{id}/process-traces
```

聊天主链路由 `POST /chat-sessions/{id}/messages` 触发，内部流程是：

```text
保存用户消息
-> 识别售后意图
-> 查询订单上下文
-> 检索知识库
-> 生成本地规则判断
-> 调用 LangChain4j 真实模型增强回复
-> 失败时回退本地规则
-> 保存助手消息、AI 日志、检索日志、处理轨迹
```

### 5.3 知识库模块

管理售后规则、FAQ、政策说明和异常处理材料：

```http
GET /knowledge-categories
POST /knowledge-categories
GET /knowledge-categories/{id}
PUT /knowledge-categories/{id}
DELETE /knowledge-categories/{id}

GET /knowledge-docs
POST /knowledge-docs
GET /knowledge-docs/{id}
PUT /knowledge-docs/{id}
DELETE /knowledge-docs/{id}
GET /knowledge-docs/search?keyword=退货
```

知识库既服务前端管理页面，也为 AI 增强回复提供可追溯依据。

### 5.4 订单与售后模块

提供演示订单、订单号查询和售后记录管理：

```http
GET /orders
POST /orders
GET /orders/{id}
GET /orders/no/{orderNo}
PUT /orders/{id}
DELETE /orders/{id}

GET /after-sale-records
POST /after-sale-records
GET /after-sale-records/{id}
PUT /after-sale-records/{id}
DELETE /after-sale-records/{id}
GET /orders/{id}/after-sale-records
```

AI 回复必须基于订单状态和售后记录，不能脱离业务数据直接编造结论。

### 5.5 日志模块

提供 AI 调用日志、知识检索日志和操作日志：

```http
GET /ai-call-logs
GET /retrieval-logs
GET /operation-logs
```

这些日志用于调试和答辩展示，重点说明系统具备可追溯性：用户问了什么、识别成什么意图、命中了哪些知识、模型是否调用成功、失败后是否兜底。

## 6. 统一接口规范

后端沿用参考项目 `D:\DEVELOP\web-ai\webproject2\tilians` 的风格，返回结构统一为：

```json
{
  "code": 1,
  "msg": "success",
  "data": {}
}
```

分页结构统一为：

```json
{
  "code": 1,
  "msg": "success",
  "data": {
    "total": 100,
    "rows": []
  }
}
```

路径设计采用 RESTful 风格：

- 资源名使用复数：`/chat-sessions`、`/knowledge-docs`、`/orders`。
- 子资源使用层级路径：`/chat-sessions/{id}/messages`。
- 查询使用 `GET`，新增使用 `POST`，修改使用 `PUT`，删除使用 `DELETE`。

完整接口说明见：

```text
D:\复制软件系统\docs\backend-api-design.md
```

## 7. LangChain4j 真实模型接入设计

LangChain4j 在本系统中只负责 AI 增强层：

- 组合用户问题、订单上下文、知识命中和业务规则判断。
- 调用真实模型生成更自然的客服回复。
- 返回模型状态、耗时和错误摘要。
- 调用失败时交给本地规则兜底。

不交给 AI 决定的内容：

- 订单是否存在。
- 是否满足退换货条件。
- 售后记录是否已完成。
- 退款、换货、投诉等业务状态流转。

### 7.1 依赖和版本

当前工程接入的是 LangChain4j OpenAI 模块：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

这样可以保留当前 Spring Boot `3.3.13`，并在 `AiServiceImpl` 中按需懒加载 `OpenAiChatModel`。如果后续改为官方 Spring Boot starter 方案，再单独升级 Spring Boot 并做接口回归。

### 7.2 配置方式

真实模型配置放在环境变量和 `application.yml` 中，不能把 API Key 写进 Git：

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

本地 PowerShell 示例：

```powershell
$env:OPENAI_API_KEY="你的真实 Key"
$env:OPENAI_BASE_URL="https://api.openai.com/v1"
$env:OPENAI_MODEL="gpt-4o-mini"
$env:AI_ENABLED="true"
```

第三方兼容服务也可以接入，只要提供 OpenAI-compatible 的 `/v1/chat/completions` 兼容接口，并给出正确的 `base-url` 和 `model-name`。

### 7.3 代码接入点

真实模型接入主要改动：

| 文件 | 改动 |
| --- | --- |
| `server/pom.xml` | 增加 `langchain4j-open-ai` 依赖 |
| `server/src/main/resources/application.yml` | 增加 `app.ai` 和 `langchain4j.open-ai.chat-model` 配置 |
| `AiServiceImpl` | 懒加载 `OpenAiChatModel`，调用真实模型，捕获异常 |
| `ChatServiceImpl` | 装配提示词，记录 `AI_ENHANCED` 或 `FALLBACK` |
| `SystemController` | 返回 AI 配置和可用状态 |
| `AiTestController` | 提供模型可用性测试 |

### 7.4 状态语义

| 状态 | 说明 | 前端展示 |
| --- | --- | --- |
| `SUCCESS` | 真实模型调用成功 | 显示 AI 增强回复 |
| `FAILED` | 真实模型调用失败 | 显示本地兜底回复和失败摘要 |
| `SKIPPED` | 未启用或未配置真实模型 | 显示本地规则模式 |

`POST /chat-sessions/{id}/messages` 即使模型失败也应返回 `code=1`，因为业务链路已经通过本地兜底生成回复。只有参数错误、会话不存在、数据库异常等业务或系统错误才返回失败。

详细接入说明见：

```text
D:\复制软件系统\docs\langchain4j-real-model-integration.md
```

## 8. 启动与验证

启动后端：

```powershell
cd D:\复制软件系统\server
mvn spring-boot:run
```

默认端口：

```text
http://localhost:8081
```

基础验证：

```http
GET http://localhost:8081/system/status
GET http://localhost:8081/orders/no/DD202604290001
GET http://localhost:8081/knowledge-docs?page=1&pageSize=5
POST http://localhost:8081/chat-sessions
POST http://localhost:8081/chat-sessions/{id}/messages
```

真实模型接入后的验证：

```http
POST http://localhost:8081/ai-tests
GET http://localhost:8081/ai-call-logs?page=1&pageSize=10
```

验收重点：

- 系统状态接口能显示数据库和 AI 层状态。
- 聊天接口能返回意图、知识命中、订单上下文和助手回复。
- 真实模型可用时 `assistantMessage.sourceType=AI_ENHANCED`。
- 真实模型失败时 `assistantMessage.sourceType=FALLBACK`，接口仍能正常返回。
- `ai_call_log`、`retrieval_log`、`process_trace` 中能看到完整处理记录。

## 9. 开发约定

- Controller 只做参数接收、调用 Service、返回 `Result`，不写复杂业务逻辑。
- Service 负责业务编排和事务边界。
- Mapper 保持 SQL 简洁，复杂查询优先拆成可读的小接口。
- AI 调用必须经过 `AiService` 封装，不能在 Controller 中直接调用模型。
- API Key、Authorization Header、完整请求头不能写入日志表。
- 任何 AI 失败都不能阻断聊天主链路，必须保留本地规则兜底。
- 后续 Vue 前端只依赖 RESTful 接口字段，不依赖后端内部类名。
