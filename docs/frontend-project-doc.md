# 前端项目说明书

## 1. 项目概述

本前端项目是“电商退换货智能客服系统”的 Vue 3 客户端，建议工程目录为：

```text
D:\复制软件系统\web
```

前端用于展示客服工作台、知识库管理、订单与售后记录、AI 调试和系统日志。它不直接访问数据库，也不直接调用大模型；所有业务数据和 AI 结果都通过 Spring Boot 后端 RESTful 接口获取。

当前后端服务地址：

```text
http://localhost:8081
```

后端已经接入本机 `D:\sub2api` OpenAI-compatible 服务，前端只需要展示接口返回的 AI 状态、回复来源和处理轨迹，不需要感知 LangChain4j 或 sub2api 的内部实现。

## 2. 技术选型

| 类别 | 技术 | 用途 |
| --- | --- | --- |
| 前端框架 | Vue 3 | 页面和组件开发 |
| 构建工具 | Vite | 本地开发、打包构建 |
| 路由 | Vue Router | 页面导航 |
| 状态管理 | Pinia | 会话、系统状态、枚举缓存 |
| HTTP 请求 | Axios | 调用后端 RESTful 接口 |
| UI 组件 | Element Plus | 表格、表单、抽屉、弹窗、标签、分页 |
| 图标 | Element Plus Icons 或 lucide-vue-next | 工作台操作按钮和状态图标 |
| 样式 | CSS variables + scoped CSS | 保持页面统一、便于后续调整 |

推荐保持“管理系统 + 客服工作台”的产品形态，页面应安静、清晰、信息密度适中，避免做成营销落地页。

## 3. 工程结构

建议目录结构：

```text
web/
  package.json
  vite.config.js
  index.html
  .env.development
  src/
    main.js
    App.vue
    router/
      index.js
    stores/
      systemStore.js
      chatStore.js
    api/
      request.js
      systemApi.js
      chatApi.js
      knowledgeApi.js
      orderApi.js
      afterSaleApi.js
      logApi.js
      aiApi.js
    views/
      DashboardView.vue
      ChatWorkbenchView.vue
      KnowledgeCategoryView.vue
      KnowledgeDocView.vue
      OrderView.vue
      AfterSaleRecordView.vue
      LogCenterView.vue
      AiTestView.vue
    components/
      layout/
        AppLayout.vue
        AppSidebar.vue
        AppHeader.vue
      chat/
        SessionList.vue
        MessagePanel.vue
        ReplyInsightPanel.vue
        OrderContextPanel.vue
        TraceTimeline.vue
      knowledge/
        KnowledgeDocForm.vue
        KnowledgeSearchPanel.vue
      common/
        StatusTag.vue
        PageToolbar.vue
        EmptyState.vue
    styles/
      variables.css
      global.css
```

如果课程时间有限，可以先实现 `DashboardView`、`ChatWorkbenchView`、`KnowledgeDocView`、`OrderView`、`LogCenterView`、`AiTestView` 六个页面，分类和售后记录页面可作为增强功能。

## 4. 环境配置

`.env.development`：

```env
VITE_APP_TITLE=电商退换货智能客服系统
VITE_API_BASE_URL=/api
```

开发期通过 Vite 代理转发到后端，避免浏览器跨域问题：

```js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8081',
      changeOrigin: true,
      rewrite: path => path.replace(/^\/api/, '')
    }
  }
}
```

`src/api/request.js` 建议统一处理基础地址、响应结构和错误提示：

```js
import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000
})

request.interceptors.response.use(
  response => {
    const result = response.data
    if (result.code !== 1) {
      ElMessage.error(result.msg || '请求失败')
      return Promise.reject(result)
    }
    return result.data
  },
  error => {
    ElMessage.error(error.message || '网络异常')
    return Promise.reject(error)
  }
)

export default request
```

后端统一返回结构：

```json
{
  "code": 1,
  "msg": "success",
  "data": {}
}
```

分页结构：

```json
{
  "total": 100,
  "rows": []
}
```

## 5. 路由设计

| 路由 | 页面 | 功能 |
| --- | --- | --- |
| `/` | `DashboardView` | 系统总览、数据库状态、AI 状态、快速入口 |
| `/chat` | `ChatWorkbenchView` | 咨询工作台，核心演示页面 |
| `/knowledge/categories` | `KnowledgeCategoryView` | 知识分类管理 |
| `/knowledge/docs` | `KnowledgeDocView` | 知识文档管理和检索 |
| `/orders` | `OrderView` | 演示订单查询和维护 |
| `/after-sales` | `AfterSaleRecordView` | 售后记录管理 |
| `/logs` | `LogCenterView` | AI 调用日志、知识检索日志、处理轨迹 |
| `/ai-test` | `AiTestView` | 真实模型连通性测试 |

默认首页进入 `/chat` 或 `/` 均可。课程展示时建议默认进入 `/chat`，更快展示核心智能客服能力。

## 6. 页面设计

### 6.1 系统总览页

对应接口：

```http
GET /system/status
GET /system/enums
```

页面内容：

- 后端服务状态。
- 数据库状态和数据库名。
- AI 状态：`UP`、`SKIPPED`、`FAILED`。
- 模型提供方、模型名、API Key 是否配置、兜底是否开启。
- 顶部状态栏提供模型下拉选择，调用 `GET /system/ai-models` 和 `PUT /system/ai-models/current` 完成运行时切换。
- 快速入口：进入客服工作台、知识库、AI 测试、日志中心。

展示建议：

- `ai.status=UP` 显示绿色标签“真实模型可用”。
- `ai.status=SKIPPED` 显示灰色标签“本地规则模式”。
- `fallbackEnabled=true` 显示“兜底已启用”，便于答辩说明系统稳定性。

### 6.2 咨询工作台

对应接口：

```http
GET /chat-sessions?page=1&pageSize=10&status=ACTIVE&keyword=退货
POST /chat-sessions
GET /chat-sessions/{id}
GET /chat-sessions/{id}/messages
POST /chat-sessions/{id}/messages
GET /chat-sessions/{id}/process-traces
GET /orders/no/{orderNo}
```

推荐布局：

```text
左侧：会话列表 + 新建会话
中间：聊天消息 + 输入框 + 预置问题按钮
右侧：订单上下文 + 意图标签 + 知识命中 + 处理轨迹
```

核心交互：

1. 点击“新建会话”，可选择或输入订单号。
2. 用户输入问题，例如“这个订单能不能退货？”。
3. 前端调用 `POST /chat-sessions/{id}/messages`。
4. 中间消息区先立即展示用户消息和“思考中”动画，等待接口返回后替换为正式助手回复。
5. 右侧展示 `intent`、`orderContext`、`knowledgeHits`、`ai`、`trace`。
6. 如果 `assistantMessage.sourceType=AI_ENHANCED`，展示“AI 增强回复”标签。
7. 如果 `assistantMessage.sourceType=FALLBACK`，展示“本地兜底回复”标签。

消息发送请求：

```json
{
  "content": "这个订单能不能退货？",
  "orderNo": "DD202604290001",
  "useAi": true
}
```

重点响应字段：

| 字段 | 前端用途 |
| --- | --- |
| `assistantMessage.content` | 助手回复正文 |
| `assistantMessage.sourceType` | 判断 AI 增强或本地兜底 |
| `intent.intentCode` | 展示意图标签 |
| `orderContext` | 展示订单状态、物流状态、售后状态 |
| `knowledgeHits` | 展示命中依据 |
| `ai.status` | 展示模型调用状态 |
| `ai.latencyMs` | 展示模型耗时 |
| `trace` | 展示处理步骤时间线 |
| `suggestedQuestions` | 展示追问按钮 |

预置演示问题：

- 这个订单能不能退货？
- 退货后多久能退款？
- 物流一直不更新怎么办？
- 商品质量有问题可以换货吗？
- 商家一直不处理可以投诉吗？

### 6.3 知识文档管理页

对应接口：

```http
GET /knowledge-categories?enabled=1
GET /knowledge-docs?page=1&pageSize=10&categoryId=1&status=ENABLED&keyword=退货
POST /knowledge-docs
GET /knowledge-docs/{id}
PUT /knowledge-docs/{id}
DELETE /knowledge-docs/{id}
GET /knowledge-docs/search?query=退货多久到账&intentCode=RETURN_APPLY&limit=5
```

页面功能：

- 按分类、状态、意图、关键词筛选知识文档。
- 表格展示标题、分类、文档类型、适用意图、状态、优先级、更新时间。
- 抽屉或弹窗编辑文档。
- 提供“检索调试”区域，输入用户问题后展示命中文档。

表单字段：

| 字段 | 说明 |
| --- | --- |
| `categoryId` | 知识分类 |
| `title` | 文档标题 |
| `docType` | FAQ、POLICY、SCRIPT、NOTICE |
| `intentCode` | 适用意图 |
| `scenario` | 使用场景 |
| `question` | 常见问法 |
| `answer` | 标准答复 |
| `content` | 详细规则内容 |
| `keywords` | 检索关键词 |
| `priority` | 优先级 |
| `status` | ENABLED、DISABLED |

### 6.4 知识分类管理页

对应接口：

```http
GET /knowledge-categories?enabled=1
POST /knowledge-categories
GET /knowledge-categories/{id}
PUT /knowledge-categories/{id}
DELETE /knowledge-categories/{id}
```

页面功能：

- 表格展示分类名称、编码、排序、启用状态。
- 支持新增、编辑、删除。
- 删除前需要确认，避免误删影响知识文档。

### 6.5 订单管理页

对应接口：

```http
GET /orders?page=1&pageSize=10&keyword=耳机&orderStatus=SIGNED
GET /orders/{id}
GET /orders/no/{orderNo}
POST /orders
PUT /orders/{id}
DELETE /orders/{id}
GET /orders/{id}/after-sale-records
```

页面功能：

- 查询演示订单。
- 按订单号、商品名、订单状态筛选。
- 展示订单金额、订单状态、物流状态、售后状态、签收时间。
- 进入详情后展示售后记录。

订单状态建议用标签展示：

| 状态 | 展示文案 |
| --- | --- |
| `PAID` | 已支付 |
| `SHIPPED` | 已发货 |
| `SIGNED` | 已签收 |
| `AFTER_SALE` | 售后中 |
| `CLOSED` | 已关闭 |

### 6.6 售后记录页

对应接口：

```http
GET /after-sale-records?page=1&pageSize=10&orderId=1&status=APPLIED
POST /after-sale-records
GET /after-sale-records/{id}
PUT /after-sale-records/{id}
DELETE /after-sale-records/{id}
```

页面功能：

- 管理退货、换货、退款、投诉等售后记录。
- 展示售后类型、原因、状态、申请时间、完成时间。
- 用于配合客服工作台解释订单售后上下文。

### 6.7 日志中心

对应接口：

```http
GET /ai-call-logs?page=1&pageSize=10&status=FAILED
GET /retrieval-logs?page=1&pageSize=10&keyword=退货
GET /chat-sessions/{id}/process-traces
```

页面建议分为三个标签页：

- AI 调用日志：展示提供方、模型名、状态、耗时、错误摘要、请求摘要、响应摘要。
- 知识检索日志：展示查询词、命中文档、命中原因、分数、快照。
- 处理轨迹：输入会话 ID 后展示完整处理步骤。

AI 日志状态展示：

| 状态 | 含义 |
| --- | --- |
| `SUCCESS` | 真实模型调用成功 |
| `FAILED` | 真实模型调用失败，已走兜底 |
| `SKIPPED` | 未启用真实模型或本轮关闭 AI |

### 6.8 AI 测试页

对应接口：

```http
POST /ai-tests
```

请求：

```json
{
  "prompt": "请用一句话说明退货申请流程。"
}
```

页面功能：

- 输入测试提示词。
- 展示模型是否使用、调用状态、模型名、耗时、错误信息和回复内容。
- 用于证明后端已经通过 LangChain4j 和 sub2api 调用真实模型。

成功时重点展示：

```text
used=true
status=SUCCESS
fallbackUsed=false
provider=openai-compatible
modelName=gpt-4o-mini
```

## 7. API 封装规划

### 7.1 系统 API

```js
export const getSystemStatus = () => request.get('/system/status')
export const getEnums = () => request.get('/system/enums')
```

### 7.2 会话 API

```js
export const pageSessions = params => request.get('/chat-sessions', { params })
export const createSession = data => request.post('/chat-sessions', data)
export const getSession = id => request.get(`/chat-sessions/${id}`)
export const updateSession = (id, data) => request.put(`/chat-sessions/${id}`, data)
export const deleteSession = id => request.delete(`/chat-sessions/${id}`)
export const listMessages = id => request.get(`/chat-sessions/${id}/messages`)
export const sendMessage = (id, data) => request.post(`/chat-sessions/${id}/messages`, data)
export const listTraces = id => request.get(`/chat-sessions/${id}/process-traces`)
```

### 7.3 知识库 API

```js
export const listCategories = params => request.get('/knowledge-categories', { params })
export const createCategory = data => request.post('/knowledge-categories', data)
export const updateCategory = (id, data) => request.put(`/knowledge-categories/${id}`, data)
export const deleteCategory = id => request.delete(`/knowledge-categories/${id}`)

export const pageKnowledgeDocs = params => request.get('/knowledge-docs', { params })
export const createKnowledgeDoc = data => request.post('/knowledge-docs', data)
export const getKnowledgeDoc = id => request.get(`/knowledge-docs/${id}`)
export const updateKnowledgeDoc = (id, data) => request.put(`/knowledge-docs/${id}`, data)
export const deleteKnowledgeDoc = id => request.delete(`/knowledge-docs/${id}`)
export const searchKnowledgeDocs = params => request.get('/knowledge-docs/search', { params })
```

### 7.4 订单与售后 API

```js
export const pageOrders = params => request.get('/orders', { params })
export const getOrder = id => request.get(`/orders/${id}`)
export const getOrderByNo = orderNo => request.get(`/orders/no/${orderNo}`)
export const createOrder = data => request.post('/orders', data)
export const updateOrder = (id, data) => request.put(`/orders/${id}`, data)
export const deleteOrder = id => request.delete(`/orders/${id}`)
export const listOrderAfterSales = id => request.get(`/orders/${id}/after-sale-records`)

export const pageAfterSaleRecords = params => request.get('/after-sale-records', { params })
export const createAfterSaleRecord = data => request.post('/after-sale-records', data)
export const updateAfterSaleRecord = (id, data) => request.put(`/after-sale-records/${id}`, data)
export const deleteAfterSaleRecord = id => request.delete(`/after-sale-records/${id}`)
```

### 7.5 日志与 AI API

```js
export const pageAiCallLogs = params => request.get('/ai-call-logs', { params })
export const pageRetrievalLogs = params => request.get('/retrieval-logs', { params })
export const testAi = data => request.post('/ai-tests', data)
```

## 8. 状态管理规划

### 8.1 `systemStore`

保存：

- `status`：系统状态。
- `enums`：意图、文档类型等枚举。
- `aiAvailable`：是否真实模型可用。

用途：

- 页面启动时加载一次系统状态。
- 导航栏展示 AI 状态。
- 各页面复用枚举文案。

### 8.2 `chatStore`

保存：

- `sessions`：当前会话列表。
- `currentSession`：当前会话详情。
- `messages`：当前消息列表。
- `orderContext`：当前订单上下文。
- `lastIntent`：最近识别意图。
- `knowledgeHits`：最近命中的知识。
- `trace`：最近处理轨迹。

用途：

- 保持咨询工作台左右面板同步。
- 发送消息后统一刷新右侧洞察面板。

## 9. UI 规范

### 9.1 整体风格

- 使用后台管理系统布局：左侧导航、顶部状态栏、右侧内容区。
- 页面以信息扫描和演示效率为主，不做大面积装饰性视觉。
- 表格、表单、标签、时间线和状态卡片保持统一间距。
- 颜色用于表达状态，不使用过多渐变和装饰图形。

### 9.2 状态颜色

| 状态 | 建议颜色 | 用途 |
| --- | --- | --- |
| 成功、可用 | 绿色 | `UP`、`SUCCESS`、`AI_ENHANCED` |
| 失败、异常 | 红色 | `FAILED`、接口错误 |
| 跳过、未启用 | 灰色 | `SKIPPED`、`FALLBACK` |
| 处理中 | 蓝色 | 加载中、发送中 |
| 警告 | 橙色 | 缺少订单号、知识未命中 |

### 9.3 关键组件

`StatusTag`：

- 输入 `status`、`type`，输出统一状态标签。
- 支持 AI 状态、订单状态、售后状态、消息来源。

`TraceTimeline`：

- 展示 `INTENT_RECOGNIZE`、`ORDER_CONTEXT`、`KNOWLEDGE_RETRIEVAL`、`AI_GENERATION`、`FINAL_REPLY`。
- 成功步骤绿色，跳过步骤灰色，失败步骤红色。

`ReplyInsightPanel`：

- 展示意图、知识命中、AI 状态、建议追问。
- 用于答辩时说明系统不是简单聊天，而是有处理链路。

## 10. 开发顺序

建议按以下顺序开发：

1. 初始化 Vue 3 + Vite 工程，配置 Element Plus、Vue Router、Pinia、Axios。
2. 完成 `request.js` 和基础 API 封装。
3. 完成 `AppLayout`、侧边栏、顶部 AI 状态栏。
4. 完成系统总览页，验证 `/system/status`。
5. 完成 AI 测试页，验证 `/ai-tests`。
6. 完成咨询工作台，打通 `POST /chat-sessions/{id}/messages`。
7. 完成知识文档管理页和检索调试。
8. 完成订单页和售后记录页。
9. 完成日志中心。
10. 做移动端或窄屏适配，整理演示用测试案例。

优先级最高的是咨询工作台，因为它最能体现系统主题和 LangChain4j 接入效果。

补充增强：

- 新增 `/login` 演示登录页，默认账号 `admin / 123456`。
- 前端请求统一注入 `Authorization` token，后台新增、修改、删除操作需要管理员权限。
- 聊天发送优先使用 `POST /chat-sessions/{id}/message-stream`，先展示处理进度，再展示最终回复。
- 咨询工作台继续保留本地临时气泡，用户点击发送后消息立即出现在页面中，不再等模型完整响应后才显示。

## 11. 联调验收

### 11.1 基础联调

后端启动：

```powershell
powershell -ExecutionPolicy Bypass -File D:\复制软件系统\tools\start-server-with-sub2api.ps1
```

前端启动：

```powershell
cd D:\复制软件系统\web
npm install
npm run dev
```

验收接口：

```http
GET http://localhost:8081/system/status
POST http://localhost:8081/ai-tests
POST http://localhost:8081/chat-sessions
POST http://localhost:8081/chat-sessions/{id}/messages
GET http://localhost:8081/ai-call-logs?page=1&pageSize=10
```

### 11.2 页面验收点

- 首页能显示数据库 `UP`、AI `UP`、模型 `gpt-4o-mini`。
- AI 测试页能返回 `SUCCESS`。
- 客服工作台能新建会话并发送问题。
- 发送“这个订单能不能退货？”后能展示 AI 增强回复。
- 右侧能展示意图 `RETURN_APPLY`、订单上下文、知识命中和处理轨迹。
- 日志中心能看到 `ai_call_log.status=SUCCESS` 的记录。
- 断开 sub2api 或关闭 AI 后，聊天接口仍显示本地兜底回复。

## 12. 演示数据

建议前端内置几个快捷案例：

| 案例 | 订单号 | 问题 |
| --- | --- | --- |
| 退货申请 | `DD202604290001` | 这个订单能不能退货？ |
| 退款进度 | `DD202604290001` | 退货后多久能退款？ |
| 换货申请 | `DD202604290002` | 商品有质量问题能不能换货？ |
| 物流异常 | `DD202604290003` | 物流一直不更新怎么办？ |
| 投诉转人工 | `DD202604290004` | 商家一直不处理可以投诉吗？ |

这些案例用于快速演示：订单上下文、意图识别、知识命中、AI 增强和兜底机制。

## 13. 与后端文档关系

前端开发时需要同时参考：

```text
D:\复制软件系统\docs\backend-api-design.md
D:\复制软件系统\docs\backend-project-doc.md
D:\复制软件系统\docs\database-design.md
D:\复制软件系统\docs\langchain4j-real-model-integration.md
```

本说明书只规定前端页面、交互和工程组织；接口字段以 `backend-api-design.md` 为准，数据库字段以 `database-design.md` 为准。
