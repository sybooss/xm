# 人工接管回复工作台功能计划

## 1. 功能定位

人工接管回复工作台是第 6 个功能，目标是把前面五个能力收束成真实客服协作闭环：AI 负责识别、诊断、生成草稿和提示风险，人工客服在工单里查看上下文并发送最终回复。最终回复必须写入原聊天会话，用户刷新聊天页后能看到人工客服回复。

这个功能不是重新做一套实时 IM。第一版只实现“工单详情中接管、查看会话、发送人工回复、更新状态、留痕”。

## 2. 当前项目基础

| 已有能力 | 可复用点 |
| --- | --- |
| `service_ticket` | 已有工单列表、状态、优先级、会话/订单关联 |
| `chat_message` | 已有会话消息表，可写入 `ASSISTANT` 消息 |
| `reply_draft` | 已有 AI 副驾驶草稿，可作为人工回复参考 |
| `after_sale_application` | 可通过 `ticket_id` 关联售后单，写处理日志 |
| `after_sale_risk_assessment` | 可在右侧展示单个售后风险 |
| `evidence_audit` | 可展示凭证审核结果 |
| `product_issue_alert` | 可提示商品集中问题 |
| `ServiceTicketView.vue` | 已有工单列表和基础详情，适合升级为工作台 |

## 3. 业务边界

第一版坚持以下边界：

- 人工回复只代表客服沟通，不自动退款、不自动驳回、不自动完成售后。
- AI 草稿只是参考，最终发送动作必须由管理员点击。
- 发送人工回复后写入 `chat_message`，用户侧可见。
- 如果工单关联售后单，写入 `after_sale_process_log`，便于答辩展示“可追溯”。
- 如果工单未关联售后单，也应能发送会话回复，保证普通投诉工单可用。

## 4. 数据模型变更

扩展 `chat_message.source_type` 允许：

```text
MANUAL
AI_DRAFT
SYSTEM_NOTICE
```

第一版主要使用 `MANUAL`。`AI_DRAFT` 和 `SYSTEM_NOTICE` 先作为兼容扩展，便于后续把草稿或系统通知直接写入会话。

新增请求对象：

- `ManualReplyRequest`

字段：

| 字段 | 说明 |
| --- | --- |
| `content` | 人工回复内容，必填 |
| `useDraftId` | 可选，表示从某条草稿采纳发送 |
| `resolveTicket` | 是否发送后标记工单已解决 |

新增响应对象可直接返回：

- `ChatMessage`：新写入的人工回复消息。

## 5. 后端接口设计

新增接口：

```http
GET  /service-tickets/{id}/conversation
POST /service-tickets/{id}/take-over
POST /service-tickets/{id}/manual-replies
```

第一版必须实现：

- `GET /service-tickets/{id}/conversation`
- `POST /service-tickets/{id}/manual-replies`
- `POST /service-tickets/{id}/take-over`

`resolveTicket=true` 只作用于 `POST /service-tickets/{id}/manual-replies` 的发送后状态更新；单独标记状态继续复用现有 `PUT /service-tickets/{id}`，避免第一版接口过多。

请求示例：

```json
{
  "content": "您好，已收到您反馈的耳机断连问题。请您补充一段故障视频，我们会优先安排换货检测。",
  "useDraftId": 12,
  "resolveTicket": false
}
```

写入逻辑：

1. 校验管理员权限。
2. 查询工单，确认 `session_id` 存在。
3. 查询当前会话最大 `seq_no`。
4. 插入 `chat_message`：
   - `role = ASSISTANT`
   - `message_type = TEXT`
   - `source_type = MANUAL`
   - `intent_code = ticket.intent_code`
5. 更新工单状态：
   - `PENDING` -> `PROCESSING`
   - 如果 `resolveTicket=true`，置为 `RESOLVED`
6. 如果 `useDraftId` 存在，复用现有草稿状态更新为 `USED`。
7. 如果工单有关联售后单，写入 `after_sale_process_log`，action 为 `MANUAL_REPLY_SENT`。
8. 接管会话只把工单转为 `PROCESSING` 并记录处理人，不自动改变售后单状态。

## 6. 后端文件范围

新增：

- `ManualReplyRequest.java`
- `ManualReplyService.java`
- `ManualReplyServiceImpl.java`

修改：

- `ServiceTicketController.java`
- `ChatMessageMapper.java`
- `ChatMessageMapper.xml`
- `ServiceTicketService` / `ServiceTicketServiceImpl` 如需要增加内部更新方法
- `AfterSaleApplicationServiceImpl` 支持 `MANUAL_REPLY_SENT` 日志
- `sql/schema.sql` 扩展 CHECK 约束

## 7. 前端页面设计

改造 `ServiceTicketView.vue` 为三栏工作台：

| 区域 | 内容 |
| --- | --- |
| 左侧 | 工单列表、状态、优先级、筛选 |
| 中间 | 原始会话记录、人工回复输入框、发送按钮 |
| 右侧 | 工单摘要、订单/售后信息、AI 草稿、风险和建议 |

第一版可在当前列表 + 详情布局上渐进改造，不强行重写整个页面。必须新增：

- 会话记录面板：显示用户消息、AI 回复、人工回复。
- 人工回复输入框：支持从 AI 草稿/建议动作填充。
- 发送人工回复按钮。
- 接管会话按钮：把工单状态置为 `PROCESSING`。
- 发送成功后刷新会话记录，能看到 `MANUAL` 标识。

客户侧不用新增页面；因为人工回复写入 `chat_message`，现有 `ChatWorkbenchView.vue` 查询消息时自然能看到。

## 8. 与六项功能闭环的关系

| 功能 | 在工作台中的呈现 |
| --- | --- |
| 产品智能顾问 | 可通过订单商品和商品洞察辅助客服解释 |
| 售后前置诊断 | 如果售后单有关联诊断，客服可看到建议方案 |
| AI 凭证真实性审核 | 客服可看到补证建议，人工回复时要求用户补材料 |
| 售后风险识别 | 高风险工单优先接管，回复前提醒不要直接承诺退款 |
| 商品质量预警 | 若商品近期集中出问题，客服回复可说明已升级运营检查 |
| 人工接管 | 最终发送人工回复并写入会话，完成闭环 |

## 9. 自审与修订

### 初版风险

1. 如果做完整实时聊天，会超出当前项目体量，也不符合第一版演示需求。
2. 如果只在工单里写备注，不写 `chat_message`，用户侧看不到人工回复，闭环不完整。
3. 如果发送人工回复顺手自动完成售后，会破坏已有售后状态机。
4. 如果页面同时堆太多卡片，会影响可用性；第一版应突出“会话 + 回复 + 右侧证据”。
5. 如果不更新 `source_type` CHECK 约束，接口会在数据库层失败。

### 修订结论

采用“工单接管 + 会话记录 + 人工回复写入 chat_message + 售后日志留痕”的实现路线。它足够真实，能明显回应老师对“客服智能对齐、人工确认、真实可用”的期待，同时不会把系统做成过重的实时 IM。

### 二次自审结论

实现前再次收敛范围：第一版不新增独立 `resolve` 接口，不做实时 WebSocket，也不把 AI 草稿自动发给用户。人工回复接口负责唯一的对客发送动作，并在需要时把草稿标记为 `USED`。这保证演示闭环清晰：AI 生成建议，客服接管确认，最终回复进入原会话，售后处理日志可追踪。

## 10. 验证标准

后端：

```powershell
mvn -q -DskipTests package
```

前端：

```powershell
npm run build
```

数据库：

```powershell
Get-Content -Path sql\schema.sql -Raw -Encoding UTF8 | mysql -uroot -p1234 --default-character-set=utf8mb4
```

接口冒烟：

- `GET /service-tickets/{id}/conversation`
- `POST /service-tickets/{id}/take-over`
- `POST /service-tickets/{id}/manual-replies`
- `GET /chat-sessions/{sessionId}/messages` 能看到 `sourceType=MANUAL`
- 关联售后单的 `processLogs` 包含 `MANUAL_REPLY_SENT`

浏览器：

- `npm run test:browser`
- 管理员在人工工单页能接管并发送人工回复。
- 发送后会话记录中出现人工回复。
