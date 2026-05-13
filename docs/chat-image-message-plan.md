# 客服聊天图片消息补强计划

## 1. 问题定位

当前“AI 凭证真实性审核”已经支持顾客在售后详情中上传图片凭证，但用户和客服聊天时仍只能发送文字。这样演示时会出现断层：用户说“我把坏产品照片发给客服”，实际必须先跳到售后详情补凭证，客服聊天窗口看不到图片。

本次补强目标是把图片入口前移到客服聊天过程：

1. 顾客在 `/chat` 发送消息时可以选择本地图片。
2. 图片上传后随同本轮聊天消息写入 `chat_message`。
3. 聊天气泡能直接预览图片。
4. 人工工单的“原始会话”也能看到图片，客服接管时不需要再让用户重复发送。
5. 现有售后详情图片凭证能力继续保留，聊天图片作为会话证据，不自动等同于售后凭证。

## 2. 实现边界与自审

第一版做真实上传和真实预览，不做复杂 IM：

- 不做实时 WebSocket，继续复用现有发送消息接口和 SSE 回复。
- 不做自动鉴定“这张图一定是 AI 生成”，只把图片作为后续凭证审核、人工复核和答辩演示的证据入口。
- 不把聊天图片自动写入 `after_sale_evidence`，避免用户只是咨询时系统擅自创建正式售后凭证。
- 上传格式和大小沿用图片凭证限制：jpg、jpeg、png、webp、gif，最大 5MB。
- 管理员也能在聊天页上传图片，方便测试台演示；顾客和管理员都必须登录。

自审结论：这个补强是必要的。之前只在售后详情上传图片，符合“补充凭证”，但不符合“和客服聊天的时候就发照片”。本次应改聊天消息模型，而不是继续堆售后详情 UI。

## 3. 后端改动

扩展 `chat_message`：

| 字段 | 说明 |
| --- | --- |
| `file_url` | 上传后的图片 URL |
| `original_filename` | 原始文件名 |
| `content_type` | 图片 MIME |
| `file_size` | 文件大小 |

接口：

```http
POST /chat-sessions/{id}/image-files
POST /chat-sessions/{id}/messages
POST /chat-sessions/{id}/message-stream
GET  /chat-sessions/{id}/messages
GET  /service-tickets/{id}/conversation
```

`POST /chat-sessions/{id}/image-files` 只负责上传并返回图片元数据。发送消息时，前端把 `fileUrl/originalFilename/contentType/fileSize` 放进 `ChatMessageRequest`，后端写入 `chat_message`。

如果用户只发图片不写文字，后端自动使用“用户上传了一张商品问题图片，请客服结合订单和图片判断。”作为消息内容，保证意图识别和日志仍有可读文本。

## 4. 前端改动

`ChatWorkbenchView.vue`：

- 在输入框操作区增加图片选择按钮。
- 上传成功后显示图片预览和文件名。
- 支持取消当前图片。
- 点击发送时把图片元数据随消息一起发送。
- 消息气泡中如果存在 `fileUrl`，显示图片预览。

`ServiceTicketView.vue`：

- 原始会话列表中如果消息存在 `fileUrl`，显示图片预览。

复用 `/uploads/**` 到 `/api/uploads/**` 的开发环境 URL 映射，确保 Vite 代理下图片能显示。

## 5. 验证标准

- 后端编译通过。
- 前端构建通过。
- 全链路冒烟测试覆盖：上传聊天图片、发送图片消息、查询会话消息、客服工单会话可读。
- 浏览器测试覆盖：聊天页选择图片、发送、消息气泡显示图片，工单页原始会话显示图片。
- `git status --short` 只包含本次任务相关文件和已有未提交的用户文档。
